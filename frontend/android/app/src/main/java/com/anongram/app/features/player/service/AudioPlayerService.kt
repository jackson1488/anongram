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
import androidx.core.app.NotificationCompat
import com.anongram.app.MainActivity
import com.anongram.app.R
import com.anongram.app.features.player.logic.AudioPlayerController

/**
 * Foreground Service для воспроизведения аудио (ТЗ, шаг 8):
 * MediaSessionService / Foreground-уведомление с управлением.
 * Пауза при отключении наушников, аудиофокус.
 */
class AudioPlayerService : Service() {

    private lateinit var controller: AudioPlayerController

    override fun onCreate() {
        super.onCreate()
        controller = AudioPlayerController.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> controller.toggle()
            ACTION_NEXT -> controller.next()
            ACTION_PREV -> controller.prev()
            ACTION_STOP -> {
                controller.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val currentTrack = controller.state.value.currentTrack
        val title = currentTrack?.title ?: "AnonGram Audio"
        val artist = currentTrack?.artist ?: "AnonGram Player"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_stat_anongram)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
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
