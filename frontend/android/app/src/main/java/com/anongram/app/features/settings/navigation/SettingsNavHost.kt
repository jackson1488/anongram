package com.anongram.app.features.settings.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.core.navigation.AnonGramTransitionScreen
import com.anongram.app.core.navigation.PhysicsSprings
import com.anongram.app.core.navigation.TransitionType
import com.anongram.app.core.navigation.swipeBackGesture
import kotlin.coroutines.cancellation.CancellationException
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.settings.account.ui.AccountSettingsScreen
import com.anongram.app.features.settings.battery.ui.BatterySettingsScreen
import com.anongram.app.features.settings.chat_settings.ui.ChatAppearanceSettingsScreen
import com.anongram.app.features.settings.data_storage.ui.DataStorageSettingsScreen
import com.anongram.app.features.settings.devices.ui.DevicesSettingsScreen
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.animations.ui.AnimationsSettingsScreen
import com.anongram.app.features.settings.animations.ui.messages.MessageAnimationsSettingsScreen
import com.anongram.app.features.settings.animations.ui.transitions.ScreenTransitionsSettingsScreen
import com.anongram.app.features.settings.language.ui.LanguageSettingsScreen
import com.anongram.app.features.settings.main.ui.SettingsMainScreen
import com.anongram.app.features.settings.modules.ui.ModulesSettingsScreen
import com.anongram.app.features.settings.modules.ui.translation.TranslationModuleSettingsScreen
import com.anongram.app.features.settings.modules.ui.voice.VoiceModuleSettingsScreen
import com.anongram.app.features.settings.notifications.ui.NotificationsSettingsScreen
import com.anongram.app.features.settings.privacy.ui.PrivacySecuritySettingsScreen
import com.anongram.app.features.settings.security.ui.SecuritySettingsScreen
import com.anongram.app.features.settings.search.ui.SettingsSearchScreen
import com.anongram.app.features.settings.qr.ui.MyQrCodeScreen
import com.anongram.app.features.settings.qr.ui.QrCameraScannerScreen
import kotlinx.coroutines.launch

/**
 * Навигационный хост раздела «Настройки» (`features/settings/`).
 * Обеспечивает плавные переходы между Главным меню, QR-хабом и всеми подразделами
 * через единый движок [AnonGramTransitionScreen].
 */
