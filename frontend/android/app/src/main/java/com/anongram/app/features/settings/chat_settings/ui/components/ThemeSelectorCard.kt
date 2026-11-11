package com.anongram.app.features.settings.chat_settings.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.AnonGramScheme
import com.anongram.app.core.theme.AnonGramThemes

/**
 * Карточка выбора цветовой темы с интерактивным живым превью сообщений и плавной анимацией перехода цветов.
 * Изолированный компонент модуля `features/settings/chat_settings/`.
 */
@Composable
fun ThemeSelectorCard(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = AnonGramThemes.byId(currentThemeId)

    // Плавная анимация изменения акцентных цветов при переключении темы
    val animatedAccent by animateColorAsState(
        targetValue = activeTheme.accent,
        animationSpec = tween(durationMillis = 350),
        label = "themeAccentAnimation"
    )
    val animatedAccentDim by animateColorAsState(
        targetValue = activeTheme.accentDim,
        animationSpec = tween(durationMillis = 350),
        label = "themeAccentDimAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141318))
            .padding(16.dp)
    ) {
        Text(
            text = "Цветовая тема оформления",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Выберите акцентный стиль интерфейса и пузырей сообщений",
            color = Color(0xFF8E99A8),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Интерактивное живое превью сообщений с анимированным градиентом
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0C0B10))
                .padding(14.dp)
        ) {
            // Входящее сообщение
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp))
                        .background(Color(0xFF1E1C24))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = "AnonGram 100% анонимность",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Пересылка без следов и плашек 🛡️",
                            color = Color(0xFFBAC2CD),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Исходящее сообщение с живым анимированным акцентом выбранной темы
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(animatedAccent, animatedAccentDim)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Тема: ${activeTheme.displayName}",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Квантовая защита ML-DSA-87 ✓✓",
                            color = Color.Black.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(12.dp))

        // Список всех 10 тем со скроллом и анимацией выделения
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnonGramThemes.all.forEach { theme ->
                ThemeItemRow(
                    theme = theme,
                    isSelected = theme.id == currentThemeId,
                    onSelect = { onThemeSelect(theme.id) }
                )
            }
        }
    }
}

@Composable
private fun ThemeItemRow(
    theme: AnonGramScheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) theme.accent else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "themeItemBorder"
    )
    val animatedBackground by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
        animationSpec = tween(durationMillis = 250),
        label = "themeItemBackground"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBackground)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) animatedBorderColor else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Круговой индикатор цветов темы
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(theme.accent, theme.accentDim)
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = theme.displayName,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(theme.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Выбрано",
                    tint = Color.Black,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
