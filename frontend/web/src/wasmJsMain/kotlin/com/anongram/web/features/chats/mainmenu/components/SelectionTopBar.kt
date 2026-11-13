package com.anongram.web.features.chats.mainmenu.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.safeStatusBarPadding

@Composable
internal fun SelectionTopBar(
    selectedCount: Int,
    allPinned: Boolean,
    allMuted: Boolean,
    isArchiveMode: Boolean,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onMute: () -> Unit,
    onArchive: () -> Unit,
    onMarkRead: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeStatusBarPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = scheme.textPrimary)
            }
            Text(
                text = selectedCount.toString(),
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isArchiveMode) {
                IconButton(onClick = onTogglePin) {
                    Icon(Icons.Outlined.PushPin, contentDescription = if (allPinned) "Unpin" else "Pin", tint = scheme.textPrimary)
                }
            }
            IconButton(onClick = onMute) {
                Icon(
                    imageVector = if (allMuted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                    contentDescription = "Mute",
                    tint = scheme.textPrimary
                )
            }
            IconButton(onClick = onArchive) {
                Icon(
                    imageVector = if (isArchiveMode) Icons.Outlined.Unarchive else Icons.Outlined.Inventory2,
                    contentDescription = "Archive",
                    tint = scheme.textPrimary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = scheme.textPrimary)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More", tint = scheme.textPrimary)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = scheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text("Отметить прочитанным", color = scheme.textPrimary) },
                        leadingIcon = { Icon(Icons.Outlined.DoneAll, contentDescription = null, tint = scheme.textPrimary) },
                        onClick = { menuOpen = false; onMarkRead() }
                    )
                    DropdownMenuItem(
                        text = { Text("Очистить историю", color = scheme.textPrimary) },
                        onClick = { menuOpen = false; onClearHistory() }
                    )
                }
            }
        }
    }
}
