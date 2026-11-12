package com.anongram.app.features.chats.attach.tabs.files

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryMedia
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import com.anongram.app.features.chats.attach.tabs.files.FilesGallerySubScreen
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.anongram.app.core.ui.safeStatusBarPadding
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.anongram.app.features.chats.attach.components.AttachHeaderScrim
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.i18n.AttachI18nStrings
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@androidx.compose.runtime.Immutable
data class DeviceFileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val extension: String
)

enum class FileSortOrder(val title: String) {
    BY_DATE("Недавние файлы"),
    BY_NAME("Файлы А-Я");

    fun label(strings: AttachI18nStrings): String = when (this) {
        BY_DATE -> strings.recentFiles
        BY_NAME -> strings.filesAZ
    }
}

@Composable
fun FilesTab(
    onSelectFile: (AttachmentDraft.File) -> Unit,
    onExpandToFullscreen: () -> Unit = {},
    selectedFiles: List<DeviceFileItem> = emptyList(),
    onToggleSelectFile: (DeviceFileItem) -> Unit = {},
    isFullscreen: Boolean = false,
    onBack: () -> Unit = {},
    onSelectFiles: (List<AttachmentDraft.File>) -> Unit = { list -> list.forEach(onSelectFile) },
    onOpenUncompressedGallery: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current
    val context = LocalContext.current
    var isGallerySubScreenOpen by remember { mutableStateOf(false) }
    // Выбор хранится здесь: выход из подраздела его не сбрасывает
    val galleryPicked = remember { mutableStateListOf<GalleryMedia>() }
    var galleryCaption by remember { mutableStateOf("") }

    BackHandler(enabled = isGallerySubScreenOpen) {
        isGallerySubScreenOpen = false
    }

    if (isGallerySubScreenOpen) {
        FilesGallerySubScreen(
            isFullscreen = isFullscreen,
            onBack = { isGallerySubScreenOpen = false },
            onSelectMediaAsFile = { media ->
                val meta = getFileMetadata(context, media.uri)
                onSelectFile(
                    AttachmentDraft.File(
                        uri = media.uri,
                        name = meta.name,
                        size = meta.size
                    )
                )
            },
            selectedMedia = galleryPicked,
            onToggleMedia = { media ->
                val idx = galleryPicked.indexOfFirst { it.uri == media.uri }
                if (idx >= 0) galleryPicked.removeAt(idx) else galleryPicked.add(media)
            },
            caption = galleryCaption,
            onCaptionChange = { galleryCaption = it },
            onSendSelected = {
                val drafts = galleryPicked.mapIndexed { i, m ->
                    val meta = getFileMetadata(context, m.uri)
                    AttachmentDraft.File(
                        uri = m.uri,
                        name = meta.name,
                        size = meta.size,
                        caption = if (i == 0) galleryCaption else ""
                    )
                }
                galleryPicked.clear()
                galleryCaption = ""
                onSelectFiles(drafts)
            },
            modifier = modifier
        )
        return
    }

    var files by remember { mutableStateOf<List<DeviceFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(FileSortOrder.BY_DATE) }

    // System Document Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            val meta = getFileMetadata(context, uri)
            onSelectFile(
                AttachmentDraft.File(
                    uri = uri,
                    name = meta.name,
                    size = meta.size
                )
            )
        }
    }

    // Uncompressed Gallery Picker (hd=true)
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val meta = getFileMetadata(context, uri)
            onSelectFile(
                AttachmentDraft.File(
                    uri = uri,
                    name = meta.name,
                    size = meta.size
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        files = withContext(Dispatchers.IO) {
            loadDeviceRecentFiles(context)
        }
        kotlinx.coroutines.delay(220)
        isLoading = false
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val sortedFiles = remember(filteredFiles, sortOrder) {
        when (sortOrder) {
            FileSortOrder.BY_DATE -> filteredFiles.sortedByDescending { it.dateModified }
            FileSortOrder.BY_NAME -> filteredFiles.sortedBy { it.name.lowercase() }
        }
    }

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Плавающие таблетки сверху в полном экране (высота 50dp как у ChatTopBar)
        if (isFullscreen) {
            AttachHeaderScrim(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(5f)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isFullscreen) Modifier.safeStatusBarPadding() else Modifier)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Кнопка назад (в режиме поиска выходит из поиска, иначе закрывает полноэкранный режим)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .border(1.dp, scheme.islandBorder, CircleShape)
                        .clickable {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isSearchActive) {
                    // Режим поиска: поле ввода на всю ширину
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(scheme.islandBg)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(25.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(scheme.accent),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(strings.searchFilesPlaceholder, color = scheme.textMuted, fontSize = 14.sp)
                                }
                                inner()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Крестик ТОЛЬКО когда что-то написано в поиск
                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(scheme.islandBg)
                                .border(1.dp, scheme.islandBorder, CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.clear,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    // Обычный режим: заголовок «Выбрать файл», затем кнопка поиска, затем кнопка фильтра в углу (все 50x50dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(scheme.islandBg)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(25.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = strings.chooseFileTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 1. Кнопка поиска
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(scheme.islandBg)
                            .border(1.dp, scheme.islandBorder, CircleShape)
                            .clickable { isSearchActive = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = strings.search,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2. В угол кнопка фильтра/сортировки
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(scheme.islandBg)
                            .border(1.dp, scheme.islandBorder, CircleShape)
                            .clickable {
                                sortOrder = if (sortOrder == FileSortOrder.BY_DATE) FileSortOrder.BY_NAME else FileSortOrder.BY_DATE
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = strings.sort,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (isFullscreen) statusBarTopDp + 64.dp else 10.dp,
                bottom = 84.dp
            )
        ) {
            // Общая вертикальная карточка со скруглёнными углами (ТЗ, скрин media_1793211129173.png)
            item(key = "storage_cards") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
                ) {
                    // 1. Внутреннее хранилище
                    StorageCardRow(
                        title = strings.internalStorage,
                        subtitle = strings.searchInFilesystem,
                        icon = Icons.Outlined.SdStorage,
                        iconBackground = Color(0xFF22C55E),
                        onClick = { documentPickerLauncher.launch(arrayOf("*/*")) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = scheme.divider,
                        thickness = 0.5.dp
                    )

                    // 2. AnonGram
                    StorageCardRow(
                        title = "AnonGram",
                        subtitle = strings.searchInAppFolder,
                        icon = Icons.Outlined.Folder,
                        iconBackground = scheme.accent,
                        iconTint = scheme.onAccent,
                        onClick = {
                            val appFiles = context.filesDir.listFiles()?.map { f ->
                                DeviceFileItem(
                                    uri = Uri.fromFile(f),
                                    name = f.name,
                                    size = f.length(),
                                    dateModified = f.lastModified(),
                                    extension = f.extension
                                )
                            } ?: emptyList()
                            files = appFiles
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = scheme.divider,
                        thickness = 0.5.dp
                    )

                    // 3. Галерея
                    StorageCardRow(
                        title = strings.tabGallery,
                        subtitle = strings.galleryFilesSubtitle,
                        icon = Icons.Outlined.PhotoLibrary,
                        iconBackground = Color(0xFFF59E0B),
                        onClick = {
                            if (onOpenUncompressedGallery != null) {
                                onOpenUncompressedGallery()
                            } else {
                                onExpandToFullscreen()
                                isGallerySubScreenOpen = true
                            }
                        }
                    )
                }
            }

            // Заголовок раздела под карточкой
            item(key = "section_header") {
                Text(
                    text = sortOrder.label(strings),
                    color = scheme.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 18.dp)
                )
            }

            if (isLoading) {
                item(key = "files_loading_skeleton") {
                    com.anongram.app.features.chats.attach.components.RecentFilesSkeleton()
                }
            } else if (sortedFiles.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.filesNotFound,
                            color = scheme.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(sortedFiles, key = { it.uri.toString() }) { file ->
                    val isSelected = selectedFiles.any { it.uri == file.uri }
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        FileRowItem(
                            file = file,
                            isSelected = isSelected,
                            onClick = {
                                onToggleSelectFile(file)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCardRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = scheme.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun FileRowItem(
    file: DeviceFileItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val fileColor = remember(file.extension) { getFileExtensionColor(file.extension) }
    val extLabel = remember(file.extension) {
        if (file.extension.isNotBlank()) file.extension.uppercase().take(4) else "FILE"
    }
    val isImage = remember(file.extension) {
        file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
    }
    var imgBitmap by remember(file.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(file.uri, isImage) {
        if (isImage) {
            val bmp = withContext(Dispatchers.IO) {
                loadMediaThumbnail(context, file.uri)
            }
            imgBitmap = bmp?.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imgBitmap != null) {
            Image(
                bitmap = imgBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fileColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = fileColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = extLabel,
                        color = fileColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val dateStr = remember(file.dateModified) {
                if (file.dateModified > 0) {
                    val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
                    sdf.format(Date(file.dateModified))
                } else ""
            }
            Text(
                text = "${formatFileSize(file.size, LocalAttachStrings.current)}${if (dateStr.isNotEmpty()) " • $dateStr" else ""}",
                color = scheme.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Кружок выбора справа (ТЗ, шаг 12)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) scheme.accent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) scheme.accent else scheme.textMuted.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = LocalAttachStrings.current.selected,
                    tint = scheme.onAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getFileExtensionColor(ext: String): Color {
    return when (ext.lowercase()) {
        "pdf" -> Color(0xFFEF4444) // Red
        "zip", "rar", "7z", "tar", "gz" -> Color(0xFFF97316) // Orange
        "apk" -> Color(0xFF10B981) // Green
        "mp3", "wav", "flac", "ogg", "m4a" -> Color(0xFF8B5CF6) // Purple
        "mp4", "mkv", "avi", "mov" -> Color(0xFFEC4899) // Pink
        "doc", "docx", "txt" -> Color(0xFF3B82F6) // Blue
        "xls", "xlsx", "csv" -> Color(0xFF14B8A6) // Teal
        "ppt", "pptx" -> Color(0xFFEAB308) // Yellow
        else -> {
            val palette = listOf(
                Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4),
                Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899)
            )
            val idx = ext.hashCode().absoluteValue % palette.size
            palette[idx]
        }
    }
}

internal fun formatFileSize(bytes: Long, strings: AttachI18nStrings): String {
    if (bytes <= 0) return strings.sizeBytes(0)
    if (bytes < 1024) return strings.sizeBytes(bytes)
    val kb = bytes / 1024.0
    if (kb < 1024) return strings.sizeKb(String.format(Locale.US, "%.1f", kb))
    val mb = kb / 1024.0
    if (mb < 1024) return strings.sizeMb(String.format(Locale.US, "%.1f", mb))
    val gb = mb / 1024.0
    return strings.sizeGb(String.format(Locale.US, "%.2f", gb))
}

private fun loadDeviceRecentFiles(context: Context): List<DeviceFileItem> {
    val result = mutableListOf<DeviceFileItem>()
    val contentResolver = context.contentResolver

    // Query Downloads directory directly
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (downloadsDir.exists() && downloadsDir.isDirectory) {
        downloadsDir.listFiles()?.filter { it.isFile }?.take(50)?.forEach { f ->
            result.add(
                DeviceFileItem(
                    uri = Uri.fromFile(f),
                    name = f.name,
                    size = f.length(),
                    dateModified = f.lastModified(),
                    extension = f.extension
                )
            )
        }
    }

    // Also query MediaStore Files
    val uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_MODIFIED
    )
    val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

    try {
        val cursor = contentResolver.query(uri, projection, null, null, sortOrder)
        cursor?.use { c ->
            val idIndex = c.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val nameIndex = c.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeIndex = c.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
            val dateIndex = c.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

            var count = 0
            while (c.moveToNext() && count < 60) {
                val id = if (idIndex >= 0) c.getLong(idIndex) else 0L
                val name = if (nameIndex >= 0) c.getString(nameIndex) ?: "file" else "file"
                val size = if (sizeIndex >= 0) c.getLong(sizeIndex) else 0L
                val dateSec = if (dateIndex >= 0) c.getLong(dateIndex) else 0L
                val fileUri = ContentUris.withAppendedId(uri, id)

                val ext = name.substringAfterLast('.', "")
                if (result.none { it.name == name && it.size == size }) {
                    result.add(
                        DeviceFileItem(
                            uri = fileUri,
                            name = name,
                            size = size,
                            dateModified = dateSec * 1000L,
                            extension = ext
                        )
                    )
                    count++
                }
            }
        }
    } catch (_: Exception) {}

    return result
}

internal data class Metadata(val name: String, val size: Long)

internal fun getFileMetadata(context: Context, uri: Uri): Metadata {
    var name = "file"
    var size = 0L
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (nameIndex >= 0) name = c.getString(nameIndex) ?: "file"
                if (sizeIndex >= 0) size = c.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}
    return Metadata(name, size)
}