@Composable
fun SettingsNavHost(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onLogout: () -> Unit = {},
    onSubScreenVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vault = remember { KeystoreVault(context) }
    val identity = remember { vault.loadIdentity() }

    var customDisplayName by remember {
        mutableStateOf(identity?.nickname?.takeIf { it.isNotBlank() } ?: "Пользователь")
    }
    var customHandle by remember {
        mutableStateOf(customDisplayName.replace(" ", "").uppercase().take(8))
    }
    val publicKeyHex = identity?.masterVkHex ?: "8f91c2a0b47d12e8903fa12b94c8e391"

    val animState by AnimationsSettingsStore.state.collectAsState()
    LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized(context)
    }

    var predictiveSwipeEdge by remember { mutableStateOf(0) }
    var predictiveTouchYFraction by remember { mutableStateOf(0.5f) }
    var isPredictiveHolding by remember { mutableStateOf(false) }
    val navConfig = remember(animState, predictiveSwipeEdge, predictiveTouchYFraction, isPredictiveHolding) {
        animState.toNavigationTransitionConfig().copy(
            swipeEdge = predictiveSwipeEdge,
            touchYFraction = predictiveTouchYFraction,
            isPredictiveHolding = isPredictiveHolding
        )
    }
    val backStack = remember { mutableStateListOf<SettingsRoute>(SettingsRoute.Main) }
    val currentRoute = backStack.lastOrNull() ?: SettingsRoute.Main
    var underlayRoute by remember { mutableStateOf<SettingsRoute?>(null) }
    var isExiting by remember { mutableStateOf(false) }
    val transitionProgress = remember { Animatable(0f) }

    val navigateTo: (SettingsRoute) -> Unit = { route ->
        if (currentRoute != route) {
            val prev = currentRoute
            underlayRoute = prev
            isExiting = false
            backStack.add(route)
            onSubScreenVisibilityChange(route != SettingsRoute.Main)
            scope.launch {
                val skipEnter = navConfig.transitionType == TransitionType.INSTANT ||
                    (navConfig.transitionType.isBidirectional && !navConfig.enableEnterAnimation && !navConfig.enableBaseSmoothEnter) ||
                    (!navConfig.transitionType.isBidirectional && !navConfig.enableBaseSmoothEnter)
                if (skipEnter) {
                    transitionProgress.snapTo(0f)
                } else {
                    transitionProgress.snapTo(1f)
                    androidx.compose.runtime.withFrameNanos { }
                    transitionProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = if (navConfig.transitionType == TransitionType.SPRING_SWIPE) {
                            PhysicsSprings.screenSpringSpec(
                                damping = navConfig.springDamping,
                                stiffness = navConfig.springStiffness * navConfig.speedMultiplier
                            )
                        } else {
                            tween(durationMillis = navConfig.effectiveDurationMs)
                        }
                    )
                }
                underlayRoute = null
            }
        }
    }

    val popBack: () -> Unit = {
        if (backStack.size > 1) {
            val target = backStack[backStack.lastIndex - 1]
            underlayRoute = target
            isExiting = true
            scope.launch {
                if (navConfig.transitionType != TransitionType.INSTANT) {
                    transitionProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = if (navConfig.transitionType == TransitionType.SPRING_SWIPE) {
                            PhysicsSprings.screenSpringSpec(
                                damping = navConfig.springDamping,
                                stiffness = navConfig.springStiffness * navConfig.speedMultiplier
                            )
                        } else {
                            tween(durationMillis = navConfig.effectiveDurationMs)
                        }
                    )
                }
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
                transitionProgress.snapTo(0f)
                isExiting = false
                underlayRoute = null
                onSubScreenVisibilityChange((backStack.lastOrNull() ?: SettingsRoute.Main) != SettingsRoute.Main)
            }
        }
    }

    val cancelBackGesture: () -> Unit = {
        scope.launch {
            if (navConfig.transitionType != TransitionType.INSTANT) {
                transitionProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = if (navConfig.transitionType == TransitionType.SPRING_SWIPE) {
                        PhysicsSprings.screenSpringSpec(
                            damping = navConfig.springDamping,
                            stiffness = navConfig.springStiffness * navConfig.speedMultiplier
                        )
                    } else {
                        tween(durationMillis = navConfig.effectiveDurationMs)
                    }
                )
            } else {
                transitionProgress.snapTo(0f)
            }
            isExiting = false
            underlayRoute = null
        }
    }

    val usePredictiveBack = navConfig.transitionType == TransitionType.PREDICTIVE_BACK ||
        navConfig.enablePredictiveBackCombo
    androidx.activity.compose.BackHandler(enabled = backStack.size > 1 && !usePredictiveBack) {
        popBack()
    }
    PredictiveBackHandler(enabled = backStack.size > 1 && usePredictiveBack) { backEvents ->
        underlayRoute = backStack[backStack.lastIndex - 1]
        isExiting = true
        isPredictiveHolding = true
        val screenH = context.resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)
        try {
            backEvents.collect { event ->
                predictiveSwipeEdge = if (event.swipeEdge == 1) 1 else 0
                predictiveTouchYFraction = (event.touchY / screenH).coerceIn(0f, 1f)
                transitionProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            isPredictiveHolding = false
            popBack()
            predictiveSwipeEdge = 0
            predictiveTouchYFraction = 0.5f
        } catch (_: CancellationException) {
            isPredictiveHolding = false
            cancelBackGesture()
            predictiveSwipeEdge = 0
            predictiveTouchYFraction = 0.5f
        }
    }

    @Composable
    fun RenderRoute(route: SettingsRoute) {
        when (route) {
            SettingsRoute.Main -> {
                SettingsMainScreen(
                    onNavigate = navigateTo,
                    onLogout = onLogout
                )
            }

            SettingsRoute.Search -> {
                SettingsSearchScreen(
                    onNavigate = navigateTo,
                    onBack = popBack
                )
            }

            SettingsRoute.MyQr -> {
                MyQrCodeScreen(
                    handle = customHandle,
                    publicKeyHex = publicKeyHex,
                    onClose = popBack,
                    onOpenScanner = { navigateTo(SettingsRoute.QrScanner) }
                )
            }

            SettingsRoute.QrScanner -> {
                QrCameraScannerScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.Account -> {
                AccountSettingsScreen(
                    initialDisplayName = customDisplayName,
                    initialHandle = customHandle,
                    onSaveAccount = { newName, newHandle ->
                        customDisplayName = newName
                        customHandle = newHandle.uppercase()
                    },
                    onBack = popBack
                )
            }

            SettingsRoute.ChatSettings -> {
                ChatAppearanceSettingsScreen(
                    currentThemeId = currentThemeId,
                    onThemeSelect = onThemeSelect,
                    onBack = popBack
                )
            }

            SettingsRoute.Privacy -> {
                PrivacySecuritySettingsScreen(
                    publicKeyHex = publicKeyHex,
                    onBack = popBack
                )
            }

            SettingsRoute.Security -> {
                SecuritySettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.Notifications -> {
                NotificationsSettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.DataStorage -> {
                DataStorageSettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.Animations -> {
                AnimationsSettingsScreen(
                    onOpenMessagesAnimations = { navigateTo(SettingsRoute.AnimationsMessages) },
                    onOpenTransitionsAnimations = { navigateTo(SettingsRoute.AnimationsTransitions) },
                    onBack = popBack
                )
            }

            SettingsRoute.AnimationsMessages -> {
                MessageAnimationsSettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.AnimationsTransitions -> {
                ScreenTransitionsSettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.Devices -> {
                DevicesSettingsScreen(
                    onOpenQrScanner = { navigateTo(SettingsRoute.QrScanner) },
                    onBack = popBack
                )
            }

            SettingsRoute.Battery -> {
                BatterySettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.Language -> {
                LanguageSettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.Modules -> {
                ModulesSettingsScreen(
                    onOpenVoiceModule = { navigateTo(SettingsRoute.ModulesVoice) },
                    onOpenTranslationModule = { navigateTo(SettingsRoute.ModulesTranslation) },
                    onBack = popBack
                )
            }

            SettingsRoute.ModulesVoice -> {
                VoiceModuleSettingsScreen(
                    onBack = popBack
                )
            }

            SettingsRoute.ModulesTranslation -> {
                TranslationModuleSettingsScreen(
                    onBack = popBack
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        underlayRoute?.let { bgRoute ->
            Box(modifier = Modifier.fillMaxSize()) {
                RenderRoute(bgRoute)
            }
        }

        AnonGramTransitionScreen(
            progress = transitionProgress.value,
            isExit = isExiting,
            config = navConfig,
            modifier = Modifier.swipeBackGesture(
                enabled = backStack.size > 1 && navConfig.enableGesturePullCombo,
                onDragStart = {
                    if (backStack.size > 1) {
                        underlayRoute = backStack[backStack.lastIndex - 1]
                        isExiting = true
                    }
                },
                onDragProgress = { p -> scope.launch { transitionProgress.snapTo(p) } },
                onDragEnd = { shouldClose ->
                    if (shouldClose) popBack() else cancelBackGesture()
                }
            )
        ) {
            RenderRoute(currentRoute)
        }
    }
}
