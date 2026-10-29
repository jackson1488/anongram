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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.features.attach.ui.theme.AttachPurple
import com.anongram.app.features.chats.ui.components.EmojiStickerPanel

/**
 * Плавающая нижняя панель ввода подписи:
 * - Появляется вместо панели вкладок, как только выбран хотя бы один элемент.
 * - Кнопка стикеров/эмодзи вместо счетчика выбора (как в чате, с открытием панели стикеров и эмодзи).
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
    modifier: Modifier = Modifier,
    isEmojiOpen: Boolean = false,
    onToggleEmoji: (() -> Unit)? = null,
    onCloseEmoji: (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var internalEmojiOpen by remember { mutableStateOf(false) }
    val effectiveEmojiOpen = onToggleEmoji?.let { isEmojiOpen } ?: internalEmojiOpen

    fun toggleEmoji() {
        if (onToggleEmoji != null) {
            onToggleEmoji()
        } else {
            internalEmojiOpen = !internalEmojiOpen
        }
    }

    fun closeEmoji() {
        if (onCloseEmoji != null) {
            onCloseEmoji()
        } else {
            internalEmojiOpen = false
        }
    }

    LaunchedEffect(selectedCount) {
        if (selectedCount == 0) {
            closeEmoji()
        }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    AnimatedVisibility(
        visible = visible && selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(240)) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)) + fadeOut(tween(160)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = if (effectiveEmojiOpen) 0.dp else navBottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = if (effectiveEmojiOpen) 8.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xF51E1E1E))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                        .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка эмодзи / стикеров (как в чате, вместо счетчика)
                    IconButton(
                        onClick = {
                            if (!effectiveEmojiOpen) {
                                keyboardController?.hide()
                            }
                            toggleEmoji()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (effectiveEmojiOpen) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfiedAlt,
                            contentDescription = "Стикеры и эмодзи",
                            tint = if (effectiveEmojiOpen) AttachPurple else Color(0xFF8E8E93),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

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
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && effectiveEmojiOpen) {
                                    closeEmoji()
                                }
                            }
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

            if (effectiveEmojiOpen) {
                EmojiStickerPanel(
                    onEmojiClick = { emoji -> onCaptionChange(caption + emoji) },
                    onStickerClick = { symbol, _ -> onCaptionChange(caption + symbol) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }
        }
    }
}
