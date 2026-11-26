package com.anongram.app.features.profile.avatar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.profile.appearance.ProfileColorPresets
import com.anongram.app.features.profile.i18n.ProfileI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class ProfileGalleryItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long,
    val dateAddedSec: Long
)

/**
 * Экран выбора фотографии, видео или эмодзи для аватара профиля
 * (как в Telegram: шапка с текущим аватаром, заголовок, сетка с Камерой, «Выбрать эмодзи» и галереей).
 */
@Composable
fun ProfileAvatarPickerScreen(
    displayName: String,
    subtitleText: String,
    onPickMedia: (uri: Uri, isVideo: Boolean, durationMs: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val context = LocalContext.current
    val currentAvatar by ProfileAvatarStore.avatar.collectAsState()

    var mediaItems by remember { mutableStateOf<List<ProfileGalleryItem>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(hasMediaPermissions(context)) }
    var emojiSheetOpen by remember { mutableStateOf(false) }

    val cameraFile = remember {
        File(context.cacheDir, "profile_camera_${System.currentTimeMillis()}.jpg")
    }
    val cameraUri = remember(cameraFile) {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", cameraFile)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onPickMedia(cameraUri, false, 0L)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.any { it } || hasMediaPermissions(context)
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(requiredMediaPermissions())
        } else {
            mediaItems = loadProfileGallery(context)
        }
    }

    BackHandler {
        if (emojiSheetOpen) emojiSheetOpen = false else onBack()
    }

    AnonGramScreenSurface(
        applySystemBarsPadding = false,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeStatusBarPadding()
                    .safeNavigationBarPadding()
            ) {
                // Верхний бар: Назад + Удалить фото (если установлено)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.card)
                            .border(0.8.dp, scheme.cardBorder, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (currentAvatar !is ProfileAvatar.None) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    ProfileAvatarStore.setAvatar(context, ProfileAvatar.None)
                                    onBack()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = scheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.removePhoto,
                                color = scheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Мини-шапка профиля: круглый аватар, имя и подпись
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, scheme.cardBorder, CircleShape)
                    ) {
                        ProfileAvatarView(
                            avatar = currentAvatar,
                            displayName = displayName,
                            monogramFontSize = 26.sp,
                            emojiFontSize = 32.sp,
                            playVideo = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayName,
                        color = scheme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitleText,
                        color = scheme.textSecondary,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = strings.choosePhotoOrVideo,
                    color = scheme.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )

                if (!hasPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.permissionsNeeded,
                                color = scheme.textSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(scheme.accent)
                                    .clickable { permissionLauncher.launch(requiredMediaPermissions()) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = strings.done,
                                    color = scheme.onAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Плитка Камера
                        item(key = "camera_tile") {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(scheme.card)
                                    .clickable { takePictureLauncher.launch(cameraUri) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.CameraAlt,
                                        contentDescription = strings.camera,
                                        tint = scheme.accent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = strings.camera,
                                        color = scheme.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Плитка «Выбрать эмодзи»
                        item(key = "emoji_tile") {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF62C8FA), Color(0xFF2B6FD9))
                                        )
                                    )
                                    .clickable { emojiSheetOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "🦆", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = strings.chooseEmoji,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        items(
                            items = mediaItems,
                            key = { "${it.isVideo}_${it.id}" },
                            span = { GridItemSpan(1) }
                        ) { item ->
                            GalleryGridTile(
                                item = item,
                                onClick = {
                                    onPickMedia(item.uri, item.isVideo, item.durationMs)
                                }
                            )
                        }
                    }
                }
            }

            // Выбор эмодзи + градиентного фона
            if (emojiSheetOpen) {
                EmojiAvatarSheet(
                    onSelect = { emoji, start, end ->
                        ProfileAvatarStore.setAvatar(
                            context,
                            ProfileAvatar.Emoji(
                                emoji = emoji,
                                bgStartArgb = start,
                                bgEndArgb = end
                            )
                        )
                        emojiSheetOpen = false
                        onBack()
                    },
                    onDismiss = { emojiSheetOpen = false }
                )
            }
        }
    }
}

@Composable
private fun GalleryGridTile(
    item: ProfileGalleryItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumb by remember(item.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.uri) {
        thumb = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(item.uri, Size(320, 320), null).asImageBitmap()
                } else {
                    @Suppress("DEPRECATION")
                    val bmp: Bitmap? = if (item.isVideo) {
                        MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver,
                            item.id,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    } else {
                        MediaStore.Images.Thumbnails.getThumbnail(
                            context.contentResolver,
                            item.id,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            null
                        )
                    }
                    bmp?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFF14161D))
            .clickable(onClick = onClick)
    ) {
        val bmp = thumb
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = formatVideoDuration(item.durationMs),
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmojiAvatarSheet(
    onSelect: (emoji: String, bgStartArgb: Long, bgEndArgb: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val emojis = remember {
        listOf(
            "🦆", "🦊", "🐺", "🦁", "🐉", "🦅", "🦉", "🐼",
            "😎", "🤖", "👾", "🎭", "🔥", "⚡", "💎", "🛡️"
        )
    }
    var selectedPreset by remember { mutableStateOf(ProfileColorPresets.gradientPresets.first()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.overlayScrim)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(scheme.card)
                .clickable(enabled = false) {}
                .safeNavigationBarPadding()
                .padding(18.dp)
        ) {
            Text(
                text = strings.chooseEmoji,
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileColorPresets.gradientPresets.forEach { p ->
                    val isSel = p.id == selectedPreset.id
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(p.primary, p.secondary)))
                            .then(
                                if (isSel) Modifier.border(2.dp, scheme.textPrimary, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedPreset = p }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(230.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(listOf(selectedPreset.primary, selectedPreset.secondary))
                            )
                            .clickable {
                                val startArgb = (selectedPreset.primary.value shr 32).toLong()
                                val endArgb = (selectedPreset.secondary.value shr 32).toLong()
                                onSelect(emoji, startArgb, endArgb)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 32.sp)
                    }
                }
            }
        }
    }
}

private fun formatVideoDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "$min:${sec.toString().padStart(2, '0')}"
}

private fun requiredMediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun hasMediaPermissions(context: Context): Boolean =
    requiredMediaPermissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private suspend fun loadProfileGallery(context: Context): List<ProfileGalleryItem> =
    withContext(Dispatchers.IO) {
        val result = mutableListOf<ProfileGalleryItem>()

        val imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imgProj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
        runCatching {
            context.contentResolver.query(
                imgUri,
                imgProj,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                var count = 0
                while (c.moveToNext() && count < 120) {
                    val id = c.getLong(idIdx)
                    result.add(
                        ProfileGalleryItem(
                            id = id,
                            uri = ContentUris.withAppendedId(imgUri, id),
                            isVideo = false,
                            durationMs = 0L,
                            dateAddedSec = c.getLong(dateIdx)
                        )
                    )
                    count++
                }
            }
        }

        val vidUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val vidProj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )
        runCatching {
            context.contentResolver.query(
                vidUri,
                vidProj,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val durIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                var count = 0
                while (c.moveToNext() && count < 60) {
                    val id = c.getLong(idIdx)
                    result.add(
                        ProfileGalleryItem(
                            id = id,
                            uri = ContentUris.withAppendedId(vidUri, id),
                            isVideo = true,
                            durationMs = c.getLong(durIdx),
                            dateAddedSec = c.getLong(dateIdx)
                        )
                    )
                    count++
                }
            }
        }

        result.sortedByDescending { it.dateAddedSec }
    }
