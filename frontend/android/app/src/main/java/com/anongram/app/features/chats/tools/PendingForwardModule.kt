package com.anongram.app.features.chats.tools

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.tools.i18n.ToolsI18n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingForwardState(
    val sourceSession: IChatSession,
    val targetSessionId: String,
    val items: List<ChatMessage>
)

/**
 * Модуль отложенной анонимной пересылки через поле ввода целевого чата.
 * Позволяет просмотреть пересылаемые сообщения/фото, убрать лишние по одному
 * или отменить всё с возвратом в исходный чат.
 */
object PendingForwardStore {
    private val _state = MutableStateFlow<PendingForwardState?>(null)
    val state: StateFlow<PendingForwardState?> = _state.asStateFlow()

    fun start(
        sourceSession: IChatSession,
        targetSessionId: String,
        items: List<ChatMessage>
    ) {
        if (items.isEmpty()) return
        _state.value = PendingForwardState(
            sourceSession = sourceSession,
            targetSessionId = targetSessionId,
            items = items
        )
    }

    /**
     * Удаляет одно сообщение из списка пересылки.
     * Если список опустел — очищает состояние и возвращает исходный чат для возврата.
     */
    fun removeItem(messageId: String): IChatSession? {
        val current = _state.value ?: return null
        val updated = current.items.filterNot { it.id == messageId }
        return if (updated.isEmpty()) {
            _state.value = null
            current.sourceSession
        } else {
            _state.value = current.copy(items = updated)
            null
        }
    }

    /** Отменяет всю пересылку и возвращает исходный чат. */
    fun cancelAll(): IChatSession? {
        val current = _state.value ?: return null
        _state.value = null
        return current.sourceSession
    }

    fun clear() {
        _state.value = null
    }
}

@Composable
fun PendingForwardDockBar(
    pending: PendingForwardState,
    onTapPreview: () -> Unit,
    onCancelAll: () -> Unit,
    onSendNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current
    val title = if (pending.items.size == 1) {
        strings.pendingForwardSingle
    } else {
        strings.pendingForwardMultiple(pending.items.size)
    }
    val previewLine = pending.items.firstOrNull()?.let { first ->
        first.text.takeIf { it.isNotBlank() } ?: attachmentSummary(first.attachment)
    } ?: strings.pendingForwardTapHint

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card.copy(alpha = 0.94f))
            .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onTapPreview)
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(scheme.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Redo,
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = scheme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = previewLine,
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onSendNow,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onCancelAll,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.pendingForwardCancelAll,
                tint = scheme.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PendingForwardPreviewDialog(
    pending: PendingForwardState,
    onRemoveItem: (String) -> Unit,
    onCancelAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.pendingForwardPreviewTitle,
                    color = scheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = strings.pendingForwardCancelAll,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            onCancelAll()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp, modifier = Modifier.padding(top = 6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                items(pending.items, key = { it.id }) { msg ->
                    val summary = msg.text.takeIf { it.isNotBlank() } ?: attachmentSummary(msg.attachment)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (msg.attachment != null) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = summary,
                            color = scheme.textPrimary,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemoveItem(msg.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = scheme.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = scheme.divider, thickness = 0.4.dp)
                }
            }
        }
    }
}

private fun attachmentSummary(attachment: MessageAttachment?): String = when (attachment) {
    is MessageAttachment.Photo -> if (attachment.isVideo) "Video" else "Photo"
    is MessageAttachment.MediaAlbum -> "Album (${attachment.items.size})"
    is MessageAttachment.Document -> attachment.fileName
    is MessageAttachment.Audio -> attachment.title
    is MessageAttachment.Voice -> "Voice"
    is MessageAttachment.Geo -> attachment.placeName
    null -> "..."
}
