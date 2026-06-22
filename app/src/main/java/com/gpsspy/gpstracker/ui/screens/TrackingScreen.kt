package com.gpsspy.gpstracker.ui.screens

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.res.stringResource
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.service.LocationTrackingService
import com.gpsspy.gpstracker.ui.viewmodels.TrackingViewModel
import com.gpsspy.gpstracker.utils.GpxGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
private val CONSTELLATION_MAP = mapOf(
    1 to "GPS", 2 to "SBAS", 3 to "GLONASS", 4 to "QZSS", 5 to "BEIDOU", 6 to "GALILEO", 7 to "IRNSS"
)

@Composable
fun TrackingScreen(viewModel: TrackingViewModel = viewModel(), modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val frequencyMs by viewModel.trackingFrequencyMs.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val satellites by viewModel.satellites.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showGpsDisabledDialog by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val checkGps = {
            showGpsDisabledDialog = !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        checkGps()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    checkGps()
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    if (showGpsDisabledDialog) {
        AlertDialog(
            onDismissRequest = { /* Force user to enable or dismiss, maybe just dismissible */ },
            title = { Text(stringResource(R.string.gps_disabled_title)) },
            text = { Text(stringResource(R.string.gps_disabled_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.gps_disabled_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDisabledDialog = false }) {
                    Text(stringResource(R.string.gps_disabled_dismiss))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Title
        Text(
            text = stringResource(R.string.track_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        // Main Controls
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isTracking) stringResource(R.string.track_status_recording) else stringResource(R.string.track_status_idle),
                style = MaterialTheme.typography.titleMedium,
                color = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = if (isTracking) LocationTrackingService.ACTION_STOP else LocationTrackingService.ACTION_START
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                modifier = Modifier.size(width = 200.dp, height = 50.dp)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) stringResource(R.string.track_button_stop) else stringResource(R.string.track_button_start)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (isTracking) stringResource(R.string.track_button_stop) else stringResource(R.string.track_button_start))
            }
        }

        if (isTracking && currentLocation != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(R.string.track_data_title), style = MaterialTheme.typography.titleMedium)
                Text(text = "Lat: ${currentLocation?.latitude}, Lon: ${currentLocation?.longitude}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Alt: ${currentLocation?.altitude}m, Speed: ${currentLocation?.speed}m/s", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = stringResource(R.string.track_gnss_title), style = MaterialTheme.typography.titleMedium)
                val usedConstellations = remember(satellites) { satellites.filter { it.usedInFix }.map { it.constellationType }.toSet() }
                val availableConstellations = remember(satellites) { satellites.map { it.constellationType }.toSet() }

                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    availableConstellations.forEach { type ->
                        val name = CONSTELLATION_MAP[type] ?: "Unknown ($type)"
                        val color = if (usedConstellations.contains(type)) Color.Green else Color.Red
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Frequency Control
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.track_freq_label, frequencyMs / 1000))
            
            val sliderIndex = if (frequencyMs <= 1000L) 0f else (frequencyMs / 5000.0).let { Math.round(it) }.coerceIn(1L, 12L).toFloat()
            
            Slider(
                value = sliderIndex,
                onValueChange = { index -> 
                    val newFreq = if (index.toInt() == 0) 1000L else index.toLong() * 5000L
                    viewModel.updateFrequency(newFreq)
                },
                valueRange = 0f..12f,
                steps = 11,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = stringResource(R.string.track_freq_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Export Control
        Button(
            onClick = {
                scope.launch {
                    val sessionId = viewModel.getLatestSessionId()
                    if (sessionId != null) {
                        val points = viewModel.getPointsForSession(sessionId)
                        if (points.isNotEmpty()) {
                            val gpxString = GpxGenerator.generateGpx(points, "Session $sessionId")
                            exportGpxFile(context, gpxString, "Session_${sessionId}.gpx")
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_no_data), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_no_sessions), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.padding(bottom = 32.dp),
            enabled = !isTracking // Prevent export while active to ensure data consistency
        ) {
            Text(stringResource(R.string.track_export_button))
        }
    }
}

private fun exportGpxFile(context: Context, gpxContent: String, fileName: String) {
    try {
        val path = File(context.cacheDir, "gpx_exports")
        if (!path.exists()) path.mkdirs()

        val file = File(path, fileName)
        FileOutputStream(file).use {
            it.write(gpxContent.toByteArray())
        }

        // Use FileProvider to share the file securely
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.chooser_export_gpx)))
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.toast_export_failed, e.message.toString()), Toast.LENGTH_LONG).show()
    }
}


