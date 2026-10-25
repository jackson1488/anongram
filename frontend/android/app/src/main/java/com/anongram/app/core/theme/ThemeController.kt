package com.anongram.app.core.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Контроллер тем: единый источник истины активной схемы для всего приложения.
 * Settings-модуль вызывает selectTheme(id), UI читает [scheme].
 */
class ThemeController(repository: ThemeRepository) {

    private val _scheme = MutableStateFlow(AnonGramThemes.byId(repository.loadThemeId()))
    val scheme: StateFlow<AnonGramScheme> = _scheme.asStateFlow()

    fun selectTheme(id: String) {
        repository.saveThemeId(id)
        _scheme.value = AnonGramThemes.byId(id)
    }
}
