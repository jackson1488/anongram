package com.anongram.app.features.chats.links.preview

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.links.browser.InAppBrowserI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val thumbCache = LruCache<String, ImageBitmap>(48)

@Composable
fun LinkPreviewCard(
    preview: LinkPreviewData,
    onClick: () -> Unit,
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
                    val targetMax = if (preview.mediaType == LinkMediaType.NONE) 320 else 720
                    while (maxOf(opts.outWidth, opts.outHeight) / sample > targetMax) sample *= 2
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.accent.copy(alpha = 0.11f))
            .clickable(onClick = onClick)
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
                .padding(vertical = 6.dp),
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
            if (preview.description.isNotBlank() && preview.mediaType == LinkMediaType.NONE) {
                Text(
                    text = preview.description,
                    color = scheme.textSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (preview.mediaType != LinkMediaType.NONE && thumb != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.surface)
                ) {
                    Image(
                        bitmap = thumb!!,
                        contentDescription = preview.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    if (preview.mediaType == LinkMediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(scheme.background.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = strings.watchVideoLabel,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            if (preview.mediaType == LinkMediaType.VIDEO) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.accent.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = strings.watchVideoLabel,
                        color = scheme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            } else if (preview.mediaType == LinkMediaType.IMAGE) {
                Text(
                    text = strings.viewPhotoLabel,
                    color = scheme.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
        if (preview.mediaType == LinkMediaType.NONE) {
            thumb?.let { bmp ->
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    bitmap = bmp,
                    contentDescription = preview.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.surface)
                )
            }
        }
    }
}
