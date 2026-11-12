package com.anongram.app.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.features.chats.input.i18n.InputI18n
import com.anongram.app.features.chats.input.i18n.LocalInputStrings

/** Активная цветовая схема; компоненты читают её вместо хардкода цветов и акцентов. */
val LocalAnonGramScheme = staticCompositionLocalOf { AnonGramThemes.CyberStealth }

/** Контроллер тем для прямого вызова конструктора/сохранения с любого экрана. */
val LocalThemeController = staticCompositionLocalOf<ThemeController?> { null }

@Composable
fun AnonGramTheme(
    scheme: AnonGramScheme = AnonGramThemes.CyberStealth,
    controller: ThemeController? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        LanguageSettingsStore.ensureInitialized(context)
    }
    val currentLanguage by LanguageSettingsStore.language.collectAsState()
    val inputStrings = remember(currentLanguage) { InputI18n.forLanguage(currentLanguage) }

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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = scheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = scheme.background.toArgb()
            val controllerInsets = WindowCompat.getInsetsController(window, view)
            controllerInsets.isAppearanceLightStatusBars = !scheme.isDark
            controllerInsets.isAppearanceLightNavigationBars = !scheme.isDark
        }
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
