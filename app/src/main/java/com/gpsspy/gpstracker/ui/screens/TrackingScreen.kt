package com.gpsspy.gpstracker.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.service.LocationTrackingService
import com.gpsspy.gpstracker.ui.viewmodels.TrackingViewModel
import com.gpsspy.gpstracker.utils.GpxExportHelper
import kotlinx.coroutines.launch
import java.util.Locale

private val CONSTELLATION_MAP = mapOf(
    1 to "GPS", 2 to "SBAS", 3 to "GLONASS", 4 to "QZSS", 5 to "BEIDOU", 6 to "GALILEO", 7 to "IRNSS"
)

@Composable
fun TrackingScreen(viewModel: TrackingViewModel = viewModel(), modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
    val frequencyMs by viewModel.trackingFrequencyMs.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val satellites by viewModel.satellites.collectAsStateWithLifecycle()
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
        context.registerReceiver(
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    if (showGpsDisabledDialog) {
        AlertDialog(
            onDismissRequest = { /* Force user to enable or dismiss */ },
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        Text(
            text = stringResource(R.string.track_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Main Controls Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTracking) stringResource(R.string.track_status_recording) else stringResource(R.string.track_status_idle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(context, LocationTrackingService::class.java).apply {
                            action = if (isTracking) LocationTrackingService.ACTION_STOP else LocationTrackingService.ACTION_START
                        }
                        if (isTracking) {
                            context.startService(intent)
                        } else {
                            context.startForegroundService(intent)
                        }
                    },
                    colors = if (isTracking) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(
                        imageVector = if (isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isTracking) stringResource(R.string.track_button_stop) else stringResource(R.string.track_button_start)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTracking) stringResource(R.string.track_button_stop) else stringResource(R.string.track_button_start),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live Telemetry Card
        if (isTracking && currentLocation != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.track_data_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Latitude",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.6f°", currentLocation?.latitude ?: 0.0),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Longitude",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.6f°", currentLocation?.longitude ?: 0.0),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MSL Altitude",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val displayAltitude = if (currentLocation?.hasMslAltitude() == true) {
                                currentLocation?.mslAltitudeMeters
                            } else {
                                currentLocation?.altitude
                            } ?: 0.0
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f m", displayAltitude),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Speed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f m/s", currentLocation?.speed ?: 0f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // GNSS Constellation Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val usedConstellations = remember(satellites) { satellites.filter { it.usedInFix }.map { it.constellationType }.toSet() }
                    val availableConstellations = remember(satellites) { satellites.map { it.constellationType }.toSet() }
                    val usedCount = remember(satellites) { satellites.count { it.usedInFix } }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.track_gnss_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$usedCount / ${satellites.size} in fix",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (availableConstellations.isEmpty()) {
                        Text(
                            text = "Searching for satellites...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableConstellations.forEach { type ->
                                val name = CONSTELLATION_MAP[type] ?: "Unknown ($type)"
                                val isUsed = usedConstellations.contains(type)
                                FilterChip(
                                    selected = isUsed,
                                    onClick = { },
                                    label = { Text(name) },
                                    leadingIcon = {
                                        if (isUsed) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Used in fix",
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Frequency Control Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.track_freq_label, frequencyMs / 1000),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                val sliderIndex = if (frequencyMs <= 1000L) 0f else (frequencyMs / 5000.0).let { Math.round(it) }.coerceIn(1L, 12L).toFloat()

                Slider(
                    value = sliderIndex,
                    onValueChange = { index ->
                        val newFreq = if (index.toInt() == 0) 1000L else index.toLong() * 5000L
                        viewModel.updateFrequency(newFreq)
                    },
                    valueRange = 0f..12f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.track_freq_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Export Control
        OutlinedButton(
            onClick = {
                scope.launch {
                    val sessionId = viewModel.getLatestSessionId()
                    if (sessionId != null) {
                        val points = viewModel.getPointsForSession(sessionId)
                        if (points.isNotEmpty()) {
                            GpxExportHelper.exportGpxFile(
                                context,
                                points,
                                "Session $sessionId",
                                "Session_${sessionId}.gpx"
                            )
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_no_data), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.toast_no_sessions), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(bottom = 24.dp),
            enabled = !isTracking // Prevent export while active to ensure data consistency
        ) {
            Text(stringResource(R.string.track_export_button))
        }
    }
}
