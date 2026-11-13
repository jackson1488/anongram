package com.anongram.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.anongram.web.core.theme.AnonGramTheme
import com.anongram.web.core.theme.ThemeController
import com.anongram.web.core.theme.WebThemeRepository
import com.anongram.web.features.navigation.MainAppShell

@Composable
fun AnonGramWebApp() {
    val themeRepository = remember { WebThemeRepository() }
    val themeController = remember(themeRepository) { ThemeController(themeRepository) }
    val scheme by themeController.scheme.collectAsState()

    AnonGramTheme(scheme = scheme, controller = themeController) {
        MainAppShell(themeController = themeController)
    }
}
