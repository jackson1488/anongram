package com.anongram.app.features.settings.language.model

/**
 * Модель элемента языка в списке настроек.
 *
 * @param code Код языка (например, "ru", "en", "ar", "be", "ca", ...)
 * @param nativeName Название на родном языке (например, "Русский", "العربية")
 * @param englishName Название на английском языке (например, "Russian", "Arabic")
 * @param isSupported Поддерживается ли полная локализация приложения прямо сейчас
 */
data class LanguageItem(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isSupported: Boolean = false
)
