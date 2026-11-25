package com.anongram.app.features.wallpapers.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhotoLibrary
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.camera.api.CameraScreen
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import com.anongram.app.features.chats.attach.tabs.gallery.ALL_MEDIA_ALBUM_ID
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryAlbum
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryTab

/**
 * Штатная шторка галереи и камеры ([GalleryTab] + [CameraScreen]) для выбора фото на обои.
 * При съёмке или тапе на фото передаёт Uri в редактор обоев; при отмене редактора шторка
 * остаётся открытой для повторного выбора.
 */
@Composable
fun CustomGalleryPickerSheet(
    onPickUri: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current

    var selectedAlbum by remember { mutableStateOf(ALL_MEDIA_ALBUM_ID) }
    var availableAlbums by remember { mutableStateOf<List<GalleryAlbum>>(emptyList()) }
    var isAlbumsOpen by remember { mutableStateOf(false) }
    var isCameraOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = !isCameraOpen) {
        if (isAlbumsOpen) isAlbumsOpen = false else onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(30f)
            .background(scheme.overlayScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(scheme.islandBg.copy(alpha = 0.98f))
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .navigationBarsPadding()
                .padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(scheme.textMuted.copy(alpha = 0.4f))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(scheme.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))

                val albumTitle = availableAlbums.firstOrNull { it.name == selectedAlbum }?.name
                    ?: strings.chatWallpapersGalleryBtn
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = availableAlbums.isNotEmpty()) {
                            isAlbumsOpen = !isAlbumsOpen
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = albumTitle,
                        color = scheme.textPrimary,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (availableAlbums.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.back,
                        tint = scheme.textSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GalleryTab(
                    onLaunchCamera = { isCameraOpen = true },
                    onOpenEditor = { items ->
                        val firstPhoto = items.firstOrNull { !it.isVideo }
                        if (firstPhoto != null) onPickUri(firstPhoto.uri)
                    },
                    selectedAlbum = selectedAlbum,
                    onSelectAlbum = {
                        selectedAlbum = it
                        isAlbumsOpen = false
                    },
                    isAlbumsSheetOpen = isAlbumsOpen,
                    onCloseAlbumsSheet = { isAlbumsOpen = false },
                    onOpenAlbumsSheet = { isAlbumsOpen = true },
                    onToggleSelectMedia = { media ->
                        if (!media.isVideo) onPickUri(media.uri)
                    },
                    isFullscreen = true,
                    onOpenViewer = { list, idx ->
                        val picked = list.getOrNull(idx)
                        if (picked != null && !picked.isVideo) {
                            onPickUri(picked.uri)
                        }
                    },
                    onAlbumsLoaded = { availableAlbums = it },
                    modifier = Modifier.fillMaxSize()
                )

                if (isAlbumsOpen && availableAlbums.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.overlayScrim.copy(alpha = 0.4f))
                            .clickable { isAlbumsOpen = false }
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(scheme.card)
                                .border(1.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            items(availableAlbums, key = { it.name }) { album ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAlbum = album.name
                                            isAlbumsOpen = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = album.name,
                                        color = if (album.name == selectedAlbum) scheme.accent else scheme.textPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (album.name == selectedAlbum) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = album.count.toString(),
                                        color = scheme.textMuted,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isCameraOpen) {
            CameraScreen(
                onResult = { media ->
                    isCameraOpen = false
                    if (!media.isVideo) {
                        onPickUri(media.uri)
                    }
                },
                onClose = { isCameraOpen = false },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
            )
        }
    }
}
