package com.anongram.app.features.settings.qr.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.anongram.app.core.i18n.AppLanguage

/**
 * CompositionLocal для строк раздела QR-кода.
 */
val LocalQrStrings = staticCompositionLocalOf<QrI18nStrings> { QrStringsRu }

object QrI18n {
    fun forLanguage(language: AppLanguage): QrI18nStrings {
        return when (language) {
            AppLanguage.RU -> QrStringsRu
            AppLanguage.EN -> QrStringsEn
        }
    }

    val current: QrI18nStrings
        @Composable
        @ReadOnlyComposable
        get() = LocalQrStrings.current
}
