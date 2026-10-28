package com.anongram.app.features.attach.ui.tabs

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationSharingService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var locationManager: LocationManager? = null
    private var timerJob: Job? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val durationMin = intent?.getIntExtra(EXTRA_DURATION_MIN, 15) ?: 15
        startForeground(NOTIFICATION_ID, buildNotification(durationMin))

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10_000L,
                10f,
                locationListener
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                10_000L,
                10f,
                locationListener
            )
        } catch (_: Exception) {}

        timerJob?.cancel()
        timerJob = scope.launch {
            val totalSeconds = durationMin * 60
            for (sec in 0 until totalSeconds) {
                delay(1000L)
                if (!isActive) break
            }
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (_: Exception) {}
    }

    private fun buildNotification(durationMin: Int): Notification {
        val stopIntent = Intent(this, LocationSharingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Трансляция геопозиции")
            .setContentText("AnonGram передаёт ваше местоположение ($durationMin мин.)")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AnonGram Геопозиция",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о трансляции геопозиции в реальном времени"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "anongram_location_channel"
        private const val NOTIFICATION_ID = 2024
        const val ACTION_STOP = "com.anongram.app.action.STOP_LOCATION_SHARING"
        const val EXTRA_DURATION_MIN = "extra_duration_min"

        fun start(context: Context, durationMin: Int) {
            val intent = Intent(context, LocationSharingService::class.java).apply {
                putExtra(EXTRA_DURATION_MIN, durationMin)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationSharingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
