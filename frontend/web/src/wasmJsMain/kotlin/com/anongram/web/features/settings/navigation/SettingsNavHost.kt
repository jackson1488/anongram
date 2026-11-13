package com.anongram.web.features.settings.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.anongram.web.features.auth.domain.model.UserIdentity
import com.anongram.web.features.settings.account.ui.AccountSettingsScreen
import com.anongram.web.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.web.features.settings.animations.ui.AnimationsSettingsScreen
import com.anongram.web.features.settings.animations.ui.messages.MessageAnimationsSettingsScreen
import com.anongram.web.features.settings.battery.ui.BatterySettingsScreen
import com.anongram.web.features.settings.chat_settings.ui.ChatAppearanceSettingsScreen
import com.anongram.web.features.settings.data_storage.ui.DataStorageSettingsScreen
import com.anongram.web.features.settings.devices.ui.DevicesSettingsScreen
import com.anongram.web.features.settings.language.ui.LanguageSettingsScreen
import com.anongram.web.features.settings.main.ui.SettingsMainScreen
import com.anongram.web.features.settings.modules.ui.ModulesSettingsScreen
import com.anongram.web.features.settings.modules.ui.TranslationModuleSettingsScreen
import com.anongram.web.features.settings.modules.ui.VoiceModuleSettingsScreen
import com.anongram.web.features.settings.notifications.ui.NotificationsSettingsScreen
import com.anongram.web.features.settings.privacy.ui.PrivacySecuritySettingsScreen
import com.anongram.web.features.settings.qr.ui.MyQrCodeScreen
import com.anongram.web.features.settings.search.ui.SettingsSearchScreen
import com.anongram.web.features.settings.security.ui.SecuritySettingsScreen
import kotlin.math.roundToInt

@Composable
fun SettingsNavHost(
    identity: UserIdentity?,
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onLogout: () -> Unit = {},
    onSubScreenVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var customDisplayName by remember(identity?.nickname) {
        mutableStateOf(identity?.nickname?.takeIf { it.isNotBlank() } ?: "Пользователь")
    }
    var customHandle by remember(customDisplayName) {
        mutableStateOf(customDisplayName.replace(" ", "").uppercase().take(8))
    }
    val publicKeyHex = identity?.masterVkHex ?: "8f91c2a0b47d12e8903fa12b94c8e391"

    val animState by AnimationsSettingsStore.state.collectAsState()
    LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized()
    }

    val backStack = remember { mutableStateListOf<SettingsRoute>(SettingsRoute.Main) }
    val currentRoute = backStack.lastOrNull() ?: SettingsRoute.Main

    val navigateTo: (SettingsRoute) -> Unit = { route ->
        backStack.add(route)
        onSubScreenVisibilityChange(route != SettingsRoute.Main)
    }

    val popBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        onSubScreenVisibilityChange((backStack.lastOrNull() ?: SettingsRoute.Main) != SettingsRoute.Main)
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (!animState.screenTransitionsEnabled || animState.reduceMotionEnabled) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                val durationMs = (260f * animState.speedMultiplier).roundToInt().coerceIn(70, 700)
                if (targetState == SettingsRoute.Main) {
                    (slideInHorizontally(tween(durationMs)) { -it / 3 } + fadeIn(tween(durationMs))) togetherWith
                        (slideOutHorizontally(tween(durationMs)) { it } + fadeOut(tween(durationMs)))
                } else {
                    (slideInHorizontally(tween(durationMs)) { it } + fadeIn(tween(durationMs))) togetherWith
                        (slideOutHorizontally(tween(durationMs)) { -it / 3 } + fadeOut(tween(durationMs)))
                }
            }
        },
        label = "settingsNavTransition",
        modifier = modifier
    ) { route ->
        when (route) {
            SettingsRoute.Main -> SettingsMainScreen(
                identity = identity?.copy(nickname = customDisplayName),
                onNavigate = navigateTo,
                onLogout = onLogout
            )
            SettingsRoute.Search -> SettingsSearchScreen(
                onNavigate = { target ->
                    backStack.removeAt(backStack.lastIndex)
                    navigateTo(target)
                },
                onBack = popBack
            )
            SettingsRoute.MyQr -> MyQrCodeScreen(
                handle = customHandle,
                publicKeyHex = publicKeyHex,
                onClose = popBack
            )
            SettingsRoute.Account -> AccountSettingsScreen(
                identity = identity,
                displayName = customDisplayName,
                handle = customHandle,
                onProfileSaved = { newName, newHandle ->
                    customDisplayName = newName
                    customHandle = newHandle
                },
                onBack = popBack
            )
            SettingsRoute.ChatSettings -> ChatAppearanceSettingsScreen(
                currentThemeId = currentThemeId,
                onThemeSelect = onThemeSelect,
                onBack = popBack
            )
            SettingsRoute.Privacy -> PrivacySecuritySettingsScreen(onBack = popBack)
            SettingsRoute.Security -> SecuritySettingsScreen(onBack = popBack)
            SettingsRoute.Notifications -> NotificationsSettingsScreen(onBack = popBack)
            SettingsRoute.DataStorage -> DataStorageSettingsScreen(onBack = popBack)
            SettingsRoute.Animations -> AnimationsSettingsScreen(
                onOpenMessagesAnimations = { navigateTo(SettingsRoute.AnimationsMessages) },
                onBack = popBack
            )
            SettingsRoute.AnimationsMessages -> MessageAnimationsSettingsScreen(onBack = popBack)
            SettingsRoute.Devices -> DevicesSettingsScreen(onBack = popBack)
            SettingsRoute.Battery -> BatterySettingsScreen(onBack = popBack)
            SettingsRoute.Language -> LanguageSettingsScreen(onBack = popBack)
            SettingsRoute.Modules -> ModulesSettingsScreen(
                onOpenVoice = { navigateTo(SettingsRoute.ModulesVoice) },
                onOpenTranslation = { navigateTo(SettingsRoute.ModulesTranslation) },
                onBack = popBack
            )
            SettingsRoute.ModulesVoice -> VoiceModuleSettingsScreen(onBack = popBack)
            SettingsRoute.ModulesTranslation -> TranslationModuleSettingsScreen(onBack = popBack)
        }
    }
}
