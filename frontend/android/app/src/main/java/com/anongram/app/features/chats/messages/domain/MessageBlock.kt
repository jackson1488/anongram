package com.anongram.app.features.chats.messages.domain

/**
 * Иерархия модульных блоков сообщения для поддержки комбинированных сообщений
 * («комбо» в стиле Telegram: заголовки, параграфы, цитаты, чек-листы, код, формулы).
 */
sealed interface MessageBlock {
    /**
     * Заголовок H1..H6 (чистый текст без решеток #).
     */
    data class Heading(
        val level: Int,
        val text: String
    ) : MessageBlock

    /**
     * Обычный абзац текста (с поддержкой инлайн-форматирования: жирный, курсив, спойлер, код).
     */
    data class Paragraph(
        val text: String
    ) : MessageBlock

    /**
     * Цитата (стандартная или сворачиваемая) по макету Telegram с опциональным автором.
     */
    data class Quote(
        val text: String,
        val author: String? = null,
        val isCollapsible: Boolean = false
    ) : MessageBlock

    /**
     * Сворачиваемый блок со стрелкой-шевроном (^ Заголовок / Тело).
     */
    data class Collapsible(
        val title: String,
        val content: String,
        val author: String? = null
    ) : MessageBlock

    /**
     * Интерактивный чек-лист с пунктами ☐ / ☑.
     */
    data class Checklist(
        val items: List<ChecklistItem>
    ) : MessageBlock

    /**
     * Блок исходного кода с подсветкой синтаксиса и языком.
     */
    data class Code(
        val language: String,
        val code: String
    ) : MessageBlock

    /**
     * Математическая формула LaTeX.
     */
    data class Latex(
        val formula: String
    ) : MessageBlock
}

data class ChecklistItem(
    val id: Int,
    val isChecked: Boolean,
    val text: String
)
