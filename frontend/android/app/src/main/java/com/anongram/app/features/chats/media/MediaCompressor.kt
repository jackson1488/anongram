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
import com.anongram.app.core.crypto.NativeCryptoBridge
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Системный компрессор медиа из галереи перед отправкой:
 * - Фото (Стандарт): до 1280px, JPEG 80%, зачистка EXIF/GPS через Rust [NativeCryptoBridge].
 * - Фото (HD): до 2560px, JPEG 90%, зачистка EXIF/GPS через Rust [NativeCryptoBridge].
 * - Видео: системный ремукс/сжатие без метаданных (ориентация сохраняется, треки метаданных удаляются).
 * - Файлы (`AttachmentDraft.File`, режим «Без сжатия»): остаются 1:1 без изменений.
 */
internal object MediaCompressor {

    suspend fun compressDraftsIfNeeded(
        context: Context,
        drafts: List<AttachmentDraft>
    ): List<AttachmentDraft> = withContext(Dispatchers.IO) {
        drafts.map { draft ->
            when (draft) {
                is AttachmentDraft.Media -> {
                    if (draft.isVideo) {
                        val outUri = compressOrStripVideo(context, draft.uri, hd = draft.hd)
                        draft.copy(uri = outUri)
                    } else {
                        val outUri = compressPhoto(context, draft.uri, hd = draft.hd)
                        draft.copy(uri = outUri)
                    }
                }
                else -> draft
            }
        }
    }

    private suspend fun compressPhoto(
        context: Context,
        sourceUri: Uri,
        hd: Boolean
    ): Uri {
        val maxSide = if (hd) 2560 else 1280
        val quality = if (hd) 90 else 80

        return try {
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

            val rotated = applyExifOrientation(decoded, orientation)
            if (rotated !== decoded) decoded.recycle()

            val scaled = scaleDownIfNeeded(rotated, maxSide)
            if (scaled !== rotated) rotated.recycle()

            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            scaled.recycle()

            val rawJpeg = baos.toByteArray()
            val cleanJpeg = NativeCryptoBridge.stripImageMetadata(rawJpeg)

            val outFile = File(context.cacheDir, "send_img_${UUID.randomUUID()}.jpg")
            outFile.writeBytes(cleanJpeg)
            Uri.fromFile(outFile)
        } catch (_: Exception) {
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

    /**
     * Системный ремукс видео через [MediaExtractor] + [MediaMuxer]:
     * удаляет GPS, дату съёмки и сторонние мета-дорожки, сохраняя видео/аудио и ориентацию.
     * Для HD (`hd == true`) и обычного режима создаёт чистый локальный MP4-файл в кэше.
     */
    private fun compressOrStripVideo(
        context: Context,
        sourceUri: Uri,
        @Suppress("UNUSED_PARAMETER") hd: Boolean
    ): Uri {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, sourceUri, null)
            val outFile = File(context.cacheDir, "send_vid_${UUID.randomUUID()}.mp4")
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val retriever = MediaMetadataRetriever()
            val rotation = try {
                retriever.setDataSource(context, sourceUri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            } catch (_: Exception) {
                0
            } finally {
                runCatching { retriever.release() }
            }
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
            while (true) {
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
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            Uri.fromFile(outFile)
        } catch (_: Exception) {
            runCatching { muxer?.release() }
            sourceUri
        } finally {
            runCatching { extractor.release() }
        }
    }
}
