package com.gpsspy.gpstracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val homeLocation by viewModel.homeLocation.collectAsState()
    val workLocation by viewModel.workLocation.collectAsState()
    val analysisRadius by viewModel.analysisRadius.collectAsState()

    var homeAddressText by remember { mutableStateOf("") }
    var workAddressText by remember { mutableStateOf("") }
    var isGeocodingHome by remember { mutableStateOf(false) }
    var isGeocodingWork by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        // Home Location Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_home_location),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (homeLocation.first != null && homeLocation.second != null) {
                    val lat = homeLocation.first!!
                    val lng = homeLocation.second!!
                    Text(
                        text = stringResource(id = R.string.settings_location_current, String.format(java.util.Locale.getDefault(), "%.5f", lat), String.format(java.util.Locale.getDefault(), "%.5f", lng)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.settings_location_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = homeAddressText,
                    onValueChange = { homeAddressText = it },
                    label = { Text(stringResource(id = R.string.settings_address_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val toastHomeUpdated = stringResource(id = R.string.settings_toast_home_updated)

                Button(
                    onClick = {
                        if (homeAddressText.isNotBlank()) {
                            isGeocodingHome = true
                            viewModel.geocodeAddress(homeAddressText, isHome = true) { success, error ->
                                isGeocodingHome = false
                                if (success) {
                                    Toast.makeText(context, toastHomeUpdated, Toast.LENGTH_SHORT).show()
                                    homeAddressText = ""
                                } else {
                                    val errStr = if (error != null) {
                                        context.getString(R.string.settings_toast_error, error)
                                    } else {
                                        context.getString(R.string.settings_toast_error, "Unknown")
                                    }
                                    Toast.makeText(context, errStr, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isGeocodingHome && homeAddressText.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isGeocodingHome) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(id = R.string.settings_search_save))
                    }
                }
            }
        }

        // Work Location Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_work_location),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (workLocation.first != null && workLocation.second != null) {
                    val lat = workLocation.first!!
                    val lng = workLocation.second!!
                    Text(
                        text = stringResource(id = R.string.settings_location_current, String.format(java.util.Locale.getDefault(), "%.5f", lat), String.format(java.util.Locale.getDefault(), "%.5f", lng)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.settings_location_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = workAddressText,
                    onValueChange = { workAddressText = it },
                    label = { Text(stringResource(id = R.string.settings_address_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val toastWorkUpdated = stringResource(id = R.string.settings_toast_work_updated)

                Button(
                    onClick = {
                        if (workAddressText.isNotBlank()) {
                            isGeocodingWork = true
                            viewModel.geocodeAddress(workAddressText, isHome = false) { success, error ->
                                isGeocodingWork = false
                                if (success) {
                                    Toast.makeText(context, toastWorkUpdated, Toast.LENGTH_SHORT).show()
                                    workAddressText = ""
                                } else {
                                    val errStr = if (error != null) {
                                        context.getString(R.string.settings_toast_error, error)
                                    } else {
                                        context.getString(R.string.settings_toast_error, "Unknown")
                                    }
                                    Toast.makeText(context, errStr, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isGeocodingWork && workAddressText.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isGeocodingWork) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(id = R.string.settings_search_save))
                    }
                }
            }
        }

        // Analysis Radius Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_analysis_radius),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(id = R.string.settings_radius_meters, analysisRadius.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Slider(
                    value = analysisRadius,
                    onValueChange = { viewModel.updateAnalysisRadius(it) },
                    valueRange = 50f..500f,
                    steps = 8, // 50, 100, 150... 500
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
