package com.anongram.app.features.chats.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.attach.ui.components.SpoilerOverlay
import com.anongram.app.features.attach.ui.components.calculateAlbumRows
import com.anongram.app.features.attach.ui.components.getRowHeightDp
import com.anongram.app.features.attach.ui.tabs.loadHdBitmap
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.LinkPreview
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.chats.messages.domain.MessageStatus
import com.anongram.app.features.chats.messages.domain.attachmentColor
import com.anongram.app.features.chats.messages.domain.timeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val ReplyStripIncoming = Color(0xFF0B121E)
private val ReplyStripOutgoing = Color(0xFF081A2C)

/**
 * Пузырь сообщения по ТЗ:
 * - Адаптивная ширина пузыря (не доходит до противоположного края экрана; короткий текст сжимается по ширине со временем на одной строке, длинный — время в правом нижнем углу)
 * - Поддержка динамического размера текста в чате (1..30 sp из ChatTextSettingsStore)
 * - Исключение для одиночного фото: растягивается по ширине пузыря при наличии подписи или увеличенном шрифте (альбомы, файлы, музыка и контакты не растягиваются)
 * - Слитная группировка файлов без сжатия (до 10 шт., квадратные миниатюры слева для фото без сжатия, одна кнопка «⋮» сверху справа у первого файла, поштучный выбор)
 * - Слитная группировка музыки (до 10 шт., кнопка «⋮» с «Сохранить в музыку», поштучный выбор)
 * - Раздельные карточки контактов с кнопкой «НАПИСАТЬ» и мгновенным переходом в чат
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    item: MessageRenderItem,
    replyTo: ChatMessage?,
    peerName: String,
    searchQuery: String = "",
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    selectedPhotoIndices: Set<Int> = emptySet(),
    isHighlighted: Boolean = false,
    onQuoteClick: (String) -> Unit = {},
    onClick: (anchorYPx: Float) -> Unit = {},
    onLongClick: () -> Unit = {},
    onSwipeReply: () -> Unit = {},
    onToggleReaction: (String) -> Unit = {},
    onSelect: () -> Unit = {},
    onToggleAlbumPhoto: (Int) -> Unit = {},
    onEnterAlbumPhotoSelection: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenMediaViewer: (initialPhotoIndex: Int) -> Unit = {},
    onOpenUncompressedPhoto: (uri: String) -> Unit = {},
    onOpenContactChat: (name: String, phone: String) -> Unit = { _, _ -> },
    onForwardSubItem: (subKey: String) -> Unit = {},
    onDeleteSubItem: (subKey: String) -> Unit = {}
) {
    val scheme = LocalAnonGramScheme.current
    val message = item.message
    val isIncoming = message.direction == MessageDirection.INCOMING
    val density = LocalDensity.current
    val messageTextSizeSp by ChatTextSettingsStore.messageTextSizeSp.collectAsState()

    val pulseAnim = remember { Animatable(0f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            pulseAnim.snapTo(0f)
            pulseAnim.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
            pulseAnim.animateTo(0.35f, tween(durationMillis = 200, easing = LinearOutSlowInEasing))
            pulseAnim.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            pulseAnim.animateTo(0f, tween(durationMillis = 1100, easing = LinearOutSlowInEasing))
        } else {
            pulseAnim.snapTo(0f)
        }
    }
    val pulseAlpha = pulseAnim.value

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var bubbleTopY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetPx by animateFloatAsState(targetValue = dragOffsetPx, label = "swipeReply")
    val thresholdPx = with(density) { 60.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (pulseAlpha > 0f) {
                    Modifier.background(scheme.accent.copy(alpha = pulseAlpha * 0.22f))
                } else {
                    Modifier
                }
            )
            .pointerInput(item.message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffsetPx <= -thresholdPx) {
                            onSwipeReply()
                        }
                        dragOffsetPx = 0f
                    },
                    onDragCancel = { dragOffsetPx = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        val maxDrag = with(density) { 90.dp.toPx() }
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(-maxDrag, 0f)
                    }
                )
            }
    ) {
        if (dragOffsetPx < -10f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(scheme.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Reply,
                        contentDescription = "Ответить",
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .onGloballyPositioned { coordinates ->
                    bubbleTopY = coordinates.positionInRoot().y
                }
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = shrinkHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) scheme.accent else CardDark)
                        .clickable(onClick = onSelect),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Выбрано",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f),
                contentAlignment = if (isIncoming) Alignment.TopStart else Alignment.TopEnd
            ) {
                val bubbleMaxWidth = when (message.attachment) {
                    is MessageAttachment.File -> maxWidth * 0.86f
                    is MessageAttachment.FileGroup, is MessageAttachment.MusicGroup -> maxWidth * 0.77f
                    is MessageAttachment.Contact -> maxWidth * 0.78f
                    else -> maxWidth * 0.82f
                }

                Column(
                    horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
                ) {
                    if (message.isStickerOnly) {
                        Box(
                            modifier = if (pulseAlpha > 0f) {
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(scheme.accent.copy(alpha = pulseAlpha * 0.35f))
                                    .padding(4.dp)
                            } else Modifier
                        ) {
                            StickerContent(
                                symbol = (message.attachment as MessageAttachment.Sticker).symbol,
                                timeLabel = message.timeLabel(),
                                status = message.status,
                                isIncoming = isIncoming,
                                onClick = { if (isSelectionMode) onSelect() else onClick(bubbleTopY) },
                                onLongClick = onLongClick
                            )
                        }
                    } else {
                        val isEdgeToEdgeMedia = (message.attachment is MessageAttachment.MediaAlbum) ||
                            (message.attachment is MessageAttachment.Photo && (message.attachment as MessageAttachment.Photo).uri != null)
                        val isFlushGroupAttachment = (message.attachment is MessageAttachment.FileGroup) ||
                            (message.attachment is MessageAttachment.MusicGroup) ||
                            (message.attachment is MessageAttachment.File) ||
                            (message.attachment is MessageAttachment.Music)
                        val isOverlayTimestampOnMedia = (message.attachment is MessageAttachment.MediaAlbum || message.attachment is MessageAttachment.Photo) &&
                            message.text.isBlank()
                        val isTimestampHandledInsideAttachment = isFlushGroupAttachment && message.text.isBlank()

                        val singlePhotoStretch = (message.attachment is MessageAttachment.Photo) &&
                            (message.text.isNotBlank() || messageTextSizeSp > 16)

                        val baseColor = when {
                            isSelected -> scheme.accent.copy(alpha = 0.28f)
                            isEdgeToEdgeMedia -> Color(0xFF141418)
                            isIncoming -> CardDark
                            else -> scheme.accent.copy(alpha = 0.18f)
                        }
                        val bubbleColor = if (pulseAlpha > 0f) {
                            lerp(baseColor, scheme.accent.copy(alpha = 0.72f), pulseAlpha)
                        } else {
                            baseColor
                        }
                        val bubbleBorder = when {
                            pulseAlpha > 0.005f -> BorderStroke(2.dp, scheme.accent.copy(alpha = pulseAlpha))
                            isEdgeToEdgeMedia -> BorderStroke(1.2.dp, scheme.accent.copy(alpha = 0.55f))
                            !isIncoming -> BorderStroke(1.dp, scheme.accent.copy(alpha = 0.4f))
                            else -> null
                        }

                        Surface(
                            shape = bubbleShape(item, isIncoming),
                            color = bubbleColor,
                            border = bubbleBorder,
                            shadowElevation = (pulseAlpha * 16).dp,
                            modifier = Modifier
                                .widthIn(max = bubbleMaxWidth)
                                .clip(bubbleShape(item, isIncoming))
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            onSelect()
                                        } else if (isEdgeToEdgeMedia) {
                                            onOpenMediaViewer(0)
                                        } else {
                                            onClick(bubbleTopY)
                                        }
                                    },
                                    onLongClick = onLongClick
                                )
                        ) {
                            Box {
                                Column(
                                    modifier = when {
                                        isEdgeToEdgeMedia -> Modifier.padding(
                                            start = 1.5.dp,
                                            end = 1.5.dp,
                                            top = 1.5.dp,
                                            bottom = if (message.text.isNotBlank()) 6.dp else 1.5.dp
                                        )
                                        isFlushGroupAttachment -> Modifier.padding(
                                            start = 4.dp,
                                            end = 6.dp,
                                            top = 4.dp,
                                            bottom = if (message.text.isNotBlank()) 6.dp else 4.dp
                                        )
                                        else -> Modifier.padding(start = 11.dp, end = 10.dp, top = 7.dp, bottom = 6.dp)
                                    }
                                ) {
                                    // Пометка пересылки (шаг 10)
                                    message.forwardedFrom?.let { author ->
                                        Box(
                                            modifier = if (isEdgeToEdgeMedia || isFlushGroupAttachment) {
                                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            } else Modifier
                                        ) {
                                            ForwardedBlock(author = author)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    // Цитата ответа (шаг 1)
                                    replyTo?.let { target ->
                                        Box(
                                            modifier = if (isEdgeToEdgeMedia || isFlushGroupAttachment) {
                                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            } else Modifier
                                        ) {
                                            QuoteBlock(
                                                author = if (target.direction == MessageDirection.INCOMING) peerName else "Вы",
                                                text = target.text.ifBlank { "Вложение" },
                                                accent = scheme.accent,
                                                isIncoming = isIncoming,
                                                onClick = { onQuoteClick(target.id) }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                    }

                                    // Отрисовка вложений
                                    message.attachment?.let { attachment ->
                                        Box {
                                            AttachmentContent(
                                                message = message,
                                                attachment = attachment,
                                                isIncoming = isIncoming,
                                                accent = scheme.accent,
                                                searchQuery = searchQuery,
                                                singlePhotoStretch = singlePhotoStretch,
                                                isSelectionMode = isSelectionMode,
                                                selectedSubIndices = selectedPhotoIndices,
                                                showInlineTimestamp = isTimestampHandledInsideAttachment,
                                                onSelect = onSelect,
                                                onToggleSubItem = onToggleAlbumPhoto,
                                                onEnterSubItemSelection = onEnterAlbumPhotoSelection,
                                                onLongClick = onLongClick,
                                                onRetry = onRetry,
                                                onSwipeReply = onSwipeReply,
                                                onOpenPhotoAt = { idx -> onOpenMediaViewer(idx) },
                                                onOpenUncompressedPhoto = onOpenUncompressedPhoto,
                                                onOpenContactChat = onOpenContactChat,
                                                onForwardSubItem = onForwardSubItem,
                                                onDeleteSubItem = onDeleteSubItem
                                            )

                                            if (isOverlayTimestampOnMedia) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(end = 7.dp, bottom = 6.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color.Black.copy(alpha = 0.56f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (message.isEdited) {
                                                        Text(
                                                            text = "изменено",
                                                            color = Color.White.copy(alpha = 0.85f),
                                                            fontSize = 9.sp,
                                                            fontStyle = FontStyle.Italic
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }
                                                    Text(
                                                        text = message.timeLabel(),
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    if (!isIncoming) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        DeliveryStatus(status = message.status, onRetry = onRetry)
                                                    }
                                                }
                                            }
                                        }
                                        if (message.text.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }

                                    // Превью ссылки (шаг 15)
                                    message.linkPreview?.let { preview ->
                                        LinkPreviewBlock(
                                            preview = preview,
                                            accent = scheme.accent,
                                            searchQuery = searchQuery
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    // Текст сообщения + адаптивное время (на одной строке для коротких и в правом нижнем углу для длинных)
                                    if (message.text.isNotBlank()) {
                                        val baseAnnotated = if (searchQuery.isNotBlank()) {
                                            buildHighlightedText(
                                                text = message.text,
                                                query = searchQuery,
                                                highlightColor = scheme.accent
                                            )
                                        } else {
                                            AnnotatedString(message.text)
                                        }
                                        val trailingSpacesCount = when {
                                            message.isEdited && !isIncoming -> 19
                                            message.isEdited -> 16
                                            !isIncoming -> 12
                                            else -> 9
                                        }
                                        val spacerSuffix = "\u00A0".repeat(trailingSpacesCount)
                                        val displayWithReserve = remember(baseAnnotated, spacerSuffix) {
                                            AnnotatedString.Builder(baseAnnotated).apply {
                                                pushStyle(SpanStyle(fontSize = 11.sp))
                                                append(spacerSuffix)
                                                pop()
                                            }.toAnnotatedString()
                                        }
                                        val textLineHeight = (messageTextSizeSp * 1.28f).coerceAtLeast(messageTextSizeSp + 3f).sp

                                        Box(
                                            modifier = if (isEdgeToEdgeMedia || isFlushGroupAttachment) {
                                                Modifier.padding(horizontal = 10.dp)
                                            } else {
                                                Modifier
                                            }
                                        ) {
                                            Text(
                                                text = displayWithReserve,
                                                color = TextPrimary,
                                                fontSize = messageTextSizeSp.sp,
                                                lineHeight = textLineHeight
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .offset(y = 1.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (message.isEdited) {
                                                    Text(
                                                        text = "изменено",
                                                        color = TextMuted,
                                                        fontSize = 9.sp,
                                                        fontStyle = FontStyle.Italic
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = message.timeLabel(),
                                                    color = TextMuted,
                                                    fontSize = 10.5.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                if (!isIncoming) {
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    DeliveryStatus(status = message.status, onRetry = onRetry)
                                                }
                                            }
                                        }
                                    } else if (!isOverlayTimestampOnMedia && !isTimestampHandledInsideAttachment) {
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (message.isEdited) {
                                                Text(
                                                    text = "изменено",
                                                    color = TextMuted,
                                                    fontSize = 9.sp,
                                                    fontStyle = FontStyle.Italic
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = message.timeLabel(),
                                                color = TextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (!isIncoming) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                DeliveryStatus(status = message.status, onRetry = onRetry)
                                            }
                                        }
                                    }
                                }

                                if (pulseAlpha > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(scheme.accent.copy(alpha = pulseAlpha * 0.35f))
                                    )
                                }
                            }
                        }
                    }

                    // Реакции под пузырём (шаг 11)
                    ReactionChips(
                        message = message,
                        onToggle = onToggleReaction
                    )
                }
            }
        }
    }
}

/** Отрисовка стикера без пузыря */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickerContent(
    symbol: String,
    timeLabel: String,
    status: MessageStatus,
    isIncoming: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
        horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
    ) {
        Text(text = symbol, fontSize = 68.sp)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark.copy(alpha = 0.7f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeLabel,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!isIncoming) {
                    Spacer(modifier = Modifier.width(4.dp))
                    DeliveryStatus(status = status, onRetry = {})
                }
            }
        }
    }
}

