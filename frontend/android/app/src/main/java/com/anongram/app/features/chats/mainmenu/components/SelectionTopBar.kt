package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.core.ui.safeStatusBarPadding

/**
 * Панель режима выбора (как в Telegram): крестик выхода слева, счётчик,
 * справа — закрепить, корзина и три точки. Внутри точек: Без звука,
 * В архив / Из архива, Прочитано, Очистить историю.
 */
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
            .background(SurfaceDark)
            .safeStatusBarPadding()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Выйти из режима выбора",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = selectedCount.toString(),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        SelectionIcon(
            icon = Icons.Outlined.PushPin,
            description = if (allPinned) "Открепить" else "Закрепить",
            tint = if (allPinned) scheme.accent else TextSecondary,
            onClick = onTogglePin
        )
        SelectionIcon(
            icon = if (allMuted) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
            description = if (allMuted) "Включить звук" else "Без звука",
            tint = if (allMuted) scheme.accent else TextSecondary,
            onClick = onMute
        )
        SelectionIcon(
            icon = Icons.Outlined.Delete,
            description = "Удалить",
            tint = FlameOrange,
            onClick = onDelete
        )

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Действия выбора",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = CardDark
            ) {
                MenuAction(
                    icon = if (isArchiveMode) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    label = if (isArchiveMode) "Из архива" else "В архив"
                ) {
                    menuOpen = false
                    onArchive()
                }
                MenuAction(
                    icon = Icons.Outlined.DoneAll,
                    label = "Прочитано"
                ) {
                    menuOpen = false
                    onMarkRead()
                }
                MenuAction(
                    icon = Icons.Outlined.CleaningServices,
                    label = "Очистить историю"
                ) {
                    menuOpen = false
                    onClearHistory()
                }
            }
        }
    }
}

@Composable
private fun SelectionIcon(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text = label, color = TextPrimary, fontSize = 13.sp)
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(17.dp)
            )
        },
        onClick = onClick
    )
}
