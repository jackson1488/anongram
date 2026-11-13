package com.anongram.app.features.settings.animations.ui.components

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
 * Карточка настройки визуальных эффектов и системных анимаций AnonGram.
 * Изолированный компонент раздела `features/settings/animations/`.
 */
@Composable
fun AnimationsEffectsCard(
    screenTransitionsEnabled: Boolean,
    onScreenTransitionsChange: (Boolean) -> Unit,
    glassBlurEnabled: Boolean,
    onGlassBlurChange: (Boolean) -> Unit,
    stickersLoopEnabled: Boolean,
    onStickersLoopChange: (Boolean) -> Unit,
    reactionsEffectsEnabled: Boolean,
    onReactionsEffectsChange: (Boolean) -> Unit,
    animatedBackgroundsEnabled: Boolean,
    onAnimatedBackgroundsChange: (Boolean) -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Визуальные эффекты",
            color = scheme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Плавные переходы между экранами
        AnimationToggleRow(
            title = "Плавные переходы экранов",
            subtitle = "Жестовые сдвиги и анимации прозрачности при навигации",
            checked = screenTransitionsEnabled,
            onCheckedChange = onScreenTransitionsChange,
            colors = switchColors
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 2. Размытие iPhone Glass
        AnimationToggleRow(
            title = "Размытие интерфейса (Glass)",
            subtitle = "Эффект матового стекла для шапок, меню и шторки ввода",
            checked = glassBlurEnabled,
            onCheckedChange = onGlassBlurChange,
            colors = switchColors
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 3. Зацикленные стикеры и эмодзи
        AnimationToggleRow(
            title = "Анимированные стикеры и эмодзи",
            subtitle = "Плавное непрерывное воспроизведение стикеров в чатах",
            checked = stickersLoopEnabled,
            onCheckedChange = onStickersLoopChange,
            colors = switchColors
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 4. Эффекты реакций
        AnimationToggleRow(
            title = "Эффекты реакций",
            subtitle = "Всплески частиц при отправке реакций на сообщения",
            checked = reactionsEffectsEnabled,
            onCheckedChange = onReactionsEffectsChange,
            colors = switchColors
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 5. Динамические градиентные фоны
        AnimationToggleRow(
            title = "Живые фоны сообщений",
            subtitle = "Плавное переливание градиентных оттенков в диалогах",
            checked = animatedBackgroundsEnabled,
            onCheckedChange = onAnimatedBackgroundsChange,
            colors = switchColors
        )
    }
}

@Composable
private fun AnimationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: androidx.compose.material3.SwitchColors,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = scheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = colors
        )
    }
}
