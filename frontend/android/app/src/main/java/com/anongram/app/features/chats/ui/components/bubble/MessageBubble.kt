package com.anongram.app.features.chats.ui.components.bubble

import com.anongram.app.features.chats.media.player.ChatTrack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.LinkPreview
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageRenderItem
import com.anongram.app.features.chats.messages.domain.MessageStatus
import com.anongram.app.features.chats.messages.domain.resolveSubItem
import com.anongram.app.features.chats.messages.domain.timeLabel
import com.anongram.app.features.chats.ui.components.LocalMessageTextSizeSp
import com.anongram.app.features.chats.ui.components.ReactionChips
import com.anongram.app.features.chats.ui.components.reply.ReplyAttachmentMiniThumb
import com.anongram.app.features.chats.ui.components.reply.replyPreviewSnippet
import kotlin.math.ceil
import kotlin.math.roundToInt

private val ReplyStripIncoming = Color(0xFF0B121E)
private val ReplyStripOutgoing = Color(0xFF081A2C)

/**
 * Модульный пузырь сообщения:
 * - Пропорционально уменьшает/увеличивает не только текст, но и сам бабл (отступы, скругления) и время с галочками (1..30 sp)
 * - Для коротких сообщений («привет») время идёт отдельным блоком справа после текста (чуть удлиняя пузырь)
 * - Для длинных сообщений время располагается в правом нижнем углу так, чтобы текст никогда не перекрывал дату и время
 * - В режиме выбора одиночный элемент альбома/группы показывает только галочку, а при выборе всей группы светится весь пузырь
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
    isPinned: Boolean = false,
    onQuoteClick: (String) -> Unit = {},
    onClick: (anchorYPx: Float) -> Unit = {},
    onLongClick: () -> Unit = {},
    onSwipeReply: () -> Unit = {},
    onReplySubItem: (Int) -> Unit = {},
    onToggleReaction: (String) -> Unit = {},
    onSelect: () -> Unit = {},
    onToggleAlbumPhoto: (Int) -> Unit = {},
    onEnterAlbumPhotoSelection: (Int) -> Unit = {},
    onPin: () -> Unit = {},
    onEdit: () -> Unit = {},
    onEditSubItem: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onOpenMediaViewer: (initialPhotoIndex: Int) -> Unit = {},
    onOpenUncompressedPhoto: (uri: String) -> Unit = {},
    onOpenContactChat: (name: String, phone: String) -> Unit = { _, _ -> },
    onForwardSubItem: (subKey: String) -> Unit = {},
    onDeleteSubItem: (subKey: String) -> Unit = {},
    onPlayVideoCircle: (ChatMessage) -> Unit = {},
    onPlayVoice: (ChatMessage) -> Unit = {},
    onPlayMusic: (ChatTrack) -> Unit = {},
    onOpenFullTextMessage: (ChatMessage) -> Unit = {}
) {
    val scheme = LocalAnonGramScheme.current
    val message = item.message
    val isIncoming = message.direction == MessageDirection.INCOMING
    val density = LocalDensity.current
    val messageTextSizeSp = LocalMessageTextSizeSp.current
    val textScale = remember(messageTextSizeSp) {
        (messageTextSizeSp / 16f).coerceIn(0.35f, 1.5f)
    }

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
    val bubbleTopYHolder = remember { FloatArray(1) }
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
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDx = 0f
                    var totalDy = 0f
                    var isHorizontalSwipe = false
                    val touchSlop = viewConfiguration.touchSlop
                    val maxDrag = with(density) { 90.dp.toPx() }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (isHorizontalSwipe && dragOffsetPx <= -thresholdPx) {
                                onSwipeReply()
                            }
                            dragOffsetPx = 0f
                            break
                        }

                        val dragChange = change.positionChange()
                        totalDx += dragChange.x
                        totalDy += dragChange.y

                        if (!isHorizontalSwipe) {
                            val absX = kotlin.math.abs(totalDx)
                            val absY = kotlin.math.abs(totalDy)
                            if (absX > touchSlop || absY > touchSlop) {
                                if (totalDx < -touchSlop && absX > absY * 1.5f) {
                                    // Осознанный свайп влево — захватываем жест
                                    isHorizontalSwipe = true
                                    change.consume()
                                } else {
                                    // Вертикальный скролл списка или свайп вправо — пропускаем для LazyColumn
                                    break
                                }
                            }
                        } else {
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + dragChange.x).coerceIn(-maxDrag, 0f)
                        }
                    }
                    dragOffsetPx = 0f
                }
            }
    ) {
        val replyIconThresholdPx = with(density) { 24.dp.toPx() }
        if (dragOffsetPx <= -replyIconThresholdPx) {
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
                        imageVector = Icons.AutoMirrored.Outlined.Reply,
                        contentDescription = "Ответить",
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val bubbleMaxWidth = when (message.attachment) {
            is MessageAttachment.File -> screenWidth * 0.84f
            is MessageAttachment.FileGroup, is MessageAttachment.MusicGroup -> screenWidth * 0.77f
            is MessageAttachment.Contact -> screenWidth * 0.78f
            is MessageAttachment.VideoCircle -> screenWidth * 0.96f
            else -> screenWidth * 0.80f
        }

        val incomingShiftDp by animateDpAsState(
            targetValue = if (isSelectionMode && isIncoming) 36.dp else 0.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "incomingShiftDp"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .onGloballyPositioned { coordinates ->
                    bubbleTopYHolder[0] = coordinates.positionInRoot().y
                }
                .padding(vertical = 1.dp)
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = shrinkHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) scheme.accent else Color(0x33000000))
                        .border(
                            width = 2.dp,
                            color = if (isSelected) scheme.accent else Color(0x88FFFFFF),
                            shape = CircleShape
                        )
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = incomingShiftDp),
                contentAlignment = if (isIncoming) Alignment.TopStart else Alignment.TopEnd
            ) {

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
                                onClick = { if (isSelectionMode) onSelect() else onClick(bubbleTopYHolder[0]) },
                                onLongClick = onLongClick
                            )
                        }
                    } else if (message.attachment is MessageAttachment.VideoCircle && message.text.isBlank()) {
                        Box(
                            modifier = if (pulseAlpha > 0f) {
                                Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(scheme.accent.copy(alpha = pulseAlpha * 0.35f))
                                    .padding(4.dp)
                            } else Modifier.padding(vertical = 2.dp)
                        ) {
                            ChatVideoCircleSection(
                                message = message,
                                attachment = message.attachment as MessageAttachment.VideoCircle,
                                isSelectionMode = isSelectionMode,
                                onSelect = onSelect,
                                onPlay = { onPlayVideoCircle(message) }
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
                            isSelected -> Color(0xFF3F2B66)
                            isEdgeToEdgeMedia -> Color(0xFF141418)
                            isIncoming -> CardDark
                            else -> Color(0xFF281C3D)
                        }
                        val bubbleColor = if (pulseAlpha > 0f) {
                            lerp(baseColor, scheme.accent, pulseAlpha)
                        } else {
                            baseColor
                        }
                        val bubbleBorder = when {
                            pulseAlpha > 0.005f -> BorderStroke(2.dp, scheme.accent.copy(alpha = pulseAlpha))
                            isEdgeToEdgeMedia -> BorderStroke(1.2.dp, scheme.accent.copy(alpha = 0.55f))
                            !isIncoming -> BorderStroke(1.dp, scheme.accent.copy(alpha = 0.4f))
                            else -> null
                        }

                        val shapeScale = if (message.attachment == null) textScale else 1f
                        val currentBubbleShape = bubbleShape(item, isIncoming, shapeScale)

                        Surface(
                            shape = currentBubbleShape,
                            color = bubbleColor,
                            border = bubbleBorder,
                            shadowElevation = (pulseAlpha * 16).dp,
                            modifier = Modifier
                                .widthIn(max = bubbleMaxWidth)
                                .clip(currentBubbleShape)
                                .combinedClickable(
                                    onClick = {
                                        if (isSelectionMode) {
                                            onSelect()
                                        } else if (isEdgeToEdgeMedia) {
                                            onOpenMediaViewer(0)
                                        } else {
                                            onClick(bubbleTopYHolder[0])
                                        }
                                    },
                                    onLongClick = onLongClick
                                )
                        ) {
                            val padStart = (11f * textScale).coerceIn(5f, 15f).dp
                            val padEnd = (10f * textScale).coerceIn(5f, 14f).dp
                            val padTop = (7f * textScale).coerceIn(2.5f, 10f).dp
                            val padBottom = (6f * textScale).coerceIn(2.5f, 9f).dp

                            Box {
                                Column(
                                    modifier = when {
                                        isEdgeToEdgeMedia -> Modifier.padding(
                                            start = 1.5.dp,
                                            end = 1.5.dp,
                                            top = 1.5.dp,
                                            bottom = if (message.text.isNotBlank()) padBottom else 1.5.dp
                                        )
                                        isFlushGroupAttachment -> Modifier.padding(
                                            start = 4.dp,
                                            end = 6.dp,
                                            top = 4.dp,
                                            bottom = if (message.text.isNotBlank()) padBottom else 4.dp
                                        )
                                        else -> Modifier.padding(
                                            start = padStart,
                                            end = padEnd,
                                            top = padTop,
                                            bottom = padBottom
                                        )
                                    }
                                ) {
                                    // Пометка пересылки (шаг 10)
                                    message.forwardedFrom?.let { author ->
                                        Box(
                                            modifier = if (isEdgeToEdgeMedia || isFlushGroupAttachment) {
                                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            } else Modifier
                                        ) {
                                            ForwardedBlock(author = author, textScale = textScale)
                                        }
                                        Spacer(modifier = Modifier.height((4f * textScale).coerceIn(2f, 6f).dp))
                                    }

                                    // Цитата ответа (шаг 1)
                                    replyTo?.let { rawTarget ->
                                        val target = remember(rawTarget, message.replyToSubIndex) {
                                            rawTarget.resolveSubItem(message.replyToSubIndex)
                                        }
                                        Box(
                                            modifier = if (isEdgeToEdgeMedia || isFlushGroupAttachment) {
                                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            } else Modifier
                                        ) {
                                            QuoteBlock(
                                                target = target,
                                                author = if (target.direction == MessageDirection.INCOMING) peerName else "Вы",
                                                accent = scheme.accent,
                                                isIncoming = isIncoming,
                                                textScale = textScale,
                                                onClick = { onQuoteClick(target.id) }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height((5f * textScale).coerceIn(2f, 7f).dp))
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
                                                isPinned = isPinned,
                                                onSelect = onSelect,
                                                onToggleSubItem = onToggleAlbumPhoto,
                                                onEnterSubItemSelection = onEnterAlbumPhotoSelection,
                                                onLongClick = onLongClick,
                                                onRetry = onRetry,
                                                onSwipeReply = onSwipeReply,
                                                onReplySubItem = onReplySubItem,
                                                onPin = onPin,
                                                onEdit = onEdit,
                                                onEditSubItem = onEditSubItem,
                                                onOpenPhotoAt = { idx -> onOpenMediaViewer(idx) },
                                                onOpenContactChat = onOpenContactChat,
                                                onForwardSubItem = onForwardSubItem,
                                                onDeleteSubItem = onDeleteSubItem,
                                                onPlayVideoCircle = onPlayVideoCircle,
                                                onPlayVoice = onPlayVoice,
                                                onPlayMusic = onPlayMusic
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
                                            Spacer(modifier = Modifier.height((6f * textScale).coerceIn(3f, 8f).dp))
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

                                    // Текст сообщения + отдельный блок времени (модульный BubbleTextMessage)
                                    if (message.text.isNotBlank()) {
                                        Box(
                                            modifier = if (isEdgeToEdgeMedia || isFlushGroupAttachment) {
                                                Modifier.padding(horizontal = (10f * textScale).coerceIn(5f, 12f).dp)
                                            } else {
                                                Modifier
                                            }
                                        ) {
                                            BubbleTextMessage(
                                                message = message,
                                                textSizeSp = messageTextSizeSp,
                                                textScale = textScale,
                                                searchQuery = searchQuery,
                                                onOpenFullText = { onOpenFullTextMessage(message) },
                                                onRetry = onRetry
                                            )
                                        }
                                    } else if (!isOverlayTimestampOnMedia && !isTimestampHandledInsideAttachment) {
                                        BubbleTimestampRow(
                                            timeLabel = message.timeLabel(),
                                            isEdited = message.isEdited,
                                            isIncoming = isIncoming,
                                            status = message.status,
                                            textScale = textScale,
                                            onRetry = onRetry,
                                            modifier = Modifier.align(Alignment.End)
                                        )
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
private fun ForwardedBlock(
    author: String,
    textScale: Float = 1f
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size((12f * textScale).coerceIn(7f, 16f).dp)
        )
        Spacer(modifier = Modifier.width((4f * textScale).coerceIn(2f, 6f).dp))
        Text(
            text = "Переслано от $author",
            color = TextMuted,
            fontSize = (11f * textScale).coerceIn(6f, 15f).sp,
            fontStyle = FontStyle.Italic
        )
    }
}

/** Маршрутизатор содержимого вложения в пузыре по модулям */
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
    isPinned: Boolean = false,
    onSelect: () -> Unit = {},
    onToggleSubItem: (Int) -> Unit = {},
    onEnterSubItemSelection: (Int) -> Unit = {},
    onLongClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSwipeReply: () -> Unit = {},
    onReplySubItem: (Int) -> Unit = {},
    onPin: () -> Unit = {},
    onEdit: () -> Unit = {},
    onEditSubItem: (Int) -> Unit = {},
    onOpenPhotoAt: (Int) -> Unit = {},
    onOpenContactChat: (String, String) -> Unit = { _, _ -> },
    onForwardSubItem: (String) -> Unit = {},
    onDeleteSubItem: (String) -> Unit = {},
    onPlayVideoCircle: (ChatMessage) -> Unit = {},
    onPlayVoice: (ChatMessage) -> Unit = {},
    onPlayMusic: (ChatTrack) -> Unit = {}
) {
    when (attachment) {
        is MessageAttachment.Photo -> {
            ChatSinglePhotoSection(
                attachment = attachment,
                accent = accent,
                searchQuery = searchQuery,
                singlePhotoStretch = singlePhotoStretch,
                isSelectionMode = isSelectionMode,
                onSelect = onSelect,
                onLongClick = onLongClick,
                onOpenPhotoAt = onOpenPhotoAt
            )
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
                fileIndex = 0,
                isIncoming = isIncoming,
                accent = accent,
                searchQuery = searchQuery,
                showThreeDots = true,
                showBottomTimestamp = showInlineTimestamp,
                isSelectionMode = isSelectionMode,
                isItemSelected = 0 in selectedSubIndices,
                isPinned = isPinned,
                onItemClick = {
                    if (isSelectionMode) onSelect() else null
                },
                onItemLongClick = onLongClick,
                onSwipeReply = { onReplySubItem(0) },
                onForwardItem = { onForwardSubItem(message.id) },
                onPinItem = onPin,
                onEditItem = { onEditSubItem(0) },
                onDeleteItem = { onDeleteSubItem(message.id) },
                onRetry = onRetry,
                onOpenPhotoAt = onOpenPhotoAt
            )
        }

        is MessageAttachment.FileGroup -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                attachment.items.forEachIndexed { idx, fileItem ->
                    val isItemSelected = idx in selectedSubIndices
                    ChatFileItemRow(
                        message = message,
                        file = fileItem,
                        fileIndex = idx,
                        isIncoming = isIncoming,
                        accent = accent,
                        searchQuery = searchQuery,
                        showThreeDots = (idx == 0),
                        showBottomTimestamp = showInlineTimestamp && (idx == attachment.items.lastIndex),
                        isSelectionMode = isSelectionMode,
                        isItemSelected = isItemSelected,
                        isPinned = isPinned,
                        onItemClick = {
                            if (isSelectionMode) onToggleSubItem(idx) else null
                        },
                        onItemLongClick = {
                            if (isSelectionMode) onToggleSubItem(idx) else onEnterSubItemSelection(idx)
                        },
                        onSwipeReply = { onReplySubItem(idx) },
                        onForwardItem = { onForwardSubItem("${message.id}#file_$idx") },
                        onPinItem = onPin,
                        onEditItem = { onEditSubItem(idx) },
                        onDeleteItem = { onDeleteSubItem("${message.id}#file_$idx") },
                        onRetry = onRetry,
                        onOpenPhotoAt = onOpenPhotoAt
                    )
                }
            }
        }

        is MessageAttachment.Voice -> {
            ChatVoiceSection(
                message = message,
                attachment = attachment,
                accent = accent,
                onPlay = { onPlayVoice(message) }
            )
        }

        is MessageAttachment.VideoCircle -> {
            ChatVideoCircleSection(
                message = message,
                attachment = attachment,
                isSelectionMode = isSelectionMode,
                onSelect = onSelect,
                onPlay = { onPlayVideoCircle(message) }
            )
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
                isItemSelected = 0 in selectedSubIndices,
                isPinned = isPinned,
                onPlayTrack = onPlayMusic,
                onItemClick = { if (isSelectionMode) onSelect() },
                onItemLongClick = onLongClick,
                onSwipeReply = { onReplySubItem(0) },
                onForwardItem = { onForwardSubItem(message.id) },
                onPinItem = onPin,
                onEditItem = { onEditSubItem(0) },
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
                        isPinned = isPinned,
                        onPlayTrack = onPlayMusic,
                        onItemClick = {
                            if (isSelectionMode) onToggleSubItem(idx)
                        },
                        onItemLongClick = {
                            if (isSelectionMode) onToggleSubItem(idx) else onEnterSubItemSelection(idx)
                        },
                        onSwipeReply = { onReplySubItem(idx) },
                        onForwardItem = { onForwardSubItem("${message.id}#music_$idx") },
                        onPinItem = onPin,
                        onEditItem = { onEditSubItem(idx) },
                        onDeleteItem = { onDeleteSubItem("${message.id}#music_$idx") },
                        onRetry = onRetry
                    )
                }
            }
        }

        is MessageAttachment.Contact -> {
            ChatContactCardSection(
                attachment = attachment,
                isIncoming = isIncoming,
                accent = accent,
                searchQuery = searchQuery,
                isSelectionMode = isSelectionMode,
                onSelect = onSelect,
                onOpenContactChat = onOpenContactChat
            )
        }

        is MessageAttachment.Geo -> {
            ChatGeoSection(
                attachment = attachment,
                accent = accent,
                searchQuery = searchQuery
            )
        }

        is MessageAttachment.Sticker -> {
            Text(text = attachment.symbol, fontSize = 48.sp)
        }
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
    target: ChatMessage? = null,
    author: String,
    accent: Color,
    isIncoming: Boolean,
    textScale: Float = 1f,
    onClick: () -> Unit
) {
    val text = if (target != null) replyPreviewSnippet(target) else "Вложение"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((8f * textScale).coerceIn(4f, 10f).dp))
            .background(if (isIncoming) ReplyStripIncoming else ReplyStripOutgoing)
            .clickable(onClick = onClick)
            .padding(
                horizontal = (8f * textScale).coerceIn(4f, 10f).dp,
                vertical = (5f * textScale).coerceIn(2.5f, 7f).dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width((3f * textScale).coerceIn(2f, 4f).dp)
                .height((32f * textScale).coerceIn(16f, 40f).dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width((8f * textScale).coerceIn(4f, 10f).dp))
        if (target != null && target.attachment != null) {
            ReplyAttachmentMiniThumb(
                message = target,
                accent = accent,
                size = (32f * textScale).coerceIn(20f, 36f).dp
            )
            Spacer(modifier = Modifier.width((8f * textScale).coerceIn(4f, 10f).dp))
        }
        Column {
            Text(
                text = author,
                color = accent,
                fontSize = (11f * textScale).coerceIn(6f, 15f).sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = text,
                color = TextSecondary,
                fontSize = (12f * textScale).coerceIn(6.5f, 16f).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

