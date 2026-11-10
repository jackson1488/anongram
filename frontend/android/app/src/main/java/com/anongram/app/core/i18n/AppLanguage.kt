package com.anongram.app.core.i18n

/**
 * Поддерживаемые языки интерфейса AnonGram.
 * Для добавления нового языка достаточно добавить новую запись в этот enum
 * и создать соответствующий файл реализации переводов (например, InputStringsEs.kt).
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    RU("ru", "Русский", "Русский"),
    EN("en", "English", "English");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: RU
    }
}
