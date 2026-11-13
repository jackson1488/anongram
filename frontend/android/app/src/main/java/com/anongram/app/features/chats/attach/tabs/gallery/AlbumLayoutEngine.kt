package com.anongram.app.features.chats.attach.tabs.gallery

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val mediaSizeCache = LruCache<String, Pair<Int, Int>>(256)

internal suspend fun resolveMediaDimensions(
    context: Context,
    uriString: String,
    isVideo: Boolean
): Pair<Int, Int>? {
    mediaSizeCache.get(uriString)?.let { return it }
    return withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext null
        val dims: Pair<Int, Int>? = if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    if (rot == 90 || rot == 270) h to w else w to h
                } else null
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        } else {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                var w = opts.outWidth
                var h = opts.outHeight
                if (w > 0 && h > 0) {
                    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                        orientation == ExifInterface.ORIENTATION_TRANSVERSE
                    ) {
                        val tmp = w
                        w = h
                        h = tmp
                    }
                    w to h
                } else null
            } catch (_: Exception) {
                null
            }
        }
        if (dims != null) {
            mediaSizeCache.put(uriString, dims)
        }
        dims
    }
}

@Composable
internal fun rememberMediaAspects(items: List<Pair<String, Boolean>>): List<Float> {
    val context = LocalContext.current
    val aspects by produceState<List<Float>>(
        initialValue = items.map { (uri, _) ->
            mediaSizeCache.get(uri)?.let { it.first.toFloat() / it.second.toFloat() } ?: 0.8f
        },
        key1 = items
    ) {
        value = items.map { (uri, isVideo) ->
            val dims = resolveMediaDimensions(context, uri, isVideo)
            if (dims != null) dims.first.toFloat() / dims.second.toFloat() else 0.8f
        }
    }
    return aspects
}

internal data class AlbumRowPlan(
    val indices: List<Int>,
    val weights: List<Float>,
    val heightDp: Float
)

internal sealed interface AlbumLayoutPlan {
    data object TwoVertical : AlbumLayoutPlan
    data class TwoStacked(val topHeightDp: Float, val bottomHeightDp: Float) : AlbumLayoutPlan
    data object ThreeLeftTall : AlbumLayoutPlan
    data class ThreeTopWide(
        val topHeightDp: Float,
        val bottomHeightDp: Float,
        val bottomWeights: List<Float>
    ) : AlbumLayoutPlan
    data object FourLeftTall : AlbumLayoutPlan
    data class FourTopWide(
        val topHeightDp: Float,
        val bottomHeightDp: Float,
        val bottomWeights: List<Float>
    ) : AlbumLayoutPlan
    data class MultiRow(val rows: List<AlbumRowPlan>) : AlbumLayoutPlan
}

/**
 * Единый Telegram-подобный движок раскладки альбомов (2–10 медиа) для предпросмотра и чата:
 * - Вертикальные альбомы (2, 3, 4) сохраняют привычный вертикальный макет.
 * - При наличии широких/горизонтальных фото (скрин паспорта, кроп и т.д.) строит адаптивную сетку:
 *   - 2: одно над другим
 *   - 3: 1 большое сверху + 2 поменьше внизу
 *   - 4: 1 большое сверху + 3 внизу в ряд
 *   - 5+: 2 сверху + 3 снизу и пропорциональные строки по реальным aspectRatio.
 */
