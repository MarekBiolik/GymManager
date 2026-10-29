package com.gymmanager.android.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Thin MaterialTheme wrapper bound to [GymTokens] (generated from design SSOT).
 * Do not hardcode palette hex in feature UI — change tokens and regenerate.
 */
@Composable
fun GymManagerTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = GymTokens.ColorActionPrimary,
        secondary = GymTokens.ColorActionSecondary,
        background = GymTokens.ColorBgCanvas,
        surface = GymTokens.ColorBgSurface,
        onPrimary = GymTokens.ColorTextInverse,
        onSecondary = GymTokens.ColorTextInverse,
        onBackground = GymTokens.ColorTextPrimary,
        onSurface = GymTokens.ColorTextPrimary,
        error = GymTokens.ColorFeedbackError,
    )
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}

/** Convenience aliases so UI reads semantic names without touching MaterialScheme only. */
object GymThemeColors {
    val canvas: Color get() = GymTokens.ColorBgCanvas
    val surface: Color get() = GymTokens.ColorBgSurface
    val primaryAction: Color get() = GymTokens.ColorActionPrimary
}
