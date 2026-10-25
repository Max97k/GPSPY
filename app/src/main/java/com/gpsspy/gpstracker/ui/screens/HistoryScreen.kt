package com.gpsspy.gpstracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gpsspy.gpstracker.R
import com.gpsspy.gpstracker.data.database.SessionSummary
import com.gpsspy.gpstracker.ui.viewmodels.HistoryViewModel
import com.gpsspy.gpstracker.utils.GpxExportHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(), modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var sessionToDelete by remember { mutableStateOf<SessionSummary?>(null) }

    if (sessionToDelete != null) {
        val targetSession = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.history_delete_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.history_delete_confirm_desc, targetSession.sessionId),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(targetSession.sessionId)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.history_delete_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(R.string.history_delete_cancel_button))
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.history_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions, key = { it.sessionId }) { session ->
                    SessionItem(
                        session = session,
                        modifier = Modifier.animateItem(),
                        onExport = {
                            scope.launch {
                                val points = viewModel.getPointsForSession(session.sessionId)
                                if (points.isNotEmpty()) {
                                    GpxExportHelper.exportGpxFile(
                                        context,
                                        points,
                                        "Session ${session.sessionId}",
                                        "Session_${session.sessionId}.gpx"
                                    )
                                } else {
                                    Toast.makeText(context, context.getString(R.string.toast_no_data), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = {
                            sessionToDelete = session
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionItem(
    session: SessionSummary,
    modifier: Modifier = Modifier,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }
    val startTimeStr = formatter.format(Instant.ofEpochMilli(session.startTime))
    val durationSec = session.durationMs / 1000
    val durationMin = durationSec / 60
    val durationRemSec = durationSec % 60
    val durationStr = String.format("%02d:%02d", durationMin, durationRemSec)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.history_session_num, session.sessionId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.history_start, startTimeStr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.history_duration, durationStr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text(stringResource(R.string.history_export))
                }
                Spacer(modifier = Modifier.width(12.dp))
                FilledTonalButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text(stringResource(R.string.history_delete))
                }
            }
        }
    }
}
