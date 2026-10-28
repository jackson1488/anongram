package com.anongram.app.features.attach.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.features.attach.ui.theme.AttachPurple

/**
 * Плавающая нижняя панель ввода подписи (ТЗ, шаг 11 и шаг 12):
 * - Появляется вместо панели вкладок, как только выбран хотя бы один элемент.
 * - Счётчик выбранных в бейдже.
 * - Поле "Добавить подпись...", текст передается в драфт.
 * - Круглая фиолетовая кнопка отправки.
 * - Поддерживает imePadding: при открытии клавиатуры поднимается над ней.
 */
@Composable
fun AttachInputBar(
    caption: String,
    onCaptionChange: (String) -> Unit,
    selectedCount: Int,
    visible: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    AnimatedVisibility(
        visible = visible && selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(240)) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(160)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = navBottom),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xF5131D31))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Счётчик выбранных элементов
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AttachPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedCount.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Поле ввода "Добавить подпись..."
                BasicTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(AttachPurple),
                    decorationBox = { innerTextField ->
                        if (caption.isEmpty()) {
                            Text(
                                text = "Добавить подпись...",
                                color = Color(0xFF64748B),
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Круглая фиолетовая кнопка отправки
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AttachPurple)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
