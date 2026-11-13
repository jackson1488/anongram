package com.anongram.web.core.i18n

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
