package com.anongram.app.features.chats.media.common

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Быстрая нативная обрезка MP4 (видео-кружки) и M4A (голосовые) без перекодирования
 * через MediaExtractor + MediaMuxer.
 */
object MediaTrimmer {

    suspend fun trimMediaFile(
        context: Context,
        inputPath: String,
        startFraction: Float,
        endFraction: Float,
        isVideo: Boolean
    ): File? = withContext(Dispatchers.IO) {
        val srcFile = File(inputPath)
        if (!srcFile.exists() || srcFile.length() == 0L) return@withContext null

        val safeStart = startFraction.coerceIn(0f, 0.95f)
        val safeEnd = endFraction.coerceIn(safeStart + 0.05f, 1f)
        if (safeStart <= 0.01f && safeEnd >= 0.99f) {
            return@withContext srcFile
        }

        val durationMs = readDurationMs(srcFile)
        if (durationMs <= 300L) return@withContext srcFile

        val startUs = (safeStart * durationMs * 1000L).toLong().coerceAtLeast(0L)
        val endUs = (safeEnd * durationMs * 1000L).toLong().coerceAtLeast(startUs + 250_000L)

        val ext = if (isVideo) "mp4" else "m4a"
        val prefix = if (isVideo) "video_trim_" else "voice_trim_"
        val outFile = File(context.cacheDir, "$prefix${System.currentTimeMillis()}.$ext")

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply { setDataSource(srcFile.absolutePath) }
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            if (isVideo) {
                val rotation = readVideoRotation(srcFile)
                muxer.setOrientationHint(rotation)
            }

            val trackIndexMap = mutableMapOf<Int, Int>()
            var maxBufferSize = 256 * 1024

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                val keep = mime.startsWith("audio/") || (isVideo && mime.startsWith("video/"))
                if (keep) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    trackIndexMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        maxBufferSize = maxOf(maxBufferSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                    }
                }
            }

            if (trackIndexMap.isEmpty()) {
                outFile.delete()
                return@withContext srcFile
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            var basePtsUs = -1L

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0L || sampleTimeUs > endUs) {
                    break
                }
                val trackIdx = extractor.sampleTrackIndex
                val dstTrackIdx = trackIndexMap[trackIdx]
                if (dstTrackIdx != null) {
                    if (basePtsUs < 0L) {
                        basePtsUs = sampleTimeUs
                    }
                    bufferInfo.presentationTimeUs = (sampleTimeUs - basePtsUs).coerceAtLeast(0L)
                    val extFlags = extractor.sampleFlags
                    bufferInfo.flags = if ((extFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else 0
                    muxer.writeSampleData(dstTrackIdx, buffer, bufferInfo)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()
            extractor = null

            if (outFile.exists() && outFile.length() > 0L) outFile else srcFile
        } catch (_: Exception) {
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
            runCatching { outFile.delete() }
            srcFile
        }
    }

    fun readDurationMs(file: File): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { mmr.release() }
        }
    }

    private fun readVideoRotation(file: File): Int {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } catch (_: Exception) {
            0
        } finally {
            runCatching { mmr.release() }
        }
    }
}
