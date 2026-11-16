package com.anongram.app.features.chats.links.preview

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.links.browser.InAppBrowserI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val thumbCache = LruCache<String, ImageBitmap>(48)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LinkPreviewCard(
    preview: LinkPreviewData,
    onClick: () -> Unit,
    onImageClick: (LinkPreviewData) -> Unit = {},
    onExpandVideo: (LinkPreviewData) -> Unit = { onClick() },
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = InAppBrowserI18n.current

    val thumb by produceState<ImageBitmap?>(initialValue = thumbCache.get(preview.imageUrl), preview.imageUrl) {
        if (preview.imageUrl.isBlank()) {
            value = null
            return@produceState
        }
        thumbCache.get(preview.imageUrl)?.let {
            value = it
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(preview.imageUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4500
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                }
                try {
                    if (conn.responseCode !in 200..299) return@runCatching null
                    val bytes = conn.inputStream.use { it.readBytes() }
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    var sample = 1
                    while (maxOf(opts.outWidth, opts.outHeight) / sample > 720) sample *= 2
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)?.asImageBitmap()?.also { bmp ->
                        thumbCache.put(preview.imageUrl, bmp)
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }
    }

    if (preview.mediaType == LinkMediaType.VIDEO) {
        var isInlinePlaying by remember(preview.url) { mutableStateOf(false) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.accent.copy(alpha = 0.11f))
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(scheme.surface)
            ) {
                if (isInlinePlaying) {
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
                            modifier = Modifier.fillMaxSize()
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
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(scheme.background.copy(alpha = 0.72f))
                                .clickable {
                                    isInlinePlaying = false
                                    onExpandVideo(preview)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fullscreen,
                                contentDescription = strings.watchVideoLabel,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(scheme.background.copy(alpha = 0.72f))
                                .clickable { isInlinePlaying = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = strings.closeBrowser,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    if (thumb != null) {
                        Image(
                            bitmap = thumb!!,
                            contentDescription = preview.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isInlinePlaying = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(scheme.background.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = strings.watchVideoLabel,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (preview.title.isNotBlank()) {
                    Text(
                        text = preview.title,
                        color = scheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (preview.description.isNotBlank()) {
                    Text(
                        text = preview.description,
                        color = scheme.textSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val domainLabel = preview.domain.ifBlank { preview.siteName }
                if (domainLabel.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = domainLabel,
                            color = scheme.textSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        return
    }

    // Обычная ссылка (как в Telegram) или прямая картинка: полоска акцента слева + текст + большая картинка внизу
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.accent.copy(alpha = 0.11f))
            .clickable {
                if (preview.mediaType == LinkMediaType.IMAGE) {
                    onImageClick(preview)
                } else {
                    onClick()
                }
            }
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(scheme.accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val siteLabel = preview.siteName.ifBlank { preview.domain }
            if (siteLabel.isNotBlank()) {
                Text(
                    text = siteLabel,
                    color = scheme.accent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (preview.title.isNotBlank()) {
                Text(
                    text = preview.title,
                    color = scheme.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (preview.description.isNotBlank()) {
                Text(
                    text = preview.description,
                    color = scheme.textSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            thumb?.let { bmp ->
                Spacer(modifier = Modifier.height(3.dp))
                Image(
                    bitmap = bmp,
                    contentDescription = preview.title.ifBlank { strings.viewPhotoLabel },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp, max = 185.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.surface)
                        .clickable { onImageClick(preview) }
                )
            }
        }
    }
}
