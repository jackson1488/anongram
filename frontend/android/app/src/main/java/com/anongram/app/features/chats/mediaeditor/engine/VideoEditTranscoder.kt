package com.anongram.app.features.chats.mediaeditor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import com.anongram.app.features.chats.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.chats.mediaeditor.model.BlurMode
import com.anongram.app.features.chats.mediaeditor.model.CurveChannel
import com.anongram.app.features.chats.mediaeditor.model.PhotoEditState
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Аппаратный GPU-транскодер видеоредактора (`MediaCodec` + `EGL14` + `OpenGL ES 2.0`).
 * Физически запекает в итоговый `.mp4` файл:
 * - обрезку по времени (`trimStartMs..trimEndMs`) и отключение звука (`isMuted`);
 * - кроп (`cropRect`), поворот на 90°/180°/270°, выравнивание (`straightenDegrees`) и зеркало (`mirrorHorizontal`);
 * - все 12 параметров цветокоррекции (`PhotoAdjustments`);
 * - тоновые кривые (`ALL`, `RED`, `GREEN`, `BLUE`) через 256×1 LUT-текстуру;
 * - радиальное / линейное размытие (`BlurEffectState` — «Капля»);
 * - прозрачный PNG-слой с рисунками кисти и текстом поверх всех кадров видео.
 */
internal object VideoEditTranscoder {

    data class TranscodeResult(
        val success: Boolean,
        val width: Int = 0,
        val height: Int = 0
    )

    fun transcode(
        context: Context,
        item: EditableMediaItem,
        outFile: File,
        overlayBitmap: Bitmap?
    ): TranscodeResult {
        val extractor = MediaExtractor()
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var videoFormat: MediaFormat? = null
        var audioFormat: MediaFormat? = null

        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var eglState: EditorEglInputSurface? = null
        var gpuRenderer: EditorOesGpuRenderer? = null

        return try {
            var rawW = 0
            var rawH = 0
            var origRotation = 0
            var totalDurationMs = item.durationMs
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, item.uri)
                rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                origRotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val durMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                if (durMeta > 0L) totalDurationMs = durMeta
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }

