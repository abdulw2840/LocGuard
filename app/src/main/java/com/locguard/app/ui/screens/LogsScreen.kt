package com.locguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locguard.app.data.GuardLogEntry
import com.locguard.app.data.LogCategory
import com.locguard.app.ui.components.KaruiBox
import com.locguard.app.ui.components.KaruiSubtleButton
import com.locguard.app.ui.theme.LocalKaruiPalette
import com.locguard.app.ui.theme.LocalUiScale
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
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$autoDisableCount background location sessions prevented",
                    color = palette.gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (12 * scale).sp,
                    modifier = Modifier.weight(1f)
                )

                if (logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    KaruiSubtleButton(
                        text = "Clear",
                        onClick = onClearLogs
                    )
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                KaruiBox(title = "Activity & Privacy") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NO EVENTS RECORDED YET",
                            color = palette.text,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scale).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Location auto-shutoffs and app triggers will appear here.",
                            color = palette.gray,
                            fontSize = (12 * scale).sp
                        )
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { entry ->
                val icon = when (entry.category) {
                    LogCategory.AUTO_DISABLE -> Icons.Rounded.GpsOff
                    LogCategory.AUTO_ENABLE -> Icons.Rounded.GpsFixed
                    LogCategory.TIMER -> Icons.Rounded.Timer
                    LogCategory.POWER -> Icons.Rounded.BatterySaver
                    LogCategory.PRIVILEGE -> Icons.Rounded.Security
                    LogCategory.INFO -> Icons.Rounded.Info
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.bg)
                        .drawBehind {
                            drawLine(
                                color = palette.border,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        .padding(vertical = (12 * scale).dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(palette.surface)
                            .border(1.dp, palette.border, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
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
                                color = palette.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = (14 * scale).sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = timeFormat.format(Date(entry.timestamp)),
                                color = palette.accent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (11 * scale).sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.detail,
                            color = palette.gray,
                            fontSize = (12 * scale).sp
                        )
                    }
                }
            }
        }
    }
}
