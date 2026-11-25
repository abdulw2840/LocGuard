package com.locguard.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locguard.app.monitor.GuardPhase
import com.locguard.app.monitor.GuardRuntimeState
import com.locguard.app.ui.theme.LocalGlassPalette
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidAuroraBackground(modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    val baseBg = MaterialTheme.colorScheme.background

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        val w = size.width
        val h = size.height

        val orb1Center = Offset(
            x = w * (0.25f + 0.14f * cos(phase1)),
            y = h * (0.18f + 0.08f * sin(phase1))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.orbPrimary.copy(alpha = if (palette.isDark) 0.28f else 0.22f),
                    Color.Transparent
                ),
                center = orb1Center,
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = orb1Center
        )

        val orb2Center = Offset(
            x = w * (0.78f + 0.12f * sin(phase2)),
            y = h * (0.42f + 0.10f * cos(phase2))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.orbSecondary.copy(alpha = if (palette.isDark) 0.22f else 0.18f),
                    Color.Transparent
                ),
                center = orb2Center,
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = orb2Center
        )

        val orb3Center = Offset(
            x = w * (0.45f + 0.15f * cos(phase2 * 0.7f)),
            y = h * (0.82f + 0.07f * sin(phase1 * 0.8f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.orbTertiary.copy(alpha = if (palette.isDark) 0.20f else 0.16f),
                    Color.Transparent
                ),
                center = orb3Center,
                radius = w * 0.68f
            ),
            radius = w * 0.68f,
            center = orb3Center
        )
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (palette.isDark) 12.dp else 8.dp,
                shape = shape,
                ambientColor = palette.orbPrimary.copy(alpha = 0.15f),
                spotColor = palette.orbPrimary.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.surfaceTop, palette.surfaceBottom)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(palette.borderStart, palette.borderEnd)
                ),
                shape = shape
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content
            )
        }
    }
}

@Composable
fun LiquidStatusOrb(
    state: GuardRuntimeState,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val targetColor = when (state.phase) {
        GuardPhase.IDLE_LOCATION_OFF -> colorScheme.tertiary
        GuardPhase.PROTECTED_WHITELISTED -> colorScheme.primary
        GuardPhase.COUNTING_DOWN -> colorScheme.secondary
        GuardPhase.SCREEN_OFF_GRACE -> colorScheme.error
        GuardPhase.PAUSED -> colorScheme.outline
    }

    val animatedAccent by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "orbAccent"
    )

    val progressTarget = when (state.phase) {
        GuardPhase.COUNTING_DOWN, GuardPhase.SCREEN_OFF_GRACE -> {
            val total = state.totalCountdownSeconds.coerceAtLeast(1)
            (state.remainingSeconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
        GuardPhase.PROTECTED_WHITELISTED -> 1f
        else -> 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "orbProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth * 2
            val topLeft = Offset(strokeWidth, strokeWidth)

            // Ambient inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = 0.24f),
                        Color.Transparent
                    )
                ),
                radius = (size.minDimension / 2f) * pulse
            )

            // Frosted track ring
            drawArc(
                color = animatedAccent.copy(alpha = 0.16f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active liquid arc
            if (animatedProgress > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = 0.55f),
                            animatedAccent,
                            animatedAccent.copy(alpha = 0.85f)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(initialScale = 0.85f))
                        .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.9f))
                },
                label = "orbIcon"
            ) { phase ->
                val icon: ImageVector = when (phase) {
                    GuardPhase.IDLE_LOCATION_OFF -> Icons.Rounded.GpsOff
                    GuardPhase.PROTECTED_WHITELISTED -> Icons.Rounded.Shield
                    GuardPhase.COUNTING_DOWN -> Icons.Rounded.Timer
                    GuardPhase.SCREEN_OFF_GRACE -> Icons.Rounded.GpsFixed
                    GuardPhase.PAUSED -> Icons.Rounded.PauseCircle
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = animatedAccent,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val headline = when (state.phase) {
                GuardPhase.IDLE_LOCATION_OFF -> "OFF"
                GuardPhase.PROTECTED_WHITELISTED -> "SAFE"
                GuardPhase.COUNTING_DOWN -> "${state.remainingSeconds}s"
                GuardPhase.SCREEN_OFF_GRACE -> "${state.remainingSeconds}s"
                GuardPhase.PAUSED -> "PAUSED"
            }

            Text(
                text = headline,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            val subtitle = when (state.phase) {
                GuardPhase.IDLE_LOCATION_OFF -> "Location Disabled"
                GuardPhase.PROTECTED_WHITELISTED -> "Whitelisted Active"
                GuardPhase.COUNTING_DOWN -> "Auto-off countdown"
                GuardPhase.SCREEN_OFF_GRACE -> "Screen-off shutoff"
                GuardPhase.PAUSED -> "Guard Inactive"
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class NavTabItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun LiquidGlassBottomBar(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = palette.orbPrimary.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(palette.surfaceTop, palette.surfaceBottom)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(palette.borderStart, palette.borderEnd)
                ),
                shape = shape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val bgColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tabBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "tabContent"
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
