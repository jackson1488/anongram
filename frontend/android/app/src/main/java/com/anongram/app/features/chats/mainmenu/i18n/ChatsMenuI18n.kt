package com.anongram.app.features.chats.mainmenu.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

object ChatsMenuI18n {
    fun forLanguage(language: AppLanguage): ChatsMenuI18nStrings = when (language) {
        AppLanguage.RU -> ChatsMenuStringsRu
        AppLanguage.EN -> ChatsMenuStringsEn
    }

    val current: ChatsMenuI18nStrings
        @Composable
        get() {
            val lang by LanguageSettingsStore.language.collectAsState()
            return forLanguage(lang)
        }
}
