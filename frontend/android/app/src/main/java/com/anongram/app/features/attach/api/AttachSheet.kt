package com.anongram.app.features.attach.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anongram.app.features.attach.ui.AttachSheetContent

/**
 * Публичная точка входа модуля вложений (ТЗ, attach: AttachSheet).
 * Нижнее модальное окно поверх чата, вкладки: Галерея, Файл, Геопозиция, Контакт, Музыка.
 */
@Composable
fun AttachSheet(
    visible: Boolean,
    chatId: String = "",
    recipientName: String = "",
    onDismiss: () -> Unit,
    onSend: (List<AttachmentDraft>) -> Unit,
    keyboardHeightPx: Int = 0,
    wasKeyboardOpen: Boolean = false,
    onRestoreKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (visible) {
        AttachSheetContent(
            chatId = chatId,
            recipientName = recipientName,
            onDismiss = onDismiss,
            onSend = onSend,
            keyboardHeightPx = keyboardHeightPx,
            wasKeyboardOpen = wasKeyboardOpen,
            onRestoreKeyboard = onRestoreKeyboard,
            modifier = modifier
        )
    }
}
