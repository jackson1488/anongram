package com.anongram.app.core.theme

import android.content.Context

/** Персистентность выбранной темы. */
interface ThemeRepository {
    fun loadThemeId(): String
    fun saveThemeId(id: String)
}

/** Реализация на SharedPreferences; при появлении DataStore заменяется за интерфейсом. */
class SharedPrefsThemeRepository(context: Context) : ThemeRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadThemeId(): String =
        prefs.getString(KEY_THEME, null) ?: AnonGramThemes.CyberStealth.id

    override fun saveThemeId(id: String) {
        prefs.edit().putString(KEY_THEME, id).apply()
    }

    private companion object {
        const val PREFS_NAME = "anongram_theme"
        const val KEY_THEME = "scheme_id"
    }
}
