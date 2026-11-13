package com.anongram.app.features.chats.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.tools.translate.LocalMessageTranslator
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.i18n.ModulesI18n

private val QUICK_REACTIONS = listOf("❤️", "👍", "👎", "🔥", "🥰", "👏", "😁")

/**
 * Компактное контекстное меню сообщения по быстрому тапу в стиле Telegram:
 * появляется строго рядом с пузырём (снизу или сверху, без лишних пустых отступов).
 */
@Composable
internal fun MessageContextMenu(
    message: ChatMessage,
    anchorTopYPx: Float = 0f,
    anchorBottomYPx: Float = 0f,
    isPinned: Boolean = false,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit = {},
    onShare: () -> Unit = {},
    onEdit: () -> Unit,
    onForward: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = com.anongram.app.features.chats.tools.i18n.ToolsI18n.current
    val moduleStrings = ModulesI18n.current
    val moduleSettings by ModulesSettingsStore.state.collectAsState()
    val translations by LocalMessageTranslator.translations.collectAsState()
    LaunchedEffect(Unit) { ModulesSettingsStore.ensureInitialized(context) }

    val isMyMessage = message.direction == MessageDirection.OUTGOING
    val att = message.attachment
    val isEditable = isMyMessage && att !is MessageAttachment.Sticker
    val showTranslate = moduleSettings.showTranslateButton && message.text.isNotBlank()
    val translationEntry = translations[message.id]

    val saveLabel: String? = when (att) {
        is MessageAttachment.VideoCircle -> strings.saveToGallery
        is MessageAttachment.Photo -> strings.saveToGallery
        is MessageAttachment.MediaAlbum -> strings.saveToGalleryCount(att.items.size)
        is MessageAttachment.Voice -> strings.saveToDownloads
        is MessageAttachment.File -> strings.saveToDownloads
        is MessageAttachment.FileGroup -> strings.saveFilesCount(att.items.size)
        is MessageAttachment.Music -> strings.saveToMusic
        is MessageAttachment.MusicGroup -> strings.saveTracksCount(att.items.size)
        else -> null
    }

    val showShare: Boolean = when (att) {
        is MessageAttachment.VideoCircle,
        is MessageAttachment.Voice,
        is MessageAttachment.Photo,
        is MessageAttachment.File,
        is MessageAttachment.Music -> true
        else -> false
    }

    val showCopy: Boolean = when (att) {
        is MessageAttachment.Voice,
        is MessageAttachment.VideoCircle -> false
        is MessageAttachment.Photo,
        is MessageAttachment.MediaAlbum,
        is MessageAttachment.File,
        is MessageAttachment.FileGroup,
        is MessageAttachment.Music,
        is MessageAttachment.MusicGroup -> message.text.isNotBlank()
        else -> message.text.isNotBlank() ||
            att is MessageAttachment.Contact ||
            att is MessageAttachment.Geo
    }

    val itemCount = 4 +
        (if (saveLabel != null) 1 else 0) +
        (if (showCopy) 1 else 0) +
        (if (isEditable) 1 else 0) +
        (if (showShare) 1 else 0)
    // Компактная высота: полоска реакций (~40dp) + отступ (6dp) + пункты (~36dp каждый)
    val estimatedMenuDp = (54 + itemCount * 36).dp

    val isIncoming = message.direction == MessageDirection.INCOMING
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
    val menuHeightPx = with(density) { estimatedMenuDp.toPx() }
    val gapPx = with(density) { 6.dp.toPx() }
    val topBoundPx = with(density) { 60.dp.toPx() }
    val bottomBoundPx = (screenHeightPx - imeBottomPx - with(density) { 74.dp.toPx() })
        .coerceAtLeast(topBoundPx + with(density) { 120.dp.toPx() })
    val maxTop = (bottomBoundPx - menuHeightPx).coerceAtLeast(topBoundPx)

    val computedYPx = when {
        anchorBottomYPx > 0f && (anchorBottomYPx + gapPx + menuHeightPx) <= bottomBoundPx -> {
            // Помещается прямо ПОД пузырём
            anchorBottomYPx + gapPx
        }
        anchorTopYPx > 0f && (anchorTopYPx - gapPx - menuHeightPx) >= topBoundPx -> {
            // Помещается прямо НАД пузырём
            anchorTopYPx - gapPx - menuHeightPx
        }
        anchorBottomYPx > 0f -> {
            // Высокий пузырь — прижимаем к нижнему краю доступной области над клавиатурой
            (anchorBottomYPx - menuHeightPx).coerceIn(topBoundPx, maxTop)
        }
        else -> {
            (bottomBoundPx - menuHeightPx - with(density) { 6.dp.toPx() }).coerceIn(topBoundPx, maxTop)
        }
    }

    val yOffsetDp = with(density) { computedYPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = yOffsetDp)
                .padding(horizontal = 12.dp),
            horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
        ) {
            Column(
                modifier = Modifier
                    .width(224.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onReaction(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 15.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(scheme.card),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = strings.moreReactions,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 3.dp)
                ) {
                    ContextMenuRow(
                        icon = Icons.Outlined.Reply,
                        label = strings.reply,
                        onClick = { onReply(); onDismiss() }
                    )
                    if (saveLabel != null) {
                        ContextMenuRow(
                            icon = Icons.Outlined.FileDownload,
                            label = saveLabel,
                            onClick = { onSave(); onDismiss() }
                        )
                    }
                    if (showCopy) {
                        ContextMenuRow(
                            icon = Icons.Outlined.ContentCopy,
                            label = strings.copy,
                            onClick = { onCopy(); onDismiss() }
                        )
                    }
                    if (showTranslate) {
                        ContextMenuRow(
                            icon = Icons.Outlined.Translate,
                            label = if (translationEntry?.isShowingTranslation == true) {
                                moduleStrings.showOriginalAction
                            } else {
                                moduleStrings.translateAction
                            },
                            onClick = {
                                LocalMessageTranslator.translateOrToggle(
                                    context = context,
                                    messageId = message.id,
                                    text = message.text
                                )
                                onDismiss()
                            }
                        )
                    }
                    if (isEditable) {
                        ContextMenuRow(
                            icon = Icons.Outlined.Edit,
                            label = strings.edit,
                            onClick = { onEdit(); onDismiss() }
                        )
                    }
                    ContextMenuRow(
                        icon = Icons.Outlined.Reply,
                        label = strings.forward,
                        flipIcon = true,
                        onClick = { onForward(); onDismiss() }
                    )
                    if (showShare) {
                        ContextMenuRow(
                            icon = Icons.Outlined.Share,
                            label = strings.share,
                            onClick = { onShare(); onDismiss() }
                        )
                    }
                    ContextMenuRow(
                        icon = Icons.Outlined.PushPin,
                        label = if (isPinned) strings.unpin else strings.pin,
                        onClick = { onPin(); onDismiss() }
                    )
                    HorizontalDivider(
                        color = scheme.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                    ContextMenuRow(
                        icon = Icons.Outlined.Delete,
                        label = strings.delete,
                        color = Color(0xFFFF5252),
                        onClick = { onDelete(); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = LocalAnonGramScheme.current.textPrimary,
    flipIcon: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier
                .size(18.dp)
                .then(if (flipIcon) Modifier.graphicsLayer { scaleX = -1f } else Modifier)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
