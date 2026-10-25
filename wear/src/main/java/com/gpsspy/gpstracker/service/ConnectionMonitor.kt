package com.gpsspy.gpstracker.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.wearable.Wearable

class ConnectionMonitor(private val context: Context) {

    fun checkConnectionAndTriggerFallback(isTrackingCurrently: Boolean) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            val isConnectedToPhone = nodes.any { it.isNearby }

            Log.d("ConnectionMonitor", "Connected to Phone: $isConnectedToPhone, IsTracking: $isTrackingCurrently")

            if (isTrackingCurrently && !isConnectedToPhone) {
                // Connection lost while tracking was active -> Start standalone fallback
                startFallbackService()
            } else if (isConnectedToPhone) {
                // Connected -> Stop fallback if it was running, merge data, and rely on phone
                stopFallbackService()
            }
        }.addOnFailureListener {
            Log.e("ConnectionMonitor", "Failed to retrieve connected nodes", it)
        }
    }

    private fun startFallbackService() {
        val hasPermission = context.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w("ConnectionMonitor", "Cannot start standalone fallback: ACCESS_FINE_LOCATION not granted")
            return
        }

        try {
            val intent = Intent(context, WearLocationService::class.java).apply {
                action = WearLocationService.ACTION_START
            }
            context.startForegroundService(intent)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.e("ConnectionMonitor", "Foreground service start not allowed", e)
        } catch (e: SecurityException) {
            Log.e("ConnectionMonitor", "Security exception starting fallback service", e)
        }
    }

    private fun stopFallbackService() {
        val intent = Intent(context, WearLocationService::class.java).apply {
            action = WearLocationService.ACTION_STOP
        }
        context.startService(intent)
    }
}
