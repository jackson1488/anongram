package com.anongram.app.features.chats.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
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
import com.anongram.app.features.chats.messages.domain.attachmentColor
import com.anongram.app.features.chats.messages.domain.timeLabel
import kotlin.math.roundToInt

private val ReplyStripIncoming = Color(0xFF0B121E)
private val ReplyStripOutgoing = Color(0xFF081A2C)

/**
 * Пузырь сообщения по ТЗ (шаги 1, 2, 4–7, 9–11, 15, 16):
 * - Свои / чужие цвета, хвостик у последнего в группе
 * - Поддержка всех типов вложений (фото, файл, голос, видео-кружок, музыка, контакт, гео, стикер)
 * - Превью ссылок
 * - Цитата ответа с тап-скроллом
 * - Пометка «изменено» и «переслано от»
 * - Время и тики (часы → одна → две галочки / красный «!» при сбое)
 * - Свайп для ответа (жест влево/вправо)
 * - Долгий тап для контекстного меню
 * - Режим выделения с чекбоксом
 * - Реакции-чипы под пузырём
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
    isHighlighted: Boolean = false,
    onQuoteClick: (String) -> Unit = {},
    onClick: (anchorYPx: Float) -> Unit = {},
    onLongClick: () -> Unit = {},
    onSwipeReply: () -> Unit = {},
    onToggleReaction: (String) -> Unit = {},
    onSelect: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val scheme = LocalAnonGramScheme.current
    val message = item.message
    val isIncoming = message.direction == MessageDirection.INCOMING
    val density = LocalDensity.current

    val pulseAnim = remember { Animatable(0f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            pulseAnim.snapTo(0f)
            pulseAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 750
                    0.85f at 120 using FastOutSlowInEasing
                    0.15f at 220 using FastOutSlowInEasing
                    0.95f at 340 using FastOutSlowInEasing
                    0.0f at 750 using LinearOutSlowInEasing
                }
            )
        } else {
            pulseAnim.snapTo(0f)
        }
    }

    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var bubbleTopY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetPx by animateFloatAsState(targetValue = dragOffsetPx, label = "swipeReply")
    val thresholdPx = with(density) { 60.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
                        // Свайп строго справа налево (константно)
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(-maxDrag, 0f)
                    }
                )
            }
    ) {
        // Подложка с иконкой ответа при свайпе строго справа налево
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
            // Чекбокс в режиме выделения: плавно выезжает, синхронно сдвигая пузыри вправо (как в Telegram)
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
                val bubbleMaxWidth = maxWidth * 0.82f

                Column(
                    horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
                ) {
                    val pulseAlpha = pulseAnim.value
                    if (message.isStickerOnly) {
                        // Стикер рисуется без пузыря (шаг 6) с подсветкой при сердцебиении
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
                        val baseColor = when {
                            isSelected -> scheme.accent.copy(alpha = 0.28f)
                            isIncoming -> CardDark
                            else -> scheme.accent.copy(alpha = 0.16f)
                        }
                        val bubbleColor = if (pulseAlpha > 0f) {
                            lerp(baseColor, scheme.accent, pulseAlpha * 0.75f)
                        } else {
                            baseColor
                        }
                        val bubbleBorder = when {
                            pulseAlpha > 0f -> BorderStroke(2.dp, scheme.accent.copy(alpha = 0.5f + pulseAlpha * 0.5f))
                            !isIncoming -> BorderStroke(1.dp, scheme.accent.copy(alpha = 0.4f))
                            else -> null
                        }

                        Surface(
                            shape = bubbleShape(item, isIncoming),
                            color = bubbleColor,
                            border = bubbleBorder,
                            modifier = Modifier
                                .widthIn(max = bubbleMaxWidth)
                                .clip(bubbleShape(item, isIncoming))
                                .combinedClickable(
                                    onClick = { if (isSelectionMode) onSelect() else onClick(bubbleTopY) },
                                    onLongClick = onLongClick
                                )
                        ) {
                            Box {
                                Column(
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)
                                ) {
                                    // Пометка пересылки (шаг 10)
                                    message.forwardedFrom?.let { author ->
                                        ForwardedBlock(author = author)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    // Цитата ответа (шаг 1)
                                    replyTo?.let { target ->
                                        QuoteBlock(
                                            author = if (target.direction == MessageDirection.INCOMING) peerName else "Вы",
                                            text = target.text.ifBlank { "Вложение" },
                                            accent = scheme.accent,
                                            isIncoming = isIncoming,
                                            onClick = { onQuoteClick(target.id) }
                                        )
                                        Spacer(modifier = Modifier.height(5.dp))
                                    }

                                    // Отрисовка вложений (шаги 4–6)
                                    message.attachment?.let { attachment ->
                                        AttachmentContent(
                                            attachment = attachment,
                                            isIncoming = isIncoming,
                                            accent = scheme.accent,
                                            searchQuery = searchQuery
                                        )
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

                                    // Текст сообщения с подсветкой кусочка слова
                                    if (message.text.isNotBlank()) {
                                        val displayText = if (searchQuery.isNotBlank()) {
                                            buildHighlightedText(
                                                text = message.text,
                                                query = searchQuery,
                                                highlightColor = scheme.accent
                                            )
                                        } else {
                                            AnnotatedString(message.text)
                                        }
                                        Text(
                                            text = displayText,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            lineHeight = 19.sp
                                        )
                                    }

                                    // Время, статус и «изменено»
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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

                                // Наложение пульсации сердцебиения для всех типов сообщений (включая фото/вложения)
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
    attachment: MessageAttachment,
    isIncoming: Boolean,
    accent: Color,
    searchQuery: String = ""
) {
    when (attachment) {
        is MessageAttachment.Photo -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(attachmentColor(attachment.colorSeed)),
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
            }
        }

        is MessageAttachment.File -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${attachment.kind.label} • ${attachment.sizeLabel}",
                        color = TextMuted,
                        fontSize = 11.sp
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
                // Волновая диаграмма голоса
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Играть",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            buildHighlightedText(attachment.title, searchQuery, accent)
                        } else {
                            AnnotatedString(attachment.title)
                        },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            buildHighlightedText("${attachment.artist} • ${attachment.durationLabel}", searchQuery, accent)
                        } else {
                            AnnotatedString("${attachment.artist} • ${attachment.durationLabel}")
                        },
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        is MessageAttachment.Contact -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = attachment.name.firstOrNull()?.uppercase() ?: "?",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            buildHighlightedText(attachment.name, searchQuery, accent)
                        } else {
                            AnnotatedString(attachment.name)
                        },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            buildHighlightedText(attachment.phone, searchQuery, accent)
                        } else {
                            AnnotatedString(attachment.phone)
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
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
