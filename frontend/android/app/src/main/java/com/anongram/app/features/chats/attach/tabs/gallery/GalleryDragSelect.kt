package com.anongram.app.features.chats.attach.tabs.gallery

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun rememberGalleryDragSelectController(): GalleryDragSelectController {
    return remember { GalleryDragSelectController() }
}

internal class GalleryDragSelectController {
    val tileBounds = mutableStateMapOf<String, Rect>()
    var gridOriginInRoot by mutableStateOf(Offset.Zero)
    var gridHeightPx by mutableStateOf(0f)
    var isDragging by mutableStateOf(false)
    var lastPointerLocal by mutableStateOf<Offset?>(null)
}

/**
 * Telegram-style long-press + drag-to-select по сетке медиа с авто-скроллом у верхнего и нижнего края.
 */
internal fun Modifier.galleryDragToSelect(
    enabled: Boolean,
    gridState: LazyGridState,
    controller: GalleryDragSelectController,
    items: List<GalleryMedia>,
    selectedMedia: List<GalleryMedia>,
    onToggleSelectMedia: (GalleryMedia) -> Unit
): Modifier = composed {
    if (!enabled) return@composed this

    val currentItems by rememberUpdatedState(items)
    val currentSelected by rememberUpdatedState(selectedMedia)
    val currentOnToggle by rememberUpdatedState(onToggleSelectMedia)
    val edgePx = with(LocalDensity.current) { 64.dp.toPx() }

    val visitedKeys = remember { mutableSetOf<String>() }
    var targetSelect by remember { mutableStateOf(true) }
    var lastVisitedIdx by remember { mutableStateOf(-1) }

    fun applySelection(media: GalleryMedia, shouldSelect: Boolean) {
        val alreadySelected = currentSelected.any { it.uri == media.uri }
        if (alreadySelected != shouldSelect) {
            currentOnToggle(media)
        }
    }

    fun processPointerAt(localPos: Offset) {
        val rootPt = controller.gridOriginInRoot + localPos
        val hitKey = controller.tileBounds.entries.firstOrNull { it.value.contains(rootPt) }?.key ?: return
        val list = currentItems
        val curIdx = list.indexOfFirst { it.uri.toString() == hitKey }
        if (curIdx < 0) return

        val fromIdx = if (lastVisitedIdx >= 0) minOf(lastVisitedIdx, curIdx) else curIdx
        val toIdx = if (lastVisitedIdx >= 0) maxOf(lastVisitedIdx, curIdx) else curIdx
        for (i in fromIdx..toIdx) {
            val item = list.getOrNull(i) ?: continue
            val key = item.uri.toString()
            if (visitedKeys.add(key)) {
                applySelection(item, targetSelect)
            }
        }
        lastVisitedIdx = curIdx
    }

    LaunchedEffect(controller.isDragging, enabled) {
        while (enabled && controller.isDragging) {
            val pos = controller.lastPointerLocal
            val h = controller.gridHeightPx
            if (pos != null && h > 0f && edgePx > 0f) {
                val delta = when {
                    pos.y < edgePx -> -(((edgePx - pos.y) / edgePx).coerceIn(0.15f, 1f) * 26f)
                    pos.y > h - edgePx -> (((pos.y - (h - edgePx)) / edgePx).coerceIn(0.15f, 1f) * 26f)
                    else -> 0f
                }
                if (delta != 0f) {
                    gridState.scrollBy(delta)
                    processPointerAt(pos)
                }
            }
            delay(16L)
        }
    }

    this
        .onGloballyPositioned { coords ->
            controller.gridOriginInRoot = coords.positionInRoot()
            controller.gridHeightPx = coords.size.height.toFloat()
        }
        .pointerInput(enabled) {
            detectDragGesturesAfterLongPress(
                onDragStart = { startOffset ->
                    visitedKeys.clear()
                    lastVisitedIdx = -1
                    val rootPt = controller.gridOriginInRoot + startOffset
                    val hitKey = controller.tileBounds.entries.firstOrNull { it.value.contains(rootPt) }?.key
                    val startIdx = if (hitKey != null) {
                        currentItems.indexOfFirst { it.uri.toString() == hitKey }
                    } else -1
                    if (startIdx >= 0 && hitKey != null) {
                        val startMedia = currentItems[startIdx]
                        targetSelect = currentSelected.none { it.uri == startMedia.uri }
                        visitedKeys.add(hitKey)
                        lastVisitedIdx = startIdx
                        applySelection(startMedia, targetSelect)
                        controller.isDragging = true
                        controller.lastPointerLocal = startOffset
                    }
                },
                onDragEnd = {
                    controller.isDragging = false
                    controller.lastPointerLocal = null
                    visitedKeys.clear()
                    lastVisitedIdx = -1
                },
                onDragCancel = {
                    controller.isDragging = false
                    controller.lastPointerLocal = null
                    visitedKeys.clear()
                    lastVisitedIdx = -1
                },
                onDrag = { change, _ ->
                    change.consume()
                    controller.lastPointerLocal = change.position
                    processPointerAt(change.position)
                }
            )
        }
}