internal fun computeAlbumLayoutPlan(
    aspects: List<Float>,
    targetWidthDp: Float = 340f
): AlbumLayoutPlan {
    val count = aspects.size
    if (count == 0) return AlbumLayoutPlan.MultiRow(emptyList())

    val wideIndices = (0 until count).filter { aspects[it] >= 1.1f }
    val tallIndices = (0 until count).filter { aspects[it] < 1.1f }

    // Смешанный альбом (горизонтальные видео/фото + вертикальные фото/видео):
    // горизонтальные поднимаем вверх (чуть выше и шире), вертикальные выстраиваем сеткой снизу.
    if (wideIndices.isNotEmpty() && tallIndices.isNotEmpty()) {
        val rows = mutableListOf<AlbumRowPlan>()
        if (wideIndices.size == 1) {
            val wIdx = wideIndices[0]
            val topH = (targetWidthDp / aspects[wIdx].coerceIn(1.1f, 2.2f)).coerceIn(150f, 220f)
            rows.add(AlbumRowPlan(listOf(wIdx), listOf(1f), topH))
        } else {
            // 2+ горизонтальных: по 1–2 в верхних широких рядах (для 2+2 — 2 широких сверху, 2 вертикальных снизу)
            val topChunks = if (wideIndices.size == 2 && tallIndices.size <= 2) {
                listOf(wideIndices)
            } else {
                wideIndices.chunked(2)
            }
            for (chunk in topChunks) {
                val weights = chunk.map { idx -> aspects[idx].coerceIn(1.1f, 2.2f) }
                val h = if (chunk.size == 1) {
                    (targetWidthDp / weights[0]).coerceIn(145f, 215f)
                } else {
                    (targetWidthDp / weights.sum()).coerceIn(120f, 175f)
                }
                rows.add(AlbumRowPlan(chunk, weights, h))
            }
        }

        val bottomChunks = when {
            tallIndices.size <= 3 -> listOf(tallIndices)
            tallIndices.size == 4 -> tallIndices.chunked(2)
            else -> tallIndices.chunked(3)
        }
        for (chunk in bottomChunks) {
            val weights = chunk.map { idx -> aspects[idx].coerceIn(0.55f, 1.3f) }
            val h = if (chunk.size == 1) {
                (targetWidthDp / weights[0].coerceAtLeast(0.75f)).coerceIn(150f, 230f)
            } else {
                (targetWidthDp / weights.sum().coerceAtLeast(1f)).coerceIn(130f, 200f)
            }
            rows.add(AlbumRowPlan(chunk, weights, h))
        }
        return AlbumLayoutPlan.MultiRow(rows)
    }

    val allVertical = tallIndices.size == count
    val firstIsWide = aspects.firstOrNull()?.let { it >= 1.1f } == true

    return when (count) {
        2 -> {
            if (allVertical) {
                AlbumLayoutPlan.TwoVertical
            } else {
                val h0 = (targetWidthDp / aspects[0].coerceIn(0.85f, 2.5f)).coerceIn(135f, 235f)
                val h1 = (targetWidthDp / aspects[1].coerceIn(0.85f, 2.5f)).coerceIn(135f, 235f)
                AlbumLayoutPlan.TwoStacked(h0, h1)
            }
        }
        3 -> {
            if (!allVertical && firstIsWide) {
                val topH = (targetWidthDp / aspects[0].coerceIn(1.1f, 2.5f)).coerceIn(120f, 195f)
                val w1 = aspects[1].coerceIn(0.6f, 1.85f)
                val w2 = aspects[2].coerceIn(0.6f, 1.85f)
                val bottomH = (targetWidthDp / (w1 + w2)).coerceIn(110f, 175f)
                AlbumLayoutPlan.ThreeTopWide(topH, bottomH, listOf(w1, w2))
            } else {
                AlbumLayoutPlan.ThreeLeftTall
            }
        }
        4 -> {
            if (!allVertical && firstIsWide) {
                val topH = (targetWidthDp / aspects[0].coerceIn(1.1f, 2.5f)).coerceIn(125f, 200f)
                val weights = listOf(
                    aspects[1].coerceIn(0.55f, 1.7f),
                    aspects[2].coerceIn(0.55f, 1.7f),
                    aspects[3].coerceIn(0.55f, 1.7f)
                )
                val bottomH = (targetWidthDp / weights.sum()).coerceIn(105f, 170f)
                AlbumLayoutPlan.FourTopWide(topH, bottomH, weights)
            } else {
                AlbumLayoutPlan.FourLeftTall
            }
        }
        else -> {
            val rowCounts = calculateAlbumRows(count)
            val rows = mutableListOf<AlbumRowPlan>()
            var pointer = 0
            for (c in rowCounts) {
                val end = (pointer + c).coerceAtMost(count)
                val indices = (pointer until end).toList()
                val weights = indices.map { idx -> aspects[idx].coerceIn(0.55f, 1.85f) }
                val sumW = weights.sum().coerceAtLeast(1f)
                val h = (targetWidthDp / sumW).coerceIn(105f, 205f)
                rows.add(AlbumRowPlan(indices, weights, h))
                pointer = end
            }
            AlbumLayoutPlan.MultiRow(rows)
        }
    }
}
