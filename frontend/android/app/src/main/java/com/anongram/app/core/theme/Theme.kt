package com.anongram.app.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Активная цветовая схема; компоненты читают её вместо хардкода акцентов. */
val LocalAnonGramScheme = staticCompositionLocalOf { AnonGramThemes.CyberStealth }

@Composable
fun AnonGramTheme(
    scheme: AnonGramScheme = AnonGramThemes.CyberStealth,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = scheme.accent,
        secondary = NeonEmerald,
        tertiary = scheme.accentDim,
        background = ObsidianBlack,
        surface = SurfaceDark,
        onPrimary = ObsidianBlack,
        onSecondary = ObsidianBlack,
        onTertiary = ObsidianBlack,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ObsidianBlack.toArgb()
            window.navigationBarColor = ObsidianBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalAnonGramScheme provides scheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
