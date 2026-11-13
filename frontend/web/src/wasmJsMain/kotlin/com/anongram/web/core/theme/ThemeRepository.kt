package com.anongram.web.core.theme

import com.anongram.web.platform.WebStorage

interface ThemeRepository {
    fun loadThemeId(): String
    fun saveThemeId(id: String)
    fun loadCustomThemes(): List<SavedCustomTheme> = emptyList()
    fun saveCustomThemes(themes: List<SavedCustomTheme>) {}
}

class WebThemeRepository : ThemeRepository {

    override fun loadThemeId(): String =
        WebStorage.getString(KEY_THEME) ?: AnonGramThemes.CyberStealth.id

    override fun saveThemeId(id: String) {
        WebStorage.putString(KEY_THEME, id)
    }

    override fun loadCustomThemes(): List<SavedCustomTheme> {
        val raw = WebStorage.getString(KEY_CUSTOM_THEMES) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size != 3) return@mapNotNull null
                val id = parts[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = parts[1].takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val spec = ThemeSpec.decodeOrNull(parts[2]) ?: return@mapNotNull null
                SavedCustomTheme(id = id, name = name, spec = spec)
            }
            .toList()
    }

    override fun saveCustomThemes(themes: List<SavedCustomTheme>) {
        val serialized = themes.joinToString("\n") { item ->
            val safeName = item.name.replace("\t", " ").replace("\n", " ")
            "${item.id}\t$safeName\t${item.spec.encode()}"
        }
        WebStorage.putString(KEY_CUSTOM_THEMES, serialized)
    }

    private companion object {
        const val KEY_THEME = "anongram_theme_scheme_id"
        const val KEY_CUSTOM_THEMES = "anongram_custom_themes"
    }
}
