package com.anongram.app.features.attach.ui.tabs

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import com.anongram.app.features.attach.ui.components.AttachInputBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.attach.ui.components.PartialAccessBanner
import com.anongram.app.features.attach.ui.components.PermissionDeniedView
import com.anongram.app.features.mediaeditor.api.EditableMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.runtime.Immutable

import com.anongram.app.features.attach.ui.components.SpoilerOverlay

@Immutable
data class GalleryMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val bucketName: String = "",
    val dateAdded: Long = 0L,
    val hasSpoiler: Boolean = false
)

@Immutable
data class GalleryAlbum(
    val name: String,
    val count: Int,
    val coverUri: Uri?
)

private val PurpleAccent = Color(0xFF8B5CF6)
private val CardDark = Color(0xFF131D31)
private val BorderMuted = Color(0x33FFFFFF)

@Composable
fun GalleryTab(
    onLaunchCamera: () -> Unit,
    onOpenEditor: (List<EditableMediaItem>) -> Unit,
    selectedAlbum: String = "Все медиа",
    onSelectAlbum: (String) -> Unit = {},
    isAlbumsSheetOpen: Boolean = false,
    onCloseAlbumsSheet: () -> Unit = {},
    onOpenAlbumsSheet: () -> Unit = {},
    selectedMedia: List<GalleryMedia> = emptyList(),
    onToggleSelectMedia: (GalleryMedia) -> Unit = {},
    caption: String = "",
    onCaptionChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    capturedMediaItems: List<GalleryMedia> = emptyList(),
    isFullscreen: Boolean = false,
    onOpenViewer: ((List<GalleryMedia>, Int) -> Unit)? = null,
    onAlbumsLoaded: ((List<GalleryAlbum>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(PermissionUtils.hasGalleryPermission(context))
    }
    val hasCameraPerm = remember {
        PermissionUtils.hasCameraPermission(context)
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
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            loadDeviceGalleryMedia(context)
        }
        allMedia = loaded
    }

    val allCombinedMedia = remember(capturedMediaItems, allMedia) {
        capturedMediaItems + allMedia
    }

    val albumsList = remember(allCombinedMedia) {
        val all = GalleryAlbum(
            name = "Все медиа",
            count = allCombinedMedia.size,
            coverUri = allCombinedMedia.firstOrNull()?.uri
        )
        val buckets = allCombinedMedia
            .filter { it.bucketName.isNotBlank() }
            .groupBy { it.bucketName }
            .map { (bucket, items) ->
                GalleryAlbum(
                    name = bucket,
                    count = items.size,
                    coverUri = items.firstOrNull()?.uri
                )
            }
        listOf(all) + buckets
    }

    LaunchedEffect(albumsList) {
        onAlbumsLoaded?.invoke(albumsList)
    }

    val filteredMedia = remember(allCombinedMedia, selectedAlbum) {
        if (selectedAlbum == "Все медиа" || selectedAlbum == "Все" || selectedAlbum.isBlank()) {
            allCombinedMedia
        } else {
            allCombinedMedia.filter { it.bucketName.equals(selectedAlbum, ignoreCase = true) }
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

            val topGridPadding = if (isFullscreen) 64.dp else 12.dp

            val openViewerHelper: (Int) -> Unit = { index ->
                if (onOpenViewer != null) {
                    onOpenViewer(filteredMedia, index)
                } else {
                    viewerIndex = index
                }
            }

            // Сетка медиа 3 колонки (ТЗ, шаг 5)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 2.dp, top = topGridPadding, end = 2.dp, bottom = 84.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Первая плитка: живое превью камеры высотой в 2 фото по вертикали (ТЗ, шаг 5)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val colWidth = (maxWidth - 4.dp) / 3
                        val tileHeight = colWidth * 2 + 2.dp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tileHeight),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Камера слева: 1 колонка в ширину, 2 фото в высоту
                            LiveCameraTile(
                                hasCameraPermission = hasCameraPerm,
                                onLaunchCamera = onLaunchCamera,
                                modifier = Modifier
                                    .width(colWidth)
                                    .fillMaxHeight()
                            )

                            // Справа 2x2 фото
                            Column(
                                modifier = Modifier.weight(2f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(colWidth),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (i in 0..1) {
                                        val media = filteredMedia.getOrNull(i)
                                        if (media != null) {
                                            val selectedIndex = selectedMedia.indexOfFirst { it.uri == media.uri }
                                            val isSelected = selectedIndex >= 0
                                            val effectiveMedia = if (isSelected) selectedMedia[selectedIndex] else media
                                            GalleryItemTile(
                                                media = effectiveMedia,
                                                isSelected = isSelected,
                                                selectionNumber = if (isSelected) selectedIndex + 1 else 0,
                                                onOpenViewer = { openViewerHelper(i) },
                                                onToggleSelect = { onToggleSelectMedia(effectiveMedia) },
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().height(colWidth),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    for (i in 2..3) {
                                        val media = filteredMedia.getOrNull(i)
                                        if (media != null) {
                                            val selectedIndex = selectedMedia.indexOfFirst { it.uri == media.uri }
                                            val isSelected = selectedIndex >= 0
                                            val effectiveMedia = if (isSelected) selectedMedia[selectedIndex] else media
                                            GalleryItemTile(
                                                media = effectiveMedia,
                                                isSelected = isSelected,
                                                selectionNumber = if (isSelected) selectedIndex + 1 else 0,
                                                onOpenViewer = { openViewerHelper(i) },
                                                onToggleSelect = { onToggleSelectMedia(effectiveMedia) },
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Остальные элементы медиатеки с 5-го элемента (индекс 4)
                items(filteredMedia.drop(4), key = { it.uri.toString() }) { media ->
                    val selectedIndex = selectedMedia.indexOfFirst { it.uri == media.uri }
                    val isSelected = selectedIndex >= 0
                    val effectiveMedia = if (isSelected) selectedMedia[selectedIndex] else media
                    val originalIndex = filteredMedia.indexOf(media)

                    GalleryItemTile(
                        media = effectiveMedia,
                        isSelected = isSelected,
                        selectionNumber = if (isSelected) selectedIndex + 1 else 0,
                        onOpenViewer = {
                            if (originalIndex >= 0) {
                                openViewerHelper(originalIndex)
                            }
                        },
                        onToggleSelect = {
                            onToggleSelectMedia(effectiveMedia)
                        }
                    )
                }
            }
        }

        // Полноэкранный просмотр фото/видео со свайпом влево-вправо (если не перехвачен родителем)
        if (viewerIndex != null && onOpenViewer == null) {
            GalleryMediaViewer(
                mediaList = filteredMedia,
                initialIndex = viewerIndex ?: 0,
                selectedMedia = selectedMedia,
                onToggleSelect = onToggleSelectMedia,
                caption = caption,
                onCaptionChange = onCaptionChange,
                onSend = onSend,
                onClose = { viewerIndex = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Первая плитка Галереи: реальное живое превью камеры со значком камеры в углу (ТЗ, шаг 5).
 */
@SuppressLint("MissingPermission")
@Composable
private fun LiveCameraTile(
    hasCameraPermission: Boolean,
    onLaunchCamera: () -> Unit,
    modifier: Modifier = Modifier.aspectRatio(1f)
) {
    Box(
        modifier = modifier
            .background(Color(0xFF1E293B))
            .clickable { onLaunchCamera() },
        contentAlignment = Alignment.Center
    ) {
        val isEmulatorOrWaydroid = remember {
            android.os.Build.FINGERPRINT.contains("waydroid", ignoreCase = true) ||
            android.os.Build.HARDWARE.contains("waydroid", ignoreCase = true) ||
            android.os.Build.PRODUCT.contains("waydroid", ignoreCase = true)
        }
        if (hasCameraPermission && !isEmulatorOrWaydroid) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            var cameraDevice: CameraDevice? = null
                            var captureSession: CameraCaptureSession? = null
                            var cameraThread: HandlerThread? = null
                            var cameraHandler: Handler? = null

                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                try {
                                    val thread = HandlerThread("CameraBackgroundThread").apply { start() }
                                    cameraThread = thread
                                    val handler = Handler(thread.looper)
                                    cameraHandler = handler

                                    val manager = ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
                                    val cameraId = manager.cameraIdList.firstOrNull { id ->
                                        manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                                    } ?: manager.cameraIdList.firstOrNull() ?: return

                                    manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                                        override fun onOpened(camera: CameraDevice) {
                                            cameraDevice = camera
                                            try {
                                                val previewSurface = Surface(surface)
                                                val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                                    addTarget(previewSurface)
                                                }
                                                camera.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                                                    override fun onConfigured(session: CameraCaptureSession) {
                                                        captureSession = session
                                                        try {
                                                            session.setRepeatingRequest(requestBuilder.build(), null, handler)
                                                        } catch (_: Exception) {}
                                                    }
                                                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                                                }, handler)
                                            } catch (_: Exception) {}
                                        }

                                        override fun onDisconnected(camera: CameraDevice) {
                                            camera.close()
                                        }

                                        override fun onError(camera: CameraDevice, error: Int) {
                                            camera.close()
                                        }
                                    }, handler)
                                } catch (_: SecurityException) {
                                } catch (_: Exception) {}
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                try {
                                    captureSession?.close()
                                    cameraDevice?.close()
                                    cameraThread?.quitSafely()
                                } catch (_: Exception) {}
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = "Камера",
                    tint = PurpleAccent,
                    modifier = Modifier.size(34.dp)
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

        // Значок камеры в углу (ТЗ, шаг 5)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AlbumDropdownRowItem(
    album: GalleryAlbum,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(album.coverUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(album.coverUri) {
        album.coverUri?.let { uri ->
            val bmp = withContext(Dispatchers.IO) {
                loadMediaThumbnail(context, uri)
            }
            bitmap = bmp?.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0x227C4DFF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Квадратная превьюшка 38x38dp со скруглением 6dp (media_1793211707146.png)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Название альбома жирным белым и сразу серым количество
        val displayName = if (album.name == "Все медиа") "Галерея" else album.name
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = album.count.toString(),
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun GalleryItemTile(
    media: GalleryMedia,
    isSelected: Boolean,
    selectionNumber: Int,
    onOpenViewer: () -> Unit,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(media.uri) {
        val bmp = withContext(Dispatchers.IO) {
            loadMediaThumbnail(context, media.uri)
        }
        bitmap = bmp?.asImageBitmap()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF131D31))
            .clickable { onOpenViewer() }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isSelected && media.hasSpoiler) Modifier.blur(18.dp) else Modifier)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A2338))
            )
        }

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
                    contentDescription = "Video",
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

        // Анимированный спойлер, если включён
        if (isSelected && media.hasSpoiler) {
            SpoilerOverlay(modifier = Modifier.fillMaxSize())
        }

        // Кружок выбора в правом верхнем углу (ТЗ, шаг 9: ТОЛЬКО выбирает/снимает выбор)
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
                )
                .clickable { onToggleSelect() },
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

/**
 * Полноэкранный просмотр медиа (ТЗ, шаг 9):
 * - Свайп влево-вправо между файлами
 * - Сверху: кнопка Назад, "3 из 24", кружок выбора
 * - Снизу: панель ввода с подписью и кнопкой отправки, если есть выбранные
 * - Видео: большая кнопка Play по центру
 */
@Composable
fun GalleryMediaViewer(
    mediaList: List<GalleryMedia>,
    initialIndex: Int,
    selectedMedia: List<GalleryMedia>,
    onToggleSelect: (GalleryMedia) -> Unit,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val safeInitial = initialIndex.coerceIn(0, (mediaList.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { mediaList.size })
    val currentMedia = mediaList.getOrNull(pagerState.currentPage)
    val currentSelectedIdx = currentMedia?.let { m -> selectedMedia.indexOfFirst { it.uri == m.uri } } ?: -1
    val isSelected = currentSelectedIdx >= 0

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Свайп влево-вправо между файлами
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = mediaList.getOrNull(page)
            if (item != null) {
                GalleryMediaPageItem(media = item)
            }
        }

        // Верхняя панель навигации
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset, start = 8.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${pagerState.currentPage + 1} из ${mediaList.size}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Кружок выбора
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PurpleAccent else Color(0x66000000))
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) PurpleAccent else Color.White,
                        shape = CircleShape
                    )
                    .clickable {
                        currentMedia?.let { onToggleSelect(it) }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = (currentSelectedIdx + 1).toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Нижняя панель с подписью и кнопкой отправки при выборе
        AttachInputBar(
            caption = caption,
            onCaptionChange = onCaptionChange,
            selectedCount = selectedMedia.size,
            visible = selectedMedia.isNotEmpty(),
            onSend = onSend,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GalleryMediaPageItem(
    media: GalleryMedia,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }
    var isPlayingVideo by remember { mutableStateOf(false) }

    LaunchedEffect(media.uri) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                if (media.isVideo) {
                    loadMediaThumbnail(context, media.uri)
                } else {
                    val stream = context.contentResolver.openInputStream(media.uri)
                    BitmapFactory.decodeStream(stream).also { stream?.close() }
                }
            } catch (_: Exception) {
                null
            }
        }
        bitmap = bmp?.asImageBitmap()
    }

    Box(
        modifier = modifier.fillMaxSize(),
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

        // Кнопка Play по центру для видео
        if (media.isVideo) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { isPlayingVideo = !isPlayingVideo },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Воспроизвести",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
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

private val thumbnailCache = android.util.LruCache<Uri, Bitmap>(150)

internal fun loadMediaThumbnail(context: Context, uri: Uri): Bitmap? {
    thumbnailCache.get(uri)?.let { return it }
    return try {
        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(260, 260), null)
        } else {
            val stream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream).also { stream?.close() }
        }
        if (bmp != null) {
            thumbnailCache.put(uri, bmp)
        }
        bmp
    } catch (_: Exception) {
        null
    }
}

internal fun loadDeviceGalleryMedia(context: Context): List<GalleryMedia> {
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
            while (c.moveToNext() && count < 200) {
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
            while (c.moveToNext() && count < 100) {
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
