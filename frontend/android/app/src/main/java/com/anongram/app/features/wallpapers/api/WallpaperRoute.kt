package com.anongram.app.features.wallpapers.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.features.wallpapers.logic.ThemePickerViewModel
import com.anongram.app.features.wallpapers.ui.ThemePickerSheet

/**
 * Публичная точка входа на экраны модуля обоев.
 * Вызывается из меню чата по нажатию «Изменить обои».
 *
 * @param chatId Идентификатор сессии текущего чата.
 * @param onDismiss Коллбэк закрытия окна обоев.
 * @param onNavigateToPhotos Коллбэк перехода к выбору фото из галереи.
 */
@Composable
fun WallpaperRoute(
    chatId: String,
    onDismiss: () -> Unit,
    onNavigateToPhotos: () -> Unit = {},
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { WallpaperModule.getRepository(context) }
    val viewModel = remember(chatId, repository) {
        ThemePickerViewModel(chatId = chatId, repository = repository)
    }

    ThemePickerSheet(
        viewModel = viewModel,
        onDismiss = onDismiss,
        onNavigateToPhotos = onNavigateToPhotos,
        modifier = modifier
    )
}
