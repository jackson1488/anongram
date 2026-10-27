package com.anongram.app.features.wallpapers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CyberCyan
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.wallpapers.model.ThemePreset
import com.anongram.app.features.wallpapers.ui.ThemePatternCanvas

/**
 * Карточка выбора готовой темы или карточка «Без темы».
 */
@Composable
fun ThemeCard(
    preset: ThemePreset?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (isSelected) CyberCyan else Color(0xFF222C3D)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(124.dp)
                .clip(shape)
                .background(Color(0xFF131822))
                .border(borderWidth, borderColor, shape)
        ) {
            if (preset == null) {
                // Карточка «Без темы» (красный крест)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D161A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Без темы",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                // Карточка темы с миниатюрным Canvas-паттерном и пузырями
                ThemePatternCanvas(
                    preset = preset,
                    modifier = Modifier.fillMaxSize()
                )

                // Миниатюрное превью пузырей сообщений
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Входящий пузырь
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1F293D).copy(alpha = 0.85f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Исходящий пузырь с акцентным цветом темы
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .width(52.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(preset.primaryColor, preset.secondaryColor)
                                )
                            )
                    )
                }

                // Эмодзи-бейдж по центру
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.emojiBadge,
                        fontSize = 12.sp
                    )
                }
            }

            // Градиентная подложка снизу для текста
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = preset?.title ?: "Без темы",
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
