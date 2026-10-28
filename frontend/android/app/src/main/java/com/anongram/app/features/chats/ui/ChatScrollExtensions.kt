package com.anongram.app.features.chats.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState

/**
 * Плавная прокрутка с точным центрированием элемента по вертикали экрана в LazyColumn.
 * Поддерживает reverseLayout = true (чат, где index = 0 внизу экрана):
 * В reverseLayout = true:
 * - viewportStartOffset находится у нижнего края.
 * - offset элементов отсчитывается от нижнего края вверх.
 * - Чтобы поместить центр элемента ровно в центр области просмотра:
 *   targetCenterOffset = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
 *   itemCenterOffset = item.offset + item.size / 2
 *   diff = itemCenterOffset - targetCenterOffset
 *   animateScrollBy(diff)
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
            animateScrollBy(diff, tween(durationMillis = 350, easing = FastOutSlowInEasing))
        }
    } else {
        // Если элемент за пределами экрана — плавно прокручиваем к нему с центрированием
        val targetOffset = -(viewportCenter - 100)
        animateScrollToItem(index, scrollOffset = targetOffset)
    }
}
