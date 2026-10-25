package com.gpsspy.gpstracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gpsspy.gpstracker.ui.screens.MainScreen
import com.gpsspy.gpstracker.ui.screens.PermissionScreen
import com.gpsspy.gpstracker.ui.theme.GpsTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        enableEdgeToEdge()
        setContent {
            GpsTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var permissionsGranted by rememberSaveable { mutableStateOf(checkPermissions()) }

                    LifecycleResumeEffect(Unit) {
                        permissionsGranted = checkPermissions()
                        onPauseOrDispose { }
                    }

                    if (permissionsGranted) {
                        MainScreen()
                    } else {
                        PermissionScreen(
                            onPermissionsGranted = {
                                permissionsGranted = true
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val fineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notifications = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        return fineLocation && coarseLocation && backgroundLocation && notifications
    }
}
