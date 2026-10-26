package com.anongram.app.features.chats.messages.ui.components

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary

/**
 * Поле ввода (как в Telegram): пилюля со скрепкой, текстом и эмодзи-иконкой;
 * справа круглая кнопка — самолётик при набранном тексте, микрофон при пустом.
 */
@Composable
internal fun ChatInputBar(
    input: String,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = "Прикрепить",
                    tint = TextMuted,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(21.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    ),
                    cursorBrush = SolidColor(scheme.accent),
                    maxLines = 4,
                    decorationBox = { innerField ->
                        if (input.isEmpty()) {
                            Text(
                                text = "Сообщение",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                        innerField()
                    }
                )

                Icon(
                    imageVector = Icons.Outlined.Mood,
                    contentDescription = "Эмодзи",
                    tint = TextMuted,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(21.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val buttonBackground = if (canSend) scheme.accent else SurfaceDark
        val buttonBorder = if (canSend) scheme.accent else CardBorder

        Box(
            modifier = Modifier
                .padding(bottom = 3.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(buttonBackground)
                .border(1.dp, buttonBorder, CircleShape)
                .then(if (canSend) Modifier.clickable(onClick = onSend) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (canSend) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "Отправить",
                    tint = ObsidianBlack,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 2.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Голосовое сообщение",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
