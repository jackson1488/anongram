package com.anongram.app.features.auth.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
import com.anongram.app.features.auth.ui.login.RestoreAccountScreen
import com.anongram.app.features.auth.ui.registration.BackupSeedScreen
import com.anongram.app.features.auth.ui.registration.CreateIdentityScreen
import com.anongram.app.features.auth.ui.state.AuthViewModel
import com.anongram.app.features.auth.ui.welcome.WelcomeScreen

/**
 * Внутренние состояния экранов навигационного графа модуля авторизации.
 */
sealed interface AuthScreen {
    data object Welcome : AuthScreen
    data object CreateIdentity : AuthScreen
    data class BackupSeed(val seedPhrase: SeedPhrase, val identity: UserIdentity) : AuthScreen
    data object RestoreAccount : AuthScreen
}

/**
 * Навигационный граф модуля Регистрации и Входа AnonGram.
 *
 * Управляет маршрутизацией между экранами:
 * - [WelcomeScreen]: начальный экран приветствия;
 * - [CreateIdentityScreen]: создание профиля и деривация ключей;
 * - [BackupSeedScreen]: сохранение мнемоники и проверочный тест;
 * - [RestoreAccountScreen]: восстановление аккаунта по 12 словам.
 *
 * При успешном входе или регистрации вызывает [onAuthSuccess] для открытия основного экрана [MainAppShell].
 *
 * @param viewModel Экземпляр [AuthViewModel] для сохранения сессии.
 * @param onAuthSuccess Колбэк успешного завершения авторизации.
 * @param modifier Модификатор макета.
 */
@Composable
fun AuthNavGraph(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<AuthScreen>(AuthScreen.Welcome) }

    BackHandler(enabled = currentScreen != AuthScreen.Welcome) {
        currentScreen = when (currentScreen) {
            is AuthScreen.BackupSeed -> AuthScreen.CreateIdentity
            is AuthScreen.CreateIdentity -> AuthScreen.Welcome
            is AuthScreen.RestoreAccount -> AuthScreen.Welcome
            AuthScreen.Welcome -> AuthScreen.Welcome
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState is AuthScreen.Welcome) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "auth_nav_transition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            AuthScreen.Welcome -> {
                WelcomeScreen(
                    onCreateIdentity = { currentScreen = AuthScreen.CreateIdentity },
                    onRestoreAccount = { currentScreen = AuthScreen.RestoreAccount }
                )
            }

            AuthScreen.CreateIdentity -> {
                CreateIdentityScreen(
                    onNextToBackup = { _, seedPhrase, identity ->
                        currentScreen = AuthScreen.BackupSeed(seedPhrase, identity)
                    },
                    onBack = { currentScreen = AuthScreen.Welcome }
                )
            }

            is AuthScreen.BackupSeed -> {
                BackupSeedScreen(
                    seedPhrase = screen.seedPhrase,
                    identity = screen.identity,
                    onConfirmed = {
                        viewModel.saveRegistration(screen.identity, screen.seedPhrase)
                        onAuthSuccess()
                    },
                    onBack = { currentScreen = AuthScreen.CreateIdentity }
                )
            }

            AuthScreen.RestoreAccount -> {
                RestoreAccountScreen(
                    onRestoreAccount = { seedPhrase ->
                        viewModel.restoreAccount(seedPhrase)
                    },
                    onRestoreSuccess = {
                        onAuthSuccess()
                    },
                    onBack = { currentScreen = AuthScreen.Welcome }
                )
            }
        }
    }
}
