package com.anongram.app.features.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.features.settings.navigation.SettingsNavHost

/**
 * Корневой экран раздела «Настройки» (`features/settings/`).
 * Делегирует навигацию модульному графу [SettingsNavHost], объединяющему:
 * - Главное меню настроек (`main/`)
 * - QR-хаб с проверкой разрешений Камеры и Галереи (`qr/`)
 * - Изолированные подразделы (`account/`, `chat_settings/`, `privacy/`, `notifications/`,
 *   `data_storage/`, `folders/`, `devices/`, `battery/`, `language/`).
 */
@Composable
fun SettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onOpenLanguage: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSubScreenVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ChatTextSettingsStore.ensureInitialized(context)
        LanguageSettingsStore.ensureInitialized(context)
    }

    SettingsNavHost(
        currentThemeId = currentThemeId,
        onThemeSelect = onThemeSelect,
        onLogout = onLogout,
        onSubScreenVisibilityChange = onSubScreenVisibilityChange,
        modifier = modifier
    )
}
