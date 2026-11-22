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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.state.SelfDestruct
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings

@Composable
internal fun UserProfileOverflowMenu(
    expanded: Boolean,
    selfDestruct: SelfDestruct,
    isContact: Boolean,
    showGridZoomOptions: Boolean = false,
    canZoomIn: Boolean = false,
    canZoomOut: Boolean = false,
    strings: UserProfileStrings,
    onDismiss: () -> Unit,
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {},
    onSetSelfDestruct: (SelfDestruct) -> Unit,
    onOpenCustomAutoDelete: () -> Unit,
    onShareContact: () -> Unit,
    onBlockUser: () -> Unit,
    onEditContact: () -> Unit,
    onDeleteContact: () -> Unit,
    onRestrictCopying: () -> Unit,
    onReport: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var inAutoDeleteSubmenu by remember(expanded) { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = (-8).dp, y = 6.dp),
        modifier = Modifier
            .width(256.dp)
            .background(scheme.card, RoundedCornerShape(18.dp))
            .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
    ) {
        if (!inAutoDeleteSubmenu) {
            if (showGridZoomOptions) {
                if (canZoomIn) {
                    MenuRowItem(
                        icon = Icons.Outlined.ZoomIn,
                        label = strings.menuZoomIn,
                        onClick = {
                            onDismiss()
                            onZoomIn()
                        }
                    )
                }
                if (canZoomOut) {
                    MenuRowItem(
                        icon = Icons.Outlined.ZoomOut,
                        label = strings.menuZoomOut,
                        onClick = {
                            onDismiss()
                            onZoomOut()
                        }
                    )
                }
            } else {
                MenuRowItem(
                icon = Icons.Outlined.Timer,
                label = strings.menuAutoDelete,
                trailingIcon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                onClick = { inAutoDeleteSubmenu = true }
            )
            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
            MenuRowItem(
                icon = Icons.Outlined.Share,
                label = strings.menuShareContact,
                onClick = {
                    onDismiss()
                    onShareContact()
                }
            )
            MenuRowItem(
                icon = Icons.Outlined.Block,
                label = strings.menuBlock,
                onClick = {
                    onDismiss()
                    onBlockUser()
                }
            )
            if (isContact) {
                MenuRowItem(
                    icon = Icons.Outlined.Edit,
                    label = strings.menuEditContact,
                    onClick = {
                        onDismiss()
                        onEditContact()
                    }
                )
                MenuRowItem(
                    icon = Icons.Outlined.DeleteOutline,
                    label = strings.menuDeleteContact,
                    onClick = {
                        onDismiss()
                        onDeleteContact()
                    }
                )
            }
            MenuRowItem(
                icon = Icons.Outlined.ContentCopy,
                label = strings.menuRestrictCopy,
                onClick = {
                    onDismiss()
                    onRestrictCopying()
                }
            )
                MenuRowItem(
                    icon = Icons.Outlined.Report,
                    label = strings.menuReport,
                    tint = Color(0xFFEF5350),
                    onClick = {
                        onDismiss()
                        onReport()
                    }
                )
            }
        } else {
            MenuRowItem(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                label = strings.autoDeleteBack,
                onClick = { inAutoDeleteSubmenu = false }
            )
            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
            MenuRowItem(
                icon = Icons.Outlined.Timer,
                label = strings.autoDelete1Day,
                tint = if (selfDestruct == SelfDestruct.D1) scheme.accent else scheme.textPrimary,
                onClick = {
                    onSetSelfDestruct(SelfDestruct.D1)
                    onDismiss()
                }
            )
            MenuRowItem(
                icon = Icons.Outlined.Timer,
                label = strings.autoDelete7Days,
                tint = if (selfDestruct == SelfDestruct.W1) scheme.accent else scheme.textPrimary,
                onClick = {
                    onSetSelfDestruct(SelfDestruct.W1)
                    onDismiss()
                }
            )
            MenuRowItem(
                icon = Icons.Outlined.Timer,
                label = strings.autoDelete1Month,
                tint = scheme.textPrimary,
                onClick = {
                    onSetSelfDestruct(SelfDestruct.W1)
                    onDismiss()
                }
            )
            MenuRowItem(
                icon = Icons.Outlined.Tune,
                label = strings.autoDeleteCustom,
                onClick = {
                    onDismiss()
                    onOpenCustomAutoDelete()
                }
            )
            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings.autoDeleteFooter1,
                    color = scheme.textSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
                Text(
                    text = strings.autoDeleteFooter2,
                    color = scheme.textMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    label: String,
    tint: Color? = null,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val resolvedTint = tint ?: scheme.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = resolvedTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = resolvedTint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = scheme.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
