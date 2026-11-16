package com.anongram.app.features.chats.links.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Корневой контейнер встроенного браузера.
 * Оборачивает весь интерфейс приложения:
 * - Когда браузер свёрнут (isMinimized == true), нижний бар автоматически сдвигает вверх
 *   любой активный экран (чаты, поле ввода, нижнюю навигацию, настройки) без правок в этих экранах.
 * - Когда браузер развёрнут — перекрывает экран, сохраняя живой WebView без перезагрузки.
 */
@Composable
fun InAppBrowserHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        InAppBrowserStore.ensureInitialized(context)
    }

    val state by InAppBrowserStore.state.collectAsState()
    val strings = InAppBrowserI18n.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }

            if (state.activeUrl != null && state.isMinimized) {
                InAppBrowserMinimizedBar(
                    state = state,
                    onExpand = { InAppBrowserStore.expand() },
                    onClose = { InAppBrowserStore.close() }
                )
            }
        }

        if (state.activeUrl != null) {
            InAppBrowserOverlay(state = state)
        }

        if (state.isHistoryOpen) {
            BrowserEntriesSheet(
                title = strings.history,
                emptyText = strings.emptyHistory,
                entries = state.history,
                onSelect = { url ->
                    InAppBrowserStore.closeHistory()
                    InAppBrowserStore.closeSettings()
                    InAppBrowserStore.openUrl(context, url)
                },
                onClearAll = { InAppBrowserStore.clearHistory(context) },
                onBack = { InAppBrowserStore.closeHistory() }
            )
        }

        if (state.isBookmarksOpen) {
            BrowserEntriesSheet(
                title = strings.bookmarks,
                emptyText = strings.emptyBookmarks,
                entries = state.bookmarks,
                onSelect = { url ->
                    InAppBrowserStore.closeBookmarks()
                    InAppBrowserStore.openUrl(context, url)
                },
                onDelete = { url -> InAppBrowserStore.removeBookmark(context, url) },
                onBack = { InAppBrowserStore.closeBookmarks() }
            )
        }

        if (state.isSettingsOpen) {
            InAppBrowserSettingsScreen(
                onBack = { InAppBrowserStore.closeSettings() }
            )
        }
    }
}
