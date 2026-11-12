package com.anongram.app.features.chats.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.features.chats.input.banner.ChatActionBannerBus
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.viewer.i18n.ViewerI18n
import com.anongram.app.features.chats.viewer.video.ViewerVideoPage
import com.anongram.app.features.chats.viewer.video.ViewerVideoPlaybackState
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Полноэкранный просмотрщик фото и видео в стиле Telegram:
 * - Сквозное перелистывание по всем сжатым фото/видео чата (или внутри FileGroup)
 * - Интерактивный свайп вверх И вниз с любой точки экрана с проявлением чата под прозрачным фоном
 * - Фото: Pinch-to-Zoom (1x..4x), Double-Tap (1x <-> 2.5x), ContentScale.Fit под любые пропорции
 * - Видео: встроенный плеер на TextureView + MediaPlayer, скорость 0.2x..2x, зацикливание, SeekBar, жесты ±5с и удержания
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMediaFullscreenViewer(
    message: ChatMessage,
    initialPhotoIndex: Int,
    peerName: String,
    allChatMessages: List<ChatMessage> = emptyList(),
    overrideSingleUri: String? = null,
    onClose: () -> Unit,
    onShowInChat: (ChatMessage) -> Unit,
    onReply: (ChatMessage, Int?) -> Unit = { _, _ -> },
    onForward: (List<String>) -> Unit = {},
    onRequestDelete: (ChatMessage, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = ViewerI18n.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx().coerceAtLeast(800f) }
    val dismissThresholdPx = screenHeightPx * 0.18f

    val isUncompressedMode = remember(message.id, message.attachment, overrideSingleUri) {
        !overrideSingleUri.isNullOrBlank() ||
            message.attachment is MessageAttachment.File ||
            message.attachment is MessageAttachment.FileGroup
    }

    val viewerEntries = remember(allChatMessages, message, overrideSingleUri, isUncompressedMode) {
        buildViewerEntries(allChatMessages, message, overrideSingleUri, isUncompressedMode)
    }

    if (viewerEntries.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val initialPageIndex = remember(message.id, initialPhotoIndex, overrideSingleUri) {
        val byIndex = viewerEntries.indexOfFirst { entry ->
            entry.message.id == message.id && entry.itemIndexInMessage == initialPhotoIndex
        }
        if (byIndex >= 0) {
            byIndex
        } else if (!overrideSingleUri.isNullOrBlank()) {
            val byUri = viewerEntries.indexOfFirst { it.uri == overrideSingleUri }
            if (byUri >= 0) byUri else 0
        } else {
            val byMsg = viewerEntries.indexOfFirst { it.message.id == message.id }
            if (byMsg >= 0) byMsg else 0
        }
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, (viewerEntries.size - 1).coerceAtLeast(0)),
        pageCount = { viewerEntries.size }
    )
    val thumbListState = rememberLazyListState()

    var showControls by remember { mutableStateOf(true) }
    var isSettingsMenuOpen by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }
    var activePageScale by remember { mutableFloatStateOf(1f) }
    var resetZoomSignal by remember { mutableIntStateOf(0) }

    // Параметры видеоплеера (скорость и зацикливание)
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isLooping by remember { mutableStateOf(false) }
    val videoPlaybackState = remember { ViewerVideoPlaybackState() }

    // Состояние вертикального свайпа вверх/вниз для закрытия
    val dragOffsetY = remember { Animatable(0f) }
    var isVerticalDragging by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    val safeCurrentPage = pagerState.currentPage.coerceIn(0, (viewerEntries.size - 1).coerceAtLeast(0))
    val currentEntry = viewerEntries[safeCurrentPage]
    val activeMessage = currentEntry.message
    val activeAlbumUris = currentEntry.albumUrisInMessage
    val activeAlbumFlags = currentEntry.albumVideoFlagsInMessage
    val activePosInAlbum = currentEntry.albumPositionInGroup

    val globalMediaCounterText: String? = remember(isUncompressedMode, viewerEntries.size, safeCurrentPage) {
        if (isUncompressedMode || viewerEntries.isEmpty()) null
        else strings.counterOf(safeCurrentPage + 1, viewerEntries.size)
    }

    BackHandler {
        if (activePageScale > 1.02f) {
            resetZoomSignal += 1
            activePageScale = 1f
        } else {
            onClose()
        }
    }

    LaunchedEffect(safeCurrentPage, activeMessage.id, activePosInAlbum) {
        activePageScale = 1f
        isSettingsMenuOpen = false
        isMoreMenuOpen = false
        if (activeAlbumUris.size > 1) {
            runCatching {
                thumbListState.animateScrollToItem(
                    activePosInAlbum.coerceIn(0, activeAlbumUris.lastIndex)
                )
            }
        }
    }

    val dateSubTitle = remember(activeMessage.epochMs, strings.isRussian) {
        formatViewerSubtitleDate(activeMessage.epochMs, strings)
    }

    val swipeProgress = (abs(dragOffsetY.value) / (screenHeightPx * 0.55f)).coerceIn(0f, 1f)
    val backdropAlpha = (0.96f * (1f - swipeProgress)).coerceIn(0f, 0.96f)
    val dragScale = 1f - (0.12f * swipeProgress)
    val controlsVisible = showControls && !isVerticalDragging && !isDismissing && abs(dragOffsetY.value) < 8f

    var isCaptionExpanded by remember(activeMessage.id) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backdropAlpha))
            .zIndex(300f)
            .pointerInput(activePageScale, isDismissing) {
                if (activePageScale > 1.02f || isDismissing) return@pointerInput
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var totalDx = 0f
                    var totalDy = 0f
                    var dragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size > 1) {
                            if (dragging) break
                            continue
                        }
                        val change = pressed.first()
                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y

                        if (!dragging) {
                            if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx) * 1.15f) {
                                dragging = true
                                isVerticalDragging = true
                            } else if (abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy) * 1.25f) {
                                // Горизонтальный свайп отдаём HorizontalPager
                                break
                            }
                        }

                        if (dragging) {
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch {
                                dragOffsetY.snapTo(dragOffsetY.value + delta.y)
                            }
                        }
                    }

                    if (dragging) {
                        val velocityY = velocityTracker.calculateVelocity().y
                        val currentY = dragOffsetY.value
                        val shouldDismiss = abs(currentY) > dismissThresholdPx || abs(velocityY) > 1100f
                        if (shouldDismiss) {
                            isDismissing = true
                            val directionSign = when {
                                abs(velocityY) > 500f -> if (velocityY >= 0f) 1f else -1f
                                currentY >= 0f -> 1f
                                else -> -1f
                            }
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = directionSign * screenHeightPx,
                                    animationSpec = tween(durationMillis = 200)
                                )
                                onClose()
                            }
                        } else {
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                isVerticalDragging = false
                            }
                        }
                    }
                }
            }
    ) {
        // 1. Горизонтальный пейджер фото и видео
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = activePageScale <= 1.02f && !isVerticalDragging,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffsetY.value
                    scaleX = dragScale
                    scaleY = dragScale
                }
        ) { page ->
            val entry = viewerEntries.getOrNull(page) ?: return@HorizontalPager
            val isPageActive = (page == safeCurrentPage)

            if (entry.isVideo) {
                ViewerVideoPage(
                    uriString = entry.uri,
                    isActivePage = isPageActive,
                    showControls = controlsVisible,
                    playbackSpeed = playbackSpeed,
                    isLooping = isLooping,
                    playbackState = videoPlaybackState,
                    onTap = {
                        if (isSettingsMenuOpen || isMoreMenuOpen) {
                            isSettingsMenuOpen = false
                            isMoreMenuOpen = false
                        } else {
                            showControls = !showControls
                        }
                    }
                )
            } else {
                ViewerPhotoPage(
                    uriString = entry.uri,
                    isActivePage = isPageActive,
                    resetZoomSignal = resetZoomSignal,
                    onScaleChanged = { activePageScale = it },
                    onTap = {
                        if (isSettingsMenuOpen || isMoreMenuOpen) {
                            isSettingsMenuOpen = false
                            isMoreMenuOpen = false
                        } else {
                            showControls = !showControls
                        }
                    }
                )
            }
        }

        // 2. Верхняя шапка + счётчик
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        ) {
            ViewerTopBar(
                senderTitle = peerName,
                dateSubTitle = dateSubTitle,
                isVideo = currentEntry.isVideo,
                globalMediaCounterText = globalMediaCounterText,
                activePageScale = activePageScale,
                playbackSpeed = playbackSpeed,
                isLooping = isLooping,
                isSettingsMenuOpen = isSettingsMenuOpen,
                isMoreMenuOpen = isMoreMenuOpen,
                onClose = onClose,
                onForwardClick = {
                    if (activeAlbumUris.size > 1) {
                        showForwardDialog = true
                    } else {
                        onForward(listOf(activeMessage.id))
                    }
                },
                onSettingsMenuOpenChange = { isSettingsMenuOpen = it },
                onMoreMenuOpenChange = { isMoreMenuOpen = it },
                onSpeedSelected = { playbackSpeed = it },
                onLoopToggled = { isLooping = it },
                onSaveToGallery = {
                    scope.launch {
                        val ok = saveMediaToDeviceGallery(context, currentEntry.uri, currentEntry.isVideo)
                        ChatActionBannerBus.show(
                            icon = if (ok) Icons.Outlined.FileDownload else Icons.Outlined.ErrorOutline,
                            isError = !ok
                        ) { s ->
                            if (ok) s.bannerSavedToGallery else s.bannerPhotoSaveFailed
                        }
                    }
                },
                onShowInChat = { onShowInChat(activeMessage) },
                onReply = { onReply(activeMessage, currentEntry.itemIndexInMessage) },
                onShare = {
                    sharePhotoSystemChooser(
                        context = context,
                        uriString = currentEntry.uri,
                        isVideo = currentEntry.isVideo,
                        chooserTitle = strings.shareMediaChooser
                    )
                },
                onOpenInSystem = {
                    val launched = openMediaInSystemApp(
                        context = context,
                        uriString = currentEntry.uri,
                        isVideo = currentEntry.isVideo,
                        chooserTitle = strings.openWithChooser
                    )
                    if (!launched) {
                        ChatActionBannerBus.show(
                            icon = Icons.Outlined.ErrorOutline,
                            isError = true
                        ) { strings.noAppToOpen }
                    }
                },
                onDelete = { onRequestDelete(activeMessage, currentEntry.itemIndexInMessage) }
            )
        }

        // 3. Нижняя панель (подпись + лента миниатюр + SeekBar для видео)
        AnimatedVisibility(
            visible = controlsVisible && (activeAlbumUris.size > 1 || activeMessage.text.isNotBlank() || currentEntry.isVideo),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(5f)
        ) {
            ViewerBottomBar(
                caption = activeMessage.text,
                isCaptionExpanded = isCaptionExpanded,
                onToggleCaptionExpanded = { isCaptionExpanded = it },
                albumUris = activeAlbumUris,
                albumVideoFlags = activeAlbumFlags,
                activePosInAlbum = activePosInAlbum,
                thumbListState = thumbListState,
                onSelectThumb = { thumbIdx ->
                    val targetGlobalPage = viewerEntries.indexOfFirst { entry ->
                        entry.message.id == activeMessage.id && entry.albumPositionInGroup == thumbIdx
                    }
                    if (targetGlobalPage >= 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(targetGlobalPage)
                        }
                    }
                },
                isVideo = currentEntry.isVideo,
                videoPlaybackState = videoPlaybackState
            )
        }

        // 4. Диалог выбора пересылки (1 медиафайл или весь альбом N медиа)
        if (showForwardDialog) {
            ViewerForwardChoiceDialog(
                albumCount = activeAlbumUris.size,
                onDismiss = { showForwardDialog = false },
                onForwardSingle = {
                    showForwardDialog = false
                    val singleId = when (activeMessage.attachment) {
                        is MessageAttachment.MediaAlbum -> "${activeMessage.id}#photo_${currentEntry.itemIndexInMessage}"
                        is MessageAttachment.FileGroup -> "${activeMessage.id}#file_${currentEntry.itemIndexInMessage}"
                        else -> activeMessage.id
                    }
                    onForward(listOf(singleId))
                },
                onForwardAll = {
                    showForwardDialog = false
                    onForward(listOf(activeMessage.id))
                }
            )
        }
    }
}
