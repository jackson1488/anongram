package com.anongram.app.features.settings.language.i18n

/**
 * Английская локализация экрана настроек языка.
 */
object LanguageStringsEn : LanguageI18nStrings {
    override val screenTitle: String = "Language"
    override val translationSectionTitle: String = "Message translation"
    override val showTranslateButtonTitle: String = "Show «Translate» button"
    override val translateEntireChatsTitle: String = "Translate entire chats"
    override val doNotTranslateTitle: String = "Do not translate"
    override val translationFootnote: String = "The «Translate» button will appear in the menu upon tapping a text message."
    override val languageSectionTitle: String = "Language"
    override val searchPlaceholder: String = "Search"
    override val searchEmptyNotice: String = "No languages found"
    override fun languageAddedNotice(name: String): String = "Language «$name» will be added in an upcoming update"
}
