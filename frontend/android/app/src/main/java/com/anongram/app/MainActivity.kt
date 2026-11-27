package com.anongram.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anongram.app.core.db.AnonGramEncryptedDb
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import kotlinx.coroutines.delay
import com.anongram.app.core.theme.AnonGramTheme
import com.anongram.app.core.theme.SharedPrefsThemeRepository
import com.anongram.app.core.theme.ThemeController
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.features.auth.data.repository.AuthRepositoryImpl
import com.anongram.app.features.auth.domain.model.AuthState
import com.anongram.app.features.auth.ui.navigation.AuthNavGraph
import com.anongram.app.features.auth.ui.state.AuthViewModel
import com.anongram.app.features.auth.ui.welcome.WelcomeShieldAnimation
import com.anongram.app.features.navigation.MainAppShell
import com.anongram.app.features.settings.security.data.PasscodeStore
import com.anongram.app.features.settings.security.ui.components.PasscodeLockOverlay
import com.anongram.app.features.settings.security.ui.components.PasscodeScreenMode

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
class MainActivity : FragmentActivity() {

    private var hasWindowFocus by mutableStateOf(true)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasWindowFocus = hasFocus
    }

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
                            val lang by LanguageSettingsStore.language.collectAsState()
                            val isRu = lang == AppLanguage.RU
                            val secPrefs = remember {
                                applicationContext.getSharedPreferences("anongram_security_settings", Context.MODE_PRIVATE)
                            }
                            var passcodeEnabled by remember {
                                mutableStateOf(PasscodeStore.isPasscodeEnabled(applicationContext))
                            }
                            var isUnlocked by remember { mutableStateOf(!passcodeEnabled) }
                            var showEnablePrompt by remember { mutableStateOf(!passcodeEnabled) }
                            var isCreatingPasscode by remember { mutableStateOf(false) }
                            var showInRecents by remember {
                                mutableStateOf(secPrefs.getBoolean("key_show_in_recents", true))
                            }
                            var autoLockOption by remember {
                                mutableStateOf(secPrefs.getString("key_autolock_option", "через 1 мин.") ?: "через 1 мин.")
                            }
                            var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

                            val lifecycleOwner = LocalLifecycleOwner.current
                            DisposableEffect(lifecycleOwner) {
                                val observer = LifecycleEventObserver { _, event ->
                                    when (event) {
                                        Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                                            val enabled = PasscodeStore.isPasscodeEnabled(applicationContext)
                                            passcodeEnabled = enabled
                                            showInRecents = secPrefs.getBoolean("key_show_in_recents", true)
                                            autoLockOption = secPrefs.getString("key_autolock_option", "через 1 мин.")
                                                ?: "через 1 мин."
                                            if (!enabled) {
                                                isUnlocked = true
                                                if (event == Lifecycle.Event.ON_START) {
                                                    showEnablePrompt = true
                                                }
                                            } else {
                                                lastInteractionMs = System.currentTimeMillis()
                                            }
                                        }
                                        Lifecycle.Event.ON_STOP -> {
                                            val enabled = PasscodeStore.isPasscodeEnabled(applicationContext)
                                            val opt = secPrefs.getString("key_autolock_option", "через 1 мин.")
                                                ?: "через 1 мин."
                                            val isDisabledOpt = opt.equals("выключена", ignoreCase = true) ||
                                                opt.equals("Disabled", ignoreCase = true)
                                            if (enabled && !isDisabledOpt) {
                                                isUnlocked = false
                                                AnonGramEncryptedDb.lock()
                                            }
                                        }
                                        else -> Unit
                                    }
                                }
                                lifecycleOwner.lifecycle.addObserver(observer)
                                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                            }

                            // Адаптивная автоблокировка при бездействии внутри приложения (макс 15 мин)
                            LaunchedEffect(passcodeEnabled, isUnlocked, autoLockOption) {
                                if (!passcodeEnabled || !isUnlocked) return@LaunchedEffect
                                val timeoutMs = parseAutoLockTimeoutMs(autoLockOption) ?: return@LaunchedEffect
                                while (true) {
                                    delay(1000)
                                    val liveEnabled = PasscodeStore.isPasscodeEnabled(applicationContext)
                                    val liveOpt = secPrefs.getString("key_autolock_option", autoLockOption) ?: autoLockOption
                                    if (!liveEnabled) break
                                    val liveTimeout = parseAutoLockTimeoutMs(liveOpt) ?: break
                                    if (System.currentTimeMillis() - lastInteractionMs >= liveTimeout) {
                                        isUnlocked = false
                                        AnonGramEncryptedDb.lock()
                                        break
                                    }
                                    if (liveTimeout != timeoutMs) {
                                        autoLockOption = liveOpt
                                    }
                                }
                            }

                            val shouldBlurRecents = !hasWindowFocus && !showInRecents
                            val blurRadius by animateDpAsState(
                                targetValue = if (shouldBlurRecents && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 26.dp else 0.dp,
                                animationSpec = tween(durationMillis = 180),
                                label = "recents_blur"
                            )
                            val veilAlpha by animateFloatAsState(
                                targetValue = if (shouldBlurRecents) 0.35f else 0f,
                                animationSpec = tween(durationMillis = 180),
                                label = "recents_veil"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent(PointerEventPass.Initial)
                                                lastInteractionMs = System.currentTimeMillis()
                                            }
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
                                ) {
                                    MainAppShell(
                                        themeController = themeController,
                                        widthSizeClass = windowSizeClass.widthSizeClass,
                                        onLogout = { authViewModel.logout() }
                                    )
                                }

                                if (veilAlpha > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(scheme.background.copy(alpha = veilAlpha))
                                    )
                                }

                                // Нижняя карточка-напоминание при каждом входе, если код-пароль ещё не задан
                                AnimatedVisibility(
                                    visible = !passcodeEnabled && showEnablePrompt && !isCreatingPasscode,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .safeNavigationBarPadding()
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                        .zIndex(50f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(scheme.islandBg)
                                            .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                                            .clickable { isCreatingPasscode = true }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(scheme.accentSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = null,
                                                tint = scheme.accent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isRu) "Включить код-пароль" else "Enable Passcode",
                                                color = scheme.textPrimary,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (isRu) {
                                                    "Защита чатов и шифрование базы"
                                                } else {
                                                    "Protect chats & encrypt local database"
                                                },
                                                color = scheme.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { showEnablePrompt = false },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = if (isRu) "Закрыть" else "Dismiss",
                                                tint = scheme.textMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (isCreatingPasscode) {
                                    PasscodeLockOverlay(
                                        mode = PasscodeScreenMode.CREATE,
                                        onSuccess = {
                                            passcodeEnabled = true
                                            isUnlocked = true
                                            showEnablePrompt = false
                                            isCreatingPasscode = false
                                        },
                                        onBack = { isCreatingPasscode = false },
                                        modifier = Modifier.zIndex(70f)
                                    )
                                }

                                if (passcodeEnabled && !isUnlocked) {
                                    PasscodeLockOverlay(
                                        mode = PasscodeScreenMode.UNLOCK,
                                        allowBack = false,
                                        onSuccess = { isUnlocked = true },
                                        onBack = {},
                                        modifier = Modifier.zIndex(80f)
                                    )
                                }
                            }
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

private fun parseAutoLockTimeoutMs(option: String): Long? {
    val norm = option.lowercase()
    return when {
        norm.contains("выкл") || norm.contains("disabled") -> null
        norm.contains("30") && (norm.contains("сек") || norm.contains("sec")) -> 30_000L
        norm.contains("15") -> 15 * 60_000L
        norm.contains("10") -> 10 * 60_000L
        norm.contains("5") -> 5 * 60_000L
        norm.contains("3") -> 3 * 60_000L
        norm.contains("2") -> 2 * 60_000L
        norm.contains("1") -> 60_000L
        else -> 60_000L
    }
}
