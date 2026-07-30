package com.zestyy.struct.location

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.zestyy.struct.MainActivity
import com.zestyy.struct.R
import com.zestyy.struct.util.GeoMath

/**
 * Foreground service that records GPS fixes into [TrackingManager] while the app is
 * backgrounded. Uses plain android.location.LocationManager (GPS + network providers) —
 * no Google Play Services dependency, so it works on de-Googled devices too, fully offline.
 */
class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.zestyy.struct.action.START"
        const val ACTION_PAUSE = "com.zestyy.struct.action.PAUSE"
        const val ACTION_RESUME = "com.zestyy.struct.action.RESUME"
        const val ACTION_STOP = "com.zestyy.struct.action.STOP"
        const val ACTION_LAP = "com.zestyy.struct.action.LAP"

        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 1001
        private const val MIN_UPDATE_INTERVAL_MS = 2000L
        private const val MIN_UPDATE_DISTANCE_M = 3f

        fun start(context: Context) =
            context.startForegroundService(Intent(context, LocationTrackingService::class.java).setAction(ACTION_START))
        fun pause(context: Context) =
            context.startService(Intent(context, LocationTrackingService::class.java).setAction(ACTION_PAUSE))
        fun resume(context: Context) =
            context.startService(Intent(context, LocationTrackingService::class.java).setAction(ACTION_RESUME))
        fun stop(context: Context) =
            context.startService(Intent(context, LocationTrackingService::class.java).setAction(ACTION_STOP))
        fun lap(context: Context) =
            context.startService(Intent(context, LocationTrackingService::class.java).setAction(ACTION_LAP))
    }

    private lateinit var locationManager: LocationManager
    private var clockTicker: CountDownTimer? = null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = handleLocation(location)
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification("Recording started"))
                TrackingManager.start()
                requestLocationUpdates()
                startClockTicker()
            }
            ACTION_PAUSE -> {
                TrackingManager.pause()
                updateNotification("Paused")
            }
            ACTION_RESUME -> {
                TrackingManager.resume()
                updateNotification("Recording")
            }
            ACTION_LAP -> TrackingManager.addLapMarker()
            ACTION_STOP -> {
                TrackingManager.stop()
                locationManager.removeUpdates(listener)
                clockTicker?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return

        locationManager.requestLocationUpdates(provider, MIN_UPDATE_INTERVAL_MS, MIN_UPDATE_DISTANCE_M, listener)
    }

    private fun handleLocation(location: Location) {
        val point = GeoMath.Point(
            lat = location.latitude,
            lng = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else null,
            timestampMillis = location.time,
            speedMetersPerSec = if (location.hasSpeed()) location.speed else null
        )
        TrackingManager.onNewPoint(point)
        val snapshot = TrackingManager.state.value
        updateNotification(
            "${"%.2f".format(snapshot.distanceMeters / 1000)} km · " +
                formatDuration(snapshot.elapsedMillis)
        )
    }

    private fun startClockTicker() {
        clockTicker?.cancel()
        clockTicker = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) { TrackingManager.tickClock() }
            override fun onFinish() {}
        }.start()
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Activity tracking", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows live stats while recording a GPS activity" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("maarga")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(listener)
        clockTicker?.cancel()
    }
}
