package com.gpsspy.gpstracker.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gpsspy.gpstracker.R

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf(stringResource(R.string.tab_record), stringResource(R.string.tab_manager), stringResource(R.string.tab_settings), stringResource(R.string.tab_analysis))
    val icons = listOf(Icons.Default.LocationOn, Icons.AutoMirrored.Filled.List, Icons.Default.Settings, Icons.Default.DateRange)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> TrackingScreen(modifier = Modifier.padding(innerPadding))
            1 -> HistoryScreen(modifier = Modifier.padding(innerPadding))
            2 -> SettingsScreen(modifier = Modifier.padding(innerPadding))
            3 -> RoutineAnalysisScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

