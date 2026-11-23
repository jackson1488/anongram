package com.anongram.app.features.chats.tools.forward

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.input.reply.InteractiveReplyBubble
import com.anongram.app.features.chats.tools.i18n.ToolsI18n
import com.anongram.app.features.wallpapers.ui.WallpaperBackground

/**
 * Оверлей предпросмотра пересылаемых сообщений (наследует визуал ReplyPreviewDialog):
 * обои чата, реальные пузыри сообщений (фото, видео, файлы, текст) и галочки для поштучной отмены.
 */
@Composable
fun ForwardPreviewOverlay(
    chatId: String,
    pending: PendingForwardState,
    peerName: String,
    onToggleItem: (String) -> Unit,
    onCancelAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.overlayScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .safeStatusBarPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = 340.dp, max = 580.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.islandBg)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.card)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${strings.pendingForwardPreviewTitle} (${pending.activeItems.size})",
                        color = scheme.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    WallpaperBackground(
                        chatId = chatId,
                        modifier = Modifier.matchParentSize()
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.28f))
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(pending.items, key = { _, item -> item.key }) { _, item ->
                            val isIncluded = item.key !in pending.excludedKeys
                            val album = item.resolvedMessage.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.MediaAlbum
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (isIncluded) 1f else 0.45f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleItem(item.key) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (isIncluded) scheme.accent else scheme.card.copy(alpha = 0.8f))
                                            .border(1.dp, if (isIncluded) scheme.accent else scheme.islandBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isIncluded) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = scheme.onAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        InteractiveReplyBubble(
                                            message = item.resolvedMessage,
                                            peerName = peerName,
                                            onJumpToMessage = { onToggleItem(item.key) }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 4.dp, end = 4.dp)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.62f))
                                                .border(0.8.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                                .clickable {
                                                    val cancelled = PendingForwardStore.removeItem(item.key)
                                                    if (cancelled) {
                                                        onDismiss()
                                                        onCancelAll()
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                                if (album != null && album.items.size > 1) {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 36.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(album.items) { photoIdx, mediaItem ->
                                            Box(modifier = Modifier.size(58.dp)) {
                                                com.anongram.app.features.chats.input.reply.ReplyAttachmentMiniThumb(
                                                    message = item.resolvedMessage.copy(
                                                        attachment = com.anongram.app.features.chats.messages.domain.MessageAttachment.Photo(
                                                            label = if (mediaItem.isVideo) "Video" else "Photo",
                                                            colorSeed = mediaItem.colorSeed,
                                                            uri = mediaItem.uri,
                                                            isVideo = mediaItem.isVideo
                                                        )
                                                    ),
                                                    accent = scheme.accent,
                                                    size = 58.dp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.68f))
                                                        .border(0.7.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                                        .clickable {
                                                            val cancelled = PendingForwardStore.removeAlbumPhoto(item.key, photoIdx)
                                                            if (cancelled) {
                                                                onDismiss()
                                                                onCancelAll()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Close,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.card,
                    border = BorderStroke(1.dp, scheme.cardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onDismiss()
                                onCancelAll()
                            }
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.pendingForwardCancelAll,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
