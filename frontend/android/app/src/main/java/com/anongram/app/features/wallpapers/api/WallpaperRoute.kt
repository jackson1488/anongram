package com.anongram.app.features.wallpapers.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.features.wallpapers.logic.EditorViewModel
import com.anongram.app.features.wallpapers.logic.PhotoPickerViewModel
import com.anongram.app.features.wallpapers.logic.ThemePickerViewModel
import com.anongram.app.features.wallpapers.ui.PhotoPickerScreen
import com.anongram.app.features.wallpapers.ui.ThemePickerSheet
import com.anongram.app.features.wallpapers.ui.WallpaperEditorScreen

/**
 * Внутренние экраны навигации модуля обоев.
 */
private enum class WallpaperScreen {
    THEME_PICKER,
    PHOTO_PICKER,
    EDITOR
}

/**
 * Публичная точка входа на экраны модуля обоев.
 * Вызывается из меню чата по нажатию «Изменить обои».
 *
 * @param chatId Идентификатор сессии текущего чата.
 * @param onDismiss Коллбэк закрытия окна обоев.
 */
@Composable
fun WallpaperRoute(
    chatId: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { WallpaperModule.getRepository(context) }
    val storage = remember(context) { WallpaperModule.getStorage(context) }

    var currentScreen by remember { mutableStateOf(WallpaperScreen.THEME_PICKER) }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }

    val themeViewModel = remember(chatId, repository) {
        ThemePickerViewModel(chatId = chatId, repository = repository)
    }

    val photoViewModel = remember(chatId, repository, storage) {
        PhotoPickerViewModel(chatId = chatId, repository = repository, storage = storage)
    }

    when (currentScreen) {
        WallpaperScreen.THEME_PICKER -> {
            Box(
                modifier = modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            themeViewModel.onDismiss()
                            onDismiss()
                        }
                )
                ThemePickerSheet(
                    viewModel = themeViewModel,
                    onDismiss = onDismiss,
                    onNavigateToPhotos = {
                        currentScreen = WallpaperScreen.PHOTO_PICKER
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        WallpaperScreen.PHOTO_PICKER -> {
            PhotoPickerScreen(
                viewModel = photoViewModel,
                onBack = {
                    currentScreen = WallpaperScreen.THEME_PICKER
                },
                onPhotoSelected = { path ->
                    selectedPhotoPath = path
                    currentScreen = WallpaperScreen.EDITOR
                },
                modifier = modifier
            )
        }

        WallpaperScreen.EDITOR -> {
            val editorViewModel = remember(chatId, selectedPhotoPath) {
                EditorViewModel(
                    chatId = chatId,
                    repository = repository,
                    initialPhotoPath = selectedPhotoPath
                )
            }

            WallpaperEditorScreen(
                chatId = chatId,
                viewModel = editorViewModel,
                onBack = {
                    currentScreen = WallpaperScreen.PHOTO_PICKER
                },
                onChangePhoto = {
                    currentScreen = WallpaperScreen.PHOTO_PICKER
                },
                onApplied = onDismiss,
                modifier = modifier
            )
        }
    }
}
