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
        FontModuleStore.ensureInitialized(context)
    }
    val currentLanguage by LanguageSettingsStore.language.collectAsState()
    val inputStrings = remember(currentLanguage) { InputI18n.forLanguage(currentLanguage) }
    val fontState by FontModuleStore.state.collectAsState()
    val activeFontFamily = remember(fontState.activeFontId, fontState.installedFonts) {
        FontModuleStore.resolveFontFamily(context, fontState.activeFontId)
    }
    val uiFontFamily = if (fontState.applyScope != FontApplyScope.MESSAGES_ONLY) activeFontFamily else null
    val messageFontFamily = if (fontState.applyScope != FontApplyScope.UI_ONLY) activeFontFamily else null
    val typography = remember(uiFontFamily) {
        if (uiFontFamily == null) {
            androidx.compose.material3.Typography()
        } else {
            val base = androidx.compose.material3.Typography()
            androidx.compose.material3.Typography(
                displayLarge = base.displayLarge.copy(fontFamily = uiFontFamily),
                displayMedium = base.displayMedium.copy(fontFamily = uiFontFamily),
                displaySmall = base.displaySmall.copy(fontFamily = uiFontFamily),
                headlineLarge = base.headlineLarge.copy(fontFamily = uiFontFamily),
                headlineMedium = base.headlineMedium.copy(fontFamily = uiFontFamily),
                headlineSmall = base.headlineSmall.copy(fontFamily = uiFontFamily),
                titleLarge = base.titleLarge.copy(fontFamily = uiFontFamily),
                titleMedium = base.titleMedium.copy(fontFamily = uiFontFamily),
                titleSmall = base.titleSmall.copy(fontFamily = uiFontFamily),
                bodyLarge = base.bodyLarge.copy(fontFamily = uiFontFamily),
                bodyMedium = base.bodyMedium.copy(fontFamily = uiFontFamily),
                bodySmall = base.bodySmall.copy(fontFamily = uiFontFamily),
                labelLarge = base.labelLarge.copy(fontFamily = uiFontFamily),
                labelMedium = base.labelMedium.copy(fontFamily = uiFontFamily),
                labelSmall = base.labelSmall.copy(fontFamily = uiFontFamily)
            )
        }
    }

    val liveScheme = rememberLiveRainbowScheme(scheme)

    val colorScheme = if (liveScheme.isDark) {
        darkColorScheme(
            primary = liveScheme.accent,
            onPrimary = liveScheme.onAccent,
            primaryContainer = liveScheme.accentSoft,
            onPrimaryContainer = liveScheme.textPrimary,
            secondary = liveScheme.accentDim,
            onSecondary = liveScheme.onAccent,
            secondaryContainer = liveScheme.islandBg,
            onSecondaryContainer = liveScheme.textPrimary,
            tertiary = liveScheme.accentDim,
            onTertiary = liveScheme.onAccent,
            background = liveScheme.background,
            onBackground = liveScheme.textPrimary,
            surface = liveScheme.surface,
            onSurface = liveScheme.textPrimary,
            surfaceVariant = liveScheme.card,
            onSurfaceVariant = liveScheme.textSecondary,
            outline = liveScheme.cardBorder,
            outlineVariant = liveScheme.divider
        )
    } else {
        lightColorScheme(
            primary = liveScheme.accent,
            onPrimary = liveScheme.onAccent,
            primaryContainer = liveScheme.accentSoft,
            onPrimaryContainer = liveScheme.textPrimary,
            secondary = liveScheme.accentDim,
            onSecondary = liveScheme.onAccent,
            secondaryContainer = liveScheme.islandBg,
            onSecondaryContainer = liveScheme.textPrimary,
            tertiary = liveScheme.accentDim,
            onTertiary = liveScheme.onAccent,
            background = liveScheme.background,
            onBackground = liveScheme.textPrimary,
            surface = liveScheme.surface,
            onSurface = liveScheme.textPrimary,
            surfaceVariant = liveScheme.card,
            onSurfaceVariant = liveScheme.textSecondary,
            outline = liveScheme.cardBorder,
            outlineVariant = liveScheme.divider
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.LaunchedEffect(scheme.id, scheme.isDark) {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            val controllerInsets = WindowCompat.getInsetsController(window, view)
            controllerInsets.isAppearanceLightStatusBars = !scheme.isDark
            controllerInsets.isAppearanceLightNavigationBars = !scheme.isDark
        }
    }

    CompositionLocalProvider(
        LocalAnonGramScheme provides liveScheme,
        LocalThemeController provides controller,
        LocalInputStrings provides inputStrings,
        LocalChatMessageFontFamily provides messageFontFamily
    ) {
        ThemeRevealHost {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                content = content
            )
        }
    }
}
