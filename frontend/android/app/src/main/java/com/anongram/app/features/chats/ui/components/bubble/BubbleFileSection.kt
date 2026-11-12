package com.anongram.app.features.chats.ui.components.bubble

import android.net.Uri
import com.anongram.app.features.chats.input.banner.ChatActionBannerBus
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
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
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
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
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
 * Строка файла или фотографии без сжатия внутри пузыря (одиночной или в группе до 10 шт.)
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
    isPinned: Boolean = false,
    onItemClick: () -> Unit?,
    onItemLongClick: () -> Unit,
    onSwipeReply: () -> Unit,
    onForwardItem: () -> Unit,
    onPinItem: (() -> Unit)? = null,
    onEditItem: (() -> Unit)? = null,
    onDeleteItem: () -> Unit,
    onRetry: () -> Unit,
    onOpenPhotoAt: (Int) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val bubbleText = if (isIncoming) scheme.incomingText else scheme.outgoingText
    val bubbleSubtext = bubbleText.copy(alpha = 0.70f)
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
                            .background(scheme.surface),
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
                            .background(if (isIncoming) accent else bubbleText),
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
                            tint = if (isIncoming) scheme.onAccent else scheme.outgoingBubbleStart,
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
                            .background(if (isItemSelected) scheme.accent else Color.Black.copy(alpha = 0.55f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isItemSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Выбрано",
                                tint = scheme.onAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Центральная часть: имя файла и снизу размер + расширение
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
                    color = bubbleText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${file.sizeLabel} $extLabel",
                    color = bubbleSubtext,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Кнопка «⋮» в правом верхнем углу
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
                        tint = bubbleSubtext,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    containerColor = scheme.card,
                    modifier = Modifier
                        .width(235.dp)
                        .background(scheme.card, RoundedCornerShape(16.dp))
                        .border(0.5.dp, scheme.cardBorder, RoundedCornerShape(16.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Ответить", color = scheme.textPrimary, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.Reply, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onSwipeReply()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isImageFile) "Сохранить в галерею" else "Сохранить в загрузки",
                                color = scheme.textPrimary,
                                fontSize = 14.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            scope.launch {
                                if (isImageFile && !file.uri.isNullOrBlank()) {
                                    val ok = savePhotoToDeviceGallery(context, file.uri)
                                    ChatActionBannerBus.show(
                                        icon = if (ok) Icons.Outlined.FileDownload else Icons.Outlined.ErrorOutline,
                                        isError = !ok
                                    ) { s ->
                                        if (ok) s.bannerSavedToGalleryNamed(file.name) else s.bannerPhotoSaveFailed
                                    }
                                } else {
                                    val ok = saveFileToDeviceDownloads(context, file)
                                    ChatActionBannerBus.show(
                                        icon = if (ok) Icons.Outlined.FileDownload else Icons.Outlined.ErrorOutline,
                                        isError = !ok
                                    ) { s ->
                                        if (ok) s.bannerSavedToDownloadsNamed(file.name) else s.bannerFileSaveFailed
                                    }
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Поделиться", color = scheme.textPrimary, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
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
                        text = { Text("Переслать", color = scheme.textPrimary, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Forward, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onForwardItem()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(if (isPinned) "Открепить" else "Закрепить", color = scheme.textPrimary, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.PushPin, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onPinItem?.invoke()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Изменить", color = scheme.textPrimary, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Edit, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onEditItem?.invoke()
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
                    color = bubbleSubtext,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!isIncoming) {
                    Spacer(modifier = Modifier.width(3.dp))
                    DeliveryStatus(
                        status = message.status,
                        onRetry = onRetry,
                        mutedTint = bubbleSubtext
                    )
                }
            }
        }
    }
}
