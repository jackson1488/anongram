package com.anongram.app.features.settings.security.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Карточка настроек блокировки приложения, биометрии и защиты экрана.
 * Изолированный компонент раздела `features/settings/security/`.
 */
@Composable
fun SecurityPasscodeCard(
    passcodeEnabled: Boolean,
    onPasscodeChange: (Boolean) -> Unit,
    biometricsEnabled: Boolean,
    onBiometricsChange: (Boolean) -> Unit,
    autoLockOption: String,
    onAutoLockOptionChange: (String) -> Unit,
    screenProtectionEnabled: Boolean,
    onScreenProtectionChange: (Boolean) -> Unit,
    twoFactorEnabled: Boolean,
    onTwoFactorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val autoLockOptions = listOf("Сразу", "1 мин", "5 мин", "1 час")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141318))
            .padding(16.dp)
    ) {
        // 1. Код-пароль
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Код-пароль приложения",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Запрашивать PIN при входе в AnonGram",
                    color = Color(0xFF8E99A8),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = passcodeEnabled,
                onCheckedChange = onPasscodeChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF66BB6A)
                )
            )
        }

        AnimatedVisibility(visible = passcodeEnabled) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(12.dp))

                // Разблокировка по биометрии
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Разблокировка по отпечатку / Face ID",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Использовать сканер для быстрого доступа",
                            color = Color(0xFF8E99A8),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = onBiometricsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF66BB6A)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(12.dp))

                // Выбор времени автоблокировки
                Text(
                    text = "Автоблокировка",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (opt in autoLockOptions) {
                        val isSelected = opt == autoLockOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF66BB6A) else Color.White.copy(alpha = 0.08f))
                                .clickable { onAutoLockOptionChange(opt) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(12.dp))

        // Защита от снимков экрана
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Защита от снимков экрана",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Блокировать скриншоты и предпросмотр в диспетчере задач",
                    color = Color(0xFF8E99A8),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = screenProtectionEnabled,
                onCheckedChange = onScreenProtectionChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF66BB6A)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(12.dp))

        // Двухэтапная аутентификация (2FA)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Двухэтапная аутентификация (2FA)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Облачный пароль для защиты при повторном входе",
                    color = Color(0xFF8E99A8),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = twoFactorEnabled,
                onCheckedChange = onTwoFactorChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF66BB6A)
                )
            )
        }
    }
}
