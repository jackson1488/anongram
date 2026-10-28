package com.anongram.app.features.chats.ui

import androidx.compose.foundation.lazy.LazyListState

/**
 * Плавная прокрутка с центрированием элемента по вертикали в LazyColumn.
 * Поддерживает reverseLayout = true (чат, где index = 0 внизу экрана):
 * В reverseLayout положительный scrollOffset смещает целевой элемент вверх от нижнего края,
 * поэтому scrollOffset = (viewportHeight - itemHeight) / 2 помещает сообщение ровно по центру экрана.
 */
suspend fun LazyListState.animateScrollAndCenterItem(
    index: Int,
    defaultItemHeightPx: Int = 200
) {
    val viewportHeight = layoutInfo.viewportSize.height
    val existingItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    val itemHeight = existingItem?.size ?: defaultItemHeightPx
    val centeringOffset = ((viewportHeight - itemHeight) / 2).coerceAtLeast(0)

    animateScrollToItem(index = index, scrollOffset = centeringOffset)
}
