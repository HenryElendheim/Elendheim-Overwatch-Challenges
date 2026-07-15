package com.elendheim.overwatchchallenges.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// Dark first, dark always. Rolling challenges at 1am should not sear anyone's eyes.
private val DarkColors = darkColorScheme(
    primary = Ember,
    onPrimary = EmberInk,
    secondary = Ash,
    onSecondary = Night,
    background = Night,
    onBackground = Cloud,
    surface = SurfaceDark,
    onSurface = Cloud,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Ash,
    error = Danger,
    onError = Night,
    outline = OutlineDark,
)

// brighter text, stronger lines, true black behind everything
private val HighContrastColors = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFCFCBDA),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF141318),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF232229),
    onSurfaceVariant = Color(0xFFD6D2E0),
    error = Color(0xFFFF8080),
    onError = Color(0xFF000000),
    outline = Color(0xFF8A8798),
)

@Composable
fun OverwatchChallengesTheme(
    textScale: Float = 1f,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (highContrast) HighContrastColors else DarkColors,
        typography = Typography(),
    ) {
        // scale every font in one move instead of rebuilding the typography
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * textScale),
            content = content,
        )
    }
}
