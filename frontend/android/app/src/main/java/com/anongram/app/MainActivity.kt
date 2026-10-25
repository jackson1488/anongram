package com.anongram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.anongram.app.core.theme.AnonGramTheme
import com.anongram.app.core.theme.SharedPrefsThemeRepository
import com.anongram.app.core.theme.ThemeController
import com.anongram.app.features.navigation.MainAppShell

/**
 * Тонкая точка входа: Edge-to-Edge, WindowSizeClass (адаптивность) и
 * контроллер тем. Весь UI живёт в изолированных feature-модулях.
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeController = remember {
                ThemeController(SharedPrefsThemeRepository(applicationContext))
            }
            val scheme by themeController.scheme.collectAsState()

            AnonGramTheme(scheme = scheme) {
                MainAppShell(
                    themeController = themeController,
                    widthSizeClass = windowSizeClass.widthSizeClass
                )
            }
        }
    }
}
