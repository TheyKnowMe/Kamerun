package com.ME.kamerun.ui.screens.import_songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ME.kamerun.data.remote.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val currentSong: String = "",
    val error: String? = null,
    val successCount: Int? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState

    fun importPlaylist(url: String) {
        viewModelScope.launch {
            _uiState.value = ImportUiState(isLoading = true)

            val result = youTubeRepository.importPlaylist(
                playlistUrl = url,
                onProgress = { current, total, songTitle ->
                    _uiState.value = _uiState.value.copy(
                        progress = current,
                        total = total,
                        currentSong = songTitle,
                    )
                },
            )

            result.fold(
                onSuccess = { count ->
                    _uiState.value = ImportUiState(successCount = count)
                },
                onFailure = { error ->
                    _uiState.value = ImportUiState(error = error.message ?: "Import fehlgeschlagen")
                },
            )
        }
    }

    fun resetState() {
        _uiState.value = ImportUiState()
    }
}