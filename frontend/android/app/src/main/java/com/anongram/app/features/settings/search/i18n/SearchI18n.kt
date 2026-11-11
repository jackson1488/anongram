package com.anongram.app.features.settings.search.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.anongram.app.core.i18n.AppLanguage

/**
 * CompositionLocal для строк поиска настроек.
 */
val LocalSearchStrings = staticCompositionLocalOf<SearchI18nStrings> { SearchStringsRu }

object SearchI18n {
    fun forLanguage(language: AppLanguage): SearchI18nStrings {
        return when (language) {
            AppLanguage.RU -> SearchStringsRu
            AppLanguage.EN -> SearchStringsEn
        }
    }

    val current: SearchI18nStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalSearchStrings.current
}
