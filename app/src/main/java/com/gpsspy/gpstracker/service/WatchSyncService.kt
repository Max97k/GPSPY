package com.gpsspy.gpstracker.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchSyncService : WearableListenerService() {

    companion object {
        const val START_TRACKING_PATH = "/start-tracking"
        const val STOP_TRACKING_PATH = "/stop-tracking"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("WatchSyncService", "Received message: ${messageEvent.path}")

        when (messageEvent.path) {
            START_TRACKING_PATH -> {
                val fineLocation = checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val backgroundLocation = checkSelfPermission(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!fineLocation || !backgroundLocation) {
                    Log.w("WatchSyncService", "Cannot start tracking from watch: missing location permissions")
                    return
                }

                try {
                    val startIntent = Intent(this@WatchSyncService, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_START
                    }
                    startForegroundService(startIntent)
                } catch (e: ForegroundServiceStartNotAllowedException) {
                    Log.e("WatchSyncService", "Foreground service start not allowed", e)
                } catch (e: SecurityException) {
                    Log.e("WatchSyncService", "Security exception starting foreground service", e)
                }
            }
            STOP_TRACKING_PATH -> {
                val stopIntent = Intent(this@WatchSyncService, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_STOP
                }
                startService(stopIntent)
            }
        }
    }
}
