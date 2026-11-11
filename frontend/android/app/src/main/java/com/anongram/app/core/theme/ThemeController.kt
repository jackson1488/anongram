package com.anongram.app.core.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Контроллер тем: единый источник истины активной схемы и конструктора тем для всего приложения.
 * Работает как с готовыми пресетами, так и с генератором по цветовому кругу (`ThemeSpec`)
 * и пользовательским списком «Мои темы».
 */
class ThemeController(private val repository: ThemeRepository) {

    private val _customThemes = MutableStateFlow(repository.loadCustomThemes())
    val customThemes: StateFlow<List<SavedCustomTheme>> = _customThemes.asStateFlow()

    private val _scheme = MutableStateFlow(resolveScheme(repository.loadThemeId(), _customThemes.value))
    val scheme: StateFlow<AnonGramScheme> = _scheme.asStateFlow()

    fun selectTheme(id: String) {
        repository.saveThemeId(id)
        _scheme.value = resolveScheme(id, _customThemes.value)
    }

    /** Применяет конфигурацию из цветового круга/конструктора в реальном времени. */
    fun applyCustomSpec(spec: ThemeSpec, name: String = "Custom") {
        val codeId = "custom:${spec.encode()}"
        repository.saveThemeId(codeId)
        _scheme.value = ThemePaletteEngine.buildScheme(
            id = codeId,
            displayName = name,
            spec = spec
        )
    }

    /** Сохраняет текущую сборку в «Мои темы» и сразу активирует её. */
    fun saveCustomTheme(name: String, spec: ThemeSpec): SavedCustomTheme {
        val cleanName = name.trim().ifBlank { "Custom ${spec.encode().takeLast(6)}" }
        val item = SavedCustomTheme(
            id = "saved:${UUID.randomUUID().toString().take(8)}",
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