/** Блок пересланного сообщения */
@Composable
private fun ForwardedBlock(author: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Переслано от $author",
            color = TextMuted,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

/** Содержимое вложения в пузыре */
@Composable
private fun AttachmentContent(
    message: ChatMessage,
    attachment: MessageAttachment,
    isIncoming: Boolean,
    accent: Color,
    searchQuery: String = "",
    singlePhotoStretch: Boolean = false,
    isSelectionMode: Boolean = false,
    selectedSubIndices: Set<Int> = emptySet(),
    showInlineTimestamp: Boolean = false,
    onSelect: () -> Unit = {},
    onToggleSubItem: (Int) -> Unit = {},
    onEnterSubItemSelection: (Int) -> Unit = {},
    onLongClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSwipeReply: () -> Unit = {},
    onOpenPhotoAt: (Int) -> Unit = {},
    onOpenUncompressedPhoto: (String) -> Unit = {},
    onOpenContactChat: (String, String) -> Unit = { _, _ -> },
    onForwardSubItem: (String) -> Unit = {},
    onDeleteSubItem: (String) -> Unit = {}
) {
    when (attachment) {
        is MessageAttachment.Photo -> {
            if (attachment.uri != null) {
                ChatRealPhotoBox(
                    uriString = attachment.uri,
                    isVideo = attachment.isVideo,
                    hasSpoiler = attachment.hasSpoiler,
                    onPhotoClick = {
                        if (isSelectionMode) onSelect() else onOpenPhotoAt(0)
                    },
                    onPhotoLongClick = onLongClick,
                    modifier = if (singlePhotoStretch) {
                        Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                            .width(225.dp)
                            .height(370.dp)
                            .clip(RoundedCornerShape(14.dp))
                    }
                )
            } else {
                var spoilerRevealed by remember { mutableStateOf(false) }
                val showSpoiler = attachment.hasSpoiler && !spoilerRevealed
                Box(
                    modifier = (if (singlePhotoStretch) Modifier.fillMaxWidth() else Modifier.width(225.dp))
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(attachmentColor(attachment.colorSeed))
                        .then(
                            if (showSpoiler) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { spoilerRevealed = true }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (attachment.isVideo) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Воспроизвести",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (attachment.label.isNotBlank()) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                buildHighlightedText(attachment.label, searchQuery, accent)
                            } else {
                                AnnotatedString(attachment.label)
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                    }
                    if (showSpoiler) {
                        SpoilerOverlay(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        is MessageAttachment.MediaAlbum -> {
            ChatMediaAlbumGrid(
                items = attachment.items,
                accent = accent,
                isSelectionMode = isSelectionMode,
                selectedPhotoIndices = selectedSubIndices,
                onPhotoClick = { idx ->
                    if (isSelectionMode) onToggleSubItem(idx) else onOpenPhotoAt(idx)
                },
                onPhotoLongClick = { idx ->
                    if (isSelectionMode) onToggleSubItem(idx) else onEnterSubItemSelection(idx)
                }
            )
        }

        is MessageAttachment.File -> {
            ChatFileItemRow(
                message = message,
                file = attachment,
                isIncoming = isIncoming,
                accent = accent,
                searchQuery = searchQuery,
                showThreeDots = true,
                showBottomTimestamp = showInlineTimestamp,
                isSelectionMode = isSelectionMode,
                isItemSelected = false,
                onItemClick = {
                    if (isSelectionMode) {
                        onSelect()
                    } else if (attachment.kind == FileKind.IMAGE && !attachment.uri.isNullOrBlank()) {
                        onOpenUncompressedPhoto(attachment.uri)
                    } else {
                        null // handled inside ChatFileItemRow with context
                    }
                },
                onItemLongClick = onLongClick,
                onSwipeReply = onSwipeReply,
                onForwardItem = { onForwardSubItem(message.id) },
                onDeleteItem = { onDeleteSubItem(message.id) },
                onRetry = onRetry,
                onOpenUncompressedPhoto = onOpenUncompressedPhoto
            )
        }

        is MessageAttachment.FileGroup -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                attachment.items.forEachIndexed { idx, fileItem ->
                    val isItemSelected = idx in selectedSubIndices
                    ChatFileItemRow(
                        message = message,
                        file = fileItem,
                        isIncoming = isIncoming,
                        accent = accent,
                        searchQuery = searchQuery,
                        // Кнопка «⋮» (три точки) только одна — в правом верхнем углу у первого элемента!
                        showThreeDots = (idx == 0),
                        showBottomTimestamp = showInlineTimestamp && (idx == attachment.items.lastIndex),
                        isSelectionMode = isSelectionMode,
                        isItemSelected = isItemSelected,
                        onItemClick = {
                            if (isSelectionMode) {
                                onToggleSubItem(idx)
                            } else if (fileItem.kind == FileKind.IMAGE && !fileItem.uri.isNullOrBlank()) {
                                onOpenUncompressedPhoto(fileItem.uri)
                            } else {
                                null
                            }
                        },
                        onItemLongClick = {
                            if (isSelectionMode) onToggleSubItem(idx) else onEnterSubItemSelection(idx)
                        },
                        onSwipeReply = onSwipeReply,
                        onForwardItem = { onForwardSubItem("${message.id}#file_$idx") },
                        onDeleteItem = { onDeleteSubItem("${message.id}#file_$idx") },
                        onRetry = onRetry,
                        onOpenUncompressedPhoto = onOpenUncompressedPhoto
                    )
                }
            }
        }

        is MessageAttachment.Voice -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Слушать",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    attachment.waveform.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((height * 1.5f).dp.coerceIn(4.dp, 24.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(accent.copy(alpha = 0.7f))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = attachment.durationLabel,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        is MessageAttachment.VideoCircle -> {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(attachmentColor(attachment.colorSeed)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = attachment.durationLabel,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }

        is MessageAttachment.Music -> {
            ChatMusicItemRow(
                message = message,
                music = attachment,
                isIncoming = isIncoming,
                accent = accent,
                searchQuery = searchQuery,
                showThreeDots = true,
                showBottomTimestamp = showInlineTimestamp,
                isSelectionMode = isSelectionMode,
                isItemSelected = false,
                onItemClick = { if (isSelectionMode) onSelect() },
                onItemLongClick = onLongClick,
                onSwipeReply = onSwipeReply,
                onForwardItem = { onForwardSubItem(message.id) },
                onDeleteItem = { onDeleteSubItem(message.id) },
                onRetry = onRetry
            )
        }

        is MessageAttachment.MusicGroup -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                attachment.items.forEachIndexed { idx, track ->
                    val isItemSelected = idx in selectedSubIndices
                    ChatMusicItemRow(
                        message = message,
                        music = track,
                        isIncoming = isIncoming,
                        accent = accent,
                        searchQuery = searchQuery,
                        showThreeDots = (idx == 0),
                        showBottomTimestamp = showInlineTimestamp && (idx == attachment.items.lastIndex),
                        isSelectionMode = isSelectionMode,
                        isItemSelected = isItemSelected,
                        onItemClick = {
                            if (isSelectionMode) onToggleSubItem(idx)
                        },
                        onItemLongClick = {
                            if (isSelectionMode) onToggleSubItem(idx) else onEnterSubItemSelection(idx)
                        },
                        onSwipeReply = onSwipeReply,
                        onForwardItem = { onForwardSubItem("${message.id}#music_$idx") },
                        onDeleteItem = { onDeleteSubItem("${message.id}#music_$idx") },
                        onRetry = onRetry
                    )
                }
            }
        }

        is MessageAttachment.Contact -> {
            // Карточка контакта (каждый контакт отдельно, внутри вертикальная полоска слева, аватар, имя, номер и кнопка «НАПИСАТЬ»)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable {
                        if (isSelectionMode) {
                            onSelect()
                        } else {
                            onOpenContactChat(attachment.name, attachment.phone)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .fillMaxHeight()
                            .background(if (isIncoming) accent else Color.White)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.30f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = attachment.name.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        buildHighlightedText(attachment.name, searchQuery, accent)
                                    } else {
                                        AnnotatedString(attachment.name)
                                    },
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        buildHighlightedText(attachment.phone, searchQuery, accent)
                                    } else {
                                        AnnotatedString(attachment.phone)
                                    },
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 0.7.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "НАПИСАТЬ",
                            color = if (isIncoming) accent else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelectionMode) {
                                        onSelect()
                                    } else {
                                        onOpenContactChat(attachment.name, attachment.phone)
                                    }
                                }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        is MessageAttachment.Geo -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = FlameOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Геопозиция",
                        color = FlameOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        buildHighlightedText(attachment.address, searchQuery, accent)
                    } else {
                        AnnotatedString(attachment.address)
                    },
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }
        }

        is MessageAttachment.Sticker -> {
            Text(text = attachment.symbol, fontSize = 48.sp)
        }
    }
}

