package com.anongram.app.features.chats.messages.domain

/** Элемент рендера списка: сообщение + группировка, дат-чип, разделитель непрочитанных. */
data class MessageRenderItem(
    val message: ChatMessage,
    /** Показать дат-чип над сообщением (день изменился относительно предыдущего). */
    val dateChipAbove: Boolean,
    /** Показать разделитель «Непрочитанные сообщения» над этим сообщением. */
    val unreadDividerAbove: Boolean = false,
    /** Первое сообщение в группе одного направления. */
    val isFirstInGroup: Boolean,
    /** Последнее в группе: хвостик внизу и увеличенный отступ после. */
    val isLastInGroup: Boolean,
    /** Отступ над элементом: 2dp внутри группы, 8dp между группами. */
    val spacingAboveDp: Int
)
