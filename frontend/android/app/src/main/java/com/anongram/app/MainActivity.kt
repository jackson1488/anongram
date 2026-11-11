package com.anongram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.AnonGramTheme
import com.anongram.app.core.theme.SharedPrefsThemeRepository
import com.anongram.app.core.theme.ThemeController
import com.anongram.app.features.auth.data.repository.AuthRepositoryImpl
import com.anongram.app.features.auth.domain.model.AuthState
import com.anongram.app.features.auth.ui.navigation.AuthNavGraph
import com.anongram.app.features.auth.ui.state.AuthViewModel
import com.anongram.app.features.auth.ui.welcome.WelcomeShieldAnimation
import com.anongram.app.features.navigation.MainAppShell

/**
 * Главная точка входа в приложение AnonGram.
 *
 * Интегрирует:
 * - Edge-to-Edge полноэкранный режим;
 * - WindowSizeClass для адаптивного дизайна под планшеты и смартфоны;
 * - Контроллер тем оформления AnonGram;
 * - Модуль авторизации: проверяет сессию пользователя и плавно переключает
 *   между графом авторизации [AuthNavGraph] и основным интерфейсом [MainAppShell].
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

            val authRepository = remember { AuthRepositoryImpl(applicationContext) }
            val authViewModel = remember { AuthViewModel(authRepository) }
            val authState by authViewModel.authState.collectAsState()

            AnonGramTheme(scheme = scheme, controller = themeController) {
                AnimatedContent(
                    targetState = authState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "main_auth_switch",
                    modifier = Modifier.fillMaxSize()
                ) { state ->
                    when (state) {
                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(scheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                WelcomeShieldAnimation(size = 120.dp)
                            }
                        }

                        is AuthState.Unauthenticated -> {
                            AuthNavGraph(
                                viewModel = authViewModel,
                                onAuthSuccess = {
                                    // При успешной авторизации authRepository автоматически эмитит Authenticated
                                }
                            )
                        }

                        is AuthState.Authenticated -> {
                            MainAppShell(
                                themeController = themeController,
                                widthSizeClass = windowSizeClass.widthSizeClass,
                                onLogout = { authViewModel.logout() }
                            )
                        }
                    }
                }
            }
        }
    }
}
