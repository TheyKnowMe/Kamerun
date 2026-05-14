package com.ME.kamerun.ui.screens.import_songs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.ME.kamerun.worker.DownloadWorker
import com.ME.kamerun.worker.KEY_ERROR
import com.ME.kamerun.worker.KEY_SUCCESS_COUNT
import com.ME.kamerun.worker.KEY_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val isLoading: Boolean = false,
    val workState: WorkInfo.State? = null,
    val progress: Int = 0,
    val total: Int = 0,
    val currentSong: String = "",
    val error: String? = null,
    val successCount: Int? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState

    fun importPlaylist(url: String) {
        if (url.isBlank()) return

        // WorkManager Request aufbauen
        val inputData = workDataOf(KEY_URL to url)

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("download")
            .build()

        workManager.enqueueUniqueWork(
            "playlist_download",
            ExistingWorkPolicy.REPLACE, // Neuer Import ersetzt alten
            request,
        )

        // Worker-Status beobachten
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("download")
                .collect { workInfoList ->
                    val info = workInfoList.firstOrNull() ?: return@collect

                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            // Progress aus Worker lesen
                            val current = info.progress.getInt("current", 0)
                            val total = info.progress.getInt("total", 0)
                            val song = info.progress.getString("song") ?: ""
                            _uiState.value = ImportUiState(
                                isLoading = true,
                                workState = WorkInfo.State.RUNNING,
                                progress = current,
                                total = total,
                                currentSong = song,
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val count = info.outputData.getInt(KEY_SUCCESS_COUNT, 0)
                            _uiState.value = ImportUiState(
                                workState = WorkInfo.State.SUCCEEDED,
                                successCount = count,
                            )
                        }
                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString(KEY_ERROR) ?: "Fehler"
                            _uiState.value = ImportUiState(
                                workState = WorkInfo.State.FAILED,
                                error = error,
                            )
                        }
                        WorkInfo.State.ENQUEUED -> {
                            _uiState.value = ImportUiState(
                                isLoading = true,
                                workState = WorkInfo.State.ENQUEUED,
                                currentSong = "Wird vorbereitet...",
                            )
                        }
                        else -> {}
                    }
                }
        }
    }

    fun resetState() {
        _uiState.value = ImportUiState()
    }
}
