package com.anongram.app.features.chats.ui.components.bubble

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.timeLabel
import com.anongram.app.features.chats.ui.components.savePhotoToDeviceGallery
import com.anongram.app.features.chats.ui.components.sharePhotoSystemChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Строка файла или фотографии без сжатия внутри пузыря (одиночной или в группе до 10 шт.):
 * - Для фото без сжатия (FileKind.IMAGE): квадратная миниатюра 62.dp вплотную слева без вертикальных зазоров
 * - Для документов/APK/архивов: круглая иконка 44.dp слева
 * - Справа сверху (только у первого элемента в группе): иконка «⋮» с меню («Сохранить в галерею» для фото или «Сохранить в загрузки» для файлов, «Ответить», «Переслать», «Поделиться», «Удалить»)
 * - Тап по фото без сжатия открывает полноэкранный просмотр с перелистыванием внутри своей группы (FileGroup) и лентой миниатюр снизу без счётчика
 * - В режиме выбора при выборе одного файла горит только галочка (без подсветки строки), а при выборе всей группы светится вся группа
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatFileItemRow(
    message: ChatMessage,
    file: MessageAttachment.File,
    fileIndex: Int = 0,
    isIncoming: Boolean,
    accent: Color,
    searchQuery: String,
    showThreeDots: Boolean,
    showBottomTimestamp: Boolean,
    isSelectionMode: Boolean,
    isItemSelected: Boolean,
    onItemClick: () -> Unit?,
    onItemLongClick: () -> Unit,
    onSwipeReply: () -> Unit,
    onForwardItem: () -> Unit,
    onDeleteItem: () -> Unit,
    onRetry: () -> Unit,
    onOpenPhotoAt: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    val isImageFile = file.kind == FileKind.IMAGE ||
        file.name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp")

    val parsedUri = remember(file.uri) { file.uri?.let { runCatching { Uri.parse(it) }.getOrNull() } }
    var thumbBitmap by remember(file.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(parsedUri, isImageFile) {
        if (isImageFile && parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            thumbBitmap = quick?.asImageBitmap()
        }
    }

    val extLabel = remember(file.name, file.kind) {
        val ext = file.name.substringAfterLast('.', "").uppercase()
        if (ext.isNotBlank() && ext.length <= 5) ext else file.kind.label
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onItemClick()
                    } else if (isImageFile && !file.uri.isNullOrBlank()) {
                        onOpenPhotoAt(fileIndex)
                    } else {
                        openFileInSystemViewer(context, file)
                    }
                },
                onLongClick = onItemLongClick
            )
            .padding(vertical = if (isImageFile) 0.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть: квадратная миниатюра для фото без сжатия или круглая иконка для документа
            Box(contentAlignment = Alignment.Center) {
                if (isImageFile) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E222D)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbBitmap != null) {
                            Image(
                                bitmap = thumbBitmap!!,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.InsertDriveFile,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isIncoming) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when {
                            file.kind == FileKind.APK || file.name.endsWith(".apk", ignoreCase = true) -> Icons.Outlined.Android
                            file.kind == FileKind.PDF || file.kind == FileKind.DOC -> Icons.Outlined.Description
                            else -> Icons.Outlined.InsertDriveFile
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // В режиме выбора на отдельном файле загорается только круглая галочка
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isItemSelected) accent else Color.Black.copy(alpha = 0.55f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isItemSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Выбрано",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Центральная часть: имя файла и снизу размер + расширение (например «212,0 KB JPG» или «28,2 KB PDF»)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (showThreeDots) 24.dp else 4.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        buildHighlightedText(file.name, searchQuery, accent)
                    } else {
                        AnnotatedString(file.name)
                    },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${file.sizeLabel} $extLabel",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Кнопка «⋮» в правом верхнем углу (только одна у первого файла в группе или у одиночного файла)
        if (showThreeDots) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Меню файла",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    modifier = Modifier
                        .width(235.dp)
                        .background(Color(0xFF14151B), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isImageFile) "Сохранить в галерею" else "Сохранить в загрузки",
                                color = Color.White,
                                fontSize = 14.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            scope.launch {
                                if (isImageFile && !file.uri.isNullOrBlank()) {
                                    val ok = savePhotoToDeviceGallery(context, file.uri)
                                    Toast.makeText(
                                        context,
                                        if (ok) "Сохранено в галерею: ${file.name}" else "Не удалось сохранить фото",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val ok = saveFileToDeviceDownloads(context, file)
                                    Toast.makeText(
                                        context,
                                        if (ok) "Сохранено в загрузки: ${file.name}" else "Не удалось сохранить файл",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ответить", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Reply, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onSwipeReply()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Переслать", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Forward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onForwardItem()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Поделиться", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            if (!file.uri.isNullOrBlank()) {
                                sharePhotoSystemChooser(context, file.uri)
                            } else {
                                shareTextOrFileFallback(context, file.name)
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Удалить", color = Color(0xFFFF5252), fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onDeleteItem()
                        }
                    )
                }
            }
        }

        // Время и галочки в правом нижнем углу последнего элемента
        if (showBottomTimestamp) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.timeLabel(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!isIncoming) {
                    Spacer(modifier = Modifier.width(3.dp))
                    DeliveryStatus(status = message.status, onRetry = onRetry)
                }
            }
        }
    }
}
