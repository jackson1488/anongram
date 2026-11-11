package com.anongram.app.features.settings.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.settings.account.ui.AccountSettingsScreen
import com.anongram.app.features.settings.battery.ui.BatterySettingsScreen
import com.anongram.app.features.settings.chat_settings.ui.ChatAppearanceSettingsScreen
import com.anongram.app.features.settings.data_storage.ui.DataStorageSettingsScreen
import com.anongram.app.features.settings.devices.ui.DevicesSettingsScreen
import com.anongram.app.features.settings.folders.ui.ChatFoldersSettingsScreen
import com.anongram.app.features.settings.language.ui.LanguageSettingsScreen
import com.anongram.app.features.settings.main.ui.SettingsMainScreen
import com.anongram.app.features.settings.notifications.ui.NotificationsSettingsScreen
import com.anongram.app.features.settings.privacy.ui.PrivacySecuritySettingsScreen
import com.anongram.app.features.settings.qr.ui.MyQrCodeScreen
import com.anongram.app.features.settings.qr.ui.QrCameraScannerScreen
import com.anongram.app.features.settings.qr.ui.QrGalleryPickerScreen

/**
 * Навигационный хост раздела «Настройки» (`features/settings/`).
 * Обеспечивает плавные переходы между Главным меню, QR-хабом и всеми 9 изолированными подразделами.
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
    val vault = remember { KeystoreVault(context) }
    val identity = remember { vault.loadIdentity() }

    var customDisplayName by remember {
        mutableStateOf(identity?.nickname?.takeIf { it.isNotBlank() } ?: "Кайрат")
    }
    var customHandle by remember {
        mutableStateOf(customDisplayName.replace(" ", "").uppercase().take(8))
    }
    val publicKeyHex = identity?.masterVkHex ?: "8f91c2a0b47d12e8903fa12b94c8e391"

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

    BackHandler(enabled = backStack.size > 1) {
        popBack()
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = {
            if (targetState == SettingsRoute.Main) {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            }
        },
        label = "SettingsNavHostTransition",
        modifier = modifier
    ) { route ->
        when (route) {
            SettingsRoute.Main -> {
                SettingsMainScreen(
                    onNavigate = navigateTo,
                    onLogout = onLogout
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
                    onBack = popBack,
                    onOpenGallery = { navigateTo(SettingsRoute.QrGallery) }
                )
            }

            SettingsRoute.QrGallery -> {
                QrGalleryPickerScreen(
                    onBack = popBack,
                    onQrImageSelected = {
                        // После выбора изображения с QR возвращаемся в меню
                        while (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                        onSubScreenVisibilityChange(false)
                    }
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

            SettingsRoute.Folders -> {
                ChatFoldersSettingsScreen(
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
        }
    }
}
