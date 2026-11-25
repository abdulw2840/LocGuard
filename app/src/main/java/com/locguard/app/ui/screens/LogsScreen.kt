package com.locguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.locguard.app.data.GuardLogEntry
import com.locguard.app.data.LogCategory
import com.locguard.app.ui.components.LiquidGlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<GuardLogEntry>,
    autoDisableCount: Int,
    onClearLogs: () -> Unit,
    contentPadding: PaddingValues
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity & Privacy",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$autoDisableCount background location sessions prevented",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (logs.isNotEmpty()) {
                    IconButton(onClick = onClearLogs) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear logs")
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                LiquidGlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No events recorded yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Location auto-shutoffs and app triggers will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { entry ->
                val (icon, tint) = when (entry.category) {
                    LogCategory.AUTO_DISABLE -> Icons.Rounded.GpsOff to MaterialTheme.colorScheme.tertiary
                    LogCategory.AUTO_ENABLE -> Icons.Rounded.GpsFixed to MaterialTheme.colorScheme.primary
                    LogCategory.TIMER -> Icons.Rounded.Timer to MaterialTheme.colorScheme.secondary
                    LogCategory.POWER -> Icons.Rounded.BatterySaver to MaterialTheme.colorScheme.tertiary
                    LogCategory.PRIVILEGE -> Icons.Rounded.Security to MaterialTheme.colorScheme.primary
                    LogCategory.INFO -> Icons.Rounded.Info to MaterialTheme.colorScheme.onSurfaceVariant
                }

                LiquidGlassCard(cornerRadius = 20.dp, contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = timeFormat.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = entry.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
