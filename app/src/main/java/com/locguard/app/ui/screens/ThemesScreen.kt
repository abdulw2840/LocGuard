package com.locguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locguard.app.ui.components.KaruiBox
import com.locguard.app.ui.theme.KaruiThemes
import com.locguard.app.ui.theme.LocalKaruiPalette
import com.locguard.app.ui.theme.LocalUiScale
import kotlin.math.abs

@Composable
fun ThemesScreen(
    currentTheme: String,
    currentScale: Float,
    onSelectTheme: (String) -> Unit,
    onSelectScale: (Float) -> Unit,
    contentPadding: PaddingValues
) {
    val haptic = LocalHapticFeedback.current
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            KaruiBox(title = "Themes (${KaruiThemes.size})") {
                val rows = KaruiThemes.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowThemes.forEach { item ->
                                val active = item.key == currentTheme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (active) palette.accent else palette.surface)
                                        .border(2.dp, palette.border, RectangleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectTheme(item.key)
                                        }
                                        .padding(vertical = (10 * scale).dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(item.bg)
                                                    .border(1.dp, item.border, RectangleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(item.accent)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.key.uppercase(),
                                            color = if (active) palette.accentTxt else palette.gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (10 * scale).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowThemes.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            KaruiBox(title = "UI Scale") {
                val scales = listOf(
                    "S (0.85x)" to 0.85f,
                    "M (1.0x)" to 1.0f,
                    "L (1.15x)" to 1.15f,
                    "XL (1.3x)" to 1.3f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scales.forEach { (label, value) ->
                        val active = abs(currentScale - value) < 0.03f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (active) palette.accent else palette.surface)
                                .border(2.dp, palette.border, RectangleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectScale(value)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (active) palette.accentTxt else palette.text,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
