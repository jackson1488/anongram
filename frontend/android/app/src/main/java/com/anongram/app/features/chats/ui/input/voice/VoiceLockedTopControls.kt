package com.anongram.app.features.chats.ui.input.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
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

/**
 * Плавающая верхняя плашка над строкой ввода при заблокированной записи голосового (Фото 1 и 3 в стиле Telegram):
 * Справа: круглая кнопка паузы [ ❙❙ ] и тултип «Нажмите для обрезки или прослушивания».
 */
@Composable
fun VoiceLockedTopControls(
    onPauseReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 22.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Всплывающий тултип слева от кнопки паузы
            val scheme = com.anongram.app.core.theme.LocalAnonGramScheme.current
            val strings = com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings.current
            AnimatedVisibility(
                visible = showTooltip,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.bannerBg)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(12.dp))
                        .clickable { showTooltip = false }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = strings.tapToTrimOrListen,
                        color = scheme.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Круглая кнопка паузы [ ❙❙ ]
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(scheme.islandBg.copy(alpha = 0.96f))
                    .border(1.dp, scheme.islandBorder, CircleShape)
                    .clickable {
                        showTooltip = false
                        onPauseReview()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = strings.pauseOrTrim,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
