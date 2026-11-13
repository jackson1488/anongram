package com.anongram.web.features.chats.input.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.anongram.web.core.i18n.AppLanguage

object InputI18n {
    fun forLanguage(language: AppLanguage): InputI18nStrings = when (language) {
        AppLanguage.RU -> InputStringsRu
        AppLanguage.EN -> InputStringsEn
    }

    val current: InputI18nStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalInputStrings.current
}

val LocalInputStrings = staticCompositionLocalOf<InputI18nStrings> { InputStringsRu }
