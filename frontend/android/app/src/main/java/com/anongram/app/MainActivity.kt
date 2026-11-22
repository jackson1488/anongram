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
        handleShareIntent(intent)

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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun handleShareIntent(intent: android.content.Intent?) {
        val action = intent?.action ?: return
        if (action != android.content.Intent.ACTION_SEND && action != android.content.Intent.ACTION_SEND_MULTIPLE) return

        val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
        val rawUris = mutableListOf<android.net.Uri>()
        if (action == android.content.Intent.ACTION_SEND) {
            intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)?.let { rawUris.add(it) }
        } else {
            intent.getParcelableArrayListExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
                ?.filterNotNull()
                ?.let { rawUris.addAll(it) }
        }
        if (rawUris.isEmpty() && intent.clipData != null) {
            val clip = intent.clipData!!
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i)?.uri?.let { rawUris.add(it) }
            }
        }

        if (rawUris.isEmpty()) {
            if (!sharedText.isNullOrBlank()) {
                com.anongram.app.features.chats.tools.IncomingShareBus.emit(sharedText, emptyList())
            }
            return
        }

        val appCtx = applicationContext
        Thread {
            val shareDir = java.io.File(appCtx.cacheDir, "shared_media").apply { mkdirs() }
            val resolved = rawUris.mapNotNull { uri ->
                if (uri.scheme == "file") {
                    uri.toString()
                } else {
                    runCatching {
                        val mime = appCtx.contentResolver.getType(uri).orEmpty()
                        val ext = when {
                            mime.contains("png") -> ".png"
                            mime.contains("webp") -> ".webp"
                            mime.contains("gif") -> ".gif"
                            mime.contains("mp4") || mime.contains("video") -> ".mp4"
                            else -> ".jpg"
                        }
                        val dest = java.io.File(shareDir, "share_${System.nanoTime()}$ext")
                        appCtx.contentResolver.openInputStream(uri)?.use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (dest.exists() && dest.length() > 0L) {
                            android.net.Uri.fromFile(dest).toString()
                        } else {
                            uri.toString()
                        }
                    }.getOrElse { uri.toString() }
                }
            }
            com.anongram.app.features.chats.tools.IncomingShareBus.emit(sharedText, resolved)
        }.start()
    }
}
