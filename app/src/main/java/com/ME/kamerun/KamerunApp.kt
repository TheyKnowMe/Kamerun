package com.ME.kamerun

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "KamerunApp"

@HiltAndroidApp
class KamerunApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // WorkManager 2.9+ nutzt val statt override fun
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d(TAG, "yt-dlp initialized, version: ${YoutubeDL.getInstance().version(this)}")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val status = YoutubeDL.getInstance().updateYoutubeDL(
                        this@KamerunApp,
                        YoutubeDL.UpdateChannel._STABLE
                    )
                    Log.d(TAG, "yt-dlp update result: $status")
                } catch (e: Exception) {
                    Log.e(TAG, "yt-dlp update failed: ${e.message}")
                    try {
                        YoutubeDL.getInstance().updateYoutubeDL(
                            this@KamerunApp,
                            YoutubeDL.UpdateChannel._NIGHTLY
                        )
                    } catch (e2: Exception) {
                        Log.e(TAG, "yt-dlp nightly also failed: ${e2.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
        }
    }
}
