package com.ME.kamerun.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ME.kamerun.data.local.PlaylistDao
import com.ME.kamerun.data.local.entities.PlaylistEntity
import com.ME.kamerun.data.local.entities.PlaylistSongCrossRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getPlaylistById(id: Long): PlaylistEntity? {
        return playlistDao.getPlaylistById(id)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.insertPlaylist(
                PlaylistEntity(name = name)
            )
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    fun savAiPlaylist(name: String, vibe: String, songIds: List<Long>) {
        viewModelScope.launch {
            val playlistId = playlistDao.insertPlaylist(
                PlaylistEntity(name = name, vibe = vibe, isAiGenerated = true)
            )
            val crossRefs = songIds.mapIndexed { index, songId ->
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = songId,
                    position = index,
                )
            }
            playlistDao.insertPlaylistSongs(crossRefs)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<com.ME.kamerun.data.local.entities.SongEntity>> {
        return playlistDao.getSongsForPlaylist(playlistId)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        }
    }
}