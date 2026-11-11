package com.anongram.app.features.settings.animations.ui.components

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

import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Карточка настроек производительности и скорости анимаций.
 * Изолированный компонент раздела `features/settings/animations/`.
 */
@Composable
fun AnimationsPerformanceCard(
    reduceMotionEnabled: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
    animationSpeedOption: String,
    onAnimationSpeedOptionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val speedOptions = listOf("0.5x", "1.0x", "1.5x", "2.0x")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141318))
            .padding(16.dp)
    ) {
        Text(
            text = "Производительность",
            color = scheme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Уменьшение движения (Reduce Motion)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Уменьшение движения",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Отключить сложные эффекты для экономии энергии и быстродействия",
                    color = Color(0xFF8E99A8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = reduceMotionEnabled,
                onCheckedChange = onReduceMotionChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = scheme.accent,
                    uncheckedThumbColor = Color(0xFFB0B0B0),
                    uncheckedTrackColor = Color(0xFF333333)
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(12.dp))

        // 2. Скорость анимаций интерфейса
        Text(
            text = "Скорость анимаций интерфейса",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Множитель длительности переходов между окнами и меню",
            color = Color(0xFF8E99A8),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (opt in speedOptions) {
                val isSelected = opt == animationSpeedOption
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) scheme.accent else Color.White.copy(alpha = 0.08f)
                        )
                        .clickable { onAnimationSpeedOptionChange(opt) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
