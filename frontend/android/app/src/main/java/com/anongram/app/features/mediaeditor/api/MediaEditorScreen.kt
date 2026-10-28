package com.anongram.app.features.mediaeditor.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anongram.app.features.mediaeditor.ui.MediaEditorContent

/**
 * Публичная точка входа модуля предпросмотра и редактора (ТЗ, mediaeditor: MediaEditorScreen).
 * Верх: назад, имя получателя, счётчик выбранных (с лентой миниатюр), фиолетовая галочка.
 * Центр: горизонтальный пейджер медиа.
 * Низ: подпись + ещё медиа + таймер исчезновения + панель инструментов + отправка.
 */
@Composable
fun MediaEditorScreen(
    items: List<EditableMediaItem>,
    recipientName: String,
    onSend: (List<EditedMedia>) -> Unit,
    onAddMore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    MediaEditorContent(
        items = items,
        recipientName = recipientName,
        onSend = onSend,
        onAddMore = onAddMore,
        onBack = onBack,
        modifier = modifier
    )
}
