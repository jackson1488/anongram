package com.anongram.app.features.chats.viewer

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.features.chats.attach.tabs.gallery.loadHdBitmap
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Страница просмотра фотографии с быстрым превью + HD-догрузкой,
 * Pinch-to-Zoom (1x..4x), Double-Tap Zoom (1x <-> 2.5x) и прозрачным фоном для свайпа вниз/вверх.
 */
@Composable
internal fun ViewerPhotoPage(
    uriString: String,
    isActivePage: Boolean,
    resetZoomSignal: Int,
    onScaleChanged: (Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedUri = remember(uriString) { runCatching { Uri.parse(uriString) }.getOrNull() }
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }

    var scale by remember(uriString, isActivePage) { mutableFloatStateOf(1f) }
    var offset by remember(uriString, isActivePage) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(resetZoomSignal) {
        if (isActivePage && scale > 1.02f) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    LaunchedEffect(scale, isActivePage) {
        if (isActivePage) {
            onScaleChanged(scale)
        }
    }

    LaunchedEffect(parsedUri) {
        if (parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            if (bitmap == null) bitmap = quick?.asImageBitmap()
            val hd = withContext(Dispatchers.IO) { loadHdBitmap(context, parsedUri, 2400) }
            if (hd != null) bitmap = hd.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uriString) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    },
                    onTap = { onTap() }
                )
            }
            .pointerInput(uriString) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.count { it.pressed }
                        if (pointerCount >= 2 || scale > 1.02f) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale > 1.02f) {
                                val maxX = (size.width * (newScale - 1f)) / 2f
                                val maxY = (size.height * (newScale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                )
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }
    }
}
