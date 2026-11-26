package com.anongram.app.features.profile.avatar

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.features.camera.api.CameraScreen
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.attach.i18n.AttachI18n
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.state.rememberAttachSheetDragState
import com.anongram.app.features.chats.attach.state.rememberAttachSheetState
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryAlbumsDropdownOverlay
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryFloatingTopBar
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryTab
import com.anongram.app.features.profile.appearance.ProfileColorPresets
import com.anongram.app.features.profile.i18n.ProfileI18n

/**
 * Выдвижная шторка выбора фотографии, видео или эмодзи для аватара профиля.
 * Полностью наследует физику и UI штатной шторки вложений ([rememberAttachSheetDragState],
 * [GalleryTab], [GalleryFloatingTopBar], [GalleryAlbumsDropdownOverlay] и [CameraScreen]).
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
    val appLanguage by LanguageSettingsStore.language.collectAsState()
    val attachStrings = remember(appLanguage) { AttachI18n.forLanguage(appLanguage) }
    val context = LocalContext.current

    val systemChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            val pickedUri = data?.data ?: data?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            if (pickedUri != null) {
                val mime = context.contentResolver.getType(pickedUri).orEmpty()
                val isVideo = mime.startsWith("video/")
                onPickMedia(pickedUri, isVideo, if (isVideo) MAX_PROFILE_VIDEO_MS else 0L)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalHeightPx = constraints.maxHeight.toFloat()
        val sheetState = rememberAttachSheetState(AttachTab.GALLERY)
        val dragState = rememberAttachSheetDragState(
            totalHeightPx = totalHeightPx,
            context = context,
            wasKeyboardOpen = false,
            onRestoreKeyboard = {},
            onDismiss = onBack,
            sheetState = sheetState
        )

        LaunchedEffect(totalHeightPx) {
            dragState.animateTo(dragState.openOffset, 300)
        }

        LaunchedEffect(dragState.offsetPx) {
            if (dragState.offsetPx > 10f && sheetState.isAlbumsSheetOpen) {
                sheetState.isAlbumsSheetOpen = false
            }
        }

        BackHandler {
            when {
                sheetState.isCameraOpen -> sheetState.isCameraOpen = false
                sheetState.isAlbumsSheetOpen -> sheetState.isAlbumsSheetOpen = false
                else -> dragState.requestDismiss()
            }
        }

        // Затемнение фона как в штатной шторке вложений
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = ((totalHeightPx - dragState.offsetPx) / totalHeightPx * 0.70f).coerceIn(0f, 0.70f)
                }
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { dragState.requestDismiss() }
        )

        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
            LocalAttachStrings provides attachStrings
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = dragState.offsetPx }
                    .nestedScroll(dragState.nestedScrollConnection)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(scheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* поглощаем клики внутри шторки */ }
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        GalleryTab(
                            onLaunchCamera = { sheetState.isCameraOpen = true },
                            onOpenEditor = { items ->
                                items.firstOrNull()?.let { item ->
                                    onPickMedia(item.uri, item.isVideo, item.durationMs)
                                }
                            },
                            selectedAlbum = sheetState.selectedAlbum,
                            onSelectAlbum = { sheetState.selectedAlbum = it },
                            isAlbumsSheetOpen = sheetState.isAlbumsSheetOpen,
                            onCloseAlbumsSheet = { sheetState.isAlbumsSheetOpen = false },
                            onOpenAlbumsSheet = { sheetState.isAlbumsSheetOpen = true },
                            selectedMedia = emptyList(),
                            onToggleSelectMedia = { media ->
                                sheetState.isAlbumsSheetOpen = false
                                onPickMedia(media.uri, media.isVideo, media.durationMs)
                            },
                            caption = "",
                            onCaptionChange = {},
                            onSend = {},
                            capturedMediaItems = sheetState.capturedMediaList,
                            isFullscreen = dragState.isFull,
                            onOpenViewer = { list, idx ->
                                sheetState.isAlbumsSheetOpen = false
                                list.getOrNull(idx)?.let { media ->
                                    onPickMedia(media.uri, media.isVideo, media.durationMs)
                                }
                            },
                            onAlbumsLoaded = { sheetState.availableAlbums = it },
                            showSelectionCircles = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Тонкая верхняя ручка перетаскивания шторки
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(18.dp)
                        .graphicsLayer {
                            alpha = ((dragState.offsetPx - totalHeightPx * 0.03f) / (totalHeightPx * 0.05f)).coerceIn(0f, 1f)
                        }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { d ->
                                dragState.offsetPx = (dragState.offsetPx + d).coerceIn(0f, totalHeightPx)
                            },
                            onDragStarted = { dragState.animJob?.cancel() },
                            onDragStopped = { v -> dragState.release(v) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(scheme.textMuted.copy(alpha = 0.35f))
                    )
                }

                // Плавающая шапка Галереи при раскрытии на 95%+
                if (dragState.isExpanded95) {
                    val topBarAlpha = ((totalHeightPx * 0.05f - dragState.offsetPx) / (totalHeightPx * 0.05f)).coerceIn(0f, 1f)
                    GalleryFloatingTopBar(
                        selectedAlbum = sheetState.selectedAlbum,
                        selectedMedia = emptyList(),
                        attachStrings = attachStrings,
                        topBarAlpha = topBarAlpha,
                        isAlbumsSheetOpen = sheetState.isAlbumsSheetOpen,
                        onToggleAlbumsSheet = { sheetState.isAlbumsSheetOpen = !sheetState.isAlbumsSheetOpen },
                        onBack = { dragState.requestDismiss() },
                        onOpenPreviewOrder = {},
                        onLaunchSystemChooser = { systemChooserLauncher.launch(it) },
                        onToggleSpoiler = {},
                        onSendHd = {},
                        showSendInHd = false,
                        hasActionMenu = false
                    )
                }
            }

            GalleryAlbumsDropdownOverlay(
                sheetState = sheetState,
                dragState = dragState
            )

            if (sheetState.isCameraOpen) {
                CameraScreen(
                    onResult = { captured ->
                        sheetState.isCameraOpen = false
                        onPickMedia(
                            captured.uri,
                            captured.isVideo,
                            if (captured.isVideo) MAX_PROFILE_VIDEO_MS else 0L
                        )
                    },
                    onClose = { sheetState.isCameraOpen = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                )
            }
        }
    }
}
