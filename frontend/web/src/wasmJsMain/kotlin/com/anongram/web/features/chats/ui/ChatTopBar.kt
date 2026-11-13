package com.anongram.web.features.chats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.theme.NeonEmerald
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.messages.state.SelfDestruct

@Composable
internal fun ChatTopBar(
    session: IChatSession,
    isPeerTyping: Boolean,
    peerActivity: String?,
    selfDestruct: SelfDestruct,
    showBackButton: Boolean = true,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCycleWallpaper: () -> Unit,
    onOpenKeyCheck: () -> Unit,
    onCycleSelfDestruct: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val accent = session.accentColor
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeStatusBarPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = scheme.islandBg,
                border = BorderStroke(1.dp, scheme.islandBorder),
                shadowElevation = 6.dp
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = scheme.textPrimary
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clickable(onClick = onOpenKeyCheck),
            shape = RoundedCornerShape(24.dp),
            color = scheme.islandBg,
            border = BorderStroke(1.dp, scheme.islandBorder),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = session.avatarMonogram ?: session.title.take(2).uppercase(),
                        color = scheme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        color = scheme.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val statusText = when {
                        isPeerTyping && peerActivity != null -> peerActivity
                        session.isOnline -> "в сети · ${session.tier.label}"
                        else -> session.securityLabel
                    }
                    Text(
                        text = statusText,
                        color = if (isPeerTyping || session.isOnline) NeonEmerald else scheme.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (selfDestruct != SelfDestruct.OFF) {
                    Text(
                        text = "⏱ ${selfDestruct.label}",
                        color = scheme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = scheme.islandBg,
            border = BorderStroke(1.dp, scheme.islandBorder),
            shadowElevation = 6.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = "Поиск", tint = scheme.textPrimary)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Меню", tint = scheme.textPrimary)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = scheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Проверить ключи (SAS)", color = scheme.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = scheme.accent) },
                            onClick = { menuOpen = false; onOpenKeyCheck() }
                        )
                        DropdownMenuItem(
                            text = { Text("Сменить обои", color = scheme.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null, tint = scheme.textPrimary) },
                            onClick = { menuOpen = false; onCycleWallpaper() }
                        )
                        DropdownMenuItem(
                            text = { Text("Таймер: ${selfDestruct.label}", color = scheme.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = scheme.textPrimary) },
                            onClick = { menuOpen = false; onCycleSelfDestruct() }
                        )
                        DropdownMenuItem(
                            text = { Text("Очистить историю", color = scheme.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = scheme.textPrimary) },
                            onClick = { menuOpen = false; onClearHistory() }
                        )
                    }
                }
            }
        }
    }
}
