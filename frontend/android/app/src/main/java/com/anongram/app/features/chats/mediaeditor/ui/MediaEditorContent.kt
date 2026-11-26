package com.anongram.app.features.chats.mediaeditor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.api.MediaTtl
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.chats.mediaeditor.api.EditedMedia
import com.anongram.app.features.chats.mediaeditor.engine.BitmapExportRenderer
import com.anongram.app.features.chats.mediaeditor.engine.PhotoFilterEngine
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.BrushType
import com.anongram.app.features.chats.mediaeditor.model.PhotoEditState
import com.anongram.app.features.chats.mediaeditor.model.TextLayerItem
import com.anongram.app.features.chats.mediaeditor.model.VideoQuality
import com.anongram.app.features.chats.mediaeditor.ui.adjust.AdjustSubTab
import com.anongram.app.features.chats.mediaeditor.ui.adjust.AdjustmentsPanel
import com.anongram.app.features.chats.mediaeditor.ui.adjust.BlurOverlayControl
import com.anongram.app.features.chats.mediaeditor.ui.adjust.ToneCurvesOverlay
import com.anongram.app.features.chats.mediaeditor.ui.crop.CropRotatePanel
import com.anongram.app.features.chats.mediaeditor.ui.paint.PaintModeTab
import com.anongram.app.features.chats.mediaeditor.ui.paint.PaintOverlayPanel
import com.anongram.app.features.chats.mediaeditor.ui.paint.StrokesPreviewCanvas
import com.anongram.app.features.chats.mediaeditor.ui.text.TextLayersCanvasOverlay
import com.anongram.app.features.chats.mediaeditor.ui.video.VideoCoverPickerOverlay
import com.anongram.app.features.chats.mediaeditor.ui.video.VideoTimelineBar
import com.anongram.app.features.settings.modules.ui.fonts.FontsModuleSettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private enum class EditorMode {
    MAIN,
    CROP,
    PAINT,
    ADJUST,
    VIDEO_COVER
}

