package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

/**
 * Экран/диалог настроек уведомлений чата (шаг 8):
 * - Основные: Показывать текст, Истории, Звук, Вибросигнал, Приоритет
 * - Всплывающие: Включены / Отключены (radio)
 * - Звонки: вибросигнал, рингтон
 * - Светодиод: цвет
 * - Кнопка «Сбросить настройки» (красная).
 */
@Composable
internal fun NotificationsSettingsDialog(
    onDismiss: () -> Unit
) {
    var showText by remember { mutableStateOf(true) }
    var stories by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var popupsEnabled by remember { mutableStateOf(true) }
    var ledColor by remember { mutableStateOf("Фиолетовый") }

    val scheme = LocalAnonGramScheme.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Особые уведомления",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Закрыть", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SectionTitle("ОСНОВНЫЕ")
            SettingSwitchRow("Показывать текст", showText) { showText = it }
            SettingSwitchRow("Истории", stories) { stories = it }
            SettingRow("Звук", if (soundEnabled) "По умолчанию" else "Выкл") { soundEnabled = !soundEnabled }
            SettingRow("Вибросигнал", if (vibrateEnabled) "По умолчанию" else "Выкл") { vibrateEnabled = !vibrateEnabled }
            SettingRow("Приоритет", "Срочные") {}

            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

            SectionTitle("ВСПЛЫВАЮЩИЕ УВЕДОМЛЕНИЯ")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { popupsEnabled = true }) {
                RadioButton(
                    selected = popupsEnabled,
                    onClick = { popupsEnabled = true },
                    colors = RadioButtonDefaults.colors(selectedColor = scheme.accent)
                )
                Text(text = "Включены", color = TextPrimary, fontSize = 13.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { popupsEnabled = false }) {
                RadioButton(
                    selected = !popupsEnabled,
                    onClick = { popupsEnabled = false },
                    colors = RadioButtonDefaults.colors(selectedColor = scheme.accent)
                )
                Text(text = "Отключены", color = TextPrimary, fontSize = 13.sp)
            }

            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

            SectionTitle("ЗВОНКИ")
            SettingRow("Вибросигнал", "По умолчанию") {}
            SettingRow("Рингтон", "AnonTone (E2EE)") {}

            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 10.dp))

            SectionTitle("СВЕТОДИОД")
            SettingRow("Цвет", ledColor) {
                ledColor = if (ledColor == "Фиолетовый") "Бирюзовый" else "Фиолетовый"
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    showText = true
                    stories = false
                    soundEnabled = true
                    vibrateEnabled = true
                    popupsEnabled = true
                    ledColor = "Фиолетовый"
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Сбросить настройки", color = FlameOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = LocalAnonGramScheme.current.accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = scheme.accent)
        )
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = TextSecondary, fontSize = 12.sp)
    }
}
