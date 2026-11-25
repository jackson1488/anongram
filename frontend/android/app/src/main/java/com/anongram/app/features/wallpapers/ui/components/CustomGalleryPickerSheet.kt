package com.anongram.app.features.wallpapers.ui.components

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.camera.api.CameraScreen
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.attach.i18n.AttachI18n
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.state.rememberAttachSheetDragState
import com.anongram.app.features.chats.attach.state.rememberAttachSheetState
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryAlbumsDropdownOverlay
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryFloatingTopBar
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryTab

/**
 * Шторка выбора фото для обоев на базе компонентов модуля вложений (`features/chats/attach`):
 * та же плавная физика открытия/перетаскивания, та же плавающая шапка (`GalleryFloatingTopBar`)
 * с учётом статус-бара, выбором альбомов и меню «⋮», но без нижней панели вкладок и без кружков
 * мультивыбора на фото — тап по фото или съёмка с камеры сразу передаёт Uri.
 */
@Composable
fun CustomGalleryPickerSheet(
    onPickUri: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
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
                onPickUri(pickedUri)
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val totalHeightPx = constraints.maxHeight.toFloat()
        val sheetState = rememberAttachSheetState(AttachTab.GALLERY)
        val dragState = rememberAttachSheetDragState(
            totalHeightPx = totalHeightPx,
            context = context,
            wasKeyboardOpen = false,
            onRestoreKeyboard = {},
            onDismiss = onDismiss,
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

        // Затемнение фона как в AttachSheetContent
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
                                items.firstOrNull()?.uri?.let(onPickUri)
                            },
                            selectedAlbum = sheetState.selectedAlbum,
                            onSelectAlbum = { sheetState.selectedAlbum = it },
                            isAlbumsSheetOpen = sheetState.isAlbumsSheetOpen,
                            onCloseAlbumsSheet = { sheetState.isAlbumsSheetOpen = false },
                            onOpenAlbumsSheet = { sheetState.isAlbumsSheetOpen = true },
                            selectedMedia = emptyList(),
                            onToggleSelectMedia = { media ->
                                sheetState.isAlbumsSheetOpen = false
                                onPickUri(media.uri)
                            },
                            caption = "",
                            onCaptionChange = {},
                            onSend = {},
                            capturedMediaItems = sheetState.capturedMediaList,
                            isFullscreen = dragState.isFull,
                            onOpenViewer = { list, idx ->
                                sheetState.isAlbumsSheetOpen = false
                                list.getOrNull(idx)?.uri?.let(onPickUri)
                            },
                            onAlbumsLoaded = { sheetState.availableAlbums = it },
                            showSelectionCircles = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Тонкая верхняя ручка перетаскивания
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
                            .width(24.dp)
                            .height(1.5.dp)
                            .clip(CircleShape)
                            .background(scheme.textMuted.copy(alpha = 0.25f))
                    )
                }

                // Штатная плавающая шапка Галереи (с учётом статус-бара и выбором альбомов, без ⋮)
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
                        if (!captured.isVideo) {
                            onPickUri(captured.uri)
                        }
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
