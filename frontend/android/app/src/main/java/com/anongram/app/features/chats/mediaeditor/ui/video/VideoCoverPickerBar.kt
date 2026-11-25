package com.anongram.app.features.chats.mediaeditor.ui.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var galleryCoverUri by remember(initialCoverUri) { mutableStateOf<Uri?>(initialCoverUri) }
    var showGallerySheet by remember { mutableStateOf(false) }

    LaunchedEffect(videoUri, selectedTimeMs, galleryCoverUri) {
        withContext(Dispatchers.IO) {
            if (galleryCoverUri != null) {
                runCatching {
                    context.contentResolver.openInputStream(galleryCoverUri!!)?.use {
                        previewBitmap = android.graphics.BitmapFactory.decodeStream(it)
                    }
                }
            } else {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, videoUri)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durStr?.toLongOrNull()?.let { if (it > 0L) detectedDuration = it }
                    previewBitmap = retriever.getFrameAtTime(
                        selectedTimeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
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
                    .padding(horizontal = 12.dp, vertical = 12.dp),
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

            // Preview frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Frame scrubber + Save cover button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surface.copy(alpha = 0.96f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Slider(
                    value = selectedTimeMs.toFloat().coerceIn(0f, detectedDuration.toFloat()),
                    onValueChange = {
                        galleryCoverUri = null
                        selectedTimeMs = it.toLong()
                    },
                    valueRange = 0f..detectedDuration.toFloat().coerceAtLeast(1000f),
                    colors = SliderDefaults.colors(
                        thumbColor = scheme.accent,
                        activeTrackColor = scheme.accent,
                        inactiveTrackColor = scheme.textMuted.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val savedUri = withContext(Dispatchers.IO) {
                                val bmp = previewBitmap
                                if (bmp != null) {
                                    val outFile = File(context.cacheDir, "video_cover_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(outFile).use { fos ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                                    }
                                    Uri.fromFile(outFile)
                                } else {
                                    galleryCoverUri ?: videoUri
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
                    onSaveCover(pickedUri, null)
                },
                onDismiss = { showGallerySheet = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
