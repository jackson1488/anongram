package com.anongram.app.features.chats.mediaeditor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.anongram.app.features.chats.mediaeditor.model.VideoQuality
import com.anongram.app.features.chats.mediaeditor.ui.adjust.AdjustSubTab
import com.anongram.app.features.chats.mediaeditor.ui.adjust.AdjustmentsPanel
import com.anongram.app.features.chats.mediaeditor.ui.adjust.BlurOverlayControl
import com.anongram.app.features.chats.mediaeditor.ui.adjust.ToneCurvesOverlay
import com.anongram.app.features.chats.mediaeditor.ui.crop.CropRotatePanel
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { currentItems.size })
    val currentIndex = pagerState.currentPage.coerceIn(0, currentItems.lastIndex)
    val activeItem = currentItems[currentIndex]

    var editorMode by remember { mutableStateOf(EditorMode.MAIN) }
    var adjustSubTab by remember { mutableStateOf(AdjustSubTab.SLIDERS) }
    var draftEditState by remember { mutableStateOf(PhotoEditState()) }

    var showThumbnailsStrip by remember { mutableStateOf(false) }
    var showTtlMenu by remember { mutableStateOf(false) }
    var showVideoQualityMenu by remember { mutableStateOf(false) }
    var showFontsModuleScreen by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
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

    // Lock system/engine swipeBackGesture while MediaEditor is active
    DisposableEffect(Unit) {
        com.anongram.app.core.navigation.SwipeBackGestureLock.lockCount += 1
        onDispose {
            com.anongram.app.core.navigation.SwipeBackGestureLock.lockCount =
                (com.anongram.app.core.navigation.SwipeBackGestureLock.lockCount - 1).coerceAtLeast(0)
        }
    }

    val hasUnsavedChanges = currentItems.any { item ->
        item.editState.crop.isModified ||
            item.editState.hasAnyAdjustments ||
            item.editState.hasAnyPaintOrText ||
            item.coverUri != null ||
            item.isMuted ||
            item.trimStartMs > 0L ||
            item.trimEndMs > 0L
    }

    fun requestBack() {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
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

    fun updateCurrentItem(transform: (EditableMediaItem) -> EditableMediaItem) {
        val updated = transform(currentItems[currentIndex])
        currentItems[currentIndex] = updated
    }

    BackHandler(enabled = !showFontsModuleScreen) {
        if (editorMode != EditorMode.MAIN) {
            editorMode = EditorMode.MAIN
        } else {
            requestBack()
        }
    }
    BackHandler(enabled = showFontsModuleScreen) {
        showFontsModuleScreen = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen Pager (floating controls sit on top; left/right photo swipe and vertical drag-to-close work in MAIN mode)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = editorMode == EditorMode.MAIN && !showFontsModuleScreen && currentItems.size > 1,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (editorMode == EditorMode.MAIN && !showFontsModuleScreen) {
                        Modifier.pointerInput(Unit) {
                            androidx.compose.foundation.gestures.detectVerticalDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(verticalDragOffsetY) > 150.dp.toPx()) {
                                        verticalDragOffsetY = 0f
                                        requestBack()
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Mode: MAIN UI Chrome (matching Telegram screenshots 1 & 6)
        if (editorMode == EditorMode.MAIN) {
            // Compact Top Bar: ← Title on left, (1) and purple (✓) on right
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
                        onClick = { requestBack() },
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Selection / Counter circle (1)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(scheme.islandBg)
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable {
                                if (currentItems.size > 1) {
                                    showThumbnailsStrip = !showThumbnailsStrip
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentItems.size > 1) "${currentIndex + 1}" else "1",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Purple Checkmark circle (✓)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable {
                                if (!isExporting) {
                                    isExporting = true
                                    scope.launch {
                                        val results = BitmapExportRenderer.exportAll(context, currentItems)
                                        isExporting = false
                                        (onSaveOnly ?: onSend)(results)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color = scheme.onAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = strings.confirm,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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

            // Compact Bottom Controls (matching Screenshots 1 & 6)
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
                        onToggleMute = { updateCurrentItem { it.copy(isMuted = !it.isMuted) } },
                        onTrimChange = { startMs, endMs ->
                            updateCurrentItem { it.copy(trimStartMs = startMs, trimEndMs = endMs) }
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
                        onValueChange = { newCap -> updateCurrentItem { it.copy(caption = newCap) } },
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
                                        updateCurrentItem { it.copy(ttl = ttlVal) }
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
                                            updateCurrentItem { it.copy(hd = nextHd) }
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
                                                updateCurrentItem { it.copy(videoQuality = q) }
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
                                        val results = BitmapExportRenderer.exportAll(context, currentItems)
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
                    updateCurrentItem {
                        it.copy(
                            rotationDegrees = newCrop.baseRotation90,
                            editState = it.editState.copy(crop = newCrop)
                        )
                    }
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
                            .padding(bottom = 220.dp)
                    )
                } else if (adjustSubTab == AdjustSubTab.CURVES) {
                    ToneCurvesOverlay(
                        curves = draftEditState.curves,
                        onCurvesChange = { draftEditState = draftEditState.copy(curves = it) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 80.dp)
                            .padding(bottom = 180.dp)
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
                        updateCurrentItem { it.copy(editState = draftEditState) }
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
                onApply = { strokes, texts ->
                    updateCurrentItem {
                        it.copy(editState = it.editState.copy(strokes = strokes, textLayers = texts))
                    }
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
                    updateCurrentItem { it.copy(coverUri = uri, coverTimeMs = timeMs) }
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

        // Discard unsaved edits dialog
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                containerColor = scheme.card,
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = strings.discardTitle,
                        color = scheme.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = strings.discardMessage,
                        color = scheme.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDiscardDialog = false
                            onBack()
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A))
                    ) {
                        Text(strings.discardConfirm, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(strings.cancel, color = scheme.textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun EditorPhotoPreview(
    item: EditableMediaItem,
    editState: PhotoEditState,
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

    val blurredBitmap = remember(renderedBitmap, editState.strokes) {
        if (editState.strokes.any { it.type == BrushType.BLUR }) {
            renderedBitmap?.let { img ->
                runCatching {
                    val bmp = img.asAndroidBitmap()
                    val px = IntArray(bmp.width * bmp.height)
                    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                    val pass1 = PhotoFilterEngine.fastBoxBlur(px, bmp.width, bmp.height, radius = 20)
                    val pass2 = PhotoFilterEngine.fastBoxBlur(pass1, bmp.width, bmp.height, radius = 20)
                    val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                    out.setPixels(pass2, 0, bmp.width, 0, 0, bmp.width, bmp.height)
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
                        onSelectLayer = {},
                        onUpdateLayer = {},
                        onDeleteLayer = {},
                        onEditLayerText = {},
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
