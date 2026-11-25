package com.anongram.app.features.chats.mediaeditor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

    var isVideoPlaying by remember(activeItem.id) { mutableStateOf(true) }
    var verticalDragOffsetY by remember { mutableStateOf(0f) }
    var isClosingAnimated by remember { mutableStateOf(false) }

    val closeScale by animateFloatAsState(
        targetValue = if (isClosingAnimated) {
            0.35f
        } else {
            (1f - abs(verticalDragOffsetY) / 1400f).coerceIn(0.58f, 1f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "editorCloseScale"
    )
    val closeAlpha by animateFloatAsState(
        targetValue = if (isClosingAnimated) {
            0f
        } else {
            (1f - abs(verticalDragOffsetY) / 900f).coerceIn(0.18f, 1f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "editorCloseAlpha"
    )

    val adjustBottomPadding by animateDpAsState(
        targetValue = if (editorMode == EditorMode.ADJUST) 235.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "adjustPhotoShrink"
    )
    val adjustTopPadding by animateDpAsState(
        targetValue = if (editorMode == EditorMode.ADJUST) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
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
        scope.launch {
            delay(190L)
            onBack()
        }
    }

    var activeRawBitmap by remember(activeItem.uri, activeItem.isVideo) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(activeItem.uri, activeItem.isVideo) {
        if (!activeItem.isVideo) {
            activeRawBitmap = withContext(Dispatchers.IO) {
                BitmapExportRenderer.decodeBitmapWithExif(context, activeItem.uri, maxDim = 960)
            }
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
        if (editorMode != EditorMode.MAIN) {
            editorMode = EditorMode.MAIN
        } else {
            triggerAnimatedClose()
        }
    }
    BackHandler(enabled = showFontsModuleScreen) {
        showFontsModuleScreen = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = closeAlpha))
    ) {
        // Fullscreen Pager (left/right swipe across all gallery media in MAIN mode; vertical drag-to-close)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = editorMode == EditorMode.MAIN && !showFontsModuleScreen && currentItems.size > 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = adjustTopPadding, bottom = adjustBottomPadding)
                .graphicsLayer {
                    translationY = verticalDragOffsetY
                    scaleX = closeScale
                    scaleY = closeScale
                    alpha = closeAlpha
                }
                .then(
                    if (editorMode == EditorMode.MAIN && !showFontsModuleScreen) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (abs(verticalDragOffsetY) > 130.dp.toPx()) {
                                        triggerAnimatedClose()
                                    } else {
                                        verticalDragOffsetY = 0f
                                    }
                                },
                                onDragCancel = { verticalDragOffsetY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    verticalDragOffsetY += dragAmount
                                }
                            )
                        }
                    } else Modifier
                )
        ) { page ->
            val item = currentItems[page]
            val previewState = if (page == currentIndex && editorMode == EditorMode.ADJUST) {
                draftEditState
            } else {
                item.editState
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.isVideo) {
                    EditorVideoPreview(
                        uri = item.uri,
                        isPlaying = isVideoPlaying && page == currentIndex && editorMode == EditorMode.MAIN,
                        isMuted = item.isMuted,
                        trimStartMs = item.trimStartMs,
                        trimEndMs = item.trimEndMs,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EditorPhotoPreview(
                        item = item,
                        editState = previewState,
                        onTapTextLayer = if (page == currentIndex && editorMode == EditorMode.MAIN) {
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
        if (editorMode == EditorMode.MAIN && !isClosingAnimated) {
            // Compact Top Bar: ← Title on left, Selection Circle (1) on right (no extra purple checkmark)
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
                        isPlaying = isVideoPlaying,
                        isMuted = activeItem.isMuted,
                        onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                        onToggleMute = {
                            val updated = updateCurrentItem { it.copy(isMuted = !it.isMuted) }
                            autoSaveSingleItem(updated)
                        },
                        onTrimChange = { startMs, endMs ->
                            val updated = updateCurrentItem { it.copy(trimStartMs = startMs, trimEndMs = endMs) }
                            autoSaveSingleItem(updated)
                        },
                        onOpenCoverPicker = { editorMode = EditorMode.VIDEO_COVER }
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
                        // Crop & Rotate
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !activeItem.isVideo) {
                                    draftEditState = activeItem.editState
                                    editorMode = EditorMode.CROP
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropRotate,
                                contentDescription = strings.toolCrop,
                                tint = when {
                                    activeItem.isVideo -> scheme.textMuted
                                    activeItem.editState.crop.isModified -> scheme.accent
                                    else -> scheme.textPrimary
                                },
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Paint & Text
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !activeItem.isVideo) {
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
                                tint = when {
                                    activeItem.isVideo -> scheme.textMuted
                                    activeItem.editState.hasAnyPaintOrText -> scheme.accent
                                    else -> scheme.textPrimary
                                },
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

                        // Adjustments (Sliders | Blur | Curves)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(enabled = !activeItem.isVideo) {
                                    draftEditState = activeItem.editState
                                    adjustSubTab = AdjustSubTab.SLIDERS
                                    editorMode = EditorMode.ADJUST
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = strings.toolAdjust,
                                tint = when {
                                    activeItem.isVideo -> scheme.textMuted
                                    activeItem.editState.hasAnyAdjustments -> scheme.accent
                                    else -> scheme.textPrimary
                                },
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

        // Mode: CROP & ROTATE
        if (editorMode == EditorMode.CROP && !activeItem.isVideo) {
            CropRotatePanel(
                bitmap = activeRawBitmap?.asImageBitmap(),
                initialTransform = draftEditState.crop,
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
                onCancel = { editorMode = EditorMode.MAIN }
            )
        }

        // Mode: ADJUST (Sliders | Blur | Curves)
        if (editorMode == EditorMode.ADJUST && !activeItem.isVideo) {
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

        // Mode: PAINT & TEXT
        if (editorMode == EditorMode.PAINT && !activeItem.isVideo) {
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
    onTapTextLayer: ((TextLayerItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val context = LocalContext.current

    var rawBitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
    var renderedBitmap by remember(item.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.uri) {
        rawBitmap = withContext(Dispatchers.IO) {
            BitmapExportRenderer.decodeBitmapWithExif(context, item.uri, maxDim = 960)
        }
    }

    LaunchedEffect(rawBitmap, editState.crop, editState.adjustments, editState.blur, editState.curves) {
        val src = rawBitmap ?: return@LaunchedEffect
        val processed = withContext(Dispatchers.Default) {
            var work = PhotoFilterEngine.applyCropAndTransform(src, editState)
            if (PhotoFilterEngine.needsPixelPipeline(editState)) {
                work = PhotoFilterEngine.applyPixelPipeline(work, editState)
            }
            work.asImageBitmap()
        }
        renderedBitmap = processed
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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val img = renderedBitmap
        if (img != null) {
            Box(modifier = Modifier.fillMaxSize()) {
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
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.loadingImage,
                    color = scheme.textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun EditorVideoPreview(
    uri: Uri,
    isPlaying: Boolean,
    isMuted: Boolean,
    trimStartMs: Long,
    trimEndMs: Long,
    modifier: Modifier = Modifier
) {
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(isMuted, mediaPlayerRef) {
        val vol = if (isMuted) 0f else 1f
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

    DisposableEffect(uri) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        mp.isLooping = true
                        val vol = if (isMuted) 0f else 1f
                        mp.setVolume(vol, vol)
                        if (trimStartMs > 0L) seekTo(trimStartMs.toInt())
                        if (isPlaying) start()
                    }
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
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
