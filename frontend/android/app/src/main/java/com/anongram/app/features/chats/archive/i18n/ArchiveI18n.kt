package com.anongram.app.features.chats.archive.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

object ArchiveI18n {
    fun forLanguage(language: AppLanguage): ArchiveI18nStrings = when (language) {
        AppLanguage.RU -> ArchiveStringsRu
        AppLanguage.EN -> ArchiveStringsEn
    }

    val current: ArchiveI18nStrings
        @Composable
        get() {
            val lang by LanguageSettingsStore.language.collectAsState()
            return forLanguage(lang)
        }
}
