package com.anongram.app.features.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.anongram.app.MainActivity
import com.anongram.app.R
import com.anongram.app.features.chats.media.player.ChatAudioController

/**
 * Foreground Service для воспроизведения аудио (ТЗ, шаг 8):
 * MediaSessionCompat / Foreground-уведомление с управлением (Фото 1).
 */
class AudioPlayerService : Service() {

    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "AnonGramMediaSession").apply {
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val chatController = ChatAudioController.getInstance(this)
        when (intent?.action) {
            ACTION_TOGGLE -> chatController.toggle()
            ACTION_NEXT -> chatController.next()
            ACTION_PREV -> chatController.prev()
            ACTION_STOP -> {
                chatController.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val chatController = ChatAudioController.getInstance(this)
        val state = chatController.state.value
        val track = state.currentTrack
        val title = track?.title ?: "AnonGram Audio"
        val artist = track?.artist?.ifBlank { "AnonGram Player" } ?: "AnonGram Player"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun actionIntent(action: String, requestCode: Int): PendingIntent {
            val intent = Intent(this, AudioPlayerService::class.java).apply { this.action = action }
            return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val prevPending = actionIntent(ACTION_PREV, 1)
        val togglePending = actionIntent(ACTION_TOGGLE, 2)
        val nextPending = actionIntent(ACTION_NEXT, 3)

        val playPauseIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_stat_anongram)
            .setContentIntent(openAppIntent)
            .setOngoing(state.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Предыдущий", prevPending)
            .addAction(playPauseIcon, if (state.isPlaying) "Пауза" else "Слушать", togglePending)
            .addAction(android.R.drawable.ic_media_next, "Следующий", nextPending)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        mediaSession = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Аудиоплеер",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Воспроизведение музыки в фоне"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "anongram_audio_player"

        const val ACTION_TOGGLE = "com.anongram.app.player.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.anongram.app.player.ACTION_NEXT"
        const val ACTION_PREV = "com.anongram.app.player.ACTION_PREV"
        const val ACTION_STOP = "com.anongram.app.player.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, AudioPlayerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
