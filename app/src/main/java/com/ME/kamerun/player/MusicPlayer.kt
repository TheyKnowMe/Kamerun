package com.ME.kamerun.player

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.util.Log
import com.ME.kamerun.data.local.entities.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MusicPlayer"

data class PlayerState(
    val currentSong: SongEntity? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val queue: List<SongEntity> = emptyList(),
    val queueIndex: Int = 0,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val isEqEnabled: Boolean = false,
    // EQ Bänder: 5 Bänder, Werte in Millibel (-1500 bis +1500)
    val eqBands: List<Int> = List(5) { 0 },
)

@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    fun play(song: SongEntity, queue: List<SongEntity> = emptyList()) {
        Log.d(TAG, "play() called: '${song.title}'")

        val path = song.audioPath ?: run {
            Log.e(TAG, "audioPath is NULL"); return
        }
        val file = File(path)
        if (!file.exists() || file.length() < 1000) {
            Log.e(TAG, "File missing: $path"); return
        }

        val currentState = _state.value
        val actualQueue = queue.ifEmpty { listOf(song) }
        val index = if (queue.isNotEmpty()) queue.indexOf(song).coerceAtLeast(0) else 0

        releaseEqualizer()
        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { onSongCompleted() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra"); false
                }
            }

            _state.value = currentState.copy(
                currentSong = song,
                isPlaying = true,
                duration = mediaPlayer?.duration?.toLong() ?: 0L,
                queue = actualQueue,
                queueIndex = index,
            )

            // EQ an neue Session hängen
            setupEqualizer(currentState.isEqEnabled, currentState.eqBands)

        } catch (e: Exception) {
            Log.e(TAG, "Exception in play(): ${e.message}", e)
        }
    }

    private fun onSongCompleted() {
        val s = _state.value
        when {
            s.isRepeat -> {
                // Gleichen Song nochmal spielen
                s.currentSong?.let { play(it, s.queue) }
            }
            s.isShuffle -> {
                // Zufälligen Song aus Queue
                if (s.queue.size > 1) {
                    val indices = s.queue.indices.filter { it != s.queueIndex }
                    val randomIndex = indices.random()
                    play(s.queue[randomIndex], s.queue)
                } else {
                    play(s.queue[0], s.queue)
                }
            }
            else -> next()
        }
    }

    fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        if (s.isShuffle && s.queue.size > 1) {
            val indices = s.queue.indices.filter { it != s.queueIndex }
            play(s.queue[indices.random()], s.queue)
        } else {
            play(s.queue[(s.queueIndex + 1) % s.queue.size], s.queue)
        }
    }

    fun previous() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val pos = mediaPlayer?.currentPosition ?: 0
        if (pos > 3000) {
            mediaPlayer?.seekTo(0)
            _state.value = s.copy(currentPosition = 0L)
            return
        }
        val prevIndex = if (s.queueIndex > 0) s.queueIndex - 1 else s.queue.size - 1
        play(s.queue[prevIndex], s.queue)
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            mp.start()
            _state.value = _state.value.copy(isPlaying = true)
        }
    }

    fun toggleShuffle() {
        _state.value = _state.value.copy(isShuffle = !_state.value.isShuffle)
    }

    fun toggleRepeat() {
        _state.value = _state.value.copy(isRepeat = !_state.value.isRepeat)
    }

    fun toggleEq() {
        val enabled = !_state.value.isEqEnabled
        equalizer?.enabled = enabled
        _state.value = _state.value.copy(isEqEnabled = enabled)
    }

    fun setEqBand(band: Int, value: Int) {
        // value: -100..100 → in Millibel: -1500..1500
        val mb = (value * 15).toShort()
        try {
            equalizer?.setBandLevel(band.toShort(), mb)
        } catch (_: Exception) {}
        val bands = _state.value.eqBands.toMutableList()
        bands[band] = value
        _state.value = _state.value.copy(eqBands = bands)
    }

    private fun setupEqualizer(enabled: Boolean, bands: List<Int>) {
        try {
            val sessionId = mediaPlayer?.audioSessionId ?: return
            equalizer = Equalizer(0, sessionId).apply {
                this.enabled = enabled
                bands.forEachIndexed { i, v ->
                    if (i < numberOfBands) {
                        setBandLevel(i.toShort(), (v * 15).toShort())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer setup failed: ${e.message}")
        }
    }

    private fun releaseEqualizer() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _state.value = _state.value.copy(currentPosition = positionMs)
    }

    fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        releaseEqualizer()
        stop()
        _state.value = PlayerState()
    }
}
