package com.locguard.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locguard.app.ui.InstalledAppInfo
import com.locguard.app.ui.components.KaruiBox
import com.locguard.app.ui.components.KaruiChip
import com.locguard.app.ui.components.KaruiSwitch
import com.locguard.app.ui.theme.LocalKaruiPalette
import com.locguard.app.ui.theme.LocalUiScale

private enum class AppFilter(val label: String) {
    ALL("All"),
    WHITELISTED("Whitelisted"),
    USER("User"),
    SYSTEM("System")
}

@Composable
fun AppsScreen(
    apps: List<InstalledAppInfo>,
    whitelistedPackages: Set<String>,
    isLoading: Boolean,
    onTogglePackage: (String) -> Unit,
    contentPadding: PaddingValues
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }
    val haptic = LocalHapticFeedback.current
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    val filteredApps by remember(apps, whitelistedPackages, query, filter) {
        derivedStateOf {
            apps.filter { app ->
                val matchesQuery = query.isBlank() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

                val matchesFilter = when (filter) {
                    AppFilter.ALL -> true
                    AppFilter.WHITELISTED -> whitelistedPackages.contains(app.packageName)
                    AppFilter.USER -> !app.isSystem
                    AppFilter.SYSTEM -> app.isSystem
                }
                matchesQuery && matchesFilter
            }.sortedByDescending { whitelistedPackages.contains(it.packageName) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Location stays ON while using whitelisted apps, and auto-closes after you leave.",
                color = palette.gray,
                fontSize = (12 * scale).sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        item {
            KaruiBox(title = "Filter & Search", contentPadding = 12.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .border(2.dp, palette.border, RectangleShape)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search apps or package name...",
                                color = palette.gray,
                                fontSize = (14 * scale).sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = palette.text,
                                fontSize = (14 * scale).sp
                            ),
                            cursorBrush = SolidColor(palette.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppFilter.entries.forEach { item ->
                        KaruiChip(
                            label = item.label,
                            selected = filter == item,
                            onClick = { filter = item }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = palette.accent)
                }
            }
        } else {
            items(
                items = filteredApps,
                key = { it.packageName }
            ) { appInfo ->
                val isWhitelisted = remember(appInfo.packageName, whitelistedPackages) {
                    whitelistedPackages.contains(appInfo.packageName)
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appInfo.icon != null) {
                            Image(
                                bitmap = appInfo.icon,
                                contentDescription = appInfo.label,
                                modifier = Modifier
                                    .size(38.dp)
                                    .border(1.dp, palette.border, RectangleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                tint = palette.gray,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = appInfo.label,
                                color = if (isWhitelisted) palette.accent else palette.text,
                                fontWeight = if (isWhitelisted) FontWeight.Bold else FontWeight.Medium,
                                fontSize = (15 * scale).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = appInfo.packageName,
                                color = palette.gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (11 * scale).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    KaruiSwitch(
                        checked = isWhitelisted,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePackage(appInfo.packageName)
                        }
                    )
                }
            }
        }
    }
}
