package com.anongram.app.features.chats.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import com.anongram.app.core.crypto.NativeCryptoBridge
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Системный компрессор медиа из галереи перед отправкой:
 * - Фото (Стандарт): до 1280px, JPEG 80%, зачистка EXIF/GPS через Rust [NativeCryptoBridge].
 * - Фото (HD): до 2560px, JPEG 90%, зачистка EXIF/GPS через Rust [NativeCryptoBridge].
 * - Видео (Smart Skip): лёгкие видео (<=720p/3Mbps или HD <=1080p/6Mbps) быстро ремуксятся без перекодирования.
 * - Видео (GPU Transcode): тяжёлые видео аппаратно сжимаются через [HardwareVideoTranscoder] (720p / 1080p HD).
 * - Файлы (`AttachmentDraft.File`, режим «Без сжатия»): остаются 1:1 без изменений.
 */
internal object MediaCompressor {

    fun queryMediaSizeBytes(context: Context, uri: Uri): Long {
        if (uri.scheme == "file") {
            val f = uri.path?.let { File(it) }
            if (f != null && f.exists()) return f.length()
        }
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) c.getLong(idx) else 0L
                } else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    suspend fun compressDraftsIfNeeded(
        context: Context,
        drafts: List<AttachmentDraft>
    ): List<AttachmentDraft> = withContext(Dispatchers.IO) {
        coroutineScope {
            drafts.map { draft ->
                async {
                    when (draft) {
                        is AttachmentDraft.Media -> {
                            val outUri = compressSingleMedia(
                                context = context,
                                sourceUri = draft.uri,
                                isVideo = draft.isVideo,
                                hd = draft.hd
                            )
                            draft.copy(uri = outUri)
                        }
                        else -> draft
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun compressSingleMedia(
        context: Context,
        sourceUri: Uri,
        isVideo: Boolean,
        hd: Boolean,
        onTempFileCreated: (File) -> Unit = {},
        onProgress: (Float) -> Unit = {}
    ): Uri = withContext(Dispatchers.IO) {
        if (isVideo) {
            compressOrStripVideo(context, sourceUri, hd, onTempFileCreated, onProgress)
        } else {
            compressPhoto(context, sourceUri, hd, onTempFileCreated, onProgress)
        }
    }

    private suspend fun compressPhoto(
        context: Context,
        sourceUri: Uri,
        hd: Boolean,
        onTempFileCreated: (File) -> Unit,
        onProgress: (Float) -> Unit
    ): Uri {
        val maxSide = if (hd) 2560 else 1280
        val quality = if (hd) 90 else 80

        return try {
            onProgress(0.15f)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return sourceUri

            val orientation = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            var sampleSize = 1
            val longest = max(srcW, srcH)
            while (longest / (sampleSize * 2) >= maxSide) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return sourceUri

            onProgress(0.45f)
            val rotated = applyExifOrientation(decoded, orientation)
            if (rotated !== decoded) decoded.recycle()

            val scaled = scaleDownIfNeeded(rotated, maxSide)
            if (scaled !== rotated) rotated.recycle()

            onProgress(0.70f)
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            scaled.recycle()

            val rawJpeg = baos.toByteArray()
            val cleanJpeg = NativeCryptoBridge.stripImageMetadata(rawJpeg)

            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Photo compression cancelled")
            }

            val outFile = File(context.cacheDir, "send_img_${UUID.randomUUID()}.jpg")
            onTempFileCreated(outFile)
            outFile.writeBytes(cleanJpeg)
            onProgress(1f)
            Uri.fromFile(outFile)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            onProgress(1f)
            sourceUri
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxSide: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = max(w, h)
        if (longest <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / longest.toFloat()
        val dstW = (w * ratio).roundToInt().coerceAtLeast(1)
        val dstH = (h * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, dstW, dstH, true)
    }

    private suspend fun compressOrStripVideo(
        context: Context,
        sourceUri: Uri,
        hd: Boolean,
        onTempFileCreated: (File) -> Unit,
        onProgress: (Float) -> Unit
    ): Uri {
        val targetMaxSide = if (hd) 1920 else 1280
        val targetBitrate = if (hd) 5_000_000 else 2_200_000
        val skipBitrateThreshold = if (hd) 6_000_000 else 3_000_000

        var width = 0
        var height = 0
        var rotation = 0
        var durationMs = 0L
        var bitrate = 0
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, sourceUri)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        val longest = max(width, height)
        val durationUs = (durationMs * 1000L).coerceAtLeast(1L)

        // Smart Skip: если видео уже лёгкое (укладывается в целевое разрешение и битрейт), делаем быстрый ремукс без перекодирования
        val canSmartSkip = longest in 1..targetMaxSide && bitrate in 1..skipBitrateThreshold
        if (!canSmartSkip && width > 0 && height > 0) {
            val scale = targetMaxSide.toFloat() / longest.toFloat().coerceAtLeast(1f)
            val dstW = if (scale < 1f) (width * scale).roundToInt() else width
            val dstH = if (scale < 1f) (height * scale).roundToInt() else height
            val outFile = File(context.cacheDir, "send_vid_${UUID.randomUUID()}.mp4")
            onTempFileCreated(outFile)
            val ok = HardwareVideoTranscoder.transcode(
                context = context,
                sourceUri = sourceUri,
                outFile = outFile,
                targetWidth = dstW,
                targetHeight = dstH,
                targetBitrate = targetBitrate,
                rotationHint = rotation,
                durationUs = durationUs,
                onProgress = onProgress
            )
            if (ok && outFile.exists() && outFile.length() > 0L) {
                return Uri.fromFile(outFile)
            }
            runCatching { if (outFile.exists()) outFile.delete() }
        }

        return remuxStripMetadata(
            context = context,
            sourceUri = sourceUri,
            rotation = rotation,
            durationUs = durationUs,
            onTempFileCreated = onTempFileCreated,
            onProgress = onProgress
        )
    }

    private suspend fun remuxStripMetadata(
        context: Context,
        sourceUri: Uri,
        rotation: Int,
        durationUs: Long,
        onTempFileCreated: (File) -> Unit,
        onProgress: (Float) -> Unit
    ): Uri {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        val outFile = File(context.cacheDir, "send_vid_${UUID.randomUUID()}.mp4")
        return try {
            onTempFileCreated(outFile)
            extractor.setDataSource(context, sourceUri, null)
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotation)

            val indexMap = HashMap<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    indexMap[i] = muxer.addTrack(format)
                }
            }

            if (indexMap.isEmpty()) {
                muxer.release()
                outFile.delete()
                return sourceUri
            }

            muxer.start()
            val buffer = ByteBuffer.allocate(512 * 1024)
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            while (currentCoroutineContext().isActive) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                val trackIdx = extractor.sampleTrackIndex
                val muxTrack = indexMap[trackIdx]
                if (muxTrack != null) {
                    muxer.writeSampleData(muxTrack, buffer, bufferInfo)
                }
                onProgress((bufferInfo.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 0.99f))
                extractor.advance()
            }
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Remux cancelled")
            }
            muxer.stop()
            muxer.release()
            onProgress(1f)
            Uri.fromFile(outFile)
        } catch (ce: CancellationException) {
            runCatching { muxer?.release() }
            runCatching { if (outFile.exists()) outFile.delete() }
            throw ce
        } catch (_: Exception) {
            runCatching { muxer?.release() }
            runCatching { if (outFile.exists()) outFile.delete() }
            onProgress(1f)
            sourceUri
        } finally {
            runCatching { extractor.release() }
        }
    }
}
