package com.gpsspy.gpstracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.ui.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val homeLocation by viewModel.homeLocation.collectAsStateWithLifecycle()
    val workLocation by viewModel.workLocation.collectAsStateWithLifecycle()
    val analysisRadius by viewModel.analysisRadius.collectAsStateWithLifecycle()

    var homeAddressText by rememberSaveable { mutableStateOf("") }
    var workAddressText by rememberSaveable { mutableStateOf("") }
    var isGeocodingHome by rememberSaveable { mutableStateOf(false) }
    var isGeocodingWork by rememberSaveable { mutableStateOf(false) }

    val toastHomeUpdated = stringResource(id = R.string.settings_toast_home_updated)
    val toastWorkUpdated = stringResource(id = R.string.settings_toast_work_updated)

    val performHomeSearch = {
        if (homeAddressText.isNotBlank()) {
            keyboardController?.hide()
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
    }

    val performWorkSearch = {
        if (workAddressText.isNotBlank()) {
            keyboardController?.hide()
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
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(id = R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Home Location Section
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_home_location),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = homeAddressText,
                    onValueChange = { homeAddressText = it },
                    label = { Text(stringResource(id = R.string.settings_address_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performHomeSearch() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { performHomeSearch() },
                    enabled = !isGeocodingHome && homeAddressText.isNotBlank(),
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp)
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
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_work_location),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = workAddressText,
                    onValueChange = { workAddressText = it },
                    label = { Text(stringResource(id = R.string.settings_address_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performWorkSearch() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { performWorkSearch() },
                    enabled = !isGeocodingWork && workAddressText.isNotBlank(),
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp)
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
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_analysis_radius),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
