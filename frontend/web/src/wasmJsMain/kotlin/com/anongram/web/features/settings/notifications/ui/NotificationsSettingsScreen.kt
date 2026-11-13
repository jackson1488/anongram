package com.anongram.web.features.settings.notifications.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.features.settings.ui.SettingsCardDivider
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.features.settings.ui.SettingsToggleRow
import com.anongram.web.platform.WebStorage

@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit) {
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var privateChats by remember { mutableStateOf(WebStorage.getString("anongram_notif_private")?.toBooleanStrictOrNull() ?: true) }
    var groups by remember { mutableStateOf(WebStorage.getString("anongram_notif_groups")?.toBooleanStrictOrNull() ?: true) }
    var calls by remember { mutableStateOf(WebStorage.getString("anongram_notif_calls")?.toBooleanStrictOrNull() ?: true) }
    var previews by remember { mutableStateOf(WebStorage.getString("anongram_notif_previews")?.toBooleanStrictOrNull() ?: false) }
    var sounds by remember { mutableStateOf(WebStorage.getString("anongram_notif_sounds")?.toBooleanStrictOrNull() ?: true) }
    var badgeCounter by remember { mutableStateOf(WebStorage.getString("anongram_notif_badge")?.toBooleanStrictOrNull() ?: true) }

    SettingsSubScreenScaffold(
        title = if (isRu) "Уведомления" else "Notifications",
        onBack = onBack
    ) {
        SettingsGroupCard(title = if (isRu) "Уведомления чатов" else "Chat Notifications") {
            SettingsToggleRow(
                title = if (isRu) "Личные чаты" else "Private chats",
                checked = privateChats,
                onCheckedChange = {
                    privateChats = it
                    WebStorage.putString("anongram_notif_private", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Группы" else "Groups",
                checked = groups,
                onCheckedChange = {
                    groups = it
                    WebStorage.putString("anongram_notif_groups", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Звонки" else "Calls",
                checked = calls,
                onCheckedChange = {
                    calls = it
                    WebStorage.putString("anongram_notif_calls", it.toString())
                }
            )
        }

        SettingsGroupCard(title = if (isRu) "Приватность и звук" else "Privacy & Sound") {
            SettingsToggleRow(
                title = if (isRu) "Предпросмотр текста" else "Message preview",
                subtitle = if (isRu) "Скрывать текст уведомления для анонимности" else "Hide message text for privacy",
                checked = previews,
                onCheckedChange = {
                    previews = it
                    WebStorage.putString("anongram_notif_previews", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Звук в приложении" else "In-app sounds",
                checked = sounds,
                onCheckedChange = {
                    sounds = it
                    WebStorage.putString("anongram_notif_sounds", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Счётчик непрочитанных" else "Unread badge counter",
                checked = badgeCounter,
                onCheckedChange = {
                    badgeCounter = it
                    WebStorage.putString("anongram_notif_badge", it.toString())
                }
            )
        }
    }
}
