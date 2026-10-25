package com.gpsspy.gpstracker.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.altitude.AltitudeConverter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.gpsspy.gpstracker.MainActivity
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.data.database.LocationDao
import com.gpsspy.gpstracker.data.database.LocationPoint
import com.gpsspy.gpstracker.data.preferences.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private lateinit var dataClient: DataClient
    private val altitudeConverter = AltitudeConverter()

    private var currentSessionId: Long = 0L
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trackingJob: Job? = null
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
        dataClient = Wearable.getDataClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_START) {
            if (startForegroundService()) {
                startTracking()
            }
        }

        return START_STICKY
    }

    private fun startForegroundService(): Boolean {
        val fineLocationGranted = checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted) {
            Log.e("LocationService", "Cannot start foreground service: FINE_LOCATION not granted")
            stopSelf()
            return false
        }

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

        return try {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            true
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.e("LocationService", "Foreground service start not allowed", e)
            stopSelf()
            false
        } catch (e: SecurityException) {
            Log.e("LocationService", "Security exception when starting foreground service", e)
            stopSelf()
            false
        }
    }

    private fun startTracking() {
        if (_isTracking.value) return

        _isTracking.value = true
        syncTrackingStatus(true)
        gnssStatusManager.startListening()
        routineAnalysisManager.startListening()

        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            val latestSession = locationDao.getLatestSessionId() ?: 0L
            currentSessionId = latestSession + 1

            // Dynamic battery optimization: combine tracking frequency with detected activity
            combine(
                settingsRepository.trackingFrequencyFlow,
                routineAnalysisManager.currentPhysicalActivity
            ) { frequencyMs, activity ->
                if (activity == DetectedActivity.STILL) {
                    maxOf(frequencyMs, 60_000L)
                } else {
                    frequencyMs
                }
            }.distinctUntilChanged().collect { effectiveFrequency ->
                updateLocationRequest(effectiveFrequency)
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
                        serviceScope.launch(Dispatchers.Default) {
                            try {
                                altitudeConverter.addMslAltitudeToLocation(applicationContext, location)
                            } catch (e: Exception) {
                                Log.w("LocationService", "Failed to add MSL altitude to location", e)
                            }

                            val finalAltitude = if (location.hasMslAltitude()) {
                                location.mslAltitudeMeters
                            } else {
                                location.altitude
                            }

                            // Update live location state after MSL altitude is computed
                            _currentLocation.value = location

                            val point = LocationPoint(
                                sessionId = currentSessionId,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                altitude = finalAltitude,
                                speed = location.speed,
                                bearing = location.bearing,
                                timestamp = location.time
                            )
                            locationDao.insertLocationPoint(point)
                            syncLocationUpdate(location.latitude, location.longitude, finalAltitude)
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                mainLooper
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission missing", e)
            stopTracking()
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
                Log.w("LocationService", "Failed to sync location to wear", e)
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
                Log.w("LocationService", "Failed to sync tracking status to wear", e)
            }
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        super.onTimeout(startId, fgsType)
        Log.w("LocationService", "Foreground service timed out for fgsType $fgsType, stopping")
        stopTracking()
    }

    private fun stopTracking() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            locationCallback = null
        }

        _isTracking.value = false
        syncTrackingStatus(false)
        gnssStatusManager.stopListening()
        routineAnalysisManager.stopListening()

        trackingJob?.cancel()
        trackingJob = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
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
