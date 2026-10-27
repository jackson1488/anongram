package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage

/**
 * Реакции под пузырём (шаг 11): чипы «эмодзи + счётчик»; тап ставит/убирает
 * свою реакцию, активная подсвечена акцентом темы.
 */
@Composable
internal fun ReactionChips(
    message: ChatMessage,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (message.reactions.isEmpty()) return
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier.padding(top = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        message.reactions.entries.sortedBy { it.key }.forEach { (emoji, count) ->
            val isMine = message.myReaction == emoji
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isMine) scheme.accent.copy(alpha = 0.25f) else CardDark)
                    .clickable { onToggle(emoji) }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = emoji, fontSize = 12.sp)
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        color = if (isMine) scheme.accent else TextSecondary
                    )
                }
            }
        }
    }
}

/** Разделитель «Непрочитанные сообщения» (шаг 13). */
@Composable
internal fun UnreadDivider(modifier: Modifier = Modifier) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(TextSecondary.copy(alpha = 0.3f))
        )
        Text(
            text = "Непрочитанные сообщения",
            color = scheme.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(TextSecondary.copy(alpha = 0.3f))
        )
    }
}
