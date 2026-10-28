package com.anongram.app.features.attach.ui.tabs

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.attach.ui.components.PartialAccessBanner
import com.anongram.app.features.attach.ui.components.PermissionDeniedView
import com.anongram.app.features.mediaeditor.api.EditableMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GalleryMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val bucketName: String = "",
    val dateAdded: Long = 0L
)

private val PurpleAccent = Color(0xFF8B5CF6)

@Composable
fun GalleryTab(
    onLaunchCamera: () -> Unit,
    onOpenEditor: (List<EditableMediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(PermissionUtils.hasGalleryPermission(context))
    }
    val isPartial = remember(hasPermission) {
        PermissionUtils.isGalleryPartialAccess(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = PermissionUtils.hasGalleryPermission(context)
    }

    if (!hasPermission) {
        PermissionDeniedView(
            message = "Для выбора фото и видео предоставьте доступ к галерее.",
            onRequestPermission = {
                launcher.launch(PermissionUtils.getGalleryPermissions())
            },
            modifier = modifier
        )
        return
    }

    var allMedia by remember { mutableStateOf<List<GalleryMedia>>(emptyList()) }
    var albums by remember { mutableStateOf<List<String>>(listOf("Все", "Видео")) }
    var selectedAlbum by remember { mutableStateOf("Все") }
    var showAlbumMenu by remember { mutableStateOf(false) }

    // Multi-selection list (order matters, max 10)
    val selectedItems = remember { mutableStateListOf<GalleryMedia>() }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            loadDeviceGalleryMedia(context)
        }
        allMedia = loaded
        val bucketNames = loaded.map { it.bucketName }.filter { it.isNotBlank() }.distinct()
        albums = listOf("Все", "Видео") + bucketNames
    }

    val filteredMedia = remember(allMedia, selectedAlbum) {
        when (selectedAlbum) {
            "Все" -> allMedia
            "Видео" -> allMedia.filter { it.isVideo }
            else -> allMedia.filter { it.bucketName.equals(selectedAlbum, ignoreCase = true) }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isPartial) {
                PartialAccessBanner(
                    onManageAccess = {
                        launcher.launch(PermissionUtils.getGalleryPermissions())
                    }
                )
            }

            // Album selector pill
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF182238))
                    .clickable { showAlbumMenu = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedAlbum,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select album",
                    tint = PurpleAccent
                )

                DropdownMenu(
                    expanded = showAlbumMenu,
                    onDismissRequest = { showAlbumMenu = false },
                    modifier = Modifier.background(Color(0xFF182238))
                ) {
                    albums.forEach { album ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = album,
                                    color = if (selectedAlbum == album) PurpleAccent else Color.White
                                )
                            },
                            onClick = {
                                selectedAlbum = album
                                showAlbumMenu = false
                            }
                        )
                    }
                }
            }

            // 3-column media grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // First tile: Camera Preview Tile
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(Color(0xFF1E293B))
                            .clickable { onLaunchCamera() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Camera",
                                tint = PurpleAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Камера",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Media items
                items(filteredMedia, key = { it.uri.toString() }) { media ->
                    val selectedIndex = selectedItems.indexOfFirst { it.uri == media.uri }
                    val isSelected = selectedIndex >= 0

                    GalleryItemTile(
                        media = media,
                        isSelected = isSelected,
                        selectionNumber = if (isSelected) selectedIndex + 1 else 0,
                        onToggleSelect = {
                            if (isSelected) {
                                selectedItems.removeAll { it.uri == media.uri }
                            } else {
                                if (selectedItems.size < 10) {
                                    selectedItems.add(media)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Bottom Selected Action Bar
        AnimatedVisibility(
            visible = selectedItems.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFA131D31))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Выбрано: ${selectedItems.size} (из 10)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = {
                        val editableList = selectedItems.map { m ->
                            EditableMediaItem(
                                uri = m.uri,
                                isVideo = m.isVideo
                            )
                        }
                        onOpenEditor(editableList)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = "Далее", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryItemTile(
    media: GalleryMedia,
    isSelected: Boolean,
    selectionNumber: Int,
    onToggleSelect: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(media.uri) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bmp = context.contentResolver.loadThumbnail(media.uri, Size(256, 256), null)
                    bitmap = bmp.asImageBitmap()
                } else {
                    val input = context.contentResolver.openInputStream(media.uri)
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bmp = BitmapFactory.decodeStream(input, null, opts)
                    bitmap = bmp?.asImageBitmap()
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFF131D31))
            .clickable { onToggleSelect() }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark dim overlay if selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PurpleAccent.copy(alpha = 0.25f))
                    .border(2.dp, PurpleAccent)
            )
        }

        // Video indicator badge
        if (media.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                if (media.durationMs > 0) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatVideoTime(media.durationMs),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Multi-selection numbered badge on top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) PurpleAccent else Color(0x66000000))
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) PurpleAccent else Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text(
                    text = selectionNumber.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    val m = sec / 60
    val s = sec % 60
    return String.format("%d:%02d", m, s)
}

private fun loadDeviceGalleryMedia(context: Context): List<GalleryMedia> {
    val list = mutableListOf<GalleryMedia>()
    val contentResolver = context.contentResolver

    // Query Images
    try {
        val imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imgProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val imgOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        contentResolver.query(imgUri, imgProjection, null, null, imgOrder)?.use { c ->
            val idIdx = c.getColumnIndex(MediaStore.Images.Media._ID)
            val bucketIdx = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateIdx = c.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

            var count = 0
            while (c.moveToNext() && count < 100) {
                val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                val bucket = if (bucketIdx >= 0) c.getString(bucketIdx) ?: "" else ""
                val dateAdded = if (dateIdx >= 0) c.getLong(dateIdx) else 0L

                list.add(
                    GalleryMedia(
                        uri = ContentUris.withAppendedId(imgUri, id),
                        isVideo = false,
                        bucketName = bucket,
                        dateAdded = dateAdded
                    )
                )
                count++
            }
        }
    } catch (_: Exception) {}

    // Query Videos
    try {
        val vidUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val vidProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        val vidOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        contentResolver.query(vidUri, vidProjection, null, null, vidOrder)?.use { c ->
            val idIdx = c.getColumnIndex(MediaStore.Video.Media._ID)
            val bucketIdx = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val durIdx = c.getColumnIndex(MediaStore.Video.Media.DURATION)
            val dateIdx = c.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

            var count = 0
            while (c.moveToNext() && count < 60) {
                val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                val bucket = if (bucketIdx >= 0) c.getString(bucketIdx) ?: "" else ""
                val dur = if (durIdx >= 0) c.getLong(durIdx) else 0L
                val dateAdded = if (dateIdx >= 0) c.getLong(dateIdx) else 0L

                list.add(
                    GalleryMedia(
                        uri = ContentUris.withAppendedId(vidUri, id),
                        isVideo = true,
                        durationMs = dur,
                        bucketName = bucket,
                        dateAdded = dateAdded
                    )
                )
                count++
            }
        }
    } catch (_: Exception) {}

    return list.sortedByDescending { it.dateAdded }
}
