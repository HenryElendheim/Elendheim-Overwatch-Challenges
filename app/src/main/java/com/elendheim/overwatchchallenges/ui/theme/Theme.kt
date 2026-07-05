package com.elendheim.overwatchchallenges.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun OverwatchChallengesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography(),
        content = content,
    )
}
