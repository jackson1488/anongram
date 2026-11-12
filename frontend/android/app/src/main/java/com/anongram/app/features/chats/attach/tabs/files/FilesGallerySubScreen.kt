package com.anongram.app.features.chats.attach.tabs.files

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.components.AttachHeaderScrim
import com.anongram.app.features.chats.attach.components.GalleryGridSkeleton
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryMedia
import com.anongram.app.features.chats.attach.tabs.files.formatFileSize
import com.anongram.app.features.chats.attach.tabs.files.getFileMetadata
import com.anongram.app.features.chats.attach.tabs.gallery.loadDeviceGalleryMedia
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun FilesGallerySubScreen(
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onSelectMediaAsFile: (GalleryMedia) -> Unit,
    selectedMedia: List<GalleryMedia>,
    onToggleMedia: (GalleryMedia) -> Unit,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onSendSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anongram_attach_prefs", Context.MODE_PRIVATE) }
    var isGridMode by remember {
        mutableStateOf(prefs.getBoolean("files_gallery_grid_mode", true))
    }

    var mediaList by remember { mutableStateOf<List<GalleryMedia>>(emptyList()) }
    var isLoadingMedia by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isLoadingMedia = true
        mediaList = withContext(Dispatchers.IO) {
            loadDeviceGalleryMedia(context)
        }
        kotlinx.coroutines.delay(220)
        isLoadingMedia = false
    }

    val openInChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            val isVid = try {
                context.contentResolver.getType(uri)?.startsWith("video") == true
            } catch (_: Exception) { false }
            onSelectMediaAsFile(GalleryMedia(uri = uri, isVideo = isVid))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* отдельный экран, не пробрасывает жесты на шторку */ }
    ) {
        val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        if (isLoadingMedia) {
            GalleryGridSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(top = statusBarTopDp + 64.dp, bottom = 96.dp)
            )
        } else if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocalAttachStrings.current.mediaNotFound,
                    color = scheme.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else if (isGridMode) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(top = statusBarTopDp + 64.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(mediaList, key = { it.uri.toString() }) { media ->
                    FilesGalleryGridItem(
                        media = media,
                        selectionNumber = selectedMedia.indexOfFirst { it.uri == media.uri } + 1,
                        onClick = { onToggleMedia(media) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = statusBarTopDp + 64.dp, bottom = 140.dp)
            ) {
                items(mediaList, key = { it.uri.toString() }) { media ->
                    FilesGalleryListItem(
                        media = media,
                        selectionNumber = selectedMedia.indexOfFirst { it.uri == media.uri } + 1,
                        onClick = { onToggleMedia(media) }
                    )
                    HorizontalDivider(
                        color = scheme.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 64.dp)
                    )
                }
            }
        }

        // Нижняя панель подписи и отправки при выборе медиа
        if (selectedMedia.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(11f)
                    .background(scheme.surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(scheme.inputBg)
                        .border(1.dp, scheme.inputBorder, RoundedCornerShape(23.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (caption.isEmpty()) {
                        Text(LocalAttachStrings.current.addCaptionHint, color = scheme.textMuted, fontSize = 15.sp)
                    }
                    BasicTextField(
                        value = caption,
                        onValueChange = onCaptionChange,
                        singleLine = true,
                        textStyle = TextStyle(color = scheme.textPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(scheme.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(scheme.accent)
                        .clickable { onSendSelected() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = LocalAttachStrings.current.send,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                            .clip(CircleShape)
                            .background(scheme.surface)
                            .border(1.dp, scheme.accent, CircleShape)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedMedia.size.toString(),
                            color = scheme.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Плавающая шапка сверху с блюр-скримом (как в основной Галерее)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
        ) {
            AttachHeaderScrim()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Островок 1: Кнопка Назад (размер 50dp как у ChatTopBar)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.islandBorder, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = LocalAttachStrings.current.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Островок 2: Заголовок «Все медиа»
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(25.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = LocalAttachStrings.current.allMedia,
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Островок 3: Меню действий из 3 точек
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    val strings = LocalAttachStrings.current

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(scheme.islandBg)
                            .border(1.dp, scheme.islandBorder, CircleShape)
                            .clickable { menuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = strings.actionMenu,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = scheme.card,
                        modifier = Modifier
                            .background(scheme.card, RoundedCornerShape(14.dp))
                            .border(0.5.dp, scheme.islandBorder, RoundedCornerShape(14.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isGridMode) strings.showAsList else strings.showAsGrid,
                                    color = scheme.textPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isGridMode) Icons.Outlined.FormatListBulleted else Icons.Outlined.GridView,
                                    contentDescription = null,
                                    tint = scheme.accent
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                isGridMode = !isGridMode
                                prefs.edit().putBoolean("files_gallery_grid_mode", isGridMode).apply()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    strings.openInDots,
                                    color = scheme.textPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = null,
                                    tint = scheme.accent
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                try {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                    openInChooserLauncher.launch(Intent.createChooser(intent, strings.openInDots))
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FilesGalleryGridItem(
    media: GalleryMedia,
    selectionNumber: Int,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    var thumbnail by remember(media.uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(media.uri) {
        thumbnail = withContext(Dispatchers.IO) {
            loadMediaThumbnail(context, media.uri)
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.card)
            .clickable(onClick = onClick)
    ) {
        FilesSelectionCircle(
            selectionNumber = selectionNumber,
            overMedia = true,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .zIndex(2f)
        )
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (media.isVideo) Icons.Rounded.Videocam else Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = scheme.textMuted,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (media.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    if (media.durationMs > 0) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatDuration(media.durationMs),
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FilesGalleryListItem(
    media: GalleryMedia,
    selectionNumber: Int,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    var thumbnail by remember(media.uri) { mutableStateOf<Bitmap?>(null) }
    val meta = remember(media.uri) { getFileMetadata(context, media.uri) }

    LaunchedEffect(media.uri) {
        thumbnail = withContext(Dispatchers.IO) {
            loadMediaThumbnail(context, media.uri)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.card),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = if (media.isVideo) Icons.Rounded.Videocam else Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = scheme.textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (media.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color(0xCC000000), RoundedCornerShape(topStart = 4.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meta.name,
                color = scheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${formatFileSize(meta.size, LocalAttachStrings.current)}${if (media.isVideo && media.durationMs > 0) " • ${formatDuration(media.durationMs)}" else ""}",
                color = scheme.textSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        FilesSelectionCircle(selectionNumber = selectionNumber, overMedia = false)
        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
private fun FilesSelectionCircle(
    selectionNumber: Int,
    overMedia: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val isSelected = selectionNumber > 0
    val unselectedBg = if (overMedia) Color(0x66000000) else Color.Transparent
    val unselectedBorder = if (overMedia) Color.White else scheme.textMuted.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) scheme.accent else unselectedBg)
            .border(1.5.dp, if (isSelected) scheme.accent else unselectedBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Text(
                text = selectionNumber.toString(),
                color = scheme.onAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}
