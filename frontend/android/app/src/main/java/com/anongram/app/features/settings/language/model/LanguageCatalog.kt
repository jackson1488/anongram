package com.anongram.app.features.settings.language.model

/**
 * Каталог доступных языков интерфейса AnonGram в стиле Telegram.
 *
 * Русский и Английский полностью активны и переключают весь интерфейс на лету.
 * Список подготовлен для лёгкого расширения при добавлении новых переводов.
 */
object LanguageCatalog {
    val languages: List<LanguageItem> = listOf(
        LanguageItem(code = "ru", nativeName = "Русский", englishName = "Russian", isSupported = true),
        LanguageItem(code = "en", nativeName = "English", englishName = "English", isSupported = true),
        LanguageItem(code = "ar", nativeName = "العربية", englishName = "Arabic", isSupported = false),
        LanguageItem(code = "be", nativeName = "Беларуская", englishName = "Belarusian", isSupported = false),
        LanguageItem(code = "ca", nativeName = "Català", englishName = "Catalan", isSupported = false),
        LanguageItem(code = "zh_cn", nativeName = "简体中文", englishName = "Chinese (Simplified)", isSupported = false),
        LanguageItem(code = "zh_tw", nativeName = "繁體中文", englishName = "Chinese (Traditional)", isSupported = false),
        LanguageItem(code = "hr", nativeName = "Hrvatski", englishName = "Croatian", isSupported = false),
        LanguageItem(code = "cs", nativeName = "Čeština", englishName = "Czech", isSupported = false),
        LanguageItem(code = "nl", nativeName = "Nederlands", englishName = "Dutch", isSupported = false),
        LanguageItem(code = "fi", nativeName = "Suomi", englishName = "Finnish", isSupported = false),
        LanguageItem(code = "fr", nativeName = "Français", englishName = "French", isSupported = false),
        LanguageItem(code = "de", nativeName = "Deutsch", englishName = "German", isSupported = false)
    )

    const val CUSTOM_EASTER_EGG_NAME = "SpermoGram"
}
