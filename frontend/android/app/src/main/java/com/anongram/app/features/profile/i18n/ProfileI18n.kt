package com.anongram.app.features.profile.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

object ProfileI18n {
    val current: ProfileI18nStrings
        @Composable
        get() = when (LanguageSettingsStore.language.collectAsState().value) {
            AppLanguage.RU -> ProfileStringsRu
            AppLanguage.EN -> ProfileStringsEn
        }
}
