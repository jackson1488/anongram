package com.anongram.app.features.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.anongram.app.MainActivity
import com.anongram.app.R
import com.anongram.app.features.chats.media.player.AudioMetadataHelper
import com.anongram.app.features.chats.media.player.ChatAudioController

/**
 * Foreground Service для воспроизведения аудио и голосовых сообщений:
 * Нативное уведомление NotificationCompat.MediaStyle и синхронизация MediaSessionCompat.
 */
class AudioPlayerService : Service() {

    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "AnonGramMediaSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    ChatAudioController.getInstance(this@AudioPlayerService).togglePlayPause()
                    updateNotification()
                }

                override fun onPause() {
                    ChatAudioController.getInstance(this@AudioPlayerService).togglePlayPause()
                    updateNotification()
                }

                override fun onSkipToNext() {
                    ChatAudioController.getInstance(this@AudioPlayerService).next()
                    updateNotification()
                }

                override fun onSkipToPrevious() {
                    ChatAudioController.getInstance(this@AudioPlayerService).prev()
                    updateNotification()
                }

                override fun onSeekTo(pos: Long) {
                    ChatAudioController.getInstance(this@AudioPlayerService).seek(pos)
                    updateNotification()
                }

                override fun onStop() {
                    ChatAudioController.getInstance(this@AudioPlayerService).stop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
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

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val chatController = ChatAudioController.getInstance(this)
        val state = chatController.state.value
        val track = state.currentTrack
        val isVoice = track?.isVoice == true

        val title = if (isVoice) {
            track?.chatTitle?.ifBlank { "Голосовое сообщение" } ?: "Голосовое сообщение"
        } else {
            track?.title ?: "AnonGram Music"
        }

        val artist = if (isVoice) {
            "Голосовое сообщение • ${track?.durationLabel ?: "0:00"}"
        } else {
            track?.artist?.ifBlank { "AnonGram Player" } ?: "AnonGram Player"
        }

        val largeIcon: Bitmap? = if (isVoice) {
            AudioMetadataHelper.generateFallbackArt(title, "AnonGram Voice")
        } else {
            track?.albumArtBitmap
                ?: track?.let { AudioMetadataHelper.getCachedArt(it.uri.toString()) }
                ?: AudioMetadataHelper.generateFallbackArt(title, artist)
        }

        // Обновление состояния MediaSession для системного скраббера Android 13/14+
        val playState = if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = if (isVoice) {
            PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP
        } else {
            PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_STOP
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(playState, state.positionMs, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, state.durationMs.coerceAtLeast(1L))
        if (largeIcon != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeIcon)
        }
        mediaSession?.setMetadata(metadataBuilder.build())

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

        val playPauseIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val togglePending = actionIntent(ACTION_TOGGLE, 2)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_stat_anongram)
            .setContentIntent(openAppIntent)
            .setOngoing(state.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        if (isVoice) {
            // Для голосовых: только кнопка Play/Pause, без стрелок переключения
            builder.addAction(playPauseIcon, if (state.isPlaying) "Пауза" else "Слушать", togglePending)
            builder.setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
        } else {
            // Для музыки: Предыдущий, Play/Pause, Следующий
            val prevPending = actionIntent(ACTION_PREV, 1)
            val nextPending = actionIntent(ACTION_NEXT, 3)
            builder.addAction(android.R.drawable.ic_media_previous, "Предыдущий", prevPending)
            builder.addAction(playPauseIcon, if (state.isPlaying) "Пауза" else "Слушать", togglePending)
            builder.addAction(android.R.drawable.ic_media_next, "Следующий", nextPending)
            builder.setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

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
                description = "Воспроизведение музыки и голосовых сообщений"
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