/**
 * Строка файла или фотографии без сжатия внутри пузыря (одиночной или в группе до 10 шт.):
 * - Для фото без сжатия (FileKind.IMAGE): квадратная миниатюра 62.dp вплотную слева без вертикальных зазоров
 * - Для документов/APK/архивов: круглая иконка 44.dp слева
 * - Справа сверху (только у первого элемента в группе): иконка «⋮» с меню («Сохранить в галерею» для фото или «Сохранить в загрузки» для файлов, «Ответить», «Переслать», «Поделиться», «Удалить»)
 * - Тап по фото без сжатия открывает полноэкранный просмотр; тап по файлу открывает системное приложение просмотра/установки
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatFileItemRow(
    message: ChatMessage,
    file: MessageAttachment.File,
    isIncoming: Boolean,
    accent: Color,
    searchQuery: String,
    showThreeDots: Boolean,
    showBottomTimestamp: Boolean,
    isSelectionMode: Boolean,
    isItemSelected: Boolean,
    onItemClick: () -> Unit?,
    onItemLongClick: () -> Unit,
    onSwipeReply: () -> Unit,
    onForwardItem: () -> Unit,
    onDeleteItem: () -> Unit,
    onRetry: () -> Unit,
    onOpenUncompressedPhoto: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    val isImageFile = file.kind == FileKind.IMAGE ||
        file.name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "gif")

    val parsedUri = remember(file.uri) { file.uri?.let { runCatching { Uri.parse(it) }.getOrNull() } }
    var thumbBitmap by remember(file.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(parsedUri, isImageFile) {
        if (isImageFile && parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            thumbBitmap = quick?.asImageBitmap()
        }
    }

    val extLabel = remember(file.name, file.kind) {
        val ext = file.name.substringAfterLast('.', "").uppercase()
        if (ext.isNotBlank() && ext.length <= 5) ext else file.kind.label
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isItemSelected) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.22f))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onItemClick()
                    } else if (isImageFile && !file.uri.isNullOrBlank()) {
                        onOpenUncompressedPhoto(file.uri)
                    } else {
                        openFileInSystemViewer(context, file)
                    }
                },
                onLongClick = onItemLongClick
            )
            .padding(vertical = if (isImageFile) 0.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть: квадратная миниатюра для фото без сжатия или круглая иконка для документа
            Box(contentAlignment = Alignment.Center) {
                if (isImageFile) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E222D)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbBitmap != null) {
                            Image(
                                bitmap = thumbBitmap!!,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isIncoming) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when {
                            file.kind == FileKind.APK || file.name.endsWith(".apk", ignoreCase = true) -> Icons.Outlined.Android
                            file.kind == FileKind.PDF || file.kind == FileKind.DOC -> Icons.Outlined.Description
                            else -> Icons.Outlined.InsertDriveFile
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isItemSelected) accent else Color.Black.copy(alpha = 0.55f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isItemSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Выбрано",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Центральная часть: имя файла и снизу размер + расширение (например «212,0 KB JPG» или «28,2 KB PDF»)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (showThreeDots) 24.dp else 4.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        buildHighlightedText(file.name, searchQuery, accent)
                    } else {
                        AnnotatedString(file.name)
                    },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${file.sizeLabel} $extLabel",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Кнопка «⋮» в правом верхнем углу (только одна у первого файла в группе или у одиночного файла)
        if (showThreeDots) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Меню файла",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    modifier = Modifier
                        .width(235.dp)
                        .background(Color(0xFF14151B), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                ) {
                    // Строгое разделение: «Сохранить в галерею» ТОЛЬКО для фото/видео, «Сохранить в загрузки» для файлов
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isImageFile) "Сохранить в галерею" else "Сохранить в загрузки",
                                color = Color.White,
                                fontSize = 14.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            scope.launch {
                                if (isImageFile && !file.uri.isNullOrBlank()) {
                                    val ok = savePhotoToDeviceGallery(context, file.uri)
                                    Toast.makeText(
                                        context,
                                        if (ok) "Сохранено в галерею: ${file.name}" else "Не удалось сохранить фото",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val ok = saveFileToDeviceDownloads(context, file)
                                    Toast.makeText(
                                        context,
                                        if (ok) "Сохранено в загрузки: ${file.name}" else "Не удалось сохранить файл",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ответить", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Reply, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onSwipeReply()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Переслать", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Forward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onForwardItem()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Поделиться", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            if (!file.uri.isNullOrBlank()) {
                                sharePhotoSystemChooser(context, file.uri)
                            } else {
                                shareTextOrFileFallback(context, file.name)
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Удалить", color = Color(0xFFFF5252), fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onDeleteItem()
                        }
                    )
                }
            }
        }

        // Время и галочки в правом нижнем углу последнего элемента
        if (showBottomTimestamp) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.timeLabel(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!isIncoming) {
                    Spacer(modifier = Modifier.width(3.dp))
                    DeliveryStatus(status = message.status, onRetry = onRetry)
                }
            }
        }
    }
}

/**
 * Строка музыкального трека (одиночного или в слитной группе до 10 треков):
 * - Слева круглая кнопка Play/Pause 44.dp
 * - Справа название трека и снизу прогресс «0:00 / X:XX»
 * - В правом верхнем углу (только у первого трека в группе) — кнопка «⋮» со строгим пунктом «Сохранить в музыку»
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMusicItemRow(
    message: ChatMessage,
    music: MessageAttachment.Music,
    isIncoming: Boolean,
    accent: Color,
    searchQuery: String,
    showThreeDots: Boolean,
    showBottomTimestamp: Boolean,
    isSelectionMode: Boolean,
    isItemSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onSwipeReply: () -> Unit,
    onForwardItem: () -> Unit,
    onDeleteItem: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    val displayTitle = remember(music.artist, music.title) {
        if (music.artist.isNotBlank() && music.artist != "Неизвестен" && !music.title.contains(music.artist)) {
            "${music.artist} - ${music.title}"
        } else {
            music.title
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isItemSelected) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.22f))
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onItemClick()
                    } else {
                        isPlaying = !isPlaying
                    }
                },
                onLongClick = onItemLongClick
            )
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isIncoming) accent else Color.White.copy(alpha = 0.24f))
                        .clickable {
                            if (isSelectionMode) onItemClick() else isPlaying = !isPlaying
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Играть",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isItemSelected) accent else Color.Black.copy(alpha = 0.55f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isItemSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Выбрано",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (showThreeDots) 24.dp else 4.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        buildHighlightedText(displayTitle, searchQuery, accent)
                    } else {
                        AnnotatedString(displayTitle)
                    },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isPlaying) "0:14 / ${music.durationLabel}" else "0:00 / ${music.durationLabel}",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1
                )
            }
        }

        if (showThreeDots) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Меню музыки",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    modifier = Modifier
                        .width(235.dp)
                        .background(Color(0xFF14151B), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                ) {
                    // Для музыки строго «Сохранить в музыку» (НЕ в загрузки и НЕ в галерею)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Сохранить в музыку",
                                color = Color.White,
                                fontSize = 14.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            scope.launch {
                                val ok = saveMusicToDeviceMusic(context, music)
                                Toast.makeText(
                                    context,
                                    if (ok) "Сохранено в музыку: $displayTitle" else "Не удалось сохранить трек",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ответить", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Reply, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onSwipeReply()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Переслать", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Forward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onForwardItem()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Поделиться", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            shareTextOrFileFallback(context, displayTitle)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Удалить", color = Color(0xFFFF5252), fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onDeleteItem()
                        }
                    )
                }
            }
        }

        if (showBottomTimestamp) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.timeLabel(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!isIncoming) {
                    Spacer(modifier = Modifier.width(3.dp))
                    DeliveryStatus(status = message.status, onRetry = onRetry)
                }
            }
        }
    }
}

/**
 * Открытие файла (.pdf, .apk, .doc, .zip и т.д.) в системном приложении просмотра/установки Android.
 */
