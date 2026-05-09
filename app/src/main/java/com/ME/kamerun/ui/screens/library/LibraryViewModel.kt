package com.ME.kamerun.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ME.kamerun.data.local.SongDao
import com.ME.kamerun.data.local.entities.SongEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songDao: SongDao,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val songs: StateFlow<List<SongEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) songDao.getAllSongs()
            else songDao.searchSongs(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch {
            songDao.deleteSong(song)
        }
    }
}