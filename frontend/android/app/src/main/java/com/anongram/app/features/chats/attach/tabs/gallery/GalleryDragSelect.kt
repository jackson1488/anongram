package com.anongram.app.features.chats.attach.tabs.gallery

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun rememberGalleryDragSelectController(): GalleryDragSelectController {
    return remember { GalleryDragSelectController() }
}

internal class GalleryDragSelectController {
    var gridHeightPx by mutableStateOf(0f)
    var isDragging by mutableStateOf(false)
    var lastPointerLocal by mutableStateOf<Offset?>(null)
}

/**
 * Telegram-style long-press + drag-to-select по сетке медиа с точным hit-test
 * по живым `gridState.layoutInfo.visibleItemsInfo` (без устаревших координат) и авто-скроллом у краёв.
 */
internal fun Modifier.galleryDragToSelect(
    enabled: Boolean,
    gridState: LazyGridState,
    controller: GalleryDragSelectController,
    items: List<GalleryMedia>,
    selectedMedia: List<GalleryMedia>,
    onToggleSelectMedia: (GalleryMedia) -> Unit,
    hasCameraHeaderRow: Boolean = false
): Modifier = composed {
    if (!enabled) return@composed this

    val currentItems by rememberUpdatedState(items)
    val currentSelected by rememberUpdatedState(selectedMedia)
    val currentOnToggle by rememberUpdatedState(onToggleSelectMedia)
    val density = LocalDensity.current
    val edgePx = with(density) { 64.dp.toPx() }
    val gapPx = with(density) { 2.dp.toPx() }

    val visitedKeys = remember { mutableSetOf<String>() }
    var targetSelect by remember { mutableStateOf(true) }
    var lastVisitedIdx by remember { mutableStateOf(-1) }

    fun applySelection(media: GalleryMedia, shouldSelect: Boolean) {
        val alreadySelected = currentSelected.any { it.uri == media.uri }
        if (alreadySelected != shouldSelect) {
            currentOnToggle(media)
        }
    }

    fun findItemIndexAt(localPos: Offset): Int {
        val list = currentItems
        if (list.isEmpty()) return -1
        val hit = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
            localPos.x >= info.offset.x &&
                localPos.x <= info.offset.x + info.size.width &&
                localPos.y >= info.offset.y &&
                localPos.y <= info.offset.y + info.size.height
        } ?: return -1

        if (hasCameraHeaderRow && hit.index == 0) {
            val colW = (hit.size.width - 2f * gapPx) / 3f
            val rowH = (hit.size.height - gapPx) / 2f
            if (colW <= 0f || rowH <= 0f) return -1
            val col = ((localPos.x - hit.offset.x) / (colW + gapPx)).toInt()
            val row = ((localPos.y - hit.offset.y) / (rowH + gapPx)).toInt().coerceIn(0, 1)
            if (col !in 1..2) return -1 // col == 0 это плитка камеры
            val idx = row * 2 + (col - 1)
            return if (idx in list.indices) idx else -1
        }

        val hitKey = hit.key as? String
        if (hitKey != null) {
            val byKey = list.indexOfFirst { it.uri.toString() == hitKey }
            if (byKey >= 0) return byKey
        }
        val fallbackIdx = if (hasCameraHeaderRow) hit.index + 3 else hit.index
        return if (fallbackIdx in list.indices) fallbackIdx else -1
    }

    fun processPointerAt(localPos: Offset) {
        val list = currentItems
        val curIdx = findItemIndexAt(localPos)
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
        .onSizeChanged { size ->
            controller.gridHeightPx = size.height.toFloat()
        }
        .pointerInput(enabled, hasCameraHeaderRow) {
            detectDragGesturesAfterLongPress(
                onDragStart = { startOffset ->
                    visitedKeys.clear()
                    lastVisitedIdx = -1
                    val startIdx = findItemIndexAt(startOffset)
                    if (startIdx >= 0) {
                        val startMedia = currentItems[startIdx]
                        targetSelect = currentSelected.none { it.uri == startMedia.uri }
                        visitedKeys.add(startMedia.uri.toString())
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
