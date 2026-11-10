package com.anongram.app.features.settings.language.model

/**
 * Каталог доступных языков интерфейса AnonGram.
 *
 * На данный момент активны 2 основных языка: Русский и English.
 * Архитектура каталога позволяет легко добавлять новые языки в будущем
 * путём добавления новых элементов в список [languages].
 */
object LanguageCatalog {
    val languages: List<LanguageItem> = listOf(
        LanguageItem(code = "ru", nativeName = "Русский", englishName = "Russian", isSupported = true),
        LanguageItem(code = "en", nativeName = "English", englishName = "English", isSupported = true)
    )
}