            extractor.setDataSource(context, item.uri, null)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (videoTrackIndex < 0 && mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = fmt
                    if (rawW <= 0 && fmt.containsKey(MediaFormat.KEY_WIDTH)) {
                        rawW = fmt.getInteger(MediaFormat.KEY_WIDTH)
                    }
                    if (rawH <= 0 && fmt.containsKey(MediaFormat.KEY_HEIGHT)) {
                        rawH = fmt.getInteger(MediaFormat.KEY_HEIGHT)
                    }
                } else if (audioTrackIndex < 0 && mime.startsWith("audio/") && !item.isMuted) {
                    audioTrackIndex = i
                    audioFormat = fmt
                }
            }
            if (videoTrackIndex < 0 || videoFormat == null || rawW <= 0 || rawH <= 0) {
                return TranscodeResult(false)
            }

            val editState = item.editState
            val crop = editState.crop
            val extraRot90 = if (crop.baseRotation90 != 0) crop.baseRotation90 else item.rotationDegrees
            val totalRot90 = ((origRotation + extraRot90) % 360 + 360) % 360
            val isSwapped = totalRot90 == 90 || totalRot90 == 270
            val uprightW = if (isSwapped) rawH else rawW
            val uprightH = if (isSwapped) rawW else rawH

            val croppedW = (uprightW * crop.cropRect.width).roundToInt().coerceAtLeast(32)
            val croppedH = (uprightH * crop.cropRect.height).roundToInt().coerceAtLeast(32)

            val maxTarget = item.videoQuality.maxHeight.coerceAtLeast(480)
            val longestOut = maxOf(croppedW, croppedH)
            val scale = if (longestOut > maxTarget) maxTarget.toFloat() / longestOut.toFloat() else 1f
            val outW = ((croppedW * scale).roundToInt() and 1.inv()).coerceAtLeast(32)
            val outH = ((croppedH * scale).roundToInt() and 1.inv()).coerceAtLeast(32)

            val targetBitrate = when {
                maxTarget >= 1080 -> 4_500_000
                maxTarget >= 720 -> 2_600_000
                else -> 1_400_000
            }

            val encFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outW, outH).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            eglState = EditorEglInputSurface(inputSurface)
            eglState.makeCurrent()

            val lutBitmap = buildCurveLutBitmap(editState)
            gpuRenderer = EditorOesGpuRenderer(
                editState = editState,
                totalRot90 = totalRot90,
                outW = outW,
                outH = outH,
                lutBitmap = lutBitmap,
                overlayBitmap = overlayBitmap
            )
            lutBitmap.recycle()

            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME) ?: return TranscodeResult(false)
            decoder = MediaCodec.createDecoderByType(videoMime)
            decoder.configure(videoFormat, gpuRenderer.surface, null, 0)
            decoder.start()

            // Orientation is physically baked into the upright outW x outH frames -> orientationHint = 0
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(0)

            val trimStartUs = item.trimStartMs.coerceAtLeast(0L) * 1000L
            val trimEndUs = if (item.trimEndMs > item.trimStartMs + 100L) {
                item.trimEndMs * 1000L
            } else if (totalDurationMs > 0L) {
                totalDurationMs * 1000L
            } else {
                Long.MAX_VALUE
            }

            extractor.selectTrack(videoTrackIndex)
            if (trimStartUs > 0L) {
                extractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            var muxVideoTrack = -1
            var muxAudioTrack = -1
            var muxerStarted = false

            var inputDone = false
            var decoderDone = false
            var encoderDone = false
            val decInfo = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10_000L

            while (!encoderDone) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)
                        val sampleSize = if (buf != null) extractor.readSampleData(buf, 0) else -1
                        val sampleTimeUs = extractor.sampleTime
                        if (sampleSize < 0 || sampleTimeUs > trimEndUs + 250_000L) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, sampleTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!decoderDone) {
                    val outIdx = decoder.dequeueOutputBuffer(decInfo, timeoutUs)
                    if (outIdx >= 0) {
                        val ptsUs = decInfo.presentationTimeUs
                        val isEos = (decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val inTrimRange = ptsUs >= trimStartUs && ptsUs <= trimEndUs
                        val doRender = decInfo.size > 0 && inTrimRange
                        decoder.releaseOutputBuffer(outIdx, doRender)
                        if (doRender) {
                            gpuRenderer.awaitAndDrawImage()
                            val shiftedPtsNs = (ptsUs - trimStartUs).coerceAtLeast(0L) * 1000L
                            eglState.setPresentationTime(shiftedPtsNs)
                            eglState.swapBuffers()
                        }
                        if (isEos || ptsUs > trimEndUs) {
                            decoderDone = true
                            encoder.signalEndOfInputStream()
                        }
                    }
                }

                while (true) {
                    val encIdx = encoder.dequeueOutputBuffer(encInfo, timeoutUs)
                    if (encIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (encIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) break
                        muxVideoTrack = muxer.addTrack(encoder.outputFormat)
                        if (audioTrackIndex >= 0 && audioFormat != null) {
                            muxAudioTrack = runCatching { muxer.addTrack(audioFormat) }.getOrDefault(-1)
                        }
                        muxer.start()
                        muxerStarted = true
                    } else if (encIdx >= 0) {
                        val encodedBuf = encoder.getOutputBuffer(encIdx)
                        if ((encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                            encInfo.size > 0 && muxerStarted && encodedBuf != null
                        ) {
                            encodedBuf.position(encInfo.offset)
                            encodedBuf.limit(encInfo.offset + encInfo.size)
                            muxer.writeSampleData(muxVideoTrack, encodedBuf, encInfo)
                        }
                        encoder.releaseOutputBuffer(encIdx, false)
                        if ((encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderDone = true
                            break
                        }
                    }
                }
            }

            // Copy audio track trimmed strictly to [trimStartUs .. trimEndUs]
            if (muxerStarted && muxAudioTrack >= 0 && audioTrackIndex >= 0) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                if (trimStartUs > 0L) {
                    extractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                } else {
                    extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                }
                val audioBuf = ByteBuffer.allocate(256 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                while (true) {
                    audioInfo.offset = 0
                    audioInfo.size = extractor.readSampleData(audioBuf, 0)
                    if (audioInfo.size < 0) break
                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs > trimEndUs) break
                    if (sampleTimeUs >= trimStartUs) {
                        audioInfo.presentationTimeUs = (sampleTimeUs - trimStartUs).coerceAtLeast(0L)
                        audioInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(muxAudioTrack, audioBuf, audioInfo)
                    }
                    extractor.advance()
                }
            }

            val ok = muxerStarted && outFile.exists() && outFile.length() > 0L
            TranscodeResult(success = ok, width = outW, height = outH)
        } catch (_: Throwable) {
            runCatching { if (outFile.exists()) outFile.delete() }
            TranscodeResult(false)
        } finally {
            runCatching { decoder?.stop(); decoder?.release() }
            runCatching { encoder?.stop(); encoder?.release() }
            runCatching { muxer?.stop(); muxer?.release() }
            runCatching { gpuRenderer?.release() }
            runCatching { eglState?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun buildCurveLutBitmap(state: PhotoEditState): Bitmap {
        val curves = state.curves
        val allLut = PhotoFilterEngine.buildCurveLut(curves.pointsFor(CurveChannel.ALL))
        val rLut = PhotoFilterEngine.buildCurveLut(curves.pointsFor(CurveChannel.RED))
        val gLut = PhotoFilterEngine.buildCurveLut(curves.pointsFor(CurveChannel.GREEN))
        val bLut = PhotoFilterEngine.buildCurveLut(curves.pointsFor(CurveChannel.BLUE))
        val pixels = IntArray(256)
        for (i in 0..255) {
            val r = allLut[rLut[i]]
            val g = allLut[gLut[i]]
            val b = allLut[bLut[i]]
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val bmp = Bitmap.createBitmap(256, 1, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, 256, 0, 0, 256, 1)
        return bmp
    }
}

private class EditorEglInputSurface(private val surface: Surface) {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            0x3142, 1, // EGL_RECORDABLE_ANDROID
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
    }

    fun makeCurrent() {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(eglDisplay, eglSurface)

    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
    }

    fun release() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        surface.release()
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }
}

