package com.anongram.app.features.chats.input.reply

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.input.i18n.LocalInputStrings

/**
 * Слитно встроенная плашка ответа (Фото 1) — без разделительной полоски.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FusedReplyBar(
    replyTo: ChatMessage,
    peerName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val author = if (replyTo.direction == MessageDirection.INCOMING) peerName else strings.replyToSelf
    val snippet = replyPreviewSnippet(replyTo)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            contentDescription = null,
            tint = scheme.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Вертикальная полоска акцента рядом с полем ввода
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(scheme.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (replyTo.attachment != null) {
            ReplyAttachmentMiniThumb(
                message = replyTo,
                accent = scheme.accent,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.inReplyTo(author),
                color = scheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = snippet,
                color = scheme.textPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.cancelReply,
                tint = scheme.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
