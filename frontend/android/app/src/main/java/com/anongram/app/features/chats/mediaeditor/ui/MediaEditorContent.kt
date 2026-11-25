package com.anongram.app.features.chats.mediaeditor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Shader
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    var isExporting by remember { mutableStateOf(false) }

    var isVideoPlaying by remember(activeItem.id) { mutableStateOf(true) }

    // Cache decoded preview bitmap for the active photo
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

    BackHandler(enabled = !showFontsModuleScreen && editorMode != EditorMode.MAIN) {
        editorMode = EditorMode.MAIN
    }
    BackHandler(enabled = showFontsModuleScreen) {
        showFontsModuleScreen = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Horizontal Pager
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = editorMode == EditorMode.MAIN && !showFontsModuleScreen,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = currentItems[page]
            val previewState = if (page == currentIndex && editorMode == EditorMode.ADJUST) {
                draftEditState
            } else {
                item.editState
            }

            Box(
                modifier = Modifier.fillMaxSize(),
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

        // Mode: MAIN UI Chrome
        if (editorMode == EditorMode.MAIN) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg.copy(alpha = 0.90f))
                        .border(1.dp, scheme.islandBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary
                    )
                }

                // Recipient title + counter pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.islandBg.copy(alpha = 0.90f))
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                        .clickable { showThumbnailsStrip = !showThumbnailsStrip }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = recipientName.ifBlank { strings.previewTitle },
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(scheme.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${currentIndex + 1}/${currentItems.size}",
                            color = scheme.onAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Confirm Checkmark
                IconButton(
                    onClick = {
                        if (!isExporting) {
                            isExporting = true
                            scope.launch {
                                val results = BitmapExportRenderer.exportAll(context, currentItems)
                                isExporting = false
                                onSend(results)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.accent)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = scheme.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = strings.confirm,
                            tint = scheme.onAccent
                        )
                    }
                }
            }

            // Collapsible Thumbnails Strip
            AnimatedVisibility(
                visible = showThumbnailsStrip,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
                    .padding(top = 68.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scheme.surface.copy(alpha = 0.92f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentItems.forEachIndexed { idx, item ->
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (idx == currentIndex) 2.dp else 0.dp,
                                    color = if (idx == currentIndex) scheme.accent else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
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

            // Bottom Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 14.dp, start = 16.dp, end = 16.dp),
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
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // HD hint banner for photos
                AnimatedVisibility(visible = !activeItem.isVideo && activeItem.hd) {
                    Text(
                        text = strings.hdEnabledHint,
                        color = scheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.islandBg.copy(alpha = 0.90f))
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Caption Row + Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = activeItem.caption,
                        onValueChange = { newCap -> updateCurrentItem { it.copy(caption = newCap) } },
                        placeholder = {
                            Text(strings.addCaptionHint, color = scheme.textMuted, fontSize = 14.sp)
                        },
                        leadingIcon = {
                            IconButton(onClick = onAddMore) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = strings.addMore,
                                    tint = scheme.textPrimary
                                )
                            }
                        },
                        trailingIcon = {
                            Box {
                                IconButton(onClick = { showTtlMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (activeItem.ttl != MediaTtl.Never) scheme.accent else scheme.textMuted
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
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = scheme.inputBg.copy(alpha = 0.94f),
                            unfocusedContainerColor = scheme.inputBg.copy(alpha = 0.94f),
                            focusedBorderColor = scheme.accent,
                            unfocusedBorderColor = scheme.inputBorder,
                            focusedTextColor = scheme.textPrimary,
                            unfocusedTextColor = scheme.textPrimary,
                            cursorColor = scheme.accent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(52.dp)
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
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = strings.send,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tools Pill Capsule: [Crop] [Brush] [HD / Video Quality] [Tune]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.islandBg.copy(alpha = 0.92f))
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Crop & Rotate
                    IconButton(
                        enabled = !activeItem.isVideo,
                        onClick = {
                            draftEditState = activeItem.editState
                            editorMode = EditorMode.CROP
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropRotate,
                            contentDescription = strings.toolCrop,
                            tint = when {
                                activeItem.isVideo -> scheme.textMuted.copy(alpha = 0.4f)
                                activeItem.editState.crop.isModified -> scheme.accent
                                else -> scheme.textPrimary
                            }
                        )
                    }

                    // Paint & Text
                    IconButton(
                        enabled = !activeItem.isVideo,
                        onClick = {
                            draftEditState = activeItem.editState
                            editorMode = EditorMode.PAINT
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Brush,
                            contentDescription = strings.toolBrush,
                            tint = when {
                                activeItem.isVideo -> scheme.textMuted.copy(alpha = 0.4f)
                                activeItem.editState.hasAnyPaintOrText -> scheme.accent
                                else -> scheme.textPrimary
                            }
                        )
                    }

                    // HD toggle (Photo) or Quality Menu (Video)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (activeItem.isVideo) {
                                        showVideoQualityMenu = true
                                    } else {
                                        updateCurrentItem { it.copy(hd = !it.hd) }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = if (activeItem.isVideo || activeItem.hd) scheme.accent else scheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (activeItem.isVideo) {
                                    activeItem.videoQuality.label
                                } else {
                                    if (activeItem.hd) strings.hdLabel else strings.sdLabel
                                },
                                color = if (activeItem.isVideo || activeItem.hd) scheme.accent else scheme.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
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
                    IconButton(
                        enabled = !activeItem.isVideo,
                        onClick = {
                            draftEditState = activeItem.editState
                            adjustSubTab = AdjustSubTab.SLIDERS
                            editorMode = EditorMode.ADJUST
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = strings.toolAdjust,
                            tint = when {
                                activeItem.isVideo -> scheme.textMuted.copy(alpha = 0.4f)
                                activeItem.editState.hasAnyAdjustments -> scheme.accent
                                else -> scheme.textPrimary
                            }
                        )
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
                // Interactive overlays directly over the photo
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
                initialCoverTimeMs = activeItem.coverTimeMs,
                onSaveCover = { uri, timeMs ->
                    updateCurrentItem { it.copy(coverUri = uri, coverTimeMs = timeMs) }
                    editorMode = EditorMode.MAIN
                },
                onCancel = { editorMode = EditorMode.MAIN }
            )
        }

        // Fonts Module Settings Screen overlay (when clicking "+ Скачать шрифты")
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

    val blurredShader = remember(renderedBitmap, editState.strokes) {
        if (editState.strokes.any { it.type == com.anongram.app.features.chats.mediaeditor.model.BrushType.BLUR }) {
            renderedBitmap?.let { img ->
                runCatching {
                    val bmp = img.asAndroidBitmap()
                    val px = IntArray(bmp.width * bmp.height)
                    bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                    val blr = PhotoFilterEngine.fastBoxBlur(px, bmp.width, bmp.height, radius = 14)
                    val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                    out.setPixels(blr, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                    BitmapShader(out, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
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
                        blurredShader = blurredShader,
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
