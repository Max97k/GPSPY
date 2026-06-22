package com.gpsspy.gpstracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gpsspy.gpstracker.MainActivity
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.data.database.LocationDao
import com.gpsspy.gpstracker.data.database.LocationPoint
import com.gpsspy.gpstracker.data.preferences.SettingsRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var locationDao: LocationDao

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var gnssStatusManager: GnssStatusManager

    @Inject
    lateinit var routineAnalysisManager: RoutineAnalysisManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var wakeLock: PowerManager.WakeLock

    private var currentSessionId: Long = 0L
    private var serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var locationCallback: LocationCallback? = null

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        private const val CHANNEL_ID = "LocationServiceChannel"
        private const val NOTIFICATION_ID = 1

        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

        private val _currentLocation = MutableStateFlow<android.location.Location?>(null)
        val currentLocation: StateFlow<android.location.Location?> = _currentLocation.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup WakeLock to prevent dozing during tracking
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsTracker::TrackingWakeLock")

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_START) {
            if (!wakeLock.isHeld) {
                // Acquire indefinitely for long tracking sessions
                wakeLock.acquire()
            }
            startForegroundService()
            startTracking()
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notif_title))
            .setContentText(getString(R.string.service_notif_desc))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.service_notif_stop), stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTracking() {
        if (_isTracking.value) return

        _isTracking.value = true
        gnssStatusManager.startListening()
        routineAnalysisManager.startListening()
        serviceScope = CoroutineScope(Dispatchers.IO + Job())

        serviceScope.launch {
            // Determine a new session ID based on current max
            val latestSession = locationDao.getLatestSessionId() ?: 0L
            currentSessionId = latestSession + 1

            // Observe settings to dynamically update location request interval
            settingsRepository.trackingFrequencyFlow.distinctUntilChanged().collect { frequencyMs ->
                updateLocationRequest(frequencyMs)
            }
        }
    }

    private fun updateLocationRequest(frequencyMs: Long) {
        try {
            if (locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback!!)
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, frequencyMs)
                .setMinUpdateIntervalMillis(frequencyMs / 2)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        _currentLocation.value = location
                        serviceScope.launch {
                            val point = LocationPoint(
                                sessionId = currentSessionId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                altitude = location.altitude,
                                speed = location.speed,
                                bearing = location.bearing,
                                timestamp = location.time
                            )
                            locationDao.insertLocationPoint(point)
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                mainLooper // Using main looper for callback thread, though DAO runs in IO scope
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission missing", e)
            stopTracking()
        }
    }

    private fun stopTracking() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
        }
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        _isTracking.value = false
        gnssStatusManager.stopListening()
        routineAnalysisManager.stopListening()
        serviceScope.cancel()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Used to show persistent notification during GPS tracking"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}


