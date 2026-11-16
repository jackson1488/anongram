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
import androidx.compose.foundation.shape.RoundedCornerShape
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
                    while (maxOf(opts.outWidth, opts.outHeight) / sample > 320) sample *= 2
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
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
