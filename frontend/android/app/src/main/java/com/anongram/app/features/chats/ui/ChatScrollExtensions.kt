package com.anongram.app.features.chats.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState

/**
 * Мгновенный переход с центрированием элемента по вертикали экрана в LazyColumn (без анимации скролла).
 * Поддерживает reverseLayout = true (чат, где index = 0 внизу экрана).
 */
suspend fun LazyListState.animateScrollAndCenterItem(
    index: Int
) {
    if (index < 0) return

    val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    val viewportSpan = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).takeIf { it > 0 }
        ?: layoutInfo.viewportSize.height.takeIf { it > 0 }
        ?: 1200
    val viewportCenter = viewportSpan / 2

    if (visibleItem != null) {
        val itemCenter = visibleItem.offset + visibleItem.size / 2
        val diff = (itemCenter - viewportCenter).toFloat()
        if (kotlin.math.abs(diff) > 4f) {
            scrollBy(diff)
        }
    } else {
        val targetOffset = -(viewportCenter - 100)
        scrollToItem(index, scrollOffset = targetOffset)
    }
}
