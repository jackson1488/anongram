package com.anongram.app.features.attach.ui.tabs

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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.anongram.app.core.ui.safeStatusBarPadding
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.WindowInsets
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
import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachCardDark
import com.anongram.app.features.attach.ui.theme.AttachPillDark
import com.anongram.app.features.attach.ui.theme.AttachSheetBackground
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
    BY_NAME("Файлы А-Я")
}

@Composable
fun FilesTab(
    onSelectFile: (AttachmentDraft.File) -> Unit,
    onExpandToFullscreen: () -> Unit = {},
    selectedFiles: List<DeviceFileItem> = emptyList(),
    onToggleSelectFile: (DeviceFileItem) -> Unit = {},
    isFullscreen: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isGallerySubScreenOpen by remember { mutableStateOf(false) }

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
            modifier = modifier
        )
        return
    }

    var files by remember { mutableStateOf<List<DeviceFileItem>>(emptyList()) }
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
        files = withContext(Dispatchers.IO) {
            loadDeviceRecentFiles(context)
        }
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
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
            ) {
                // Фоновый градиент (scrim) СЗАДИ плавающей шапки
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarTopDp + 64.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to AttachSheetBackground,
                                0.75f to AttachSheetBackground.copy(alpha = 0.95f),
                                1.0f to Color.Transparent
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeStatusBarPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Кнопка назад (в режиме поиска выходит из поиска, иначе закрывает полноэкранный режим)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AttachPillDark)
                        .border(1.dp, AttachBorder, CircleShape)
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
                        contentDescription = "Назад",
                        tint = Color.White,
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
                            .background(AttachPillDark)
                            .border(1.dp, AttachBorder, RoundedCornerShape(25.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Поиск файлов...", color = Color(0xFF94A3B8), fontSize = 14.sp)
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
                                .background(AttachPillDark)
                                .border(1.dp, AttachBorder, CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Очистить",
                                tint = Color.White,
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
                            .background(AttachPillDark)
                            .border(1.dp, AttachBorder, RoundedCornerShape(25.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Выбрать файл",
                            color = Color.White,
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
                            .background(AttachPillDark)
                            .border(1.dp, AttachBorder, CircleShape)
                            .clickable { isSearchActive = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 2. В угол кнопка фильтра/сортировки
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AttachPillDark)
                            .border(1.dp, AttachBorder, CircleShape)
                            .clickable {
                                sortOrder = if (sortOrder == FileSortOrder.BY_DATE) FileSortOrder.BY_NAME else FileSortOrder.BY_DATE
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Сортировка",
                            tint = Color.White,
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
                top = if (isFullscreen) statusBarTopDp + 60.dp else 10.dp,
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
                        .background(AttachCardDark)
                ) {
                    // 1. Внутреннее хранилище
                    StorageCardRow(
                        title = "Внутреннее хранилище",
                        subtitle = "Поиск в файловой системе",
                        icon = Icons.Outlined.SdStorage,
                        iconBackground = Color(0xFF22C55E),
                        onClick = { documentPickerLauncher.launch(arrayOf("*/*")) }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp),
                        color = Color(0x1AFFFFFF),
                        thickness = 0.5.dp
                    )

                    // 2. AnonGram
                    StorageCardRow(
                        title = "AnonGram",
                        subtitle = "Поиск в папке приложения",
                        icon = Icons.Outlined.Folder,
                        iconBackground = Color(0xFF8B5CF6),
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
                        color = Color(0x1AFFFFFF),
                        thickness = 0.5.dp
                    )

                    // 3. Галерея
                    StorageCardRow(
                        title = "Галерея",
                        subtitle = "Для отправки изображений без сжатия",
                        icon = Icons.Outlined.PhotoLibrary,
                        iconBackground = Color(0xFFF59E0B),
                        onClick = { isGallerySubScreenOpen = true }
                    )
                }
            }

            // Заголовок раздела под карточкой (ТЗ: фиолетовый текст media_1793211129173.png)
            item(key = "section_header") {
                Text(
                    text = sortOrder.title,
                    color = Color(0xFFA78BFA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 18.dp)
                )
            }

            if (sortedFiles.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Файлы не найдены",
                            color = Color(0xFF94A3B8),
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
    onClick: () -> Unit
) {
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF9E9E9E),
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
                color = Color.White,
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
                text = "${formatFileSize(file.size)}${if (dateStr.isNotEmpty()) " • $dateStr" else ""}",
                color = Color(0xFF94A3B8),
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
                .background(if (isSelected) Color(0xFF8B5CF6) else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) Color(0xFF8B5CF6) else Color(0x66FFFFFF),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Выбран",
                    tint = Color.White,
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

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 Б"
    if (bytes < 1024) return "$bytes Б"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f КБ", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f МБ", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f ГБ", gb)
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

private data class Metadata(val name: String, val size: Long)

private fun getFileMetadata(context: Context, uri: Uri): Metadata {
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

@Composable
private fun FilesGallerySubScreen(
    isFullscreen: Boolean,
    onBack: () -> Unit,
    onSelectMediaAsFile: (GalleryMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anongram_attach_prefs", Context.MODE_PRIVATE) }
    var isGridMode by remember {
        mutableStateOf(prefs.getBoolean("files_gallery_grid_mode", true))
    }

    var mediaList by remember { mutableStateOf<List<GalleryMedia>>(emptyList()) }
    LaunchedEffect(Unit) {
        mediaList = withContext(Dispatchers.IO) {
            loadDeviceGalleryMedia(context)
        }
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

    Column(modifier = modifier.fillMaxSize()) {
        // Плавающие таблетки сверху (высота 50dp как у ChatTopBar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullscreen) Modifier.safeStatusBarPadding() else Modifier.padding(top = 4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Островок 1: Кнопка Назад (размер 50dp как у ChatTopBar)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(AttachPillDark)
                    .border(1.dp, AttachBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
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
                    .background(AttachPillDark)
                    .border(1.dp, AttachBorder, RoundedCornerShape(25.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Все медиа",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Островок 3: Меню действий из 3 точек
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                var menuExpanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(AttachPillDark)
                        .border(1.dp, AttachBorder, CircleShape)
                        .clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Меню действий",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(AttachCardDark)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isGridMode) "Показать списком" else "В виде сетки",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isGridMode) Icons.Outlined.FormatListBulleted else Icons.Outlined.GridView,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA)
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
                                "Открыть в...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA)
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
                                openInChooserLauncher.launch(Intent.createChooser(intent, "Открыть в..."))
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (mediaList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Медиафайлы не найдены",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        } else if (isGridMode) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(bottom = 84.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(mediaList, key = { it.uri.toString() }) { media ->
                    FilesGalleryGridItem(
                        media = media,
                        onClick = { onSelectMediaAsFile(media) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(bottom = 84.dp)
            ) {
                items(mediaList, key = { it.uri.toString() }) { media ->
                    FilesGalleryListItem(
                        media = media,
                        onClick = { onSelectMediaAsFile(media) }
                    )
                    HorizontalDivider(
                        color = Color(0x15FFFFFF),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 64.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilesGalleryGridItem(
    media: GalleryMedia,
    onClick: () -> Unit
) {
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
            .background(AttachCardDark)
            .clickable(onClick = onClick)
    ) {
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
                    tint = Color(0x66FFFFFF),
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
private fun FilesGalleryListItem(
    media: GalleryMedia,
    onClick: () -> Unit
) {
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
                .background(AttachCardDark),
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
                    tint = Color(0x88FFFFFF),
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
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${formatFileSize(meta.size)}${if (media.isVideo && media.durationMs > 0) " • ${formatDuration(media.durationMs)}" else ""}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
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
