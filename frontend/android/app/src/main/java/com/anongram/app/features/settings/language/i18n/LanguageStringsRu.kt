package com.anongram.app.features.settings.language.i18n

/**
 * Русская локализация экрана настроек языка (в точности по скриншотам Telegram).
 */
object LanguageStringsRu : LanguageI18nStrings {
    override val screenTitle: String = "Язык"
    override val translationSectionTitle: String = "Перевод сообщений"
    override val showTranslateButtonTitle: String = "Показывать кнопку «Перевести»"
    override val translateEntireChatsTitle: String = "Переводить чаты целиком"
    override val doNotTranslateTitle: String = "Не переводить"
    override val translationFootnote: String = "Кнопка «Перевести» появится в меню по нажатию на текстовое сообщение."
    override val languageSectionTitle: String = "Язык"
    override val searchPlaceholder: String = "Поиск"
    override val searchEmptyNotice: String = "Язык не найден"
    override fun languageAddedNotice(name: String): String = "Язык «$name» будет добавлен в ближайшем обновлении"
}
