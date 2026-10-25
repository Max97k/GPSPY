package com.gpsspy.gpstracker.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WearLocationService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_STANDALONE"
        const val ACTION_STOP = "ACTION_STOP_STANDALONE"
        private const val CHANNEL_ID = "WearLocationServiceChannel"
        private const val NOTIFICATION_ID = 2
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dataClient: DataClient
    private var locationCallback: LocationCallback? = null
    private var isRecording = false

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dataClient = Wearable.getDataClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRecording) {
                    if (startForegroundService()) {
                        startStandaloneTracking()
                    }
                }
            }
            ACTION_STOP -> stopStandaloneTracking()
        }
        return START_STICKY
    }

    private fun startForegroundService(): Boolean {
        val fineLocationGranted = checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted) {
            Log.e("WearLocationService", "Cannot start foreground service: FINE_LOCATION not granted")
            stopSelf()
            return false
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPSPY Standalone")
            .setContentText("Recording location autonomously...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        return try {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            true
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.e("WearLocationService", "Foreground service start not allowed", e)
            stopSelf()
            false
        } catch (e: SecurityException) {
            Log.e("WearLocationService", "Security exception starting foreground service", e)
            stopSelf()
            false
        }
    }

    private fun startStandaloneTracking() {
        isRecording = true
        syncTrackingStatus(true)

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2500L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    for (location in result.locations) {
                        syncLocationUpdate(location.latitude, location.longitude, location.altitude)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                mainLooper
            )
        } catch (e: SecurityException) {
            Log.e("WearLocationService", "Location permission missing", e)
            stopStandaloneTracking()
        }
    }

    private fun syncLocationUpdate(latitude: Double, longitude: Double, altitude: Double) {
        serviceScope.launch {
            try {
                val dataMapRequest = PutDataMapRequest.create("/location-update").apply {
                    dataMap.putDouble("latitude", latitude)
                    dataMap.putDouble("longitude", longitude)
                    dataMap.putDouble("altitude", altitude)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val request = dataMapRequest.asPutDataRequest().setUrgent()
                dataClient.putDataItem(request)
            } catch (e: Exception) {
                Log.w("WearLocationService", "Failed to sync location update", e)
            }
        }
    }

    private fun syncTrackingStatus(tracking: Boolean) {
        serviceScope.launch {
            try {
                val dataMapRequest = PutDataMapRequest.create("/tracking-status").apply {
                    dataMap.putBoolean("isTracking", tracking)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val request = dataMapRequest.asPutDataRequest().setUrgent()
                dataClient.putDataItem(request)
            } catch (e: Exception) {
                Log.w("WearLocationService", "Failed to sync tracking status", e)
            }
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        Log.w("WearLocationService", "Foreground service timed out, stopping")
        stopStandaloneTracking()
    }

    private fun stopStandaloneTracking() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            locationCallback = null
        }

        isRecording = false
        syncTrackingStatus(false)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStandaloneTracking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Wear Location Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
