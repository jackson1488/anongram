package com.anongram.app.features.chats.attach.tabs.gallery

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.attach.components.AttachHeaderScrim
import com.anongram.app.features.chats.attach.components.AttachTopBar
import com.anongram.app.features.chats.attach.i18n.AttachI18nStrings

@Composable
internal fun BoxScope.GalleryFloatingTopBar(
    selectedAlbum: String,
    selectedMedia: List<GalleryMedia>,
    attachStrings: AttachI18nStrings,
    topBarAlpha: Float,
    isAlbumsSheetOpen: Boolean,
    onToggleAlbumsSheet: () -> Unit,
    onBack: () -> Unit,
    onOpenPreviewOrder: () -> Unit,
    onLaunchSystemChooser: (Intent) -> Unit,
    onToggleSpoiler: () -> Unit,
    onSendHd: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val displayAlbumTitle = if (
        selectedAlbum == ALL_MEDIA_ALBUM_ID ||
        selectedAlbum == "Все медиа" ||
        selectedAlbum == "Галерея" ||
        selectedAlbum == "Все" ||
        selectedAlbum.isBlank()
    ) {
        attachStrings.tabGallery
    } else {
        selectedAlbum
    }

    AttachHeaderScrim(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(9f)
            .graphicsLayer { alpha = topBarAlpha }
    )

    AttachTopBar(
        title = displayAlbumTitle,
        searchQuery = "",
        isSearchActive = false,
        searchPlaceholder = "",
        onBack = onBack,
        onSearchQueryChange = {},
        onToggleSearch = {},
        hasActionMenu = true,
        hasSearchToggle = false,
        actionMenuContent = {
            var isTopBarMenuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { isTopBarMenuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = attachStrings.menu, tint = scheme.textPrimary)
            }
            DropdownMenu(
                expanded = isTopBarMenuOpen,
                onDismissRequest = { isTopBarMenuOpen = false },
                containerColor = scheme.card,
                modifier = Modifier
                    .background(scheme.card, RoundedCornerShape(16.dp))
                    .border(0.5.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
            ) {
                if (selectedMedia.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(attachStrings.viewMessage, color = scheme.textPrimary, fontSize = 14.5.sp)
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
                            isTopBarMenuOpen = false
                            onOpenPreviewOrder()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(attachStrings.openInDots, color = scheme.textPrimary, fontSize = 14.5.sp) },
                    onClick = {
                        isTopBarMenuOpen = false
                        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        }
                        onLaunchSystemChooser(Intent.createChooser(intent, attachStrings.openWithDots))
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                    }
                )
                if (selectedMedia.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (selectedMedia.all { it.hasSpoiler }) attachStrings.removeSpoiler else attachStrings.hideUnderSpoiler,
                                color = scheme.textPrimary,
                                fontSize = 14.5.sp
                            )
                        },
                        onClick = {
                            isTopBarMenuOpen = false
                            onToggleSpoiler()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (selectedMedia.all { it.hasSpoiler }) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(attachStrings.sendInHd, color = scheme.textPrimary, fontSize = 14.5.sp) },
                    onClick = {
                        isTopBarMenuOpen = false
                        onSendHd()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.HighQuality, contentDescription = null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                    }
                )
            }
        },
        centerContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onToggleAlbumsSheet() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayAlbumTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = attachStrings.albums,
                    tint = scheme.accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .safeStatusBarPadding()
            .zIndex(10f)
            .graphicsLayer {
                alpha = topBarAlpha
                translationY = -8.dp.toPx() * (1f - topBarAlpha)
            }
    )
}
