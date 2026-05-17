package com.ME.kamerun.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.ME.kamerun.MainActivity
import com.ME.kamerun.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : Service() {

    @Inject lateinit var musicPlayer: MusicPlayer

    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isForeground = false

    companion object {
        const val ACTION_PLAY_PAUSE = "com.ME.kamerun.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.ME.kamerun.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.ME.kamerun.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.ME.kamerun.ACTION_STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "kamerun_music"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        setupMediaSession()
        observePlayerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> musicPlayer.togglePlayPause()
            ACTION_NEXT -> musicPlayer.next()
            ACTION_PREVIOUS -> musicPlayer.previous()
            ACTION_STOP -> {
                musicPlayer.release()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Music playback controls"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "KamerunMediaSession")
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { musicPlayer.togglePlayPause() }
            override fun onPause() { musicPlayer.togglePlayPause() }
            override fun onSkipToNext() { musicPlayer.next() }
            override fun onSkipToPrevious() { musicPlayer.previous() }
            override fun onStop() { musicPlayer.release(); stopSelf() }
            override fun onSeekTo(pos: Long) { musicPlayer.seekTo(pos) }
        })
        mediaSession.isActive = true
    }

    private fun observePlayerState() {
        serviceScope.launch {
            musicPlayer.state.collect { state ->
                if (state.currentSong == null) {
                    if (isForeground) {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                        isForeground = false
                    }
                    stopSelf()
                    return@collect
                }
                updateMediaSession(state)
                val notification = buildNotification(state)
                if (!isForeground) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    isForeground = true
                } else {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun updateMediaSession(state: PlayerState) {
        val song = state.currentSong ?: return
        mediaSession.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, state.duration)
                .build()
        )
        val pbState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP
            )
            .setState(
                if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                state.currentPosition,
                1.0f
            )
            .build()
        mediaSession.setPlaybackState(pbState)
    }

    private fun buildNotification(state: PlayerState): Notification {
        val song = state.currentSong!!

        val mainIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = pendingServiceIntent(ACTION_STOP, 4)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentIntent(mainIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_skip_previous),
                    "Previous",
                    pendingServiceIntent(ACTION_PREVIOUS, 1)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(
                        this,
                        if (state.isPlaying) R.drawable.ic_notification_pause
                        else R.drawable.ic_notification_play
                    ),
                    if (state.isPlaying) "Pause" else "Play",
                    pendingServiceIntent(ACTION_PLAY_PAUSE, 2)
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_skip_next),
                    "Next",
                    pendingServiceIntent(ACTION_NEXT, 3)
                ).build()
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun pendingServiceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, MusicService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
