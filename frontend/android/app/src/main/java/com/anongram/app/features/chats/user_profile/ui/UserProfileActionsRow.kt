package com.anongram.app.features.chats.user_profile.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings
import dev.chrisbanes.haze.HazeState
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
internal fun UserProfileActionsRow(
    isMuted: Boolean,
    strings: UserProfileStrings,
    onChatClick: () -> Unit,
    onMuteToggle: () -> Unit,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    isMuteMenuExpanded: Boolean = false,
    onDismissMuteMenu: () -> Unit = {},
    onEnableSound: () -> Unit = {},
    onOpenMuteDrum: () -> Unit = {},
    onOpenMuteSettings: () -> Unit = {},
    onMuteForever: () -> Unit = {},
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionTile(
            icon = Icons.Outlined.ChatBubbleOutline,
            label = strings.actionChat,
            isActive = false,
            hazeState = hazeState,
            onClick = onChatClick,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(1f)) {
            ActionTile(
                icon = if (isMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                label = if (isMuted) strings.actionMuted else strings.actionSound,
                isActive = isMuted,
                hazeState = hazeState,
                onClick = onMuteToggle,
                modifier = Modifier.fillMaxWidth()
            )
            UserProfileMuteDropdownMenu(
                expanded = isMuteMenuExpanded,
                isMuted = isMuted,
                strings = strings,
                onDismiss = onDismissMuteMenu,
                onEnableSound = onEnableSound,
                onOpenMuteDrum = onOpenMuteDrum,
                onOpenMuteSettings = onOpenMuteSettings,
                onMuteForever = onMuteForever
            )
        }
        ActionTile(
            icon = Icons.Outlined.Call,
            label = strings.actionCall,
            isActive = false,
            hazeState = hazeState,
            onClick = onCallClick,
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            icon = Icons.Outlined.Videocam,
            label = strings.actionVideo,
            isActive = false,
            hazeState = hazeState,
            onClick = onVideoClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UserProfileMuteDropdownMenu(
    expanded: Boolean,
    isMuted: Boolean,
    strings: UserProfileStrings,
    onDismiss: () -> Unit,
    onEnableSound: () -> Unit,
    onOpenMuteDrum: () -> Unit,
    onOpenMuteSettings: () -> Unit,
    onMuteForever: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val destructiveRed = Color(0xFFEF4444)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 0.dp, y = 6.dp),
        modifier = Modifier
            .width(228.dp)
            .background(scheme.card, RoundedCornerShape(18.dp))
            .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
    ) {
        if (isMuted) {
            MuteMenuRow(
                icon = Icons.Outlined.VolumeUp,
                label = strings.muteEnableSound,
                tint = scheme.textPrimary,
                onClick = {
                    onDismiss()
                    onEnableSound()
                }
            )
        }
        MuteMenuRow(
            icon = Icons.Outlined.Schedule,
            label = strings.muteCustomDrum,
            tint = scheme.textPrimary,
            onClick = {
                onDismiss()
                onOpenMuteDrum()
            }
        )
        MuteMenuRow(
            icon = Icons.Outlined.Tune,
            label = strings.muteSettings,
            tint = scheme.textPrimary,
            onClick = {
                onDismiss()
                onOpenMuteSettings()
            }
        )
        if (!isMuted) {
            MuteMenuRow(
                icon = Icons.Outlined.VolumeOff,
                label = strings.muteForever,
                tint = destructiveRed,
                onClick = {
                    onDismiss()
                    onMuteForever()
                }
            )
        }
    }
}

@Composable
private fun MuteMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    hazeState: HazeState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val shape = RoundedCornerShape(18.dp)
    val tileModifier = when {
        isActive -> modifier
            .clip(shape)
            .background(scheme.accentSoft)
            .border(0.6.dp, scheme.islandBorder, shape)
        hazeState != null -> modifier.settingsGlassPill(
            hazeState = hazeState,
            shape = shape
        )
        else -> modifier
            .clip(shape)
            .background(scheme.card.copy(alpha = 0.72f))
            .border(0.6.dp, scheme.islandBorder, shape)
    }
    Column(
        modifier = tileModifier
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) scheme.accent else scheme.textPrimary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) scheme.accent else scheme.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
