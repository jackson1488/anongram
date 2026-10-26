package com.anongram.app.features.chats.messages.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

/** Контекстное меню сообщения (long-press): ответить / копировать / удалить. */
@Composable
internal fun MessageContextMenu(
    expanded: Boolean,
    canDelete: Boolean,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = CardDark
    ) {
        DropdownMenuItem(
            text = { Text(text = "Ответить", color = TextPrimary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Reply, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
            },
            onClick = {
                onDismiss()
                onReply()
            }
        )
        DropdownMenuItem(
            text = { Text(text = "Копировать", color = TextPrimary, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(17.dp))
            },
            onClick = {
                onDismiss()
                onCopy()
            }
        )
        if (canDelete) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Удалить",
                        color = FlameOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(17.dp))
                },
                onClick = {
                    onDismiss()
                    onDelete()
                }
            )
        }
    }
}