private fun openFileInSystemViewer(context: Context, file: MessageAttachment.File) {
    val ext = file.name.substringAfterLast('.', "").lowercase()
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        ?: when (file.kind) {
            FileKind.PDF -> "application/pdf"
            FileKind.APK -> "application/vnd.android.package-archive"
            FileKind.ARCHIVE -> "application/zip"
            FileKind.DOC -> "application/msword"
            FileKind.IMAGE -> "image/*"
            else -> "*/*"
        }

    runCatching {
        val targetUri: Uri? = if (!file.uri.isNullOrBlank()) {
            Uri.parse(file.uri)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Создаём безопасный content:// URI во временных загрузках для демо-файлов
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AnonGram")
            }
            val resolver = context.contentResolver
            val inserted = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (inserted != null) {
                resolver.openOutputStream(inserted)?.use { out ->
                    out.write("AnonGram Document: ${file.name} (${file.sizeLabel})".toByteArray())
                }
            }
            inserted
        } else {
            null
        }

        if (targetUri != null) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, file.name).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } else {
            Toast.makeText(context, "Открытие файла: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }.onFailure {
        Toast.makeText(context, "Нет приложения для открытия ${file.name}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Сохранение обычного файла в системную папку «Загрузки» (MediaStore.Downloads).
 */
private suspend fun saveFileToDeviceDownloads(context: Context, file: MessageAttachment.File): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val ext = file.name.substringAfterLast('.', "").lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AnonGram")
                }
                val destUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                resolver.openOutputStream(destUri)?.use { out ->
                    val srcUri = file.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    val copied = if (srcUri != null) {
                        resolver.openInputStream(srcUri)?.use { input -> input.copyTo(out) }
                    } else null
                    if (copied == null) {
                        out.write("AnonGram File: ${file.name} (${file.sizeLabel})".toByteArray())
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

/**
 * Сохранение музыкального трека в системную папку «Музыка» (MediaStore.Audio.Media / DIRECTORY_MUSIC).
 */
private suspend fun saveMusicToDeviceMusic(context: Context, music: MessageAttachment.Music): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val safeFileName = "${music.artist} - ${music.title}.mp3".replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Audio.Media.TITLE, music.title)
                    put(MediaStore.Audio.Media.ARTIST, music.artist)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AnonGram")
                }
                val destUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                resolver.openOutputStream(destUri)?.use { out ->
                    val srcUri = music.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    val copied = if (srcUri != null) {
                        resolver.openInputStream(srcUri)?.use { input -> input.copyTo(out) }
                    } else null
                    if (copied == null) {
                        out.write("ID3 AnonGram Audio Track: $safeFileName".toByteArray())
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

private fun shareTextOrFileFallback(context: Context, label: String) {
    runCatching {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, label)
        }
        val chooser = Intent.createChooser(shareIntent, "Поделиться").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

/** Превью веб-ссылки */
@Composable
private fun LinkPreviewBlock(
    preview: LinkPreview,
    accent: Color,
    searchQuery: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = preview.domain,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (searchQuery.isNotBlank()) {
                    buildHighlightedText(preview.title, searchQuery, accent)
                } else {
                    AnnotatedString(preview.title)
                },
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (searchQuery.isNotBlank()) {
                    buildHighlightedText(preview.description, searchQuery, accent)
                } else {
                    AnnotatedString(preview.description)
                },
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Цитата внутри пузыря */
@Composable
private fun QuoteBlock(
    author: String,
    text: String,
    accent: Color,
    isIncoming: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isIncoming) ReplyStripIncoming else ReplyStripOutgoing)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = author,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeliveryStatus(
    status: MessageStatus,
    onRetry: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    when (status) {
        MessageStatus.SENDING -> Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = "Отправляется",
            tint = TextMuted,
            modifier = Modifier.size(12.dp)
        )

        MessageStatus.SENT -> Icon(
            painter = painterResource(id = R.drawable.ic_tick_single),
            contentDescription = "Отправлено",
            tint = TextMuted,
            modifier = Modifier.size(13.dp)
        )

        MessageStatus.READ -> Icon(
            painter = painterResource(id = R.drawable.ic_tick_double),
            contentDescription = "Прочитано",
            tint = scheme.accent,
            modifier = Modifier.size(13.dp)
        )

        MessageStatus.FAILED -> Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = "Ошибка доставки. Нажмите для повтора",
            tint = FlameOrange,
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onRetry)
        )
    }
}

/** По ТЗ хвостик — только у последнего сообщения группы. */
private fun bubbleShape(item: MessageRenderItem, isIncoming: Boolean): RoundedCornerShape {
    val radius = 16.dp
    val tail = 5.dp
    val bottomTail = if (item.isLastInGroup) tail else radius
    return if (isIncoming) {
        RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = bottomTail,
            bottomEnd = radius
        )
    } else {
        RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = radius,
            bottomEnd = bottomTail
        )
    }
}

