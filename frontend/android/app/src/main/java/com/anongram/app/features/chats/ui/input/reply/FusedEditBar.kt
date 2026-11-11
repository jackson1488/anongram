package com.anongram.app.features.chats.ui.input.reply

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Edit
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
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.ui.components.reply.ReplyAttachmentMiniThumb
import com.anongram.app.features.chats.ui.components.reply.replyPreviewSnippet
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings

/**
 * Слитно встроенная плашка редактирования / замены файла / обрезки и перезаписи ГС.
 */
@Composable
fun FusedEditBar(
    editing: ChatMessage,
    onClose: () -> Unit,
    onReplaceAttachment: () -> Unit,
    onEditAttachment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val hasAttachment = editing.attachment != null
    val isPhoto = editing.attachment is MessageAttachment.Photo || editing.attachment is MessageAttachment.MediaAlbum
    val isVoice = editing.attachment is MessageAttachment.Voice
    val isVideoCircle = editing.attachment is MessageAttachment.VideoCircle
    val isTrimEditableAudioVideo = isVoice || isVideoCircle
    val snippet = replyPreviewSnippet(editing)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (hasAttachment && !isTrimEditableAudioVideo) Icons.Outlined.Autorenew else Icons.Outlined.Edit,
            contentDescription = null,
            tint = scheme.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Вертикальная полоска акцента
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(scheme.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        if (hasAttachment) {
            ReplyAttachmentMiniThumb(
                message = editing,
                accent = scheme.accent,
                size = 32.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    enabled = hasAttachment,
                    onClick = { if (isTrimEditableAudioVideo) onEditAttachment() else onReplaceAttachment() }
                )
        ) {
            Text(
                text = when {
                    isVoice -> "Редактирование ГС"
                    isVideoCircle -> "Редактирование кружка"
                    hasAttachment -> strings.replace
                    else -> strings.editMessage
                },
                color = scheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = when {
                    isVoice -> "${(editing.attachment as MessageAttachment.Voice).durationLabel} · обрезать или перезаписать"
                    isVideoCircle -> "${(editing.attachment as MessageAttachment.VideoCircle).durationLabel} · обрезать или перезаписать"
                    hasAttachment -> snippet
                    else -> editing.text.ifBlank { strings.message }
                },
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isTrimEditableAudioVideo) {
            IconButton(
                onClick = onEditAttachment,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCut,
                    contentDescription = if (isVoice) "Обрезать ГС" else "Обрезать кружок",
                    tint = scheme.accent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
        } else if (isPhoto) {
            IconButton(
                onClick = onEditAttachment,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Brush,
                    contentDescription = strings.editPhoto,
                    tint = scheme.accent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.cancelEdit,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
