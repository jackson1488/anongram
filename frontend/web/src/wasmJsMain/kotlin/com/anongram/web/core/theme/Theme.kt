package com.anongram.web.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.features.chats.input.i18n.InputI18n
import com.anongram.web.features.chats.input.i18n.LocalInputStrings

val LocalAnonGramScheme = staticCompositionLocalOf { AnonGramThemes.CyberStealth }

val LocalThemeController = staticCompositionLocalOf<ThemeController?> { null }

@Composable
fun AnonGramTheme(
    scheme: AnonGramScheme = AnonGramThemes.CyberStealth,
    controller: ThemeController? = null,
    content: @Composable () -> Unit
) {
    LaunchedEffect(Unit) {
        LanguageSettingsStore.ensureInitialized()
        ChatTextSettingsStore.ensureInitialized()
    }
    val language by LanguageSettingsStore.language.collectAsState()
    val inputStrings = InputI18n.forLanguage(language)

    val colorScheme = if (scheme.isDark) {
        darkColorScheme(
            primary = scheme.accent,
            onPrimary = scheme.onAccent,
            primaryContainer = scheme.accentSoft,
            onPrimaryContainer = scheme.textPrimary,
            secondary = scheme.accentDim,
            onSecondary = scheme.onAccent,
            secondaryContainer = scheme.islandBg,
            onSecondaryContainer = scheme.textPrimary,
            tertiary = scheme.accentDim,
            onTertiary = scheme.onAccent,
            background = scheme.background,
            onBackground = scheme.textPrimary,
            surface = scheme.surface,
            onSurface = scheme.textPrimary,
            surfaceVariant = scheme.card,
            onSurfaceVariant = scheme.textSecondary,
            outline = scheme.cardBorder,
            outlineVariant = scheme.divider
        )
    } else {
        lightColorScheme(
            primary = scheme.accent,
            onPrimary = scheme.onAccent,
            primaryContainer = scheme.accentSoft,
            onPrimaryContainer = scheme.textPrimary,
            secondary = scheme.accentDim,
            onSecondary = scheme.onAccent,
            secondaryContainer = scheme.islandBg,
            onSecondaryContainer = scheme.textPrimary,
            tertiary = scheme.accentDim,
            onTertiary = scheme.onAccent,
            background = scheme.background,
            onBackground = scheme.textPrimary,
            surface = scheme.surface,
            onSurface = scheme.textPrimary,
            surfaceVariant = scheme.card,
            onSurfaceVariant = scheme.textSecondary,
            outline = scheme.cardBorder,
            outlineVariant = scheme.divider
        )
    }

    CompositionLocalProvider(
        LocalAnonGramScheme provides scheme,
        LocalThemeController provides controller,
        LocalInputStrings provides inputStrings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
