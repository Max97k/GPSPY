package com.gpsspy.gpstracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.data.database.routine.RoutineState
import com.gpsspy.gpstracker.ui.viewmodels.RoutineAnalysisViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RoutineAnalysisScreen(
    modifier: Modifier = Modifier,
    viewModel: RoutineAnalysisViewModel = hiltViewModel()
) {
    val isSetupComplete by viewModel.isSetupComplete.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val summary by viewModel.routineSummary.collectAsState()
    val context = LocalContext.current

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    if (!isSetupComplete) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.setup_required_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.setup_required_desc),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            // Date Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { viewModel.previousDay() }) {
                    Text("<")
                }
                Text(
                    text = dateFormat.format(currentDate.time),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { viewModel.nextDay() }) {
                    Text(">")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pie Chart
            val currentSummary = summary
            if (currentSummary != null && currentSummary.stateDurations.isNotEmpty()) {
                val totalDuration = currentSummary.stateDurations.values.sum()
                if (totalDuration > 0) {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            currentSummary.stateDurations.forEach { (state, duration) ->
                                val sweepAngle = (duration.toFloat() / totalDuration) * 360f
                                val color = when (state) {
                                    RoutineState.HOME -> Color(0xFF42A5F5) // Blue
                                    RoutineState.WORK -> Color(0xFF66BB6A) // Green
                                    RoutineState.MOVING -> Color(0xFFEF5350) // Red
                                    RoutineState.OUTDOOR_STAY -> Color(0xFFFFA726) // Orange
                                }
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Legend and Details
                    Column(modifier = Modifier.fillMaxWidth()) {
                        currentSummary.stateDurations.forEach { (state, durationMs) ->
                            val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
                            val color = when (state) {
                                RoutineState.HOME -> Color(0xFF42A5F5)
                                RoutineState.WORK -> Color(0xFF66BB6A)
                                RoutineState.MOVING -> Color(0xFFEF5350)
                                RoutineState.OUTDOOR_STAY -> Color(0xFFFFA726)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Color block
                                Surface(color = color, modifier = Modifier.size(16.dp)) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${state.name}: $hours h $minutes min",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        Text("No tracking time accumulated", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                    Text("No data for this day", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.exportAndShareJson(context) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Export & Share as JSON")
            }
        }
    }
}
