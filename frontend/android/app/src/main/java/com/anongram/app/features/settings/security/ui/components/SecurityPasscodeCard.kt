package com.anongram.app.features.settings.security.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.anongram.app.core.theme.LocalAnonGramScheme

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
    val scheme = LocalAnonGramScheme.current
    val autoLockOptions = listOf("Сразу", "1 мин", "5 мин", "1 час")
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = scheme.onAccent,
        checkedTrackColor = scheme.accent,
        uncheckedThumbColor = scheme.textSecondary,
        uncheckedTrackColor = scheme.surface,
        uncheckedBorderColor = scheme.cardBorder
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
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
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Запрашивать PIN при входе в AnonGram",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = passcodeEnabled,
                onCheckedChange = onPasscodeChange,
                colors = switchColors
            )
        }

        AnimatedVisibility(visible = passcodeEnabled) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = scheme.divider)
                Spacer(modifier = Modifier.height(12.dp))

                // Разблокировка по биометрии
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Разблокировка по отпечатку / Face ID",
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Использовать сканер для быстрого доступа",
                            color = scheme.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = onBiometricsChange,
                        colors = switchColors
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = scheme.divider)
                Spacer(modifier = Modifier.height(12.dp))

                // Выбор времени автоблокировки
                Text(
                    text = "Автоблокировка",
                    color = scheme.textPrimary,
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
                                .background(if (isSelected) scheme.accent else scheme.surface)
                                .border(
                                    1.dp,
                                    if (isSelected) scheme.accent else scheme.cardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onAutoLockOptionChange(opt) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) scheme.onAccent else scheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // Защита от снимков экрана
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Защита от снимков экрана",
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Блокировать скриншоты и предпросмотр в диспетчере задач",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = screenProtectionEnabled,
                onCheckedChange = onScreenProtectionChange,
                colors = switchColors
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // Двухэтапная аутентификация (2FA)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Двухэтапная аутентификация (2FA)",
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Облачный пароль для защиты при повторном входе",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = twoFactorEnabled,
                onCheckedChange = onTwoFactorChange,
                colors = switchColors
            )
        }
    }
}
