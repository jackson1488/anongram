package com.anongram.app.features.settings.battery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Карточка настроек энергосбережения и плавных анимаций.
 * Изолированный компонент модуля `features/settings/battery/`.
 */
@Composable
fun BatterySaverCard(
    powerSaverEnabled: Boolean,
    onPowerSaverChange: (Boolean) -> Unit,
    smoothAnimationsEnabled: Boolean,
    onSmoothAnimationsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
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
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Режим энергосбережения",
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Снижать нагрузку на GPU при низком заряде батареи",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = powerSaverEnabled,
                onCheckedChange = onPowerSaverChange,
                colors = switchColors
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Плавные переходы и свечение",
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Неоновые эффекты и анимированные иконки",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = smoothAnimationsEnabled,
                onCheckedChange = onSmoothAnimationsChange,
                colors = switchColors
            )
        }
    }
}
