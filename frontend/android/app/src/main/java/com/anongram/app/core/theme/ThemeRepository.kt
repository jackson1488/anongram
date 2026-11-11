package com.anongram.app.core.theme

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Персистентность активной темы и пользовательских тем («Мои темы»). */
interface ThemeRepository {
    fun loadThemeId(): String
    fun saveThemeId(id: String)
    fun loadCustomThemes(): List<SavedCustomTheme> = emptyList()
    fun saveCustomThemes(themes: List<SavedCustomTheme>) {}
}

/** Реализация на SharedPreferences; при появлении DataStore заменяется за интерфейсом. */
class SharedPrefsThemeRepository(context: Context) : ThemeRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadThemeId(): String =
        prefs.getString(KEY_THEME, null) ?: AnonGramThemes.CyberStealth.id

    override fun saveThemeId(id: String) {
        prefs.edit().putString(KEY_THEME, id).apply()
    }

    override fun loadCustomThemes(): List<SavedCustomTheme> {
        val raw = prefs.getString(KEY_CUSTOM_THEMES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val spec = ThemeSpec.decodeOrNull(obj.optString("spec")) ?: continue
                    add(SavedCustomTheme(id = id, name = name, spec = spec))
                }
            }
        }.getOrDefault(emptyList())
    }

    override fun saveCustomThemes(themes: List<SavedCustomTheme>) {
        val arr = JSONArray()
        themes.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("spec", item.spec.encode())
                }
            )
        }
        prefs.edit().putString(KEY_CUSTOM_THEMES, arr.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "anongram_theme"
        const val KEY_THEME = "scheme_id"
        const val KEY_CUSTOM_THEMES = "custom_themes_json"
    }
}
