package com.anongram.app.features.chats.links.browser

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.chats.links.preview.LinkMediaType
import com.anongram.app.features.chats.links.preview.LinkPreviewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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

        if (state.isSettingsOpen) {
            InAppBrowserSettingsScreen(
                onBack = { InAppBrowserStore.closeSettings() }
            )
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

        state.inlineMediaPreview?.let { mediaPreview ->
            InlineLinkMediaViewerOverlay(
                preview = mediaPreview,
                onClose = { InAppBrowserStore.closeInlineMedia() },
                onOpenInBrowser = {
                    InAppBrowserStore.closeInlineMedia()
                    InAppBrowserStore.openUrl(context, mediaPreview.url)
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InlineLinkMediaViewerOverlay(
    preview: LinkPreviewData,
    onClose: () -> Unit,
    onOpenInBrowser: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = InAppBrowserI18n.current

    BackHandler { onClose() }

    AnonGramScreenSurface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(scheme.background)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = strings.closeBrowser,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preview.title.ifBlank { preview.domain },
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (preview.domain.isNotBlank()) {
                        Text(
                            text = preview.domain,
                            color = scheme.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .clickable(onClick = onOpenInBrowser)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.openInBrowserLabel,
                        color = scheme.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(scheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (preview.mediaType == LinkMediaType.IMAGE) {
                    val imgUrl = preview.imageUrl.ifBlank { preview.url }
                    val bitmap by produceState<ImageBitmap?>(initialValue = null, imgUrl) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                val conn = (URL(imgUrl).openConnection() as HttpURLConnection).apply {
                                    connectTimeout = 5000
                                    readTimeout = 6000
                                    instanceFollowRedirects = true
                                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                                }
                                try {
                                    if (conn.responseCode !in 200..299) return@runCatching null
                                    val bytes = conn.inputStream.use { it.readBytes() }
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                } finally {
                                    conn.disconnect()
                                }
                            }.getOrNull()
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!,
                            contentDescription = preview.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        CircularProgressIndicator(color = scheme.accent)
                    }
                } else {
                    val targetVideo = preview.embedUrl.ifBlank { preview.url }
                    val isDirectVideo = targetVideo.substringBefore('?').lowercase().let { p ->
                        p.endsWith(".mp4") || p.endsWith(".webm") || p.endsWith(".mov") || p.endsWith(".m4v")
                    }
                    if (isDirectVideo) {
                        var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
                        DisposableEffect(targetVideo) {
                            onDispose {
                                runCatching { videoViewRef?.stopPlayback() }
                            }
                        }
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    videoViewRef = this
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoURI(Uri.parse(targetVideo))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                    } else {
                        var webViewRef by remember { mutableStateOf<WebView?>(null) }
                        DisposableEffect(targetVideo) {
                            onDispose {
                                runCatching {
                                    webViewRef?.stopLoading()
                                    webViewRef?.loadUrl("about:blank")
                                    webViewRef?.destroy()
                                }
                            }
                        }
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewRef = this
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    loadUrl(targetVideo)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                    }
                }
            }
        }
    }
}
