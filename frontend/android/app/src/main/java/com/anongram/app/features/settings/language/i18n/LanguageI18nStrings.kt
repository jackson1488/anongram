package com.anongram.app.features.settings.language.i18n

/**
 * Контракт строк локализации для экрана настроек языка и перевода в стиле Telegram.
 */
interface LanguageI18nStrings {
    val screenTitle: String
    val translationSectionTitle: String
    val showTranslateButtonTitle: String
    val translateEntireChatsTitle: String
    val doNotTranslateTitle: String
    val translationFootnote: String
    val languageSectionTitle: String
    val searchPlaceholder: String
    val searchEmptyNotice: String
    fun languageAddedNotice(name: String): String
}
