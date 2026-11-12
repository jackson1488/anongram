package com.anongram.app.features.chats.input.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.anongram.app.core.i18n.AppLanguage

/**
 * Единая точка доступа и CompositionLocal для локализации модуля ввода и клавиатуры.
 * Для добавления любого нового языка достаточно добавить ветку в [forLanguage].
 */
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

/**
 * CompositionLocal с активными строками локализации.
 */
val LocalInputStrings = staticCompositionLocalOf<InputI18nStrings> { InputStringsRu }
