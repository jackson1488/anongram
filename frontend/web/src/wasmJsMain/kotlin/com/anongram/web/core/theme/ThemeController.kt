package com.anongram.web.core.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class ThemeController(private val repository: ThemeRepository) {

    private val _customThemes = MutableStateFlow(repository.loadCustomThemes())
    val customThemes: StateFlow<List<SavedCustomTheme>> = _customThemes.asStateFlow()

    private val _scheme = MutableStateFlow(resolveScheme(repository.loadThemeId(), _customThemes.value))
    val scheme: StateFlow<AnonGramScheme> = _scheme.asStateFlow()

    fun selectTheme(id: String) {
        repository.saveThemeId(id)
        _scheme.value = resolveScheme(id, _customThemes.value)
    }

    fun applyCustomSpec(spec: ThemeSpec, name: String = "Custom") {
        val codeId = "custom:${spec.encode()}"
        repository.saveThemeId(codeId)
        _scheme.value = ThemePaletteEngine.buildScheme(
            id = codeId,
            displayName = name,
            spec = spec
        )
    }

    fun saveCustomTheme(name: String, spec: ThemeSpec): SavedCustomTheme {
        val cleanName = name.trim().ifBlank { "Custom ${spec.encode().takeLast(6)}" }
        val suffix = Random.nextInt(100000, 999999).toString(16)
        val item = SavedCustomTheme(
            id = "saved:$suffix",
            name = cleanName,
            spec = spec
        )
        val updated = listOf(item) + _customThemes.value
        _customThemes.value = updated
        repository.saveCustomThemes(updated)
        selectTheme(item.id)
        return item
    }

    fun deleteCustomTheme(id: String) {
        val updated = _customThemes.value.filterNot { it.id == id }
        _customThemes.value = updated
        repository.saveCustomThemes(updated)
        if (_scheme.value.id == id) {
            selectTheme(AnonGramThemes.CyberStealth.id)
        }
    }

    fun importThemeCode(rawCode: String, customName: String? = null): Boolean {
        val parsed = ThemeSpec.decodeOrNull(rawCode.removePrefix("custom:").trim()) ?: return false
        if (!customName.isNullOrBlank()) {
            saveCustomTheme(customName, parsed)
        } else {
            applyCustomSpec(parsed, name = "Imported")
        }
        return true
    }

    private fun resolveScheme(id: String, saved: List<SavedCustomTheme>): AnonGramScheme {
        if (id.startsWith("saved:")) {
            saved.firstOrNull { it.id == id }?.let { item ->
                return ThemePaletteEngine.buildScheme(
                    id = item.id,
                    displayName = item.name,
                    spec = item.spec
                )
            }
        }
        return AnonGramThemes.byId(id)
    }
}
