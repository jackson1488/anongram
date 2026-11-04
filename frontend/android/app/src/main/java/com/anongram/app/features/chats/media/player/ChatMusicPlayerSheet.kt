package com.anongram.app.features.chats.media.player

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Адаптивная шторка музыкального плеера чата в стиле iOS Apple Music (ТЗ):
 * - Фиксированная карточка плеера сверху (обложка, название, iOS-скраббер, центрированные кнопки)
 * - Список треков текущего чата плавно скроллится под карточкой
 * - Адаптивная высота (для 1..3 треков компактно, при большем количестве — до 90% экрана)
 * - Кастомное меню трёх точек ⋮ со скруглением 16 dp
 * - Кнопка перехода в полноэкранный плеер
 */
@Composable
fun ChatMusicPlayerSheet(
    onDismiss: () -> Unit,
    controller: ChatAudioController? = null,
    onForwardTrack: (ChatTrack) -> Unit = {},
    onShareTrack: ((ChatTrack) -> Unit)? = null,
    onShowInChat: ((ChatTrack) -> Unit)? = null,
    onJumpToMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ctrl = controller ?: ChatAudioController.getInstance(context)
    val state by ctrl.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = LocalAnonGramScheme.current

    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    val sheetOffsetAnim = remember { Animatable(screenHeightPx) }
    var isClosing by remember { mutableStateOf(false) }

    fun dismissAnimated() {
        if (isClosing) return
        isClosing = true
        scope.launch {
            sheetOffsetAnim.animateTo(screenHeightPx, tween(260, easing = FastOutLinearInEasing))
            ctrl.closeSheet()
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        sheetOffsetAnim.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
    }

    BackHandler {
        dismissAnimated()
    }

    val scrimAlpha = ((1f - (sheetOffsetAnim.value / screenHeightPx)) * 0.65f).coerceIn(0f, 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissAnimated() }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeightDp * 0.88f)
                .align(Alignment.BottomCenter)
                .offset { IntOffset(0, sheetOffsetAnim.value.roundToInt()) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(IslandBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                // Полоска-держатель
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 12.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                )

                // 1. СПИСОК ТРЕКОВ ЧАТА СВЕРХУ (до 50 треков с адаптивной высотой)
                val trackCount = state.queue.size
                val maxListHeight = when {
                    trackCount <= 1 -> 68.dp
                    trackCount <= 2 -> 132.dp
                    trackCount <= 3 -> 196.dp
                    else -> (screenHeightDp * 0.52f).coerceIn(240.dp, 440.dp)
                }

                if (state.queue.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxListHeight)
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        items(state.queue, key = { it.id }) { track ->
                            val isCurrent = track.id == state.currentTrack?.id
                            ChatQueueTrackRow(
                                track = track,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && state.isPlaying,
                                accent = scheme.accent,
                                onPlay = { ctrl.play(state.queue, track) },
                                onSave = {
                                    Toast.makeText(context, "Сохранено: ${track.title}", Toast.LENGTH_SHORT).show()
                                },
                                onForward = {
                                    dismissAnimated()
                                    onForwardTrack(track)
                                },
                                onShare = {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "${track.artist} — ${track.title}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Поделиться треком"))
                                    } catch (_: Throwable) {}
                                },
                                onShowInChat = {
                                    dismissAnimated()
                                    if (onShowInChat != null) {
                                        onShowInChat(track)
                                    } else {
                                        onJumpToMessage?.invoke(track.messageId)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }

                    // Тонкий разделитель над карточкой плеера
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }

                // 2. ФИКСИРОВАННАЯ КАРТОЧКА ПЛЕЕРА В САМОМ НИЗУ (всегда под рукой)
                val currentTrack = state.currentTrack
                if (currentTrack != null) {
                    val artBitmap = currentTrack.albumArtBitmap
                        ?: AudioMetadataHelper.getCachedArt(currentTrack.uri.toString())
                        ?: AudioMetadataHelper.generateFallbackArt(currentTrack.title, currentTrack.artist)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Обложка + Название + Кнопка FullScreen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E2130))
                                    .clickable {
                                        ctrl.openFullScreen()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (artBitmap != null) {
                                    Image(
                                        bitmap = artBitmap.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = scheme.accent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { ctrl.openFullScreen() }
                            ) {
                                MarqueeText(
                                    text = currentTrack.title,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentTrack.artist.ifBlank { "AnonGram" },
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Кнопка разворота в полный экран
                            IconButton(
                                onClick = { ctrl.openFullScreen() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Fullscreen,
                                    contentDescription = "Полный экран",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Плавный iOS-скраббер
                        var isDragging by remember { mutableStateOf(false) }
                        var dragPos by remember { mutableFloatStateOf(0f) }
                        val currentScrubProgress = if (isDragging) dragPos else state.progress

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .pointerInput(state.durationMs) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            isDragging = true
                                            dragPos = (offset.x / size.width).coerceIn(0f, 1f)
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            val targetMs = (dragPos * state.durationMs.coerceAtLeast(1L)).toLong()
                                            ctrl.seek(targetMs)
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                        },
                                        onHorizontalDrag = { change, _ ->
                                            change.consume()
                                            dragPos = (change.position.x / size.width).coerceIn(0f, 1f)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val widthPx = constraints.maxWidth.toFloat()
                            val thumbXPx = (currentScrubProgress * widthPx).coerceIn(0f, widthPx)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.18f))
                            )
                            Box(
                                modifier = Modifier
                                    .width(with(density) { thumbXPx.toDp() })
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((thumbXPx - if (isDragging) 7.dp.toPx() else 5.dp.toPx()).roundToInt(), 0) }
                                    .size(if (isDragging) 14.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.positionLabel,
                                color = TextMuted,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = state.remainingLabel,
                                color = TextMuted,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Центрированные кнопки управления: стиль шторки Android + реальный Shuffle и Repeat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Перемешать (РЕАЛЬНЫЙ SHUFFLE БЕЗ TODO!)
                            IconButton(
                                onClick = { ctrl.toggleShuffle() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (state.isShuffleEnabled) scheme.accent else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Предыдущий (стиль шторки Android со скругленной подложкой)
                            IconButton(
                                onClick = { ctrl.prev() },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Предыдущий",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Большая центральная Play/Pause
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(scheme.accent)
                                    .clickable { ctrl.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Пауза" else "Играть",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Следующий (стиль шторки Android со скругленной подложкой)
                            IconButton(
                                onClick = { ctrl.next() },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Следующий",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Зацикливание (РЕАЛЬНЫЙ REPEAT БЕЗ TODO!)
                            IconButton(
                                onClick = { ctrl.cycleRepeat() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                val repeatIcon = when (state.repeatMode) {
                                    ChatRepeatMode.ONE -> Icons.Rounded.RepeatOne
                                    else -> Icons.Rounded.Repeat
                                }
                                val repeatTint = if (state.repeatMode != ChatRepeatMode.OFF) scheme.accent else Color.White.copy(alpha = 0.6f)
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = "Repeat",
                                    tint = repeatTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Строка трека в очереди воспроизведения шторки.
 */
@Composable
internal fun ChatQueueTrackRow(
    track: ChatTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onPlay: () -> Unit,
    onSave: () -> Unit,
    onForward: () -> Unit,
    onShare: () -> Unit,
    onShowInChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isCurrent) accent else Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrent) accent else TextPrimary,
                fontSize = 14.5.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist.ifBlank { "AnonGram" }} • ${track.durationLabel}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(
                onClick = { isMenuOpen = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Опции",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Кастомное меню с закруглением 16.dp в стиле темы чата
            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = { isMenuOpen = false },
                modifier = Modifier
                    .width(220.dp)
                    .background(Color(0xFF161926), RoundedCornerShape(16.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            ) {
                DropdownMenuItem(
                    text = { Text("Сохранить в загрузки", color = Color.White, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.FileDownload, null, tint = accent, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        isMenuOpen = false
                        onSave()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Переслать", color = Color.White, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Outlined.Reply, null, tint = accent, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        isMenuOpen = false
                        onForward()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Поделиться", color = Color.White, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Share, null, tint = accent, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        isMenuOpen = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Показать в чате", color = Color.White, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Visibility, null, tint = accent, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        isMenuOpen = false
                        onShowInChat()
                    }
                )
            }
        }
    }
}
