package com.anongram.app.features.settings.language.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.anongram.app.core.i18n.AppLanguage

/**
 * CompositionLocal для строк экрана настроек языка.
 */
val LocalLanguageStrings = staticCompositionLocalOf<LanguageI18nStrings> { LanguageStringsRu }

object LanguageI18n {
    fun forLanguage(language: AppLanguage): LanguageI18nStrings {
        return when (language) {
            AppLanguage.RU -> LanguageStringsRu
            AppLanguage.EN -> LanguageStringsEn
        }
    }

    val current: LanguageI18nStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalLanguageStrings.current
}
