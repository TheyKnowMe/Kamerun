package com.ME.kamerun.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ME.kamerun.data.remote.YouTubeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "DownloadWorker"
const val CHANNEL_ID = "kamerun_download"
const val NOTIFICATION_ID = 42
const val KEY_URL = "playlist_url"
const val KEY_SUCCESS_COUNT = "success_count"
const val KEY_ERROR = "error_message"

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val youTubeRepository: YouTubeRepository,
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Keine URL angegeben"))

        createNotificationChannel()
        setForeground(createForegroundInfo("Playlist wird analysiert..."))

        return try {
            val result = youTubeRepository.importPlaylist(
                playlistUrl = url,
                onProgress = { current: Int, total: Int, songTitle: String ->
                    Log.d(TAG, "Progress: $current/$total – $songTitle")
                    updateNotification(songTitle, current, total)
                },
            )

            result.fold(
                onSuccess = { count: Int ->
                    showFinishedNotification(count)
                    Result.success(workDataOf(KEY_SUCCESS_COUNT to count))
                },
                onFailure = { error: Throwable ->
                    val msg = error.message ?: "Unbekannter Fehler"
                    showErrorNotification(msg)
                    Result.failure(workDataOf(KEY_ERROR to msg))
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Worker exception", e)
            val msg = e.message ?: "Unbekannter Fehler"
            showErrorNotification(msg)
            Result.failure(workDataOf(KEY_ERROR to msg))
        }
    }

    private fun updateNotification(currentSong: String, progress: Int, total: Int) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("⬇ Kamerun Download")
            .setContentText(currentSong)
            .setProgress(total, progress, total == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // ForegroundInfo MIT ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    private fun createForegroundInfo(text: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("⬇ Kamerun Download")
            .setContentText(text)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSilent(true)
            .build()

        // Ab Android 10 (API 29) muss der ForegroundServiceType angegeben werden
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showFinishedNotification(count: Int) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✓ Download abgeschlossen")
            .setContentText("$count Songs erfolgreich importiert")
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("✗ Download fehlgeschlagen")
            .setContentText(error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Kamerun Downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Zeigt den Fortschritt von YouTube Playlist Downloads"
        }
        notificationManager.createNotificationChannel(channel)
    }
}