private class EditorOesGpuRenderer(
    private val editState: PhotoEditState,
    private val totalRot90: Int,
    private val outW: Int,
    private val outH: Int,
    lutBitmap: Bitmap,
    overlayBitmap: Bitmap?
) : SurfaceTexture.OnFrameAvailableListener {

    private val videoTexId: Int
    private val lutTexId: Int
    private val overlayTexId: Int
    private val hasOverlay: Boolean = overlayBitmap != null

    private val surfaceTexture: SurfaceTexture
    val surface: Surface
    private val frameSyncObject = Object()
    private var frameAvailable = false

    private val stMatrix = FloatArray(16)
    private val program: Int
    private val vertexBuffer: FloatBuffer

    private val posHandle: Int
    private val texHandle: Int
    private val stMatrixHandle: Int

    private val videoTexUniform: Int
    private val lutTexUniform: Int
    private val overlayTexUniform: Int
    private val hasOverlayUniform: Int
    private val cropRectUniform: Int
    private val rot90Uniform: Int
    private val mirrorUniform: Int
    private val straightenCosSinZoomUniform: Int
    private val expContrastSatWarmthUniform: Int
    private val fadeHiShadVigUniform: Int
    private val sharpenGrainUniform: Int
    private val blurParamsUniform: Int
    private val blurModeAngleUniform: Int
    private val texelSizeUniform: Int
    private val hasCurvesUniform: Int

    init {
        val textures = IntArray(3)
        GLES20.glGenTextures(3, textures, 0)
        videoTexId = textures[0]
        lutTexId = textures[1]
        overlayTexId = textures[2]

        // 1. OES Video Texture
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTexId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // 2. 256x1 Curves LUT Texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTexId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, lutBitmap, 0)

        // 3. Optional Transparent Paint & Text Overlay Texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTexId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        if (overlayBitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, overlayBitmap, 0)
        } else {
            val empty = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, empty, 0)
            empty.recycle()
        }

        surfaceTexture = SurfaceTexture(videoTexId)
        surfaceTexture.setOnFrameAvailableListener(this)
        surface = Surface(surfaceTexture)

        // Fullscreen quad where u,v are in upright display coordinates: (0,0)=top-left, (1,1)=bottom-right
        val quad = floatArrayOf(
            -1f, -1f, 0f, 0f, 1f,
             1f, -1f, 0f, 1f, 1f,
            -1f,  1f, 0f, 0f, 0f,
             1f,  1f, 0f, 1f, 0f
        )
        vertexBuffer = ByteBuffer.allocateDirect(quad.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(quad); position(0) }

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }

        posHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texHandle = GLES20.glGetAttribLocation(program, "aDisplayUv")
        stMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")

        videoTexUniform = GLES20.glGetUniformLocation(program, "sVideoTex")
        lutTexUniform = GLES20.glGetUniformLocation(program, "sCurveLut")
        overlayTexUniform = GLES20.glGetUniformLocation(program, "sOverlayTex")
        hasOverlayUniform = GLES20.glGetUniformLocation(program, "uHasOverlay")
        cropRectUniform = GLES20.glGetUniformLocation(program, "uCropRect")
        rot90Uniform = GLES20.glGetUniformLocation(program, "uRot90")
        mirrorUniform = GLES20.glGetUniformLocation(program, "uMirror")
        straightenCosSinZoomUniform = GLES20.glGetUniformLocation(program, "uStraighten")
        expContrastSatWarmthUniform = GLES20.glGetUniformLocation(program, "uAdj1")
        fadeHiShadVigUniform = GLES20.glGetUniformLocation(program, "uAdj2")
        sharpenGrainUniform = GLES20.glGetUniformLocation(program, "uAdj3")
        blurParamsUniform = GLES20.glGetUniformLocation(program, "uBlurParams")
        blurModeAngleUniform = GLES20.glGetUniformLocation(program, "uBlurModeAngle")
        texelSizeUniform = GLES20.glGetUniformLocation(program, "uTexelSize")
        hasCurvesUniform = GLES20.glGetUniformLocation(program, "uHasCurves")
    }

    private fun compileShader(type: Int, src: String): Int =
        GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src)
            GLES20.glCompileShader(it)
        }

    override fun onFrameAvailable(st: SurfaceTexture?) {
        synchronized(frameSyncObject) {
            frameAvailable = true
            frameSyncObject.notifyAll()
        }
    }

    fun awaitAndDrawImage() {
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                frameSyncObject.wait(2500L)
                if (!frameAvailable) break
            }
            frameAvailable = false
        }
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(stMatrix)

        GLES20.glViewport(0, 0, outW, outH)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTexId)
        GLES20.glUniform1i(videoTexUniform, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTexId)
        GLES20.glUniform1i(lutTexUniform, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTexId)
        GLES20.glUniform1i(overlayTexUniform, 2)
        GLES20.glUniform1f(hasOverlayUniform, if (hasOverlay) 1f else 0f)

        val crop = editState.crop
        GLES20.glUniform4f(
            cropRectUniform,
            crop.cropRect.left,
            crop.cropRect.top,
            crop.cropRect.width,
            crop.cropRect.height
        )
        GLES20.glUniform1f(rot90Uniform, totalRot90.toFloat())
        GLES20.glUniform1f(mirrorUniform, if (crop.mirrorHorizontal) 1f else 0f)

        val strRad = Math.toRadians(crop.straightenDegrees.toDouble())
        val absRad = abs(strRad)
        val aspect = (outW.toFloat() / outH.toFloat()).coerceIn(0.25f, 4f)
        val zoom = if (abs(crop.straightenDegrees) > 0.05f) {
            (cos(absRad) + sin(absRad) * maxOf(aspect, 1f / aspect)).toFloat().coerceIn(1f, 2f)
        } else 1f
        GLES20.glUniform3f(
            straightenCosSinZoomUniform,
            cos(strRad).toFloat(),
            sin(strRad).toFloat(),
            zoom
        )

        val adj = editState.adjustments
        val expFactor = 2f.pow(adj.exposure * 1.4f)
        val enhanceContrast = 1f + adj.contrast * 0.65f + adj.enhance * 0.25f
        val satFactor = (1f + adj.saturation + adj.enhance * 0.2f).coerceIn(0f, 2.2f)
        val warmthShift = adj.warmth * (28f / 255f)
        GLES20.glUniform4f(expContrastSatWarmthUniform, expFactor, enhanceContrast, satFactor, warmthShift)

        GLES20.glUniform4f(
            fadeHiShadVigUniform,
            adj.fade,
            adj.highlights * 0.45f,
            adj.shadows * 0.45f,
            adj.vignette
        )
        GLES20.glUniform2f(sharpenGrainUniform, adj.sharpen, adj.grain)

        val blur = editState.blur
        val blurModeF = when (blur.mode) {
            BlurMode.OFF -> 0f
            BlurMode.RADIAL -> 1f
            BlurMode.LINEAR -> 2f
        }
        val blurRad = Math.toRadians(blur.angleDegrees.toDouble())
        GLES20.glUniform4f(blurParamsUniform, blur.centerX, blur.centerY, blur.radius, blur.intensity)
        GLES20.glUniform3f(blurModeAngleUniform, blurModeF, cos(blurRad).toFloat(), sin(blurRad).toFloat())
        GLES20.glUniform2f(texelSizeUniform, 1f / outW.toFloat().coerceAtLeast(1f), 1f / outH.toFloat().coerceAtLeast(1f))
        GLES20.glUniform1f(hasCurvesUniform, if (editState.curves.isModified) 1f else 0f)

        GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(posHandle)

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFinish()
    }

    fun release() {
        surface.release()
        surfaceTexture.release()
        GLES20.glDeleteProgram(program)
        GLES20.glDeleteTextures(3, intArrayOf(videoTexId, lutTexId, overlayTexId), 0)
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aDisplayUv;
            varying vec2 vDisplayUv;
            void main() {
                gl_Position = aPosition;
                vDisplayUv = aDisplayUv;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vDisplayUv;
            uniform samplerExternalOES sVideoTex;
            uniform sampler2D sCurveLut;
            uniform sampler2D sOverlayTex;
            uniform float uHasOverlay;
            uniform mat4 uSTMatrix;
            uniform vec4 uCropRect;
            uniform float uRot90;
            uniform float uMirror;
            uniform vec3 uStraighten;
            uniform vec4 uAdj1;
            uniform vec4 uAdj2;
            uniform vec2 uAdj3;
            uniform vec4 uBlurParams;
            uniform vec3 uBlurModeAngle;
            uniform vec2 uTexelSize;
            uniform float uHasCurves;

            vec2 mapDisplayToRawStUv(vec2 dispUv) {
                vec2 fullUv = vec2(
                    uCropRect.x + dispUv.x * uCropRect.z,
                    uCropRect.y + dispUv.y * uCropRect.w
                );
                if (uMirror > 0.5) {
                    fullUv.x = 1.0 - fullUv.x;
                }
                vec2 centered = fullUv - vec2(0.5);
                centered /= max(uStraighten.z, 0.001);
                vec2 rotUv = vec2(
                    centered.x * uStraighten.x + centered.y * uStraighten.y,
                    -centered.x * uStraighten.y + centered.y * uStraighten.x
                ) + vec2(0.5);
                rotUv = clamp(rotUv, 0.0, 1.0);

                vec2 rawUv = rotUv;
                if (uRot90 > 85.0 && uRot90 < 95.0) {
                    rawUv = vec2(rotUv.y, 1.0 - rotUv.x);
                } else if (uRot90 > 175.0 && uRot90 < 185.0) {
                    rawUv = vec2(1.0 - rotUv.x, 1.0 - rotUv.y);
                } else if (uRot90 > 265.0 && uRot90 < 275.0) {
                    rawUv = vec2(1.0 - rotUv.y, rotUv.x);
                }
                // SurfaceTexture stMatrix expects GL bottom-left origin (1.0 - rawUv.y)
                vec4 stCoord = uSTMatrix * vec4(rawUv.x, 1.0 - rawUv.y, 0.0, 1.0);
                return stCoord.xy;
            }

            void main() {
                vec2 sampleUv = mapDisplayToRawStUv(vDisplayUv);
                vec3 col = texture2D(sVideoTex, sampleUv).rgb;

                // Selective Radial / Linear Blur (Droplet)
                if (uBlurModeAngle.x > 0.5 && uBlurParams.w > 0.02) {
                    vec2 diff = vDisplayUv - uBlurParams.xy;
                    float dist = 0.0;
                    if (uBlurModeAngle.x < 1.5) {
                        dist = length(diff);
                    } else {
                        dist = abs(-diff.x * uBlurModeAngle.z + diff.y * uBlurModeAngle.y);
                    }
                    float focusR = max(uBlurParams.z, 0.04);
                    float feather = max(focusR * 0.45, 0.05);
                    float blurT = clamp((dist - focusR) / feather, 0.0, 1.0) * uBlurParams.w;
                    if (blurT > 0.01) {
                        float stepScale = 2.0 + uBlurParams.w * 4.5;
                        vec2 d = uTexelSize * stepScale;
                        vec3 acc = col;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(d.x, 0.0), 0.0, 1.0))).rgb;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv - vec2(d.x, 0.0), 0.0, 1.0))).rgb;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(0.0, d.y), 0.0, 1.0))).rgb;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv - vec2(0.0, d.y), 0.0, 1.0))).rgb;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(d.x, d.y), 0.0, 1.0))).rgb;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(-d.x, d.y), 0.0, 1.0))).rgb;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(d.x, -d.y), 0.0, 1.0))).rgb;
                        acc += texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv - vec2(d.x, d.y), 0.0, 1.0))).rgb;
                        vec3 blurred = acc / 9.0;
                        col = mix(col, blurred, blurT);
                    }
                }

                // Sharpen
                if (uAdj3.x > 0.02) {
                    vec2 sd = uTexelSize * 1.2;
                    vec3 n = texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(0.0, -sd.y), 0.0, 1.0))).rgb;
                    vec3 s = texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(0.0, sd.y), 0.0, 1.0))).rgb;
                    vec3 e = texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(sd.x, 0.0), 0.0, 1.0))).rgb;
                    vec3 w = texture2D(sVideoTex, mapDisplayToRawStUv(clamp(vDisplayUv + vec2(-sd.x, 0.0), 0.0, 1.0))).rgb;
                    vec3 avg = (n + s + e + w) * 0.25;
                    col = col + (col - avg) * (uAdj3.x * 1.1);
                }

                // Exposure
                col *= uAdj1.x;

                // Shadows & Highlights
                float lum = dot(col, vec3(0.299, 0.587, 0.114));
                if (abs(uAdj2.z) > 0.005) {
                    float sw = pow(clamp(1.0 - lum, 0.0, 1.0), 2.0);
                    col += uAdj2.z * sw * 0.47;
                }
                if (abs(uAdj2.y) > 0.005) {
                    float hw = pow(clamp(lum, 0.0, 1.0), 2.0);
                    col += uAdj2.y * hw * 0.47;
                }

                // Contrast
                col = (col - 0.5) * uAdj1.y + 0.5;

                // Warmth
                col.r += uAdj1.w;
                col.b -= uAdj1.w;

                // Fade
                if (uAdj2.x > 0.01) {
                    col = col * (1.0 - uAdj2.x * 0.12) + uAdj2.x * (36.0 / 255.0);
                }

                // Saturation
                float gray = dot(col, vec3(0.299, 0.587, 0.114));
                col = mix(vec3(gray), col, uAdj1.z);
                col = clamp(col, 0.0, 1.0);

                // Tone Curves LUT (ALL, RED, GREEN, BLUE)
                if (uHasCurves > 0.5) {
                    col.r = texture2D(sCurveLut, vec2(col.r, 0.5)).r;
                    col.g = texture2D(sCurveLut, vec2(col.g, 0.5)).g;
                    col.b = texture2D(sCurveLut, vec2(col.b, 0.5)).b;
                }

                // Vignette
                if (uAdj2.w > 0.01) {
                    vec2 vd = (vDisplayUv - vec2(0.5)) * 1.4142;
                    float distSq = dot(vd, vd);
                    float vig = clamp(1.0 - uAdj2.w * 0.85 * distSq, 0.15, 1.0);
                    col *= vig;
                }

                // Grain
                if (uAdj3.y > 0.01) {
                    float noise = fract(sin(dot(vDisplayUv, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
                    col = clamp(col + noise * uAdj3.y * 0.16, 0.0, 1.0);
                }

                // Paint strokes & text overlay
                if (uHasOverlay > 0.5) {
                    vec4 ov = texture2D(sOverlayTex, vDisplayUv);
                    col = mix(col, ov.rgb, ov.a);
                }

                gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
            }
        """
    }
}
