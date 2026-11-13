package com.anongram.web

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anongram.web.core.theme.AnonGramTheme
import com.anongram.web.core.theme.ThemeController
import com.anongram.web.core.theme.WebThemeRepository
import com.anongram.web.features.auth.data.repository.WebAuthRepository
import com.anongram.web.features.auth.domain.model.AuthState
import com.anongram.web.features.auth.ui.navigation.AuthNavGraph
import com.anongram.web.features.auth.ui.state.AuthViewModel
import com.anongram.web.features.auth.ui.welcome.WelcomeShieldAnimation
import com.anongram.web.features.navigation.MainAppShell

@Composable
fun AnonGramWebApp() {
    val themeRepository = remember { WebThemeRepository() }
    val themeController = remember(themeRepository) { ThemeController(themeRepository) }
    val scheme by themeController.scheme.collectAsState()

    val authRepository = remember { WebAuthRepository() }
    val authViewModel = remember(authRepository) { AuthViewModel(authRepository) }
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
                        onAuthSuccess = {}
                    )
                }
                is AuthState.Authenticated -> {
                    MainAppShell(
                        themeController = themeController,
                        identity = state.identity,
                        onLogout = { authViewModel.logout() }
                    )
                }
            }
        }
    }
}
