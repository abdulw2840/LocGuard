package com.locguard.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val surfaceTop: Color,
    val surfaceBottom: Color,
    val borderStart: Color,
    val borderEnd: Color,
    val specularGlow: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbTertiary: Color
)

val LocalGlassPalette = staticCompositionLocalOf {
    GlassPalette(
        isDark = true,
        surfaceTop = Color(0x40FFFFFF),
        surfaceBottom = Color(0x18FFFFFF),
        borderStart = Color(0x66FFFFFF),
        borderEnd = Color(0x14FFFFFF),
        specularGlow = Color(0x28FFFFFF),
        orbPrimary = Color(0xFF38BDF8),
        orbSecondary = Color(0xFF818CF8),
        orbTertiary = Color(0xFF2DD4BF)
    )
}

private val FallbackDarkScheme = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D68),
    secondary = Color(0xFFA5B4FC),
    tertiary = Color(0xFF5EEAD4),
    background = Color(0xFF070B14),
    surface = Color(0xFF0B1220),
    surfaceContainerHigh = Color(0xFF131D31)
)

private val FallbackLightScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF4F46E5),
    tertiary = Color(0xFF0D9488),
    background = Color(0xFFF0F6FF),
    surface = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFE2ECF8)
)

@Composable
fun LocGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) FallbackDarkScheme else FallbackLightScheme
    }

    val glassPalette = if (darkTheme) {
        GlassPalette(
            isDark = true,
            surfaceTop = colorScheme.surfaceContainerHigh.copy(alpha = 0.56f),
            surfaceBottom = colorScheme.surface.copy(alpha = 0.34f),
            borderStart = Color.White.copy(alpha = 0.28f),
            borderEnd = Color.White.copy(alpha = 0.05f),
            specularGlow = colorScheme.primary.copy(alpha = 0.14f),
            orbPrimary = colorScheme.primary,
            orbSecondary = colorScheme.secondary,
            orbTertiary = colorScheme.tertiary
        )
    } else {
        GlassPalette(
            isDark = false,
            surfaceTop = Color.White.copy(alpha = 0.72f),
            surfaceBottom = colorScheme.surfaceContainerHigh.copy(alpha = 0.50f),
            borderStart = Color.White.copy(alpha = 0.85f),
            borderEnd = colorScheme.primary.copy(alpha = 0.14f),
            specularGlow = colorScheme.primary.copy(alpha = 0.10f),
            orbPrimary = colorScheme.primary,
            orbSecondary = colorScheme.secondary,
            orbTertiary = colorScheme.tertiary
        )
    }

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