/**
 * Подсветка совпадающего кусочка слова/подстроки в тексте пузыря сообщения.
 */
private fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = trimmed.lowercase()
    var currentIndex = 0
    var matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
    if (matchIndex < 0) return AnnotatedString(text)

    val builder = AnnotatedString.Builder()
    while (matchIndex >= 0) {
        if (matchIndex > currentIndex) {
            builder.append(text.substring(currentIndex, matchIndex))
        }
        val endMatch = matchIndex + lowerQuery.length
        builder.pushStyle(
            SpanStyle(
                background = highlightColor.copy(alpha = 0.40f),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
        builder.append(text.substring(matchIndex, endMatch))
        builder.pop()
        currentIndex = endMatch
        matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
    }
    if (currentIndex < text.length) {
        builder.append(text.substring(currentIndex))
    }
    return builder.toAnnotatedString()
}

/**
 * Отрисовка альбома медиа в чате (тонкая рамка, вертикальные пропорции для 2, 3, 4, 7, 8, 9, 10 фото,
 * индивидуальный спойлер для каждого фото, поштучный выбор в режиме выделения и раскрытие фото на весь экран по тапу).
 */
@Composable
private fun ChatMediaAlbumGrid(
    items: List<MessageAttachment.AlbumMediaItem>,
    accent: Color,
    isSelectionMode: Boolean = false,
    selectedPhotoIndices: Set<Int> = emptySet(),
    onPhotoClick: (Int) -> Unit = {},
    onPhotoLongClick: (Int) -> Unit = {}
) {
    val count = items.size
    val dividerColor = accent.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        when (count) {
            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    ChatRealPhotoBox(
                        uriString = items[0].uri,
                        isVideo = items[0].isVideo,
                        hasSpoiler = items[0].hasSpoiler,
                        showSelectionBadge = isSelectionMode,
                        isPhotoSelected = 0 in selectedPhotoIndices,
                        accent = accent,
                        onPhotoClick = { onPhotoClick(0) },
                        onPhotoLongClick = { onPhotoLongClick(0) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                    ChatRealPhotoBox(
                        uriString = items[1].uri,
                        isVideo = items[1].isVideo,
                        hasSpoiler = items[1].hasSpoiler,
                        showSelectionBadge = isSelectionMode,
                        isPhotoSelected = 1 in selectedPhotoIndices,
                        accent = accent,
                        onPhotoClick = { onPhotoClick(1) },
                        onPhotoLongClick = { onPhotoLongClick(1) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            3 -> {
                // 3 фото: 1 большое вертикальное слева + 2 вертикальных (не квадратных!) справа
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                ) {
                    ChatRealPhotoBox(
                        uriString = items[0].uri,
                        isVideo = items[0].isVideo,
                        hasSpoiler = items[0].hasSpoiler,
                        showSelectionBadge = isSelectionMode,
                        isPhotoSelected = 0 in selectedPhotoIndices,
                        accent = accent,
                        onPhotoClick = { onPhotoClick(0) },
                        onPhotoLongClick = { onPhotoLongClick(0) },
                        modifier = Modifier
                            .weight(1.12f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                    ) {
                        ChatRealPhotoBox(
                            uriString = items[1].uri,
                            isVideo = items[1].isVideo,
                            hasSpoiler = items[1].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 1 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(1) },
                            onPhotoLongClick = { onPhotoLongClick(1) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(dividerColor)
                        )
                        ChatRealPhotoBox(
                            uriString = items[2].uri,
                            isVideo = items[2].isVideo,
                            hasSpoiler = items[2].hasSpoiler,
                            showSelectionBadge = isSelectionMode,
                            isPhotoSelected = 2 in selectedPhotoIndices,
                            accent = accent,
                            onPhotoClick = { onPhotoClick(2) },
                            onPhotoLongClick = { onPhotoLongClick(2) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            4 -> {
                // 4 фото: 1 большое вертикальное слева + 3 вертикальных столбиком справа
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                ) {
                    ChatRealPhotoBox(
                        uriString = items[0].uri,
                        isVideo = items[0].isVideo,
                        hasSpoiler = items[0].hasSpoiler,
                        showSelectionBadge = isSelectionMode,
                        isPhotoSelected = 0 in selectedPhotoIndices,
                        accent = accent,
                        onPhotoClick = { onPhotoClick(0) },
                        onPhotoLongClick = { onPhotoLongClick(0) },
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.82f)
                            .fillMaxHeight()
                    ) {
                        for (idx in 1..3) {
                            if (idx > 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(dividerColor)
                                )
                            }
                            ChatRealPhotoBox(
                                uriString = items[idx].uri,
                                isVideo = items[idx].isVideo,
                                hasSpoiler = items[idx].hasSpoiler,
                                showSelectionBadge = isSelectionMode,
                                isPhotoSelected = idx in selectedPhotoIndices,
                                accent = accent,
                                onPhotoClick = { onPhotoClick(idx) },
                                onPhotoLongClick = { onPhotoLongClick(idx) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
            else -> {
                val rowCounts = remember(count) { calculateAlbumRows(count) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    var itemPointer = 0
                    rowCounts.forEachIndexed { rowIndex, countInRow ->
                        if (rowIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(dividerColor)
                            )
                        }
                        val rowHeight = getRowHeightDp(countInRow)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                        ) {
                            for (c in 0 until countInRow) {
                                val currentIdx = itemPointer + c
                                if (currentIdx < items.size) {
                                    if (c > 0) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .fillMaxHeight()
                                                .background(dividerColor)
                                        )
                                    }
                                    val m = items[currentIdx]
                                    ChatRealPhotoBox(
                                        uriString = m.uri,
                                        isVideo = m.isVideo,
                                        hasSpoiler = m.hasSpoiler,
                                        showSelectionBadge = isSelectionMode,
                                        isPhotoSelected = currentIdx in selectedPhotoIndices,
                                        accent = accent,
                                        onPhotoClick = { onPhotoClick(currentIdx) },
                                        onPhotoLongClick = { onPhotoLongClick(currentIdx) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                        itemPointer += countInRow
                    }
                }
            }
        }
    }
}

/**
 * Ячейка настоящего фото/видео в чате:
 * - Индивидуальный спойлер для каждого фото: первый тап снимает спойлер именно с этого фото
 * - Повторный тап (или первый тап, если спойлера нет) открывает полноэкранный просмотр именно этого фото (без быстрого меню)
 * - В режиме выделения (showSelectionBadge == true) каждое фото в альбоме выбирается по одному со своим кружком-галочкой
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRealPhotoBox(
    uriString: String,
    isVideo: Boolean,
    hasSpoiler: Boolean,
    showSelectionBadge: Boolean = false,
    isPhotoSelected: Boolean = false,
    accent: Color = Color(0xFF00E5FF),
    onPhotoClick: () -> Unit = {},
    onPhotoLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedUri = remember(uriString) { runCatching { Uri.parse(uriString) }.getOrNull() }
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    var localSpoilerRevealed by remember(uriString) { mutableStateOf(false) }
    val showSpoiler = hasSpoiler && !localSpoilerRevealed

    LaunchedEffect(parsedUri) {
        if (parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            if (bitmap == null) bitmap = quick?.asImageBitmap()
            val hd = withContext(Dispatchers.IO) { loadHdBitmap(context, parsedUri) }
            if (hd != null) bitmap = hd.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1A1A20))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (showSelectionBadge) {
                        onPhotoClick()
                    } else if (showSpoiler) {
                        localSpoilerRevealed = true
                    } else {
                        onPhotoClick()
                    }
                },
                onLongClick = onPhotoLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (showSpoiler) Modifier.blur(22.dp) else Modifier)
            )
        }

        if (isVideo && !showSpoiler) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Воспроизвести",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (showSpoiler) {
            SpoilerOverlay(modifier = Modifier.fillMaxSize())
        }

        if (isPhotoSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accent.copy(alpha = 0.22f))
                    .border(2.dp, accent)
            )
        }

        if (showSelectionBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isPhotoSelected) accent else Color.Black.copy(alpha = 0.45f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isPhotoSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Выбрано",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}


