package com.anongram.app.features.chats.attach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.i18n.AttachI18nStrings
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.ui.tabs.GalleryMedia

internal fun getGallerySelectionTitle(
    items: List<GalleryMedia>,
    strings: AttachI18nStrings
): String {
    val count = items.size
    if (count == 0) return ""
    val hasVideo = items.any { it.isVideo }
    val hasPhoto = items.any { !it.isVideo }
    return when {
        hasPhoto && !hasVideo -> strings.selectedPhotosCount(count)
        hasVideo && !hasPhoto -> strings.selectedVideosCount(count)
        else -> strings.selectedMediaCount(count)
    }
}

@Composable
internal fun GallerySelectionBar(
    title: String,
    hasMoreItems: Boolean,
    onOpenPreviewOrder: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    menuContent: (@Composable () -> Unit)? = null
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = hasMoreItems, onClick = onOpenPreviewOrder)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hasMoreItems) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(scheme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = strings.preview,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = strings.options,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            menuContent?.invoke()
        }
    }
}

@Composable
internal fun GallerySelectionOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    allHaveSpoiler: Boolean,
    canUngroup: Boolean = true,
    onOpenPreview: () -> Unit,
    onSendUncompressed: () -> Unit,
    onSendUngrouped: () -> Unit,
    onToggleSpoiler: () -> Unit,
    onSendHd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(scheme.card, RoundedCornerShape(16.dp))
            .border(0.5.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
    ) {
        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.viewMessage, color = scheme.textPrimary, fontSize = 14.5.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = scheme.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            onClick = {
                onDismissRequest()
                onOpenPreview()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        DropdownMenuItem(
            text = { Text(strings.sendWithoutCompression, color = scheme.textPrimary, fontSize = 14.5.sp) },
            onClick = {
                onDismissRequest()
                onSendUncompressed()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        if (canUngroup) {
            DropdownMenuItem(
                text = { Text(strings.sendWithoutGrouping, color = scheme.textPrimary, fontSize = 14.5.sp) },
                onClick = {
                    onDismissRequest()
                    onSendUngrouped()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.GridOff,
                        contentDescription = null,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }

        DropdownMenuItem(
            text = {
                Text(
                    text = if (allHaveSpoiler) strings.removeSpoiler else strings.hideUnderSpoiler,
                    color = scheme.textPrimary,
                    fontSize = 14.5.sp
                )
            },
            onClick = {
                onDismissRequest()
                onToggleSpoiler()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (allHaveSpoiler) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = if (allHaveSpoiler) scheme.accent else scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        DropdownMenuItem(
            text = { Text(strings.sendInHd, color = scheme.textPrimary, fontSize = 14.5.sp) },
            onClick = {
                onDismissRequest()
                onSendHd()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}
