package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.direct.DirectChatSession

/**
 * Диалог очистки истории: локально или (только для одиночного чата 1-на-1,
 * как в Telegram) также у собеседника — в сетевой версии это удаление
 * сообщений у всех.
 */
@Composable
internal fun ClearHistoryDialog(
    sessions: List<IChatSession>,
    onConfirm: (forEveryone: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var forEveryone by remember { mutableStateOf(false) }

    val single = sessions.singleOrNull()
    val message = when {
        single != null -> "История чата «${single.title}» будет очищена."
        else -> "История ${sessions.size} чатов будет очищена."
    }
    val showPeerCheckbox = single is DirectChatSession

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = {
            Text(
                text = "Очистить историю?",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column {
                Text(text = message, color = TextSecondary, fontSize = 13.sp)
                if (showPeerCheckbox) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { forEveryone = !forEveryone }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = forEveryone,
                            onCheckedChange = { forEveryone = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = scheme.accent,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Text(
                            text = "Также очистить у собеседника",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(forEveryone) }) {
                Text(text = "Очистить", color = FlameOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена", color = TextSecondary)
            }
        }
    )
}
