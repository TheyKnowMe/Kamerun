package com.ME.kamerun.player

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
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
)

@Singleton
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private var mediaPlayer: MediaPlayer? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    fun play(song: SongEntity, queue: List<SongEntity> = emptyList()) {
        Log.d(TAG, "play() called: '${song.title}' by '${song.artist}'")
        Log.d(TAG, "  audioPath = ${song.audioPath}")

        val path = song.audioPath
        if (path == null) {
            Log.e(TAG, "  ERROR: audioPath is NULL! Song was not downloaded.")
            return
        }

        val file = File(path)
        Log.d(TAG, "  File exists = ${file.exists()}, size = ${if (file.exists()) file.length() else 0}")

        if (!file.exists()) {
            Log.e(TAG, "  ERROR: File does not exist at: $path")
            return
        }

        if (file.length() < 1000) {
            Log.e(TAG, "  ERROR: File too small (${file.length()} bytes), probably corrupted")
            return
        }

        val index = if (queue.isNotEmpty()) queue.indexOf(song).coerceAtLeast(0) else 0
        val actualQueue = queue.ifEmpty { listOf(song) }

        context.startForegroundService(Intent(context, MusicService::class.java))

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                Log.d(TAG, "  setDataSource OK")

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "  MediaPlayer ERROR: what=$what extra=$extra")
                    false
                }

                prepare()
                Log.d(TAG, "  prepare() OK, duration=${duration}ms")

                start()
                Log.d(TAG, "  start() OK, isPlaying=$isPlaying")

                setOnCompletionListener {
                    Log.d(TAG, "  Song completed, playing next")
                    next()
                }
            }

            _state.value = PlayerState(
                currentSong = song,
                isPlaying = true,
                duration = mediaPlayer?.duration?.toLong() ?: 0L,
                queue = actualQueue,
                queueIndex = index,
            )

            Log.d(TAG, "  PlayerState updated: isPlaying=true, duration=${_state.value.duration}ms")

        } catch (e: Exception) {
            Log.e(TAG, "  EXCEPTION in play(): ${e.message}", e)
        }
    }

    fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (mp.isPlaying) {
            mp.pause()
            _state.value = _state.value.copy(isPlaying = false)
            Log.d(TAG, "togglePlayPause: PAUSED")
        } else {
            mp.start()
            _state.value = _state.value.copy(isPlaying = true)
            Log.d(TAG, "togglePlayPause: RESUMED")
        }
    }

    fun next() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val nextIndex = (s.queueIndex + 1) % s.queue.size
        Log.d(TAG, "next(): index $nextIndex of ${s.queue.size}")
        play(s.queue[nextIndex], s.queue)
    }

    fun previous() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        val pos = mediaPlayer?.currentPosition ?: 0
        if (pos > 3000) {
            mediaPlayer?.seekTo(0)
            return
        }
        val prevIndex = if (s.queueIndex > 0) s.queueIndex - 1 else s.queue.size - 1
        play(s.queue[prevIndex], s.queue)
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
    }

    fun getCurrentPosition(): Long {
        return mediaPlayer?.currentPosition?.toLong() ?: 0L
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        stop()
        _state.value = PlayerState()
    }
}
