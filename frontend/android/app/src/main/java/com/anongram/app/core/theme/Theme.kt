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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.features.chats.ui.input.i18n.InputI18n
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings

/** Активная цветовая схема; компоненты читают её вместо хардкода акцентов. */
val LocalAnonGramScheme = staticCompositionLocalOf { AnonGramThemes.CyberStealth }

@Composable
fun AnonGramTheme(
    scheme: AnonGramScheme = AnonGramThemes.CyberStealth,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        LanguageSettingsStore.ensureInitialized(context)
    }
    val currentLanguage by LanguageSettingsStore.language.collectAsState()
    val inputStrings = remember(currentLanguage) { InputI18n.forLanguage(currentLanguage) }

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

    CompositionLocalProvider(
        LocalAnonGramScheme provides scheme,
        LocalInputStrings provides inputStrings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
