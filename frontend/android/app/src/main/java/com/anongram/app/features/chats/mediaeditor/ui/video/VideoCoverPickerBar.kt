package com.anongram.app.features.chats.mediaeditor.ui.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.wallpapers.ui.components.CustomGalleryPickerSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@Composable
fun VideoCoverPickerOverlay(
    videoUri: Uri,
    durationMs: Long,
    initialCoverUri: Uri?,
    initialCoverTimeMs: Long?,
    onSaveCover: (coverUri: Uri?, coverTimeMs: Long?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val scope = rememberCoroutineScope()

    var selectedTimeMs by remember(initialCoverTimeMs) { mutableLongStateOf(initialCoverTimeMs ?: 0L) }
    var detectedDuration by remember(videoUri, durationMs) { mutableLongStateOf(durationMs.coerceAtLeast(1000L)) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filmstripFrames by remember(videoUri) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var galleryCoverUri by remember(initialCoverUri) { mutableStateOf<Uri?>(initialCoverUri) }
    var showGallerySheet by remember { mutableStateOf(false) }

    // Extract filmstrip frames once when opening
    LaunchedEffect(videoUri) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val totalMs = (durStr?.toLongOrNull() ?: durationMs).coerceAtLeast(1000L)
                detectedDuration = totalMs
                val count = 12
                val list = (0 until count).mapNotNull { idx ->
                    val timeUs = (totalMs * idx / count) * 1000L
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?.let { bmp ->
                            Bitmap.createScaledBitmap(bmp, 96, 96, true).asImageBitmap()
                        }
                }
                filmstripFrames = list
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    // Update main preview frame when scrubbing or when picking from gallery
    LaunchedEffect(videoUri, selectedTimeMs, galleryCoverUri) {
        withContext(Dispatchers.IO) {
            if (galleryCoverUri != null) {
                runCatching {
                    previewBitmap = com.anongram.app.features.chats.mediaeditor.engine.BitmapExportRenderer
                        .decodeBitmapWithExif(context, galleryCoverUri!!, maxDim = 1600)
                }
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, videoUri)
                    previewBitmap = retriever.getFrameAtTime(
                        selectedTimeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    ) ?: retriever.frameAtTime
                } catch (_: Exception) {
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar: Cancel | Remove Cover (if any) | Choose from gallery >
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.cancel,
                    color = scheme.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onCancel() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )

                if (initialCoverUri != null || galleryCoverUri != null) {
                    Text(
                        text = strings.removeCover,
                        color = Color(0xFFFF453A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                galleryCoverUri = null
                                onSaveCover(null, null)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = strings.chooseFromGallery,
                    color = scheme.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showGallerySheet = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Large center preview frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            // Bottom Filmstrip Scrubber + Save Cover Button (bright & un-dimmed!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surface.copy(alpha = 0.96f))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val safeDur = detectedDuration.coerceAtLeast(1000L)
                val fraction = (selectedTimeMs.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
                val currentDurState by rememberUpdatedState(safeDur)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val density = LocalDensity.current
                    val thumbWidthDp = 44.dp
                    val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
                    val trackWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                    val usablePx = (trackWidthPx - thumbWidthPx).coerceAtLeast(1f)

                    // Bright background filmstrip of frames (no dark overlay!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(10.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    galleryCoverUri = null
                                    val f = ((offset.x - thumbWidthPx / 2f) / usablePx).coerceIn(0f, 1f)
                                    selectedTimeMs = (f * currentDurState).toLong()
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        galleryCoverUri = null
                                        val f = ((offset.x - thumbWidthPx / 2f) / usablePx).coerceIn(0f, 1f)
                                        selectedTimeMs = (f * currentDurState).toLong()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        galleryCoverUri = null
                                        val f = ((change.position.x - thumbWidthPx / 2f) / usablePx).coerceIn(0f, 1f)
                                        selectedTimeMs = (f * currentDurState).toLong()
                                    }
                                )
                            }
                    ) {
                        filmstripFrames.forEach { frame ->
                            Image(
                                bitmap = frame,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    // Movable highlighted frame window thumb
                    val thumbOffsetX = (fraction * usablePx).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(thumbOffsetX, 0) }
                            .width(thumbWidthDp)
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.card)
                            .border(2.5.dp, Color.White, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = previewBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val savedUri = withContext(Dispatchers.IO) {
                                val bmp = previewBitmap
                                if (bmp != null) {
                                    val outFile = File(context.cacheDir, "video_cover_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(outFile).use { fos ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                                    }
                                    Uri.fromFile(outFile)
                                } else {
                                    galleryCoverUri
                                }
                            }
                            onSaveCover(savedUri, if (galleryCoverUri == null) selectedTimeMs else null)
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = strings.saveCover,
                        color = scheme.onAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Wallpaper-style 1-tap gallery sheet (no ⋮, no multi-select)
        if (showGallerySheet) {
            CustomGalleryPickerSheet(
                onPickUri = { pickedUri ->
                    showGallerySheet = false
                    galleryCoverUri = pickedUri
                    scope.launch {
                        val savedUri = withContext(Dispatchers.IO) {
                            val bmp = com.anongram.app.features.chats.mediaeditor.engine.BitmapExportRenderer
                                .decodeBitmapWithExif(context, pickedUri, maxDim = 1600)
                            if (bmp != null) {
                                val outFile = File(context.cacheDir, "video_cover_gallery_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(outFile).use { fos ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                                }
                                Uri.fromFile(outFile)
                            } else {
                                pickedUri
                            }
                        }
                        onSaveCover(savedUri, null)
                    }
                },
                onDismiss = { showGallerySheet = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
