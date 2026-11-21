package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.navigation.PhysicsSprings
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.mainmenu.i18n.ChatsMenuI18n
import com.anongram.app.features.chats.ui.accentColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Карточка строки чата в стиле Telegram:
 * - Тематическая подложка `scheme.card` (чтобы тема была чётко видна во всех палитрах);
 * - В режиме выбора слева РЯДОМ с аватаром плавно выезжает кружок (закрашенный с галочкой или контурный);
 * - Свайп влево (когда `!selectionMode`) плавно открывает акцентную плашку «В архив» / «Из архива»;
 * - Приятная скруглённая иконка закрепа `Icons.Rounded.PushPin`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatListItemCard(
    session: IChatSession,
    timeLabel: String,
    selectionMode: Boolean,
    isSelected: Boolean,
    draftText: String? = null,
    isArchiveMode: Boolean = false,
    onSwipeArchive: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ChatsMenuI18n.current
    val accent = session.accentColor
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val swipeThresholdPx = with(density) { 84.dp.toPx() }
    val maxSwipePx = with(density) { 128.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var hapticTriggered by remember { mutableStateOf(false) }

    val rowBg by animateColorAsState(
        targetValue = if (isSelected) {
            scheme.accent.copy(alpha = 0.16f)
        } else {
            scheme.card.copy(alpha = 0.78f)
        },
        animationSpec = tween(160),
        label = "chatRowBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Фоновая плашка «В архив» / «Из архива» при свайпе влево
        if (!selectionMode && onSwipeArchive != null && offsetX.value < -4f) {
            val isReady = offsetX.value <= -swipeThresholdPx
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        scheme.accent.copy(alpha = if (isReady) 0.96f else 0.72f)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isArchiveMode) Icons.Outlined.Unarchive else Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isArchiveMode) strings.fromArchive else strings.toArchive,
                        color = scheme.onAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(16.dp))
                .background(rowBg)
                .border(
                    width = 0.6.dp,
                    color = if (isSelected) scheme.accent.copy(alpha = 0.45f) else scheme.cardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .then(
                    if (!selectionMode && onSwipeArchive != null) {
                        Modifier.pointerInput(session.id, selectionMode) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val shouldTrigger = offsetX.value <= -swipeThresholdPx
                                    hapticTriggered = false
                                    scope.launch {
                                        if (shouldTrigger) {
                                            onSwipeArchive()
                                        }
                                        offsetX.animateTo(0f, PhysicsSprings.screenSpringSpec())
                                    }
                                },
                                onDragCancel = {
                                    hapticTriggered = false
                                    scope.launch {
                                        offsetX.animateTo(0f, PhysicsSprings.screenSpringSpec())
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    // Свайп ТОЛЬКО влево (offsetX <= 0); правый свайп заблокирован
                                    val next = (offsetX.value + dragAmount).coerceIn(-maxSwipePx, 0f)
                                    scope.launch { offsetX.snapTo(next) }
                                    if (next <= -swipeThresholdPx && !hapticTriggered) {
                                        hapticTriggered = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (next > -swipeThresholdPx) {
                                        hapticTriggered = false
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кружок выбора РЯДОМ с аватаром (у всех чатов, как только выбран хотя бы один)
            AnimatedVisibility(
                visible = selectionMode,
                enter = expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(180)) + fadeIn(tween(150)),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(160)) + fadeOut(tween(120))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) scheme.accent else scheme.surface.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.5.dp,
                                color = if (isSelected) Color.Transparent else scheme.textMuted.copy(alpha = 0.65f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSelected,
                            enter = scaleIn(tween(140)) + fadeIn(tween(120)),
                            exit = scaleOut(tween(100)) + fadeOut(tween(100))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = strings.selected,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            ChatAvatar(
                session = session,
                accent = accent
            )

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (session.isPinned) {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = null,
                                tint = scheme.accent.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .size(13.dp)
                                    .rotate(35f)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = session.title,
                            fontWeight = FontWeight.Bold,
                            color = scheme.textPrimary,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (session.lastMessagePreview.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeLabel,
                            color = scheme.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val draftRed = Color(0xFFE53935)
                val preview = when {
                    !draftText.isNullOrBlank() -> buildAnnotatedString {
                        withStyle(SpanStyle(color = draftRed, fontWeight = FontWeight.SemiBold)) {
                            append("Черновик: ")
                        }
                        withStyle(SpanStyle(color = scheme.textSecondary)) {
                            append(draftText)
                        }
                    }
                    session.lastMessagePreview.isNotEmpty() -> buildAnnotatedString {
                        if (session.lastMessageIsOwn) {
                            withStyle(SpanStyle(color = scheme.accent, fontWeight = FontWeight.SemiBold)) {
                                append("Вы: ")
                            }
                        }
                        append(session.lastMessagePreview)
                    }
                    else -> null
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (preview != null) {
                        Text(
                            text = preview,
                            color = scheme.textSecondary,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = accent.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${session.securityLabel} · ${session.fingerprint}",
                                color = scheme.textMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    ChatTrailingBadges(session = session)
                }
            }
        }
    }
}
