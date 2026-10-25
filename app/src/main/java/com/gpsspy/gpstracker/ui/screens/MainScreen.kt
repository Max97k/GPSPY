package com.gpsspy.gpstracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gpsspy.gpstracker.R

@Composable
fun MainScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val tabs = listOf(
        stringResource(R.string.tab_record),
        stringResource(R.string.tab_manager),
        stringResource(R.string.tab_settings),
        stringResource(R.string.tab_analysis)
    )
    val icons = listOf(
        Icons.Default.LocationOn,
        Icons.AutoMirrored.Filled.List,
        Icons.Default.Settings,
        Icons.Default.DateRange
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            tabs.forEachIndexed { index, title ->
                item(
                    icon = { Icon(icons[index], contentDescription = title) },
                    label = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "MainTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> TrackingScreen(modifier = contentModifier)
                    1 -> HistoryScreen(modifier = contentModifier)
                    2 -> SettingsScreen(modifier = contentModifier)
                    3 -> RoutineAnalysisScreen(modifier = contentModifier)
                }
            }
        }
    }
}
