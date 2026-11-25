package com.anongram.app.features.chats.mediaeditor.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
