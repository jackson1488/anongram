package com.anongram.app.features.chats.messages.domain

/** Элемент рендера списка: сообщение + группировка и дат-чип (логика Telegram). */
data class MessageRenderItem(
    val message: ChatMessage,
    /** Показать дат-чип над сообщением (день изменился относительно предыдущего). */
    val dateChipAbove: Boolean,
    /** Первое сообщение в группе одного направления: рисуем хвостик-уголок. */
    val isFirstInGroup: Boolean,
    /** Последнее в группе: хвостик внизу и увеличенный отступ после. */
    val isLastInGroup: Boolean,
    /** Отступ над элементом: 2dp внутри группы, 8dp между группами. */
    val spacingAboveDp: Int
)
