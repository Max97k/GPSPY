package com.gpsspy.gpstracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.data.database.routine.RoutineState
import com.gpsspy.gpstracker.ui.viewmodels.RoutineAnalysisViewModel
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Composable
fun RoutineAnalysisScreen(
    modifier: Modifier = Modifier,
    viewModel: RoutineAnalysisViewModel = hiltViewModel()
) {
    val isSetupComplete by viewModel.isSetupComplete.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val summary by viewModel.routineSummary.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val dateFormat = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    if (!isSetupComplete) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Date Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { viewModel.previousDay() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.analysis_prev_day)
                    )
                }
                Text(
                    text = currentDate.format(dateFormat),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalIconButton(
                    onClick = { viewModel.nextDay() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.analysis_next_day)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pie / Donut Chart
            val currentSummary = summary
            if (currentSummary != null && currentSummary.stateDurations.isNotEmpty()) {
                val totalDuration = currentSummary.stateDurations.values.sum()
                if (totalDuration > 0) {
                    val totalHours = TimeUnit.MILLISECONDS.toHours(totalDuration)
                    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalDuration) % 60

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                val strokeWidth = 32.dp.toPx()
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
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth)
                                    )
                                    startAngle += sweepAngle
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${totalHours}h ${totalMinutes}m",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Legend and Details Card
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
                            currentSummary.stateDurations.forEach { (state, durationMs) ->
                                val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
                                val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
                                val percentage = Math.round((durationMs.toDouble() / totalDuration) * 100).toInt()
                                val color = when (state) {
                                    RoutineState.HOME -> Color(0xFF42A5F5)
                                    RoutineState.WORK -> Color(0xFF66BB6A)
                                    RoutineState.MOVING -> Color(0xFFEF5350)
                                    RoutineState.OUTDOOR_STAY -> Color(0xFFFFA726)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = state.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "$percentage% (${hours}h ${minutes}m)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.analysis_no_tracking_time),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.analysis_no_data),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.exportAndShareJson(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.analysis_export_share_json),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
