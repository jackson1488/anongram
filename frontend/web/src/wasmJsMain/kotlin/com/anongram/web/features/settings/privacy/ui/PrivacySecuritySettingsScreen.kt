package com.anongram.web.features.settings.privacy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.settings.ui.SettingsCardDivider
import com.anongram.web.features.settings.ui.SettingsChoiceRow
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.features.settings.ui.SettingsToggleRow
import com.anongram.web.platform.WebStorage

@Composable
fun PrivacySecuritySettingsScreen(onBack: () -> Unit) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var lastSeenMode by remember { mutableStateOf(WebStorage.getString("anongram_privacy_last_seen") ?: "nobody") }
    var hideIpCalls by remember { mutableStateOf(WebStorage.getString("anongram_privacy_p2p_relay")?.toBooleanStrictOrNull() ?: true) }
    var readReceipts by remember { mutableStateOf(WebStorage.getString("anongram_privacy_read_receipts")?.toBooleanStrictOrNull() ?: true) }
    var sealingEnabled by remember { mutableStateOf(WebStorage.getString("anongram_privacy_sealed_sender")?.toBooleanStrictOrNull() ?: true) }

    SettingsSubScreenScaffold(
        title = if (isRu) "Конфиденциальность" else "Privacy",
        onBack = onBack
    ) {
        SettingsGroupCard(title = if (isRu) "Статус в сети и время захода" else "Last Seen & Online") {
            val modes = listOf(
                "everybody" to (if (isRu) "Все" else "Everybody"),
                "contacts" to (if (isRu) "Только контакты" else "Contacts"),
                "nobody" to (if (isRu) "Никто (Режим призрака)" else "Nobody (Stealth)")
            )
            modes.forEachIndexed { index, (id, label) ->
                SettingsChoiceRow(
                    title = label,
                    selected = lastSeenMode == id,
                    onClick = {
                        lastSeenMode = id
                        WebStorage.putString("anongram_privacy_last_seen", id)
                    }
                )
                if (index < modes.lastIndex) SettingsCardDivider()
            }
        }

        SettingsGroupCard(title = if (isRu) "Сетевая приватность" else "Network Privacy") {
            SettingsToggleRow(
                title = if (isRu) "Скрывать IP в звонках (Relay)" else "Hide IP in calls (Relay)",
                subtitle = if (isRu) "Маршрутизация через анонимные узлы" else "Route calls through anonymous relays",
                checked = hideIpCalls,
                onCheckedChange = {
                    hideIpCalls = it
                    WebStorage.putString("anongram_privacy_p2p_relay", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Отчёты о прочтении" else "Read receipts",
                subtitle = if (isRu) "Отправлять двойную галочку при просмотре" else "Send double-check mark when reading",
                checked = readReceipts,
                onCheckedChange = {
                    readReceipts = it
                    WebStorage.putString("anongram_privacy_read_receipts", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = "Sealed Sender (Zero-Metadata)",
                subtitle = if (isRu) "Сервер не видит отправителя и граф связей" else "Server cannot inspect sender or social graph",
                checked = sealingEnabled,
                onCheckedChange = {
                    sealingEnabled = it
                    WebStorage.putString("anongram_privacy_sealed_sender", it.toString())
                }
            )
        }

        SettingsGroupCard(title = "AnonGram Core Protocol") {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(
                    text = if (isRu) "Анонимная пересылка: 100% (Всегда)" else "Anonymous forwarding: 100% (Always)",
                    color = scheme.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isRu) {
                        "Пересланные сообщения никогда не содержат ссылку на автора (forwardedFrom = null)."
                    } else {
                        "Forwarded messages never include original sender attribution (forwardedFrom = null)."
                    },
                    color = scheme.textSecondary,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
