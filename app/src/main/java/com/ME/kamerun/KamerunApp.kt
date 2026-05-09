package com.ME.kamerun

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "KamerunApp"

@HiltAndroidApp
class KamerunApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d(TAG, "yt-dlp initialized, version: ${YoutubeDL.getInstance().version(this)}")

            // Update yt-dlp im Hintergrund
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val status = YoutubeDL.getInstance().updateYoutubeDL(
                        this@KamerunApp,
                        YoutubeDL.UpdateChannel._STABLE
                    )
                    Log.d(TAG, "yt-dlp update result: $status")
                    Log.d(TAG, "yt-dlp new version: ${YoutubeDL.getInstance().version(this@KamerunApp)}")
                } catch (e: Exception) {
                    Log.e(TAG, "yt-dlp stable update failed: ${e.message}")
                    try {
                        val status = YoutubeDL.getInstance().updateYoutubeDL(
                            this@KamerunApp,
                            YoutubeDL.UpdateChannel._NIGHTLY
                        )
                        Log.d(TAG, "yt-dlp nightly update result: $status")
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
