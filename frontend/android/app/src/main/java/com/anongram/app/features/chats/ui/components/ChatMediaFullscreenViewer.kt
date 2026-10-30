package com.anongram.app.features.chats.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.attach.ui.tabs.loadHdBitmap
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Полноэкранный просмотр фото или альбома из чата (Telegram-style):
 * - Полупрозрачная сплошная шапка сверху (не отдельные кружки), которая плавно скрывается/показывается по тапу на экран
 * - Слева в шапке: кнопка Назад (←), имя отправителя и снизу дата/время:
 *   «сегодня в 19:34», «вчера в ...», в пределах 7 дней — день недели («вторник в 19:34»), старше недели — дата
 * - Справа в шапке: иконка Кисточка, иконка Поделиться (пока заглушки) и кнопка «⋮» (три точки)
 * - Чёрное меню «⋮» со скруглёнными краями:
 *   1. Сохранить в галерею
 *   2. Показать в чате
 *   3. Ответить
 *   4. Поделиться (системный диалог Android)
 *   5. Удалить (с окном подтверждения и галочкой «Также удалить для ...»)
 * - Если это альбом (2+ фото): внизу экрана полоса маленьких миниатюр с плавным анимированным переключением
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMediaFullscreenViewer(
    message: ChatMessage,
    initialPhotoIndex: Int,
    peerName: String,
    onClose: () -> Unit,
    onShowInChat: (ChatMessage) -> Unit,
    onReply: (ChatMessage) -> Unit,
    onRequestDelete: (ChatMessage, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mediaUris: List<String> = remember(message) {
        when (val att = message.attachment) {
            is MessageAttachment.MediaAlbum -> att.items.map { it.uri }
            is MessageAttachment.Photo -> listOfNotNull(att.uri)
            else -> emptyList()
        }
    }

    if (mediaUris.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val safeInitial = initialPhotoIndex.coerceIn(0, (mediaUris.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { mediaUris.size })
    val thumbListState = rememberLazyListState()

    var showControls by remember { mutableStateOf(true) }
    var isMenuOpen by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    LaunchedEffect(pagerState.currentPage) {
        if (mediaUris.size > 1) {
            runCatching {
                thumbListState.animateScrollToItem(pagerState.currentPage)
            }
        }
    }

    val senderTitle = peerName
    val dateSubTitle = remember(message.epochMs) { formatPhotoViewerSubtitleDate(message.epochMs) }
    val currentUriString = mediaUris.getOrNull(pagerState.currentPage) ?: mediaUris.first()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(300f)
    ) {
        // 1. Горизонтальный пейджер фотографий
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (isMenuOpen) {
                                isMenuOpen = false
                            } else {
                                showControls = !showControls
                            }
                        }
                    )
                }
        ) { page ->
            val uriStr = mediaUris[page]
            val parsedUri = remember(uriStr) { runCatching { Uri.parse(uriStr) }.getOrNull() }
            var bitmap by remember(uriStr) { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(parsedUri) {
                if (parsedUri != null) {
                    val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
                    if (bitmap == null) bitmap = quick?.asImageBitmap()
                    val hd = withContext(Dispatchers.IO) { loadHdBitmap(context, parsedUri, 2400) }
                    if (hd != null) bitmap = hd.asImageBitmap()
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 2. Полупрозрачная сплошная верхняя шапка (не кружки), скрывается/появляется по тапу
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .safeStatusBarPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка Назад (слева)
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Имя и снизу дата/время («сегодня в 19:34» / день недели / дата)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = senderTitle,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateSubTitle,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Кнопка кисточки (пока просто оставляем иконку)
                    IconButton(onClick = { /* будет реализовано позже */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Brush,
                            contentDescription = "Редактировать",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Кнопка поделиться в шапке (пока просто оставляем иконку)
                    IconButton(onClick = { /* будет реализовано позже */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Поделиться",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Кнопка «⋮» (три точки) с чёрным скруглённым меню
                    Box {
                        IconButton(onClick = { isMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "Меню",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false },
                            modifier = Modifier
                                .width(235.dp)
                                .background(Color(0xFF101014), RoundedCornerShape(18.dp))
                                .border(0.5.dp, Color(0x28FFFFFF), RoundedCornerShape(18.dp))
                        ) {
                            // 1. Сохранить в галерею
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Сохранить в галерею",
                                        color = Color.White,
                                        fontSize = 15.sp
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
                                        val ok = savePhotoToDeviceGallery(context, currentUriString)
                                        Toast.makeText(
                                            context,
                                            if (ok) "Сохранено в галерею" else "Не удалось сохранить фото",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )

                            // 2. Показать в чате
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Показать в чате",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onShowInChat(message)
                                }
                            )

                            // 3. Ответить
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Ответить",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Reply,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onReply(message)
                                }
                            )

                            // 4. Поделиться (системное меню Android)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Поделиться",
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    sharePhotoSystemChooser(context, currentUriString)
                                }
                            )

                            // 5. Удалить (с подтверждением у меня или у всех)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Удалить",
                                        color = Color(0xFFFF5252),
                                        fontSize = 15.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onRequestDelete(message, pagerState.currentPage)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Нижняя панель с подписью (если есть) и лентой миниатюр для альбома (как в Telegram)
        AnimatedVisibility(
            visible = showControls && (mediaUris.size > 1 || message.text.isNotBlank()),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .safeNavigationBarPadding()
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (mediaUris.size > 1) {
                    LazyRow(
                        state = thumbListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        itemsIndexed(mediaUris) { index, uriStr ->
                            val isSelected = (index == pagerState.currentPage)
                            val thumbWidth by animateDpAsState(
                                targetValue = if (isSelected) 48.dp else 36.dp,
                                animationSpec = tween(200),
                                label = "thumbW"
                            )
                            val thumbHeight by animateDpAsState(
                                targetValue = if (isSelected) 54.dp else 46.dp,
                                animationSpec = tween(200),
                                label = "thumbH"
                            )

                            ViewerMiniThumbnail(
                                uriString = uriStr,
                                isSelected = isSelected,
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                modifier = Modifier.size(width = thumbWidth, height = thumbHeight)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerMiniThumbnail(
    uriString: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedUri = remember(uriString) { runCatching { Uri.parse(uriString) }.getOrNull() }
    var thumbBitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(parsedUri) {
        if (parsedUri != null) {
            val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, parsedUri) }
            thumbBitmap = quick?.asImageBitmap()
        }
    }

    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF222228))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Color.White, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        if (thumbBitmap != null) {
            Image(
                bitmap = thumbBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Форматирование даты и времени для шапки просмотра фото:
 * - Сегодня -> «сегодня в 19:34»
 * - Вчера -> «вчера в 19:34»
 * - В пределах 7 дней (до одной недели) -> название дня недели («вторник в 19:34»)
 * - Старше недели -> полная дата («12 октября в 19:34»)
 */
private fun formatPhotoViewerSubtitleDate(epochMs: Long): String {
    val zone = ZoneId.systemDefault()
    val zdt = Instant.ofEpochMilli(epochMs).atZone(zone)
    val msgDate = zdt.toLocalDate()
    val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
    val ruLocale = Locale.forLanguageTag("ru")
    val timeStr = zdt.format(DateTimeFormatter.ofPattern("HH:mm", ruLocale))
    val daysDiff = ChronoUnit.DAYS.between(msgDate, today)

    return when {
        daysDiff == 0L -> "сегодня в $timeStr"
        daysDiff == 1L -> "вчера в $timeStr"
        daysDiff in 2L..6L -> {
            val dayOfWeek = zdt.format(DateTimeFormatter.ofPattern("EEEE", ruLocale)).lowercase(ruLocale)
            "$dayOfWeek в $timeStr"
        }
        msgDate.year == today.year -> {
            val datePart = zdt.format(DateTimeFormatter.ofPattern("d MMMM", ruLocale))
            "$datePart в $timeStr"
        }
        else -> {
            val datePart = zdt.format(DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale))
            "$datePart в $timeStr"
        }
    }
}

/**
 * Реальное сохранение фотографии в галерею устройства через MediaStore.
 */
private suspend fun savePhotoToDeviceGallery(context: Context, uriString: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val parsedUri = Uri.parse(uriString)
            val bitmap = loadHdBitmap(context, parsedUri, 2560) ?: loadMediaThumbnail(context, parsedUri)
                ?: return@withContext false

            val filename = "AnonGram_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnonGram")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false

            resolver.openOutputStream(destUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(destUri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

/**
 * Системное окно «Поделиться» (Intent.ACTION_SEND) для отправки фото в любой мессенджер или приложение.
 */
private fun sharePhotoSystemChooser(context: Context, uriString: String) {
    runCatching {
        val parsedUri = Uri.parse(uriString)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, parsedUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Поделиться фото").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
