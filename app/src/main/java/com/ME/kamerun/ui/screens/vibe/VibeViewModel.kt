package com.ME.kamerun.ui.screens.vibe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ME.kamerun.data.local.PlaylistDao
import com.ME.kamerun.data.local.SongDao
import com.ME.kamerun.data.local.entities.PlaylistEntity
import com.ME.kamerun.data.local.entities.PlaylistSongCrossRef
import com.ME.kamerun.data.local.entities.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

data class VibeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val resultSongs: List<SongEntity>? = null,
)

@HiltViewModel
class VibeViewModel @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
) : ViewModel() {

    val allSongs: StateFlow<List<SongEntity>> = songDao.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(VibeUiState())
    val uiState: StateFlow<VibeUiState> = _uiState

    fun generatePlaylist(vibe: String, songs: List<SongEntity>, host: String, port: Int) {
        viewModelScope.launch {
            _uiState.value = VibeUiState(isLoading = true)
            try {
                val resultIds = sendToServer(vibe, songs, host, port)
                val resultSongs = songDao.getSongsByIds(resultIds)
                _uiState.value = VibeUiState(resultSongs = resultSongs)
            } catch (e: Exception) {
                _uiState.value = VibeUiState(error = e.message ?: "Verbindung fehlgeschlagen")
            }
        }
    }

    fun saveGeneratedPlaylist(name: String, vibe: String, songs: List<SongEntity>) {
        viewModelScope.launch {
            val playlistId = playlistDao.insertPlaylist(
                PlaylistEntity(name = name, vibe = vibe, isAiGenerated = true)
            )
            val crossRefs = songs.mapIndexed { index, song ->
                PlaylistSongCrossRef(playlistId = playlistId, songId = song.id, position = index)
            }
            playlistDao.insertPlaylistSongs(crossRefs)
            _uiState.value = VibeUiState() // Reset
        }
    }

    fun resetState() {
        _uiState.value = VibeUiState()
    }

    private suspend fun sendToServer(
        vibe: String,
        songs: List<SongEntity>,
        host: String,
        port: Int,
    ): List<Long> = withContext(Dispatchers.IO) {
        Socket(host, port).use { socket ->
            socket.soTimeout = 120_000 // 2 Minuten Timeout

            val output = socket.getOutputStream()
            val input = socket.getInputStream()

            // Request bauen
            val songsArray = JSONArray()
            for (song in songs) {
                songsArray.put(JSONObject().apply {
                    put("id", song.id)
                    put("artist", song.artist)
                    put("title", song.title)
                })
            }
            val request = JSONObject().apply {
                put("vibe", vibe)
                put("songs", songsArray)
            }

            // Length-prefixed senden
            val bytes = request.toString().toByteArray(Charsets.UTF_8)
            val header = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(bytes.size)
                .array()
            output.write(header)
            output.write(bytes)
            output.flush()

            // Response empfangen
            val respHeader = ByteArray(4)
            input.read(respHeader)
            val respLength = ByteBuffer.wrap(respHeader)
                .order(ByteOrder.BIG_ENDIAN).int
            val respBytes = ByteArray(respLength)
            var read = 0
            while (read < respLength) {
                read += input.read(respBytes, read, respLength - read)
            }

            val response = JSONObject(String(respBytes, Charsets.UTF_8))

            if (response.has("error")) {
                throw Exception(response.getString("error"))
            }

            val playlist = response.getJSONArray("playlist")
            (0 until playlist.length()).map {
                playlist.getJSONObject(it).getLong("id")
            }
        }
    }
}