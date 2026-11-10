package com.cex.insightflowstore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cex.insightflowstore.analytics.AnalyticsLogEntry
import com.cex.insightflowstore.analytics.AnalyticsTracker
import com.cex.insightflowstore.simulator.AnomalyScenario
import com.cex.insightflowstore.simulator.SimulationRunRecord
import com.cex.insightflowstore.simulator.SimulationRunStatus
import com.cex.insightflowstore.simulator.TrafficSimulationEngine
import com.cex.insightflowstore.ui.theme.AccentOrange
import com.cex.insightflowstore.ui.theme.DangerRed
import com.cex.insightflowstore.ui.theme.SuccessGreen
import com.cex.insightflowstore.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationDrawer(
    simulationEngine: TrafficSimulationEngine,
    tracker: AnalyticsTracker,
    onClose: () -> Unit
) {
    val simState by simulationEngine.state.collectAsState()
    val eventLogs by tracker.eventLogs.collectAsState()
    val manifestRuns by (simulationEngine.manifestManager?.runs?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    var selectedScenario by remember { mutableStateOf(AnomalyScenario.NORMAL) }
    var sessionCount by remember { mutableStateOf(20) }
    var seedInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Live Events, 1: Run Manifest

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Telemetry & Scenario Injector",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Simulates authentic GA4 traffic for AI training",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scenario Selection Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Target Scenario to Simulate:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                AnomalyScenario.entries.forEach { scenario ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedScenario == scenario,
                            onClick = { if (!simState.isRunning) selectedScenario = scenario },
                            enabled = !simState.isRunning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${scenario.title} (Class ${scenario.targetClassIndex})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = scenario.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Session Count Presets & Seed Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sessions:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10, 25, 50, 100).forEach { count ->
                                FilterChip(
                                    selected = sessionCount == count,
                                    onClick = { if (!simState.isRunning) sessionCount = count },
                                    label = { Text("$count") },
                                    enabled = !simState.isRunning
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = seedInput,
                    onValueChange = { seedInput = it },
                    label = { Text("Random Seed (Optional)") },
                    placeholder = { Text("e.g. 42135") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !simState.isRunning
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Execution & Status Bar
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Status: ${simState.statusMessage}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (simState.simRunId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Run ID: ${simState.simRunId} | Seed: ${simState.seed ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (simState.isRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (simState.totalSessions > 0) {
                        simState.completedSessions.toFloat() / simState.totalSessions.toFloat()
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (simState.isRunning) {
                        Button(
                            onClick = { simulationEngine.stopSimulation() },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Simulation")
                        }
                    } else {
                        Button(
                            onClick = {
                                simulationEngine.startSimulation(
                                    scenario = selectedScenario,
                                    sessionCount = sessionCount,
                                    seed = seedInput.toLongOrNull()
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run $sessionCount Sessions")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs: Live Stream vs Local Manifest History
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Live Events (${eventLogs.size})") },
                icon = { Icon(Icons.Default.Analytics, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Run Manifest (${manifestRuns.size})") },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // Live Event Stream Log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { tracker.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = TextSecondary)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(eventLogs, key = { it.id }) { log ->
                    EventLogRow(log)
                }
            }
        } else {
            // Local Simulation Run Manifest History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { simulationEngine.manifestManager?.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = TextSecondary)
                }
            }
            if (manifestRuns.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No simulation runs recorded yet.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(manifestRuns, key = { it.runId }) { runRecord ->
                        RunManifestRow(runRecord)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventLogRow(log: AnalyticsLogEntry) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = log.eventName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RunManifestRow(record: SimulationRunRecord) {
    val statusColor: Color = when (record.status) {
        SimulationRunStatus.COMPLETED -> SuccessGreen
        SimulationRunStatus.RUNNING -> MaterialTheme.colorScheme.primary
        SimulationRunStatus.CANCELLED -> AccentOrange
        SimulationRunStatus.FAILED -> DangerRed
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.runId,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = record.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Scenario: ${record.scenarioCode} (Class ${record.targetClassIndex})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sessions: ${record.completedSessions} / ${record.requestedSessions} | Seed: ${record.seed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = record.formattedStartTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
