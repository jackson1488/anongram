package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.mainmenu.i18n.ChatsMenuI18n
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import dev.chrisbanes.haze.HazeState

/**
 * Панель режима выбора (Telegram Liquid Glass + i18n):
 * прозрачный стеклянный бар, стеклянные кнопки действий, скруглённое меню ⋮ и приятная иконка закрепа.
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
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassBlurEnabled: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ChatsMenuI18n.current
    var menuOpen by remember { mutableStateOf(false) }

    val glassCircleModifier: @Composable () -> Modifier = {
        if (hazeState != null) {
            Modifier.settingsGlassPill(
                hazeState = hazeState,
                shape = CircleShape,
                glassBlurEnabled = glassBlurEnabled
            )
        } else {
            Modifier
                .clip(CircleShape)
                .background(scheme.islandBg.copy(alpha = 0.90f))
                .border(0.8.dp, scheme.islandBorder, CircleShape)
        }
    }

    val actionIslandModifier = if (hazeState != null) {
        Modifier.settingsGlassPill(
            hazeState = hazeState,
            shape = RoundedCornerShape(22.dp),
            glassBlurEnabled = glassBlurEnabled
        )
    } else {
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.islandBg.copy(alpha = 0.90f))
            .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeStatusBarPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .then(glassCircleModifier())
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.exitSelection,
                tint = scheme.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = selectedCount.toString(),
            color = scheme.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // Стеклянная капсула действий: Закреп, Без звука, Удалить, Меню ⋮
        Row(
            modifier = actionIslandModifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SelectionIcon(
                icon = Icons.Rounded.PushPin,
                description = if (allPinned) strings.unpin else strings.pin,
                tint = if (allPinned) scheme.accent else scheme.textPrimary,
                iconModifier = Modifier.rotate(35f),
                onClick = onTogglePin
            )
            SelectionIcon(
                icon = if (allMuted) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                description = if (allMuted) strings.unmute else strings.mute,
                tint = if (allMuted) scheme.accent else scheme.textPrimary,
                onClick = onMute
            )
            SelectionIcon(
                icon = Icons.Outlined.Delete,
                description = strings.delete,
                tint = FlameOrange,
                onClick = onDelete
            )

            Box {
                SelectionIcon(
                    icon = Icons.Outlined.MoreVert,
                    description = strings.selectionActions,
                    tint = scheme.textPrimary,
                    onClick = { menuOpen = true }
                )

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = scheme.card,
                    modifier = Modifier.border(0.8.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    MenuAction(
                        icon = if (isArchiveMode) Icons.Outlined.Unarchive else Icons.Outlined.Inventory2,
                        label = if (isArchiveMode) strings.fromArchive else strings.toArchive
                    ) {
                        menuOpen = false
                        onArchive()
                    }
                    MenuAction(
                        icon = Icons.Outlined.DoneAll,
                        label = strings.markRead
                    ) {
                        menuOpen = false
                        onMarkRead()
                    }
                    MenuAction(
                        icon = Icons.Outlined.CleaningServices,
                        label = strings.clearHistory
                    ) {
                        menuOpen = false
                        onClearHistory()
                    }
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
    iconModifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = iconModifier.size(19.dp)
        )
    }
}

@Composable
private fun MenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = scheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.textSecondary,
                modifier = Modifier.size(19.dp)
            )
        },
        onClick = onClick
    )
}
