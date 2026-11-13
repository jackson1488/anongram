package com.anongram.web.features.chats.mainmenu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import com.anongram.web.core.theme.CrimsonKillSwitch
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.chats.common.MuteDuration
import com.anongram.web.features.chats.state.ChatListUiState
import com.anongram.web.features.chats.state.ChatListViewModel

@Composable
internal fun ChatListDialogs(
    state: ChatListUiState,
    viewModel: ChatListViewModel
) {
    val scheme = LocalAnonGramScheme.current

    if (state.muteSheetFor != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMuteSheet,
            containerColor = scheme.surface,
            title = { Text("Отключить уведомления", color = scheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    MuteDuration.entries.forEach { duration ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.muteFor(duration) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(duration.label, color = scheme.textPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMuteSheet) {
                    Text("Отмена", color = scheme.textSecondary)
                }
            }
        )
    }

    if (state.deleteDialogFor != null) {
        var alsoForPeer by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            containerColor = scheme.surface,
            title = { Text("Удалить чат", color = scheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Выбранные чаты (${state.deleteDialogFor.size}) будут удалены.", color = scheme.textSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable { alsoForPeer = !alsoForPeer },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = alsoForPeer, onCheckedChange = { alsoForPeer = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Удалить также у собеседника", color = scheme.textPrimary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteSessions(alsoForPeer) }) {
                    Text("Удалить", color = CrimsonKillSwitch, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Отмена", color = scheme.textSecondary)
                }
            }
        )
    }

    if (state.clearDialogFor != null) {
        var forEveryone by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = viewModel::dismissClearDialog,
            containerColor = scheme.surface,
            title = { Text("Очистить историю", color = scheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("История сообщений в выбранных чатах будет удалена.", color = scheme.textSecondary, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable { forEveryone = !forEveryone },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = forEveryone, onCheckedChange = { forEveryone = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Очистить для всех", color = scheme.textPrimary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClearHistory(forEveryone) }) {
                    Text("Очистить", color = CrimsonKillSwitch, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearDialog) {
                    Text("Отмена", color = scheme.textSecondary)
                }
            }
        )
    }
}
