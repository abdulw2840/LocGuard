package com.locguard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.graphics.RectangleShape
import com.locguard.app.ui.theme.LocalKaruiPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private val BuddyQuips = listOf(
    "Hehe! ✨",
    "0% drain ⚡",
    "Shielding you! 🛡️",
    "Location locked 🔒",
    "So comfy~ 💫"
)

@Composable
fun FloatingLocBuddy(
    state: GuardRuntimeState,
    modifier: Modifier = Modifier
) {
    val palette = LocalKaruiPalette.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val buddySizeDp = 74.dp
    val buddySizePx = with(density) { buddySizeDp.toPx() }
    val marginPx = with(density) { 14.dp.toPx() }
    val topBoundPx = with(density) { 88.dp.toPx() }
    val bottomBoundMarginPx = with(density) { 112.dp.toPx() }

    val posX = remember { Animatable(-1f) }
    val posY = remember { Animatable(-1f) }
    val bounceScaleX = remember { Animatable(1f) }
    val bounceScaleY = remember { Animatable(1f) }
    val danceTilt = remember { Animatable(0f) }

    var showBubble by remember { mutableStateOf(false) }
    var quipIndex by remember { mutableIntStateOf(0) }
    var isBlinking by remember { mutableStateOf(false) }
    var isDancing by remember { mutableStateOf(false) }
    var dizzyUntilMs by remember { mutableLongStateOf(0L) }
    var isDizzy by remember { mutableStateOf(false) }

    // Natural eye blink loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200L)
            isBlinking = true
            delay(140L)
            isBlinking = false
        }
    }

    // Dizzy timer watcher
    LaunchedEffect(dizzyUntilMs) {
        if (dizzyUntilMs > System.currentTimeMillis()) {
            isDizzy = true
            delay((dizzyUntilMs - System.currentTimeMillis()).coerceAtLeast(0L))
            isDizzy = false
        }
    }

    // Auto-hide speech bubble
    LaunchedEffect(showBubble, quipIndex) {
        if (showBubble) {
            delay(2200L)
            showBubble = false
        }
    }

    // Cute dance routine when tapped or when Protected
    LaunchedEffect(isDancing) {
        if (isDancing) {
            repeat(3) {
                launch {
                    bounceScaleX.animateTo(1.14f, tween(130))
                    bounceScaleX.animateTo(0.92f, tween(130))
                    bounceScaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                launch {
                    bounceScaleY.animateTo(0.88f, tween(130))
                    bounceScaleY.animateTo(1.16f, tween(130))
                    bounceScaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                danceTilt.animateTo(-14f, tween(140, easing = FastOutSlowInEasing))
                danceTilt.animateTo(14f, tween(160, easing = FastOutSlowInEasing))
            }
            danceTilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            isDancing = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "buddyLoops")

    // Gentle idle float / breathing
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatPhase"
    )

    // Eye spin angle when dizzy
    val dizzySpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dizzySpin"
    )

    // Floating Zzz progress (0f -> 1f)
    val zzzProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zzz"
    )

    // Subtle happy wiggle when Protected
    val happySway = if (state.phase == GuardPhase.PROTECTED_WHITELISTED && !isDancing && !isDizzy) {
        sin(floatPhase) * 6f
    } else {
        0f
    }

    val isSleeping = state.phase == GuardPhase.IDLE_LOCATION_OFF && !isDizzy && !isDancing

    val accentColor by animateColorAsState(
        targetValue = when (state.phase) {
            GuardPhase.IDLE_LOCATION_OFF -> palette.gray
            GuardPhase.PROTECTED_WHITELISTED -> palette.accent
            GuardPhase.COUNTING_DOWN -> palette.border
            GuardPhase.SCREEN_OFF_GRACE -> MaterialTheme.colorScheme.error
            GuardPhase.PAUSED -> MaterialTheme.colorScheme.outline
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buddyAccent"
    )

    val lightTint = remember(accentColor) { lerp(accentColor, Color.White, 0.52f) }
    val darkTint = remember(accentColor) { lerp(accentColor, Color(0xFF0A1020), 0.45f) }

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
            // Floating Zzz animation when sleeping!
            if (isSleeping && !showBubble) {
                Box(
                    modifier = Modifier.size(width = 56.dp, height = 28.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val z1Alpha = (1f - zzzProgress).coerceIn(0f, 1f)
                    val z2Prog = ((zzzProgress + 0.35f) % 1f)
                    val z2Alpha = (1f - z2Prog).coerceIn(0f, 1f)
                    val z3Prog = ((zzzProgress + 0.70f) % 1f)
                    val z3Alpha = (1f - z3Prog).coerceIn(0f, 1f)

                    Text(
                        text = "z",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor.copy(alpha = z1Alpha),
                        modifier = Modifier.graphicsLayer {
                            translationX = -10f + zzzProgress * 16f
                            translationY = -zzzProgress * 46f
                        }
                    )
                    Text(
                        text = "Z",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = lightTint.copy(alpha = z2Alpha),
                        modifier = Modifier.graphicsLayer {
                            translationX = 4f + z2Prog * 20f
                            translationY = -z2Prog * 52f
                        }
                    )
                    Text(
                        text = "z",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor.copy(alpha = z3Alpha),
                        modifier = Modifier.graphicsLayer {
                            translationX = 14f + z3Prog * 14f
                            translationY = -z3Prog * 40f
                        }
                    )
                }
            }

            // Speech bubble / live countdown pill
            AnimatedVisibility(
                visible = showBubble || state.phase == GuardPhase.COUNTING_DOWN || isDizzy,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.8f),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.8f)
            ) {
                val bubbleText = when {
                    isDizzy -> "Whoa!! @ _ @"
                    showBubble -> BuddyQuips[quipIndex % BuddyQuips.size]
                    state.phase == GuardPhase.COUNTING_DOWN -> "${state.remainingSeconds}s"
                    else -> ""
                }
                if (bubbleText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .background(palette.surface)
                            .border(
                                width = 2.dp,
                                color = palette.border,
                                shape = RectangleShape
                            )
                            .padding(horizontal = 9.dp, vertical = 3.dp)
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

            // 3D Chibi Mascot Body
            Box(
                modifier = Modifier
                    .size(buddySizeDp)
                    .graphicsLayer {
                        val floatY = sin(floatPhase) * 4.5f * density.density
                        translationY = floatY
                        scaleX = bounceScaleX.value
                        scaleY = bounceScaleY.value
                        rotationZ = danceTilt.value + happySway
                    }
                    .pointerInput(maxW, maxH) {
                        detectTapGestures(
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                quipIndex = (quipIndex + 1) % BuddyQuips.size
                                showBubble = true
                                isDancing = true
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
                                val speed = hypot(dragAmount.x, dragAmount.y)
                                if (speed > 26f) {
                                    dizzyUntilMs = System.currentTimeMillis() + 1400L
                                }
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

                    // 3D Ground shadow
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent)
                        ),
                        topLeft = Offset(w * 0.18f, h * 0.84f),
                        size = Size(w * 0.64f, h * 0.14f)
                    )

                    // Radiant outer aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.36f), Color.Transparent)
                        ),
                        radius = w * 0.50f,
                        center = Offset(w * 0.5f, h * 0.52f)
                    )

                    // Cute 3D Cat/Cyber Ears
                    val leftEar = Path().apply {
                        moveTo(w * 0.22f, h * 0.28f)
                        lineTo(w * 0.18f, h * 0.08f)
                        lineTo(w * 0.36f, h * 0.19f)
                        close()
                    }
                    val rightEar = Path().apply {
                        moveTo(w * 0.78f, h * 0.28f)
                        lineTo(w * 0.82f, h * 0.08f)
                        lineTo(w * 0.64f, h * 0.19f)
                        close()
                    }
                    drawPath(leftEar, brush = Brush.linearGradient(listOf(lightTint, darkTint)))
                    drawPath(rightEar, brush = Brush.linearGradient(listOf(lightTint, darkTint)))

                    // 3D Sphere-Shaded Chibi Body
                    val center = Offset(w * 0.5f, h * 0.52f)
                    val bodyRadius = w * 0.36f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(lightTint, accentColor, darkTint),
                            center = Offset(w * 0.36f, h * 0.36f),
                            radius = bodyRadius * 1.55f
                        ),
                        radius = bodyRadius,
                        center = center
                    )

                    // Rim light & bottom bounce light for 3D depth
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.Transparent,
                                lightTint.copy(alpha = 0.35f)
                            )
                        ),
                        radius = bodyRadius,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )

                    // Glossy 3D top-left specular highlight
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.72f), Color.Transparent)
                        ),
                        topLeft = Offset(w * 0.26f, h * 0.23f),
                        size = Size(w * 0.24f, h * 0.13f)
                    )

                    // Rosy pink kawaii cheek blushes
                    val blushColor = Color(0xFFFF6B9D).copy(alpha = 0.55f)
                    drawOval(
                        color = blushColor,
                        topLeft = Offset(w * 0.23f, h * 0.56f),
                        size = Size(w * 0.11f, h * 0.065f)
                    )
                    drawOval(
                        color = blushColor,
                        topLeft = Offset(w * 0.66f, h * 0.56f),
                        size = Size(w * 0.11f, h * 0.065f)
                    )

                    // Eyes + Mouth
                    val eyeY = h * 0.49f
                    val leftEyeX = w * 0.38f
                    val rightEyeX = w * 0.62f

                    when {
                        isDizzy -> {
                            // Spinning spiral/star eyes when swiped fast!
                            val strokeW = 2.1.dp.toPx()
                            listOf(leftEyeX, rightEyeX).forEach { ex ->
                                rotate(degrees = dizzySpin, pivot = Offset(ex, eyeY)) {
                                    drawArc(
                                        color = Color(0xFF0A1020),
                                        startAngle = 0f,
                                        sweepAngle = 260f,
                                        useCenter = false,
                                        topLeft = Offset(ex - w * 0.065f, eyeY - w * 0.065f),
                                        size = Size(w * 0.13f, w * 0.13f),
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = w * 0.024f,
                                        center = Offset(ex + w * 0.03f, eyeY)
                                    )
                                }
                            }
                            // Surprised 'o' mouth
                            drawCircle(
                                color = Color(0xFF0A1020),
                                radius = w * 0.038f,
                                center = Offset(w * 0.5f, h * 0.63f),
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }

                        isSleeping -> {
                            // Peaceful closed sleeping eyes (︶ ︶)
                            val eyeStroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                            drawArc(
                                color = Color(0xFF0A1020),
                                startAngle = 15f,
                                sweepAngle = 150f,
                                useCenter = false,
                                topLeft = Offset(leftEyeX - w * 0.055f, eyeY - w * 0.025f),
                                size = Size(w * 0.11f, w * 0.07f),
                                style = eyeStroke
                            )
                            drawArc(
                                color = Color(0xFF0A1020),
                                startAngle = 15f,
                                sweepAngle = 150f,
                                useCenter = false,
                                topLeft = Offset(rightEyeX - w * 0.055f, eyeY - w * 0.025f),
                                size = Size(w * 0.11f, w * 0.07f),
                                style = eyeStroke
                            )
                            // Tiny peaceful smile
                            drawArc(
                                color = Color(0xFF0A1020),
                                startAngle = 20f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(w * 0.45f, h * 0.58f),
                                size = Size(w * 0.10f, h * 0.06f),
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        isDancing -> {
                            // Happy ^ ^ eyes + big open smile when dancing!
                            val eyeStroke = Stroke(
                                width = 2.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                            listOf(leftEyeX, rightEyeX).forEach { ex ->
                                val p = Path().apply {
                                    moveTo(ex - w * 0.055f, eyeY + w * 0.02f)
                                    lineTo(ex, eyeY - w * 0.045f)
                                    lineTo(ex + w * 0.055f, eyeY + w * 0.02f)
                                }
                                drawPath(p, color = Color(0xFF0A1020), style = eyeStroke)
                            }
                            // Big happy grin
                            drawArc(
                                color = Color(0xFF0A1020),
                                startAngle = 0f,
                                sweepAngle = 180f,
                                useCenter = true,
                                topLeft = Offset(w * 0.43f, h * 0.57f),
                                size = Size(w * 0.14f, h * 0.10f)
                            )
                        }

                        isBlinking -> {
                            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                            drawLine(
                                color = Color(0xFF0A1020),
                                start = Offset(leftEyeX - w * 0.05f, eyeY),
                                end = Offset(leftEyeX + w * 0.05f, eyeY),
                                strokeWidth = stroke.width,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color(0xFF0A1020),
                                start = Offset(rightEyeX - w * 0.05f, eyeY),
                                end = Offset(rightEyeX + w * 0.05f, eyeY),
                                strokeWidth = stroke.width,
                                cap = StrokeCap.Round
                            )
                        }

                        else -> {
                            // Big glossy kawaii eyes with twin specular catchlights
                            val r = w * 0.068f
                            listOf(leftEyeX, rightEyeX).forEach { ex ->
                                drawCircle(
                                    color = Color(0xFF0A1020),
                                    radius = r,
                                    center = Offset(ex, eyeY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = r * 0.40f,
                                    center = Offset(ex - r * 0.28f, eyeY - r * 0.28f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = r * 0.18f,
                                    center = Offset(ex + r * 0.30f, eyeY + r * 0.30f)
                                )
                            }
                            // Cute 'w' / cat smile
                            val smile = Path().apply {
                                moveTo(w * 0.43f, h * 0.60f)
                                quadraticTo(w * 0.465f, h * 0.65f, w * 0.50f, h * 0.60f)
                                quadraticTo(w * 0.535f, h * 0.65f, w * 0.57f, h * 0.60f)
                            }
                            drawPath(
                                path = smile,
                                color = Color(0xFF0A1020),
                                style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}