@Composable
fun MediaEditorContent(
    items: List<EditableMediaItem>,
    recipientName: String,
    onSend: (List<EditedMedia>) -> Unit,
    onAddMore: () -> Unit,
    onBack: () -> Unit,
    initialIndex: Int = 0,
    selectedIds: List<String> = emptyList(),
    onToggleSelectItem: ((EditableMediaItem) -> Unit)? = null,
    onSaveOnly: ((List<EditedMedia>) -> Unit)? = null,
    targetBoundsForId: ((String) -> Rect?)? = null,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx().coerceAtLeast(360f) }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx().coerceAtLeast(720f) }

    val currentItems = remember(items) {
        mutableStateListOf<EditableMediaItem>().apply { addAll(items) }
    }
    val safeInitialPage = initialIndex.coerceIn(0, (currentItems.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitialPage, pageCount = { currentItems.size })
    val currentIndex = pagerState.currentPage.coerceIn(0, currentItems.lastIndex)
    val activeItem = currentItems[currentIndex]

    var editorMode by remember { mutableStateOf(EditorMode.MAIN) }
    var adjustSubTab by remember { mutableStateOf(AdjustSubTab.SLIDERS) }
    var draftEditState by remember { mutableStateOf(PhotoEditState()) }

    var paintInitialTab by remember { mutableStateOf(PaintModeTab.DRAWING) }
    var paintInitialTextId by remember { mutableStateOf<String?>(null) }

    var showThumbnailsStrip by remember { mutableStateOf(false) }
    var showTtlMenu by remember { mutableStateOf(false) }
    var showVideoQualityMenu by remember { mutableStateOf(false) }
    var showFontsModuleScreen by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Active page photo zoom state (1f..4f)
    var activePhotoZoomScale by remember(activeItem.id) { mutableFloatStateOf(1f) }
    var resetPhotoZoomSignal by remember { mutableIntStateOf(0) }

    // 2-second auto-disappearing HD toast trigger
    var hdToastTrigger by remember { mutableIntStateOf(0) }
    var showHdToast by remember { mutableStateOf(false) }
    LaunchedEffect(hdToastTrigger) {
        if (hdToastTrigger > 0) {
            showHdToast = true
            delay(2000L)
            showHdToast = false
        }
    }

    // When entering a video in MAIN mode, start paused (stopped) so user sees the Play icon and taps screen to play/stop
    var isVideoPlaying by remember(activeItem.id) {
        mutableStateOf(false)
    }
    var videoPlayheadMs by remember(activeItem.id) {
        mutableStateOf(activeItem.trimStartMs)
    }
    var videoSeekTargetMs by remember(activeItem.id) {
        mutableStateOf(-1L)
    }
    var videoSeekSignal by remember(activeItem.id) {
        mutableIntStateOf(0)
    }

    // Zero-lag synchronous float states for vertical drag and single-coroutine close-to-grid transition
    var editorRootBounds by remember { mutableStateOf(Rect.Zero) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragScale by remember { mutableFloatStateOf(1f) }
    var dragBgAlpha by remember { mutableFloatStateOf(1f) }
    var closeCardAlpha by remember { mutableFloatStateOf(1f) }
    var closeCornerDp by remember { mutableFloatStateOf(0f) }
    var isVerticalDragging by remember { mutableStateOf(false) }
    var isDraggingVideoTimeline by remember { mutableStateOf(false) }
    var isClosingAnimated by remember { mutableStateOf(false) }

    val adjustBottomPadding by animateDpAsState(
        targetValue = when {
            editorMode == EditorMode.ADJUST -> 235.dp
            activeItem.isVideo && editorMode == EditorMode.PAINT -> 118.dp
            activeItem.isVideo && editorMode == EditorMode.CROP -> 108.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "adjustPhotoShrink"
    )
    val adjustTopPadding by animateDpAsState(
        targetValue = when {
            editorMode == EditorMode.ADJUST -> 24.dp
            activeItem.isVideo && editorMode == EditorMode.PAINT -> 50.dp
            activeItem.isVideo && editorMode == EditorMode.CROP -> 12.dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "adjustPhotoTopPad"
    )

    // Lock system/engine swipeBackGesture while MediaEditor is active
    DisposableEffect(Unit) {
        com.anongram.app.core.navigation.SwipeBackGestureLock.lockCount += 1
        onDispose {
            com.anongram.app.core.navigation.SwipeBackGestureLock.lockCount =
                (com.anongram.app.core.navigation.SwipeBackGestureLock.lockCount - 1).coerceAtLeast(0)
        }
    }

    fun triggerAnimatedClose() {
        if (isClosingAnimated) return
        isClosingAnimated = true
        val startX = dragOffsetX
        val startY = dragOffsetY
        val startScale = dragScale
        val startBgAlpha = dragBgAlpha
        val startCorner = closeCornerDp

        val rootCenterX = if (editorRootBounds.width > 0f) editorRootBounds.center.x else (screenWidthPx * 0.5f)
        val rootCenterY = if (editorRootBounds.height > 0f) editorRootBounds.center.y else (screenHeightPx * 0.5f)
        val targetRect = targetBoundsForId?.invoke(activeItem.id)
        val endX = if (targetRect != null && targetRect.width > 0f) {
            targetRect.center.x - rootCenterX
        } else 0f
        val endY = if (targetRect != null && targetRect.height > 0f) {
            targetRect.center.y - rootCenterY
        } else {
            screenHeightPx * 0.22f
        }
        val endScale = if (targetRect != null && targetRect.width > 0f) {
            (targetRect.width / screenWidthPx).coerceIn(0.26f, 0.42f)
        } else 0.30f

        scope.launch {
            androidx.compose.animation.core.animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 165, easing = FastOutSlowInEasing)
            ) { t, _ ->
                dragOffsetX = startX + (endX - startX) * t
                dragOffsetY = startY + (endY - startY) * t
                dragScale = startScale + (endScale - startScale) * t
                closeCornerDp = startCorner + (20f - startCorner) * t
                dragBgAlpha = (startBgAlpha * (1f - t * 1.2f)).coerceIn(0f, 1f)
                closeCardAlpha = if (t < 0.84f) 1f else ((1f - t) / 0.16f).coerceIn(0f, 1f)
            }
            onBack()
        }
    }

    // Load raw bitmap for both Photos AND Videos (extracted frame for video so Crop, Paint/Text, and Adjust work on videos too!)
    var activeRawBitmap by remember(activeItem.uri, activeItem.isVideo, activeItem.coverUri, activeItem.coverTimeMs) {
        mutableStateOf<Bitmap?>(
            if (!activeItem.isVideo) com.anongram.app.features.chats.attach.tabs.gallery.getCachedGalleryBitmap(activeItem.uri)
            else activeItem.coverUri?.let { com.anongram.app.features.chats.attach.tabs.gallery.getCachedGalleryBitmap(it) }
        )
    }
    LaunchedEffect(activeItem.uri, activeItem.isVideo, activeItem.coverUri, activeItem.coverTimeMs) {
        val loaded = withContext(Dispatchers.IO) {
            if (!activeItem.isVideo) {
                BitmapExportRenderer.decodeBitmapWithExif(context, activeItem.uri, maxDim = 1280)
            } else if (activeItem.coverUri != null) {
                BitmapExportRenderer.decodeBitmapWithExif(context, activeItem.coverUri, maxDim = 1280)
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, activeItem.uri)
                    val timeUs = (activeItem.coverTimeMs ?: activeItem.trimStartMs) * 1000L
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                } catch (_: Exception) {
                    null
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }
        if (loaded != null) {
            activeRawBitmap = loaded
        }
    }

    fun updateCurrentItem(transform: (EditableMediaItem) -> EditableMediaItem): EditableMediaItem {
        val updated = transform(currentItems[currentIndex])
        currentItems[currentIndex] = updated
        return updated
    }

    fun autoSaveSingleItem(updatedItem: EditableMediaItem) {
        val saveCb = onSaveOnly ?: return
        scope.launch {
            val exported = BitmapExportRenderer.exportAll(context, listOf(updatedItem))
            saveCb(exported)
        }
    }

    BackHandler(enabled = !showFontsModuleScreen) {
        when {
            editorMode != EditorMode.MAIN -> editorMode = EditorMode.MAIN
            activePhotoZoomScale > 1.02f -> {
                resetPhotoZoomSignal += 1
                activePhotoZoomScale = 1f
            }
            else -> triggerAnimatedClose()
        }
    }
    BackHandler(enabled = showFontsModuleScreen) {
        showFontsModuleScreen = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    editorRootBounds = coords.boundsInRoot()
                }
            }
            .background(Color.Black.copy(alpha = dragBgAlpha))
    ) {
        // Fullscreen Pager (disabled while dragging video trim handles so both left & right trim edges slide smoothly!)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = editorMode == EditorMode.MAIN &&
                !showFontsModuleScreen &&
                !isVerticalDragging &&
                !isDraggingVideoTimeline &&
                activePhotoZoomScale <= 1.02f &&
                currentItems.size > 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = adjustTopPadding, bottom = adjustBottomPadding)
                .then(
                    if (editorMode == EditorMode.MAIN && !showFontsModuleScreen && !isDraggingVideoTimeline && activePhotoZoomScale <= 1.02f) {
                        Modifier.pointerInput(activeItem.id) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isVerticalDragging = true
                                },
                                onDragEnd = {
                                    isVerticalDragging = false
                                    if (abs(dragOffsetY) > 85.dp.toPx()) {
                                        triggerAnimatedClose()
                                    } else {
                                        val sX = dragOffsetX
                                        val sY = dragOffsetY
                                        val sScale = dragScale
                                        val sAlpha = dragBgAlpha
                                        val sCorner = closeCornerDp
                                        scope.launch {
                                            androidx.compose.animation.core.animate(
                                                initialValue = 0f,
                                                targetValue = 1f,
                                                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                                            ) { t, _ ->
                                                dragOffsetX = sX * (1f - t)
                                                dragOffsetY = sY * (1f - t)
                                                dragScale = sScale + (1f - sScale) * t
                                                dragBgAlpha = sAlpha + (1f - sAlpha) * t
                                                closeCornerDp = sCorner * (1f - t)
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    isVerticalDragging = false
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    dragScale = 1f
                                    dragBgAlpha = 1f
                                    closeCornerDp = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    val dx = change.position.x - change.previousPosition.x
                                    change.consume()
                                    val nextX = dragOffsetX + dx * 0.85f
                                    val nextY = dragOffsetY + dragAmount
                                    val progress = (abs(nextY) / (screenHeightPx * 0.50f)).coerceIn(0f, 1f)
                                    dragOffsetX = nextX
                                    dragOffsetY = nextY
                                    dragScale = (1f - progress * 0.40f).coerceIn(0.45f, 1f)
                                    dragBgAlpha = (1f - progress * 0.88f).coerceIn(0.08f, 1f)
                                    closeCornerDp = progress * 18f
                                }
                            )
                        }
                    } else Modifier
                )
        ) { page ->
            val item = currentItems[page]
            val isCurrentPage = page == currentIndex
            val isAdjustingThisPage = isCurrentPage && editorMode == EditorMode.ADJUST
            val isCroppingThisPage = isCurrentPage && editorMode == EditorMode.CROP
            val isPaintingThisPage = isCurrentPage && editorMode == EditorMode.PAINT
            val previewState = when {
                isAdjustingThisPage || isCroppingThisPage -> draftEditState
                isPaintingThisPage -> item.editState.copy(strokes = emptyList(), textLayers = emptyList())
                else -> item.editState
            }

            // Apply close/drag transform ONLY to the active page so offscreen pager pages aren't transformed!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isCurrentPage) {
                            Modifier.graphicsLayer {
                                translationX = dragOffsetX
                                translationY = dragOffsetY
                                scaleX = dragScale
                                scaleY = dragScale
                                alpha = closeCardAlpha
                                shape = RoundedCornerShape(closeCornerDp.dp)
                                clip = closeCornerDp > 0.5f
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isVideo) {
                    val shouldPlayVideoNow = isCurrentPage && (
                        (editorMode == EditorMode.MAIN && isVideoPlaying) ||
                            editorMode == EditorMode.ADJUST ||
                            editorMode == EditorMode.PAINT ||
                            editorMode == EditorMode.CROP
                        )
                    EditorVideoPreview(
                        item = item,
                        editState = previewState,
                        isAdjustMode = isAdjustingThisPage,
                        isEditOverlayActive = isAdjustingThisPage || isCroppingThisPage || isPaintingThisPage,
                        baseFrameBitmap = if (isCurrentPage) activeRawBitmap else null,
                        isPlaying = shouldPlayVideoNow,
                        seekTargetMs = if (isCurrentPage) videoSeekTargetMs else -1L,
                        seekSignal = if (isCurrentPage) videoSeekSignal else 0,
                        onPositionChange = { posMs ->
                            if (isCurrentPage) videoPlayheadMs = posMs
                        },
                        onTogglePlay = {
                            if (editorMode == EditorMode.MAIN) {
                                isVideoPlaying = !isVideoPlaying
                            }
                        },
                        onTapTextLayer = if (isCurrentPage && editorMode == EditorMode.MAIN) {
                            { tappedLayer ->
                                draftEditState = activeItem.editState
                                paintInitialTab = PaintModeTab.TEXT
                                paintInitialTextId = tappedLayer.id
                                editorMode = EditorMode.PAINT
                            }
                        } else null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EditorPhotoPreview(
                        item = item,
                        editState = previewState,
                        isInteractiveAdjusting = isAdjustingThisPage,
                        enableZoom = isCurrentPage && editorMode == EditorMode.MAIN,
                        resetZoomSignal = resetPhotoZoomSignal,
                        onZoomScaleChange = { if (isCurrentPage) activePhotoZoomScale = it },
                        onTapTextLayer = if (isCurrentPage && editorMode == EditorMode.MAIN) {
                            { tappedLayer ->
                                draftEditState = activeItem.editState
                                paintInitialTab = PaintModeTab.TEXT
                                paintInitialTextId = tappedLayer.id
                                editorMode = EditorMode.PAINT
                            }
                        } else null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Mode: MAIN UI Chrome
        if (editorMode == EditorMode.MAIN && !isClosingAnimated && !isVerticalDragging) {
            // Compact Top Bar: ← Title on left, Selection Circle (1) on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { triggerAnimatedClose() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = recipientName.ifBlank { strings.previewTitle },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Selection circle on top-right (toggles selection in gallery or shows index)
                val selectedIdx = selectedIds.indexOf(activeItem.id)
                val isSelectedInGallery = selectedIdx >= 0
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                onToggleSelectItem != null && isSelectedInGallery -> scheme.accent
                                onToggleSelectItem != null -> Color.Black.copy(alpha = 0.35f)
                                else -> scheme.islandBg
                            }
                        )
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable {
                            if (onToggleSelectItem != null) {
                                onToggleSelectItem(activeItem)
                            } else if (currentItems.size > 1) {
                                showThumbnailsStrip = !showThumbnailsStrip
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (onToggleSelectItem != null) {
                        if (isSelectedInGallery) {
                            Text(
                                text = "${selectedIdx + 1}",
                                color = scheme.onAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = if (currentItems.size > 1) "${currentIndex + 1}" else "1",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Collapsible Thumbnails Strip
            AnimatedVisibility(
                visible = showThumbnailsStrip && currentItems.size > 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentItems.forEachIndexed { idx, item ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (idx == currentIndex) 2.dp else 0.dp,
                                    color = if (idx == currentIndex) scheme.accent else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    scope.launch { pagerState.scrollToPage(idx) }
                                }
                        ) {
                            EditorThumbnail(item = item)
                        }
                    }
                }
            }

            // Compact Bottom Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 10.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Video timeline bar when viewing a video
                if (activeItem.isVideo) {
                    VideoTimelineBar(
                        videoUri = activeItem.uri,
                        durationMs = activeItem.durationMs,
                        trimStartMs = activeItem.trimStartMs,
                        trimEndMs = activeItem.trimEndMs,
                        currentPositionMs = videoPlayheadMs,
                        isPlaying = isVideoPlaying,
                        isMuted = activeItem.isMuted,
                        coverUri = activeItem.coverUri,
                        onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                        onToggleMute = {
                            val updated = updateCurrentItem { it.copy(isMuted = !it.isMuted) }
                            autoSaveSingleItem(updated)
                        },
                        onTrimChange = { startMs, endMs ->
                            updateCurrentItem { it.copy(trimStartMs = startMs, trimEndMs = endMs) }
                            val clamped = videoPlayheadMs.coerceIn(startMs, if (endMs > startMs) endMs else Long.MAX_VALUE)
                            videoPlayheadMs = clamped
                        },
                        onTrimFinished = { startMs, endMs ->
                            val updated = updateCurrentItem { it.copy(trimStartMs = startMs, trimEndMs = endMs) }
                            autoSaveSingleItem(updated)
                        },
                        onSeekPosition = { seekMs ->
                            videoPlayheadMs = seekMs
                            videoSeekTargetMs = seekMs
                            videoSeekSignal += 1
                        },
                        onOpenCoverPicker = {
                            isVideoPlaying = false
                            editorMode = EditorMode.VIDEO_COVER
                        },
                        onRemoveCover = {
                            val updated = updateCurrentItem { it.copy(coverUri = null, coverTimeMs = null) }
                            autoSaveSingleItem(updated)
                        },
                        onDraggingTimelineChange = { isDragging ->
                            isDraggingVideoTimeline = isDragging
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2-second HD hint toast
                AnimatedVisibility(
                    visible = showHdToast && !activeItem.isVideo && activeItem.hd,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "HD",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        Text(
                            text = strings.hdEnabledHint,
                            color = scheme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Row 1: Compact Caption Bar (40.dp tall, full width)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.inputBg)
                        .border(1.dp, scheme.inputBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onAddMore() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.addMore,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    BasicTextField(
                        value = activeItem.caption,
                        onValueChange = { newCap ->
                            val updated = updateCurrentItem { it.copy(caption = newCap) }
                            autoSaveSingleItem(updated)
                        },
                        textStyle = TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(scheme.accent),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (activeItem.caption.isEmpty()) {
                                    Text(
                                        text = strings.addCaptionHint,
                                        color = scheme.textMuted,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // TTL timer menu button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { showTtlMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (activeItem.ttl != MediaTtl.Never) scheme.accent else scheme.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showTtlMenu,
                            onDismissRequest = { showTtlMenu = false },
                            modifier = Modifier
                                .background(scheme.card)
                                .border(0.5.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = strings.ttlMenuTitle,
                                color = scheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(8.dp)
                            )

                            val attachStrings = LocalAttachStrings.current
                            val ttlOptions = listOf(
                                MediaTtl.OneView,
                                MediaTtl.Sec3,
                                MediaTtl.Sec10,
                                MediaTtl.Sec30,
                                MediaTtl.Never
                            )
                            ttlOptions.forEach { ttlVal ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = (activeItem.ttl == ttlVal),
                                                onClick = null,
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = scheme.accent,
                                                    unselectedColor = scheme.textMuted
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = ttlVal.label(attachStrings),
                                                color = scheme.textPrimary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        val updated = updateCurrentItem { it.copy(ttl = ttlVal) }
                                        autoSaveSingleItem(updated)
                                        showTtlMenu = false
                                    }
                                )
                            }

                            Text(
                                text = strings.ttlScreenshotWarning,
                                color = scheme.textMuted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Centered compact tools pill [Crop][Brush][HD/Quality][Tune] + Send button on right
                // Enabled for both Photos AND Videos!
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.islandBg)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Crop & Rotate (works on both Photo and Video)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isVideoPlaying = false
                                    draftEditState = activeItem.editState
                                    editorMode = EditorMode.CROP
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropRotate,
                                contentDescription = strings.toolCrop,
                                tint = if (activeItem.editState.crop.isModified) scheme.accent else scheme.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Paint & Text (works on both Photo and Video)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isVideoPlaying = false
                                    draftEditState = activeItem.editState
                                    paintInitialTab = PaintModeTab.DRAWING
                                    paintInitialTextId = null
                                    editorMode = EditorMode.PAINT
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Brush,
                                contentDescription = strings.toolBrush,
                                tint = if (activeItem.editState.hasAnyPaintOrText) scheme.accent else scheme.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // HD toggle (Photo) or Quality Menu (Video)
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (activeItem.isVideo || activeItem.hd) scheme.accent else scheme.textPrimary,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        if (activeItem.isVideo) {
                                            showVideoQualityMenu = true
                                        } else {
                                            val nextHd = !activeItem.hd
                                            val updated = updateCurrentItem { it.copy(hd = nextHd) }
                                            autoSaveSingleItem(updated)
                                            if (nextHd) hdToastTrigger += 1
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (activeItem.isVideo) {
                                        activeItem.videoQuality.label
                                    } else {
                                        if (activeItem.hd) strings.hdLabel else strings.sdLabel
                                    },
                                    color = if (activeItem.isVideo || activeItem.hd) scheme.accent else scheme.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            if (activeItem.isVideo) {
                                DropdownMenu(
                                    expanded = showVideoQualityMenu,
                                    onDismissRequest = { showVideoQualityMenu = false },
                                    modifier = Modifier
                                        .background(scheme.card)
                                        .border(0.5.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                                ) {
                                    Text(
                                        text = strings.videoQualityTitle,
                                        color = scheme.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                    VideoQuality.entries.forEach { q ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = q.label,
                                                    color = if (activeItem.videoQuality == q) scheme.accent else scheme.textPrimary,
                                                    fontWeight = if (activeItem.videoQuality == q) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                val updated = updateCurrentItem { it.copy(videoQuality = q) }
                                                autoSaveSingleItem(updated)
                                                showVideoQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Adjustments (Sliders | Blur | Curves) (works on both Photo and Video)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isVideoPlaying = false
                                    draftEditState = activeItem.editState
                                    adjustSubTab = AdjustSubTab.SLIDERS
                                    editorMode = EditorMode.ADJUST
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = strings.toolAdjust,
                                tint = if (activeItem.editState.hasAnyAdjustments) scheme.accent else scheme.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Purple Send Button on the far right of the bottom row
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable {
                                if (!isExporting) {
                                    isExporting = true
                                    scope.launch {
                                        val itemsToExport = when {
                                            selectedIds.isNotEmpty() -> {
                                                val byId = currentItems.associateBy { it.id }
                                                selectedIds.mapNotNull { byId[it] }.ifEmpty { listOf(activeItem) }
                                            }
                                            onToggleSelectItem != null -> listOf(activeItem)
                                            else -> currentItems.toList()
                                        }
                                        val results = BitmapExportRenderer.exportAll(context, itemsToExport)
                                        isExporting = false
                                        onSend(results)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color = scheme.onAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = strings.send,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Mode: CROP & ROTATE (for both Photo and Video)
        if (editorMode == EditorMode.CROP) {
            CropRotatePanel(
                bitmap = activeRawBitmap?.asImageBitmap(),
                initialTransform = draftEditState.crop,
                isVideo = activeItem.isVideo,
                onLiveChange = { liveCrop ->
                    draftEditState = draftEditState.copy(crop = liveCrop)
                },
                onApply = { newCrop ->
                    val updated = updateCurrentItem {
                        it.copy(
                            rotationDegrees = newCrop.baseRotation90,
                            editState = it.editState.copy(crop = newCrop)
                        )
                    }
                    autoSaveSingleItem(updated)
                    editorMode = EditorMode.MAIN
                },
                onCancel = {
                    draftEditState = activeItem.editState
                    editorMode = EditorMode.MAIN
                }
            )
        }

        // Mode: ADJUST (Sliders | Blur | Curves) (for both Photo and Video)
        if (editorMode == EditorMode.ADJUST) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (adjustSubTab == AdjustSubTab.BLUR) {
                    BlurOverlayControl(
                        blur = draftEditState.blur,
                        onBlurChange = { draftEditState = draftEditState.copy(blur = it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 235.dp)
                    )
                } else if (adjustSubTab == AdjustSubTab.CURVES) {
                    ToneCurvesOverlay(
                        curves = draftEditState.curves,
                        onCurvesChange = { draftEditState = draftEditState.copy(curves = it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 60.dp)
                            .padding(bottom = 195.dp)
                    )
                }

                AdjustmentsPanel(
                    activeTab = adjustSubTab,
                    onTabChange = { adjustSubTab = it },
                    adjustments = draftEditState.adjustments,
                    onAdjustmentsChange = { draftEditState = draftEditState.copy(adjustments = it) },
                    blur = draftEditState.blur,
                    onBlurChange = { draftEditState = draftEditState.copy(blur = it) },
                    curves = draftEditState.curves,
                    onCurvesChange = { draftEditState = draftEditState.copy(curves = it) },
                    onCancel = { editorMode = EditorMode.MAIN },
                    onDone = {
                        val updated = updateCurrentItem { it.copy(editState = draftEditState) }
                        autoSaveSingleItem(updated)
                        editorMode = EditorMode.MAIN
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Mode: PAINT & TEXT (for both Photo and Video)
        if (editorMode == EditorMode.PAINT) {
            val transformedPreview = remember(activeRawBitmap, activeItem.editState) {
                activeRawBitmap?.let { raw ->
                    var w = PhotoFilterEngine.applyCropAndTransform(raw, activeItem.editState)
                    if (PhotoFilterEngine.needsPixelPipeline(activeItem.editState)) {
                        w = PhotoFilterEngine.applyPixelPipeline(w, activeItem.editState)
                    }
                    w.asImageBitmap()
                }
            }
            PaintOverlayPanel(
                previewBitmap = transformedPreview,
                initialStrokes = activeItem.editState.strokes,
                initialTextLayers = activeItem.editState.textLayers,
                initialTab = paintInitialTab,
                initialSelectedTextId = paintInitialTextId,
                isVideo = activeItem.isVideo,
                onApply = { strokes, texts ->
                    val updated = updateCurrentItem {
                        it.copy(editState = it.editState.copy(strokes = strokes, textLayers = texts))
                    }
                    autoSaveSingleItem(updated)
                    editorMode = EditorMode.MAIN
                },
                onCancel = { editorMode = EditorMode.MAIN },
                onOpenDownloadFonts = { showFontsModuleScreen = true }
            )
        }

        // Mode: VIDEO COVER PICKER
        if (editorMode == EditorMode.VIDEO_COVER && activeItem.isVideo) {
            VideoCoverPickerOverlay(
                videoUri = activeItem.uri,
                durationMs = activeItem.durationMs,
                initialCoverUri = activeItem.coverUri,
                initialCoverTimeMs = activeItem.coverTimeMs,
                onSaveCover = { uri, timeMs ->
                    val updated = updateCurrentItem { it.copy(coverUri = uri, coverTimeMs = timeMs) }
                    isVideoPlaying = false
                    autoSaveSingleItem(updated)
                    editorMode = EditorMode.MAIN
                },
                onCancel = { editorMode = EditorMode.MAIN }
            )
        }

        // Fonts Module Settings Screen overlay
        if (showFontsModuleScreen) {
            FontsModuleSettingsScreen(
                onBack = { showFontsModuleScreen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun EditorPhotoPreview(
    item: EditableMediaItem,
    editState: PhotoEditState,
    isInteractiveAdjusting: Boolean = false,
    enableZoom: Boolean = false,
    resetZoomSignal: Int = 0,
    onZoomScaleChange: (Float) -> Unit = {},
    onTapTextLayer: ((TextLayerItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val initialCachedBmp = remember(item.uri) {
        com.anongram.app.features.chats.attach.tabs.gallery.getCachedGalleryBitmap(item.uri)
    }
    var rawBitmap by remember(item.uri) { mutableStateOf<Bitmap?>(initialCachedBmp) }
    var renderedBitmap by remember(item.uri) {
        mutableStateOf<ImageBitmap?>(initialCachedBmp?.asImageBitmap())
    }

    val fastPreviewBitmap = remember(rawBitmap) {
        rawBitmap?.let { bmp ->
            scaleDownBitmapForInteractivePreview(bmp, maxDim = 540)
        }
    }

    var zoomScale by remember(item.uri) { mutableFloatStateOf(1f) }
    var zoomOffset by remember(item.uri) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(resetZoomSignal) {
        if (zoomScale > 1.02f) {
            zoomScale = 1f
            zoomOffset = Offset.Zero
            onZoomScaleChange(1f)
        }
    }

    LaunchedEffect(enableZoom) {
        if (!enableZoom && zoomScale > 1.02f) {
            zoomScale = 1f
            zoomOffset = Offset.Zero
            onZoomScaleChange(1f)
        }
    }

    LaunchedEffect(item.uri) {
        val fullDecoded = withContext(Dispatchers.IO) {
            BitmapExportRenderer.decodeBitmapWithExif(context, item.uri, maxDim = 1600)
        }
        if (fullDecoded != null) {
            com.anongram.app.features.chats.attach.tabs.gallery.hdBitmapCache.put(item.uri, fullDecoded)
            rawBitmap = fullDecoded
        }
    }

    LaunchedEffect(
        rawBitmap,
        fastPreviewBitmap,
        isInteractiveAdjusting,
        editState.crop,
        editState.adjustments,
        editState.blur,
        editState.curves
    ) {
        val fullSrc = rawBitmap ?: return@LaunchedEffect
        val hasEdits = editState.crop.isModified || PhotoFilterEngine.needsPixelPipeline(editState)
        if (!hasEdits) {
            renderedBitmap = fullSrc.asImageBitmap()
        } else {
            val activeSrc = if (isInteractiveAdjusting) (fastPreviewBitmap ?: fullSrc) else fullSrc
            val processed = withContext(Dispatchers.Default) {
                var work = PhotoFilterEngine.applyCropAndTransform(activeSrc, editState)
                if (PhotoFilterEngine.needsPixelPipeline(editState)) {
                    work = PhotoFilterEngine.applyPixelPipeline(work, editState)
                }
                work.asImageBitmap()
            }
            renderedBitmap = processed
        }
    }

    // Composite non-blur strokes before blurring so blur strokes blur both the photo and earlier drawings
    val blurredBitmap = remember(renderedBitmap, editState.strokes) {
        if (editState.strokes.any { it.type == BrushType.BLUR }) {
            renderedBitmap?.let { img ->
                runCatching {
                    val bmp = img.asAndroidBitmap()
                    val composite = bmp.copy(Bitmap.Config.ARGB_8888, true)
                    val w = composite.width
                    val h = composite.height
                    val minDim = minOf(w, h).toFloat().coerceAtLeast(1f)
                    val nonBlur = editState.strokes.filter { it.type != BrushType.BLUR && it.points.isNotEmpty() }
                    if (nonBlur.isNotEmpty()) {
                        val strokeBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val sc = android.graphics.Canvas(strokeBmp)
                        for (st in nonBlur) {
                            val spx = (st.strokeWidthNorm * minDim).coerceAtLeast(3f)
                            val path = android.graphics.Path().apply {
                                val first = st.points.first()
                                moveTo(first.x * w, first.y * h)
                                if (st.points.size == 1) {
                                    lineTo(first.x * w + 0.5f, first.y * h + 0.5f)
                                } else {
                                    for (i in 1 until st.points.size) {
                                        val prev = st.points[i - 1]
                                        val curr = st.points[i]
                                        val midX = (prev.x + curr.x) * 0.5f * w
                                        val midY = (prev.y + curr.y) * 0.5f * h
                                        quadTo(prev.x * w, prev.y * h, midX, midY)
                                    }
                                    val last = st.points.last()
                                    lineTo(last.x * w, last.y * h)
                                }
                            }
                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = if (st.type == BrushType.MARKER) android.graphics.Paint.Cap.SQUARE else android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                strokeWidth = if (st.type == BrushType.MARKER) spx * 1.5f else spx
                                if (st.type == BrushType.ERASER) {
                                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                } else {
                                    color = st.colorArgb.toInt()
                                    if (st.type == BrushType.MARKER) alpha = 135
                                }
                            }
                            sc.drawPath(path, paint)
                        }
                        android.graphics.Canvas(composite).drawBitmap(strokeBmp, 0f, 0f, null)
                    }
                    val px = IntArray(w * h)
                    composite.getPixels(px, 0, w, 0, 0, w, h)
                    val pass1 = PhotoFilterEngine.fastBoxBlur(px, w, h, radius = 20)
                    val pass2 = PhotoFilterEngine.fastBoxBlur(pass1, w, h, radius = 20)
                    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    out.setPixels(pass2, 0, w, 0, 0, w, h)
                    out
                }.getOrNull()
            }
        } else null
    }

    Box(
        modifier = modifier
            .then(
                if (enableZoom) {
                    Modifier
                        .pointerInput(item.uri) {
                            detectTapGestures(
                                onDoubleTap = {
                                    val nextScale = if (zoomScale > 1.05f) 1f else 2.5f
                                    zoomScale = nextScale
                                    zoomOffset = Offset.Zero
                                    onZoomScaleChange(nextScale)
                                }
                            )
                        }
                        .pointerInput(item.uri) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    if (pressedCount >= 2 || zoomScale > 1.02f) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val nextScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
                                        zoomScale = nextScale
                                        onZoomScaleChange(nextScale)
                                        if (nextScale > 1.02f) {
                                            val maxX = (size.width * (nextScale - 1f)) * 0.5f
                                            val maxY = (size.height * (nextScale - 1f)) * 0.5f
                                            zoomOffset = Offset(
                                                x = (zoomOffset.x + panChange.x).coerceIn(-maxX, maxX),
                                                y = (zoomOffset.y + panChange.y).coerceIn(-maxY, maxY)
                                            )
                                            event.changes.forEach { ch ->
                                                if (ch.positionChanged()) ch.consume()
                                            }
                                        } else {
                                            zoomOffset = Offset.Zero
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val img = renderedBitmap
        if (img != null) {
            val photoAspect = remember(img) {
                if (img.width > 0 && img.height > 0) {
                    (img.width.toFloat() / img.height.toFloat()).coerceIn(0.2f, 5f)
                } else 1f
            }

            // Strictly bound Image + Strokes + Text inside the photo's aspect ratio rectangle
            Box(
                modifier = Modifier
                    .aspectRatio(photoAspect, matchHeightConstraintsFirst = photoAspect < 1f)
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = zoomOffset.x
                        translationY = zoomOffset.y
                    }
                    .clip(RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (editState.strokes.isNotEmpty()) {
                    StrokesPreviewCanvas(
                        strokes = editState.strokes,
                        blurredBitmap = blurredBitmap,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (editState.textLayers.isNotEmpty()) {
                    TextLayersCanvasOverlay(
                        layers = editState.textLayers,
                        selectedLayerId = null,
                        interactive = false,
                        isTypingText = false,
                        onSelectLayer = {},
                        onUpdateLayer = {},
                        onDeleteLayer = {},
                        onEditLayerText = {},
                        onTapLayerInPreviewMode = onTapTextLayer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun scaleDownBitmapForInteractivePreview(src: Bitmap, maxDim: Int = 540): Bitmap {
    val w = src.width
    val h = src.height
    val longest = maxOf(w, h)
    if (longest <= maxDim || w <= 0 || h <= 0) return src
    val scale = maxDim.toFloat() / longest.toFloat()
    val targetW = (w * scale).toInt().coerceAtLeast(1)
    val targetH = (h * scale).toInt().coerceAtLeast(1)
    return runCatching { Bitmap.createScaledBitmap(src, targetW, targetH, true) }.getOrDefault(src)
}

@Composable
private fun EditorVideoPreview(
    item: EditableMediaItem,
    editState: PhotoEditState,
    isAdjustMode: Boolean = false,
    isEditOverlayActive: Boolean = false,
    baseFrameBitmap: Bitmap? = null,
    isPlaying: Boolean,
    seekTargetMs: Long = -1L,
    seekSignal: Int = 0,
    onPositionChange: (Long) -> Unit = {},
    onTogglePlay: () -> Unit,
    onTapTextLayer: ((TextLayerItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    var coverBitmap by remember(item.coverUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.coverUri) {
        val cUri = item.coverUri
        if (cUri != null) {
            coverBitmap = withContext(Dispatchers.IO) {
                BitmapExportRenderer.decodeBitmapWithExif(context, cUri, maxDim = 1280)?.asImageBitmap()
            }
        } else {
            coverBitmap = null
        }
    }

    LaunchedEffect(item.isMuted, mediaPlayerRef) {
        val vol = if (item.isMuted) 0f else 1f
        runCatching { mediaPlayerRef?.setVolume(vol, vol) }
    }

    LaunchedEffect(isPlaying, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        runCatching {
            if (isPlaying) {
                if (!vv.isPlaying) vv.start()
            } else {
                if (vv.isPlaying) vv.pause()
            }
        }
    }

    // Seek video when user drags trimStartMs
    LaunchedEffect(item.trimStartMs, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        runCatching {
            vv.seekTo(item.trimStartMs.toInt())
            onPositionChange(item.trimStartMs)
        }
    }

    // Seek video when user drags the Playhead Scrubber bar inside [trimStartMs .. trimEndMs]
    LaunchedEffect(seekSignal, seekTargetMs, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        if (seekSignal > 0 && seekTargetMs >= 0L) {
            runCatching {
                val startBound = item.trimStartMs.coerceAtLeast(0L)
                val endBound = if (item.trimEndMs > startBound + 200L) item.trimEndMs else Long.MAX_VALUE
                val clamped = seekTargetMs.coerceIn(startBound, endBound)
                vv.seekTo(clamped.toInt())
                onPositionChange(clamped)
            }
        }
    }

    // Loop strictly between [trimStartMs .. trimEndMs] and update playhead position in real time
    LaunchedEffect(isPlaying, item.trimStartMs, item.trimEndMs, videoViewRef) {
        val vv = videoViewRef ?: return@LaunchedEffect
        while (isPlaying) {
            runCatching {
                val pos = vv.currentPosition.toLong()
                val startBound = item.trimStartMs.coerceAtLeast(0L)
                val endBound = if (item.trimEndMs > startBound + 300L) item.trimEndMs else Long.MAX_VALUE
                if (pos >= endBound || pos < startBound - 300L) {
                    vv.seekTo(startBound.toInt())
                    onPositionChange(startBound)
                    if (!vv.isPlaying) vv.start()
                } else {
                    onPositionChange(pos.coerceIn(startBound, endBound))
                }
            }
            delay(50L)
        }
    }

    DisposableEffect(item.uri) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    val adj = editState.adjustments
    val videoAspect = remember(baseFrameBitmap) {
        val bmp = baseFrameBitmap
        if (bmp != null && bmp.width > 0 && bmp.height > 0) {
            (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.25f, 4f)
        } else 9f / 16f
    }

    Box(
        modifier = modifier
            .then(
                if (!isEditOverlayActive) {
                    Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { onTogglePlay() }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(videoAspect, matchHeightConstraintsFirst = videoAspect < 1f)
                .graphicsLayer {
                    rotationZ = editState.crop.baseRotation90.toFloat() + editState.crop.straightenDegrees
                    val mirrorSign = if (editState.crop.mirrorHorizontal) -1f else 1f
                    val cropScaleX = (1f / editState.crop.cropRect.width).coerceIn(1f, 3.5f)
                    val cropScaleY = (1f / editState.crop.cropRect.height).coerceIn(1f, 3.5f)
                    val cropZoom = maxOf(cropScaleX, cropScaleY)
                    scaleX = mirrorSign * cropZoom
                    scaleY = cropZoom
                }
                .clip(RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(item.uri)
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            mp.isLooping = true
                            val vol = if (item.isMuted) 0f else 1f
                            mp.setVolume(vol, vol)
                            val initialStart = item.trimStartMs.coerceAtLeast(0L)
                            seekTo(initialStart.toInt())
                            onPositionChange(initialStart)
                            if (isPlaying) {
                                start()
                            } else {
                                pause()
                            }
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Show custom cover image ONLY when paused in MAIN mode and user explicitly set a custom cover
            if (!isPlaying && !isEditOverlayActive && coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Live color/contrast/exposure/warmth/saturation/fade/vignette overlay across ALL playing or paused video frames!
            if (editState.hasAnyAdjustments) {
                val exposureAlpha = (adj.exposure * 0.28f + adj.enhance * 0.12f + adj.highlights * 0.10f).coerceIn(-0.48f, 0.48f)
                val warmthAlpha = (adj.warmth * 0.24f).coerceIn(-0.38f, 0.38f)
                val contrastDarkAlpha = (adj.contrast * 0.18f - adj.shadows * 0.14f).coerceIn(-0.30f, 0.35f)
                val saturationAlpha = (adj.saturation * 0.18f).coerceIn(-0.35f, 0.35f)
                val fadeAlpha = (adj.fade * 0.25f).coerceIn(0f, 0.32f)

                if (exposureAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = exposureAlpha))
                    )
                } else if (exposureAlpha < -0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = -exposureAlpha))
                    )
                }
                if (warmthAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF9800).copy(alpha = warmthAlpha))
                    )
                } else if (warmthAlpha < -0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2196F3).copy(alpha = -warmthAlpha))
                    )
                }
                if (contrastDarkAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = contrastDarkAlpha))
                    )
                } else if (contrastDarkAlpha < -0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = -contrastDarkAlpha * 0.7f))
                    )
                }
                if (saturationAlpha < -0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = -saturationAlpha))
                    )
                } else if (saturationAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF4081).copy(alpha = saturationAlpha * 0.35f))
                    )
                }
                if (fadeAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE0E0E0).copy(alpha = fadeAlpha))
                    )
                }
            }

            // Render strokes and text layers live across ALL frames of the playing video!
            if (editState.strokes.isNotEmpty()) {
                StrokesPreviewCanvas(
                    strokes = editState.strokes,
                    blurredBitmap = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (editState.textLayers.isNotEmpty()) {
                TextLayersCanvasOverlay(
                    layers = editState.textLayers,
                    selectedLayerId = null,
                    interactive = false,
                    isTypingText = false,
                    onSelectLayer = {},
                    onUpdateLayer = {},
                    onDeleteLayer = {},
                    onEditLayerText = {},
                    onTapLayerInPreviewMode = onTapTextLayer,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Play indicator icon ONLY when stopped in MAIN mode (tapping anywhere on the screen starts/stops playback)
        if (!isPlaying && !isEditOverlayActive) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

@Composable
private fun EditorThumbnail(item: EditableMediaItem) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    var thumbBitmap by remember(item.uri, item.coverUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.uri, item.coverUri) {
        withContext(Dispatchers.IO) {
            runCatching {
                val target = item.coverUri ?: item.uri
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                context.contentResolver.openInputStream(target)?.use { input ->
                    thumbBitmap = BitmapFactory.decodeStream(input, null, opts)?.asImageBitmap()
                }
            }
        }
    }

    if (thumbBitmap != null) {
        Image(
            bitmap = thumbBitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.card),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isVideo) Icons.Rounded.Videocam else Icons.Default.HighQuality,
                contentDescription = null,
                tint = scheme.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
