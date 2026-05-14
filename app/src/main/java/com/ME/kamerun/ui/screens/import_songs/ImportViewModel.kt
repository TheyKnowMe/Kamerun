package com.ME.kamerun.ui.screens.import_songs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ME.kamerun.worker.DownloadWorker
import com.ME.kamerun.worker.KEY_ERROR
import com.ME.kamerun.worker.KEY_SUCCESS_COUNT
import com.ME.kamerun.worker.KEY_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState

    fun importPlaylist(url: String) {
        if (url.isBlank()) return

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(KEY_URL to url))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("kamerun_download")
            .build()

        workManager.enqueueUniqueWork(
            "playlist_download",
            ExistingWorkPolicy.REPLACE,
            request,
        )

        _uiState.value = ImportUiState(isLoading = true, currentSong = "Wird vorbereitet...")

        // Worker-Status beobachten
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("kamerun_download")
                .collect { workInfoList: List<WorkInfo> ->
                    val info = workInfoList.firstOrNull() ?: return@collect

                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            val current = info.progress.getInt("current", 0)
                            val total = info.progress.getInt("total", 0)
                            val song = info.progress.getString("song") ?: ""
                            _uiState.value = ImportUiState(
                                isLoading = true,
                                progress = current,
                                total = total,
                                currentSong = song,
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val count = info.outputData.getInt(KEY_SUCCESS_COUNT, 0)
                            _uiState.value = ImportUiState(successCount = count)
                        }
                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString(KEY_ERROR) ?: "Fehler"
                            _uiState.value = ImportUiState(error = error)
                        }
                        WorkInfo.State.ENQUEUED -> {
                            _uiState.value = ImportUiState(
                                isLoading = true,
                                currentSong = "Wird vorbereitet...",
                            )
                        }
                        else -> { /* BLOCKED, CANCELLED – nichts tun */ }
                    }
                }
        }
    }

    fun resetState() {
        _uiState.value = ImportUiState()
    }
}
