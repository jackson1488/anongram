package com.anongram.app.features.chats.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding

/**
 * Полноэкранный раздел «Особые уведомления» в точном соответствии со стилем Telegram (Фото 1):
 * - Сверху стрелка «Назад», аватарка собеседника, реальное имя из чата и статус «Особые уведомления».
 * - Чистый интерфейс с секциями: Основные, Всплывающие, Звонки, Светодиод, Сбросить настройки.
 */
@Composable
internal fun NotificationsSettingsDialog(
    peerName: String,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    var showText by remember { mutableStateOf(true) }
    var stories by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var priority by remember { mutableStateOf("Как в настройках") }
    var popupsEnabled by remember { mutableStateOf(false) }
    var ledColorIndex by remember { mutableStateOf(0) }

    val ledColors = listOf(Color(0xFF00F2FE), Color(0xFF9C27B0), Color(0xFF10B981), Color(0xFFFF5722))
    val scheme = LocalAnonGramScheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Верхняя плавающая панель (Telegram-style: кнопка назад + аватар + имя чата)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Аватар собеседника
                val initials = peerName.take(2).uppercase().ifBlank { "CA" }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(scheme.accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = scheme.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = peerName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Особые уведомления",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Список настроек (Telegram Style чистый список)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Секция 1: Основные
                SectionHeader("Основные")
                TelegramSettingsCard {
                    SettingsSwitchItem("Показывать текст", showText) { showText = it }
                    SettingsDivider()
                    SettingsSwitchItem("Истории", stories) { stories = it }
                    SettingsDivider()
                    SettingsValueItem("Звук", if (soundEnabled) "По умолчанию" else "Выкл") {
                        soundEnabled = !soundEnabled
                    }
                    SettingsDivider()
                    SettingsValueItem("Вибросигнал", if (vibrateEnabled) "По умолчанию" else "Выкл") {
                        vibrateEnabled = !vibrateEnabled
                    }
                    SettingsDivider()
                    SettingsValueItem("Приоритет", priority) {
                        priority = if (priority == "Как в настройках") "Срочные" else "Как в настройках"
                    }
                }
                SectionFooter("Уведомления с максимальным приоритетом будут срабатывать, даже когда устройство в режиме «Не беспокоить».")

                Spacer(modifier = Modifier.height(16.dp))

                // Секция 2: Всплывающие уведомления
                SectionHeader("Всплывающие уведомления")
                TelegramSettingsCard {
                    SettingsRadioItem("Включены", popupsEnabled) { popupsEnabled = true }
                    SettingsDivider()
                    SettingsRadioItem("Отключены", !popupsEnabled) { popupsEnabled = false }
                }
                SectionFooter("Новые сообщения от этого контакта будут всплывать поверх экрана, когда Вы не в приложении.")

                Spacer(modifier = Modifier.height(16.dp))

                // Секция 3: Звонки
                SectionHeader("Звонки")
                TelegramSettingsCard {
                    SettingsValueItem("Вибросигнал", "По умолчанию") {}
                    SettingsDivider()
                    SettingsValueItem("Рингтон", "По умолчанию") {}
                }
                SectionFooter("Вы можете настроить рингтон, который используется, когда этот контакт звонит Вам в AnonGram.")

                Spacer(modifier = Modifier.height(16.dp))

                // Секция 4: Светодиод
                SectionHeader("Светодиод")
                TelegramSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ledColorIndex = (ledColorIndex + 1) % ledColors.size
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Цвет",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(ledColors[ledColorIndex])
                        )
                    }
                }
                SectionFooter("Маленький мерцающий индикатор на некоторых устройствах, используемый для уведомления о новых сообщениях.")

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка: Сбросить настройки (красная)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showText = true
                            stories = false
                            soundEnabled = true
                            vibrateEnabled = true
                            priority = "Как в настройках"
                            popupsEnabled = false
                            ledColorIndex = 0
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сбросить настройки",
                        color = Color(0xFFFF5252),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = LocalAnonGramScheme.current.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)
    )
}

@Composable
private fun TelegramSettingsCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = CardBorder,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 16.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = scheme.accent,
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF2C3240)
            )
        )
    }
}

@Composable
private fun SettingsValueItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = scheme.accent,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SettingsRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = scheme.accent,
                uncheckedColor = TextSecondary
            )
        )
    }
}
