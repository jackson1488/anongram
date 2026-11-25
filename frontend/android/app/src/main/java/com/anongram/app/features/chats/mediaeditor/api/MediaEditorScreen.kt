package com.anongram.app.features.chats.mediaeditor.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.anongram.app.features.chats.mediaeditor.ui.MediaEditorContent

/**
 * Публичная точка входа фото/видео редактора в стиле Telegram.
 */
@Composable
fun MediaEditorScreen(
    items: List<EditableMediaItem>,
    recipientName: String,
    onSend: (List<EditedMedia>) -> Unit,
    onAddMore: () -> Unit,
    onBack: () -> Unit,
    initialIndex: Int = 0,
    selectedIds: List<String> = emptyList(),
    onToggleSelectItem: ((EditableMediaItem) -> Unit)? = null,
    onSaveOnly: ((List<EditedMedia>) -> Unit)? = null,
    targetBoundsForId: ((String) -> Rect?)? = null,
    modifier: Modifier = Modifier
) {
    MediaEditorContent(
        items = items,
        initialIndex = initialIndex,
        recipientName = recipientName,
        selectedIds = selectedIds,
        onToggleSelectItem = onToggleSelectItem,
        onSend = onSend,
        onAddMore = onAddMore,
        onBack = onBack,
        onSaveOnly = onSaveOnly,
        targetBoundsForId = targetBoundsForId,
        modifier = modifier
    )
}
