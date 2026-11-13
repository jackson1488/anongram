package com.anongram.web.features.settings.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
fun SecuritySettingsScreen(onBack: () -> Unit) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var passcodeEnabled by remember { mutableStateOf(WebStorage.getString("anongram_sec_pin_enabled")?.toBooleanStrictOrNull() ?: false) }
    var autoLockMinutes by remember { mutableStateOf(WebStorage.getString("anongram_sec_autolock") ?: "5") }
    var quantumVerify by remember { mutableStateOf(WebStorage.getString("anongram_sec_pq_verify")?.toBooleanStrictOrNull() ?: true) }
    var pinDraft by remember { mutableStateOf("") }
    var showSeed by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val storedSeed = remember { WebStorage.getString("anongram_auth_seed") ?: "" }

    SettingsSubScreenScaffold(
        title = if (isRu) "Безопасность" else "Security",
        onBack = onBack
    ) {
        SettingsGroupCard(title = if (isRu) "Код-пароль" else "Passcode Lock") {
            SettingsToggleRow(
                title = if (isRu) "Блокировка код-паролем" else "Passcode Lock",
                subtitle = if (isRu) "Запрашивать PIN при входе в сессию" else "Require PIN when opening session",
                checked = passcodeEnabled,
                onCheckedChange = {
                    passcodeEnabled = it
                    WebStorage.putString("anongram_sec_pin_enabled", it.toString())
                }
            )
            if (passcodeEnabled) {
                SettingsCardDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surface)
                            .border(1.dp, scheme.cardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (pinDraft.isEmpty()) {
                            Text(
                                text = if (isRu) "Новый 4-значный PIN..." else "New 4-digit PIN...",
                                color = scheme.textMuted,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = pinDraft,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinDraft = it },
                            singleLine = true,
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.accent)
                            .clickable {
                                if (pinDraft.length >= 4) {
                                    WebStorage.putString("anongram_sec_pin_value", pinDraft)
                                    pinDraft = ""
                                    statusText = if (isRu) "PIN-код обновлён" else "PIN updated"
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isRu) "Задать" else "Set",
                            color = scheme.onAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (statusText != null) {
                    Text(
                        text = statusText!!,
                        color = scheme.accent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                }
            }
        }

        SettingsGroupCard(title = if (isRu) "Автоблокировка" else "Auto-Lock") {
            listOf(
                "1" to (if (isRu) "Через 1 минуту" else "In 1 minute"),
                "5" to (if (isRu) "Через 5 минут" else "In 5 minutes"),
                "60" to (if (isRu) "Через 1 час" else "In 1 hour")
            ).forEachIndexed { i, (value, label) ->
                SettingsChoiceRow(
                    title = label,
                    selected = autoLockMinutes == value,
                    onClick = {
                        autoLockMinutes = value
                        WebStorage.putString("anongram_sec_autolock", value)
                    }
                )
                if (i < 2) SettingsCardDivider()
            }
        }

        SettingsGroupCard(title = if (isRu) "Квантовая защита и резервная копия" else "Quantum Guard & Backup") {
            SettingsToggleRow(
                title = "ML-KEM-1024 + ML-DSA-87",
                subtitle = if (isRu) "Проверять пост-квантовые подписи собеседников" else "Verify peer post-quantum signatures",
                checked = quantumVerify,
                onCheckedChange = {
                    quantumVerify = it
                    WebStorage.putString("anongram_sec_pq_verify", it.toString())
                }
            )
            if (storedSeed.isNotBlank()) {
                SettingsCardDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSeed = !showSeed }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isRu) "Резервная Seed-фраза (12 слов)" else "Backup Seed Phrase (12 words)",
                        color = scheme.accent,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (showSeed) storedSeed else (if (isRu) "Нажмите, чтобы показать" else "Tap to reveal"),
                        color = if (showSeed) scheme.textPrimary else scheme.textSecondary,
                        fontSize = 13.sp,
                        fontFamily = if (showSeed) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }
    }
}
