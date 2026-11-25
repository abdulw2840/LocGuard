package com.locguard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locguard.app.monitor.GuardPhase
import com.locguard.app.monitor.GuardRuntimeState
import com.locguard.app.ui.theme.LocalGlassPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BuddyQuips = listOf(
    "0% drain ⚡",
    "Privacy shielded 🛡️",
    "Location locked 🔒",
    "Battery safe 🔋",
    "I got your back ✨"
)

@Composable
fun FloatingLocBuddy(
    state: GuardRuntimeState,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val buddySizeDp = 62.dp
    val buddySizePx = with(density) { buddySizeDp.toPx() }
    val marginPx = with(density) { 14.dp.toPx() }
    val topBoundPx = with(density) { 88.dp.toPx() }
    val bottomBoundMarginPx = with(density) { 108.dp.toPx() }

    val posX = remember { Animatable(-1f) }
    val posY = remember { Animatable(-1f) }
    val bounceScale = remember { Animatable(1f) }

    var showBubble by remember { mutableStateOf(false) }
    var quipIndex by remember { mutableIntStateOf(0) }
    var isBlinking by remember { mutableStateOf(false) }

    // Natural eye blink loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(3600L)
            isBlinking = true
            delay(130L)
            isBlinking = false
        }
    }

    // Auto-hide speech bubble
    LaunchedEffect(showBubble, quipIndex) {
        if (showBubble) {
            delay(2200L)
            showBubble = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "buddyFloat")
    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val accentColor by animateColorAsState(
        targetValue = when (state.phase) {
            GuardPhase.IDLE_LOCATION_OFF -> palette.orbTertiary
            GuardPhase.PROTECTED_WHITELISTED -> palette.orbPrimary
            GuardPhase.COUNTING_DOWN -> palette.orbSecondary
            GuardPhase.SCREEN_OFF_GRACE -> MaterialTheme.colorScheme.error
            GuardPhase.PAUSED -> MaterialTheme.colorScheme.outline
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buddyAccent"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val maxX = (maxW - buddySizePx - marginPx).coerceAtLeast(marginPx)
        val maxY = (maxH - buddySizePx - bottomBoundMarginPx).coerceAtLeast(topBoundPx)

        LaunchedEffect(maxW, maxH) {
            if (posX.value < 0f) {
                posX.snapTo(maxX)
                posY.snapTo((maxY * 0.62f).coerceIn(topBoundPx, maxY))
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset {
                    IntOffset(
                        posX.value.coerceAtLeast(0f).roundToInt(),
                        posY.value.coerceAtLeast(0f).roundToInt()
                    )
                }
        ) {
            AnimatedVisibility(
                visible = showBubble || state.phase == GuardPhase.COUNTING_DOWN,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.8f),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.8f)
            ) {
                val bubbleText = when {
                    showBubble -> BuddyQuips[quipIndex % BuddyQuips.size]
                    state.phase == GuardPhase.COUNTING_DOWN -> "${state.remainingSeconds}s"
                    else -> ""
                }
                if (bubbleText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                            .border(
                                width = 1.dp,
                                color = accentColor.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = bubbleText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(buddySizeDp)
                    .graphicsLayer {
                        translationY = floatOffsetDp * density.density
                        scaleX = bounceScale.value
                        scaleY = bounceScale.value
                    }
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = accentColor.copy(alpha = 0.45f),
                        spotColor = accentColor.copy(alpha = 0.45f)
                    )
                    .pointerInput(maxW, maxH) {
                        detectTapGestures(
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                quipIndex = (quipIndex + 1) % BuddyQuips.size
                                showBubble = true
                                scope.launch {
                                    bounceScale.animateTo(
                                        1.22f,
                                        animationSpec = spring(stiffness = Spring.StiffnessHigh)
                                    )
                                    bounceScale.animateTo(
                                        1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(maxW, maxH) {
                        detectDragGestures(
                            onDragEnd = {
                                val snapX = if (posX.value + buddySizePx / 2f < maxW / 2f) {
                                    marginPx
                                } else {
                                    maxX
                                }
                                scope.launch {
                                    posX.animateTo(
                                        snapX,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    posX.snapTo((posX.value + dragAmount.x).coerceIn(marginPx, maxX))
                                    posY.snapTo((posY.value + dragAmount.y).coerceIn(topBoundPx, maxY))
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Radiant outer aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.40f), Color.Transparent)
                        ),
                        radius = w * 0.56f
                    )

                    // Chibi cyber-shield body
                    val bodyTopLeft = Offset(w * 0.12f, h * 0.14f)
                    val bodySize = Size(w * 0.76f, h * 0.74f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.92f),
                                accentColor.copy(alpha = 0.65f)
                            )
                        ),
                        topLeft = bodyTopLeft,
                        size = bodySize,
                        cornerRadius = CornerRadius(w * 0.28f, w * 0.28f)
                    )

                    // Specular glass rim
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.55f),
                        topLeft = bodyTopLeft,
                        size = bodySize,
                        cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Dark visor screen
                    val visorTopLeft = Offset(w * 0.22f, h * 0.30f)
                    val visorSize = Size(w * 0.56f, h * 0.36f)
                    drawRoundRect(
                        color = Color(0xFF0A101E),
                        topLeft = visorTopLeft,
                        size = visorSize,
                        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
                    )

                    // Expressive eyes
                    val eyeY = h * 0.48f
                    val leftEyeX = w * 0.38f
                    val rightEyeX = w * 0.62f
                    val eyeColor = Color.White

                    if (isBlinking || state.phase == GuardPhase.IDLE_LOCATION_OFF) {
                        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(
                            color = eyeColor,
                            start = Offset(leftEyeX - w * 0.05f, eyeY),
                            end = Offset(leftEyeX + w * 0.05f, eyeY),
                            strokeWidth = stroke.width,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = eyeColor,
                            start = Offset(rightEyeX - w * 0.05f, eyeY),
                            end = Offset(rightEyeX + w * 0.05f, eyeY),
                            strokeWidth = stroke.width,
                            cap = StrokeCap.Round
                        )
                    } else {
                        val r = if (state.phase == GuardPhase.COUNTING_DOWN) w * 0.062f else w * 0.055f
                        drawCircle(color = eyeColor, radius = r, center = Offset(leftEyeX, eyeY))
                        drawCircle(color = eyeColor, radius = r, center = Offset(rightEyeX, eyeY))
                        // Cute eye sparkle
                        drawCircle(
                            color = accentColor,
                            radius = r * 0.38f,
                            center = Offset(leftEyeX + r * 0.3f, eyeY - r * 0.3f)
                        )
                        drawCircle(
                            color = accentColor,
                            radius = r * 0.38f,
                            center = Offset(rightEyeX + r * 0.3f, eyeY - r * 0.3f)
                        )
                    }
                }
            }
        }
    }
}
