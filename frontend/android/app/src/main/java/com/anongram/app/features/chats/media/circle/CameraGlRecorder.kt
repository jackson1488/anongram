package com.anongram.app.features.chats.media.circle

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Непрерывный OpenGL ES 2.0 + MediaCodec (H.264/AAC) рекордер видео-кружочков в стиле Telegram InstantCameraView:
 * - Кодирует видео в квадрат 480x480 1:1 и пишет звук через AudioRecord без остановки при смене камеры
 * - На время переключения сенсора Front <-> Back на превью и в поток кодирования мягко удерживается последний кадр (0 чёрного экрана)
 * - Один общий EGL-контекст одновременно рисует и в TextureView превью, и в inputSurface видеокодера
 */
internal class CameraGlRecorder {

    companion object {
        const val OUTPUT_SIZE = 480
        private const val VIDEO_MIME = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val AUDIO_MIME = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val VIDEO_BITRATE = 1_600_000
        private const val VIDEO_FPS = 30
        private const val AUDIO_SAMPLE_RATE = 44_100
        private const val AUDIO_BITRATE = 64_000

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uMvpMatrix;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMvpMatrix * aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """

        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )

        private val QUAD_TEX_COORDS = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )
    }

    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglConfig: EGLConfig? = null
    private var pbufferSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var previewEglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var encoderEglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var previewSurfaceTexture: SurfaceTexture? = null
    private var previewWidth: Int = OUTPUT_SIZE
    private var previewHeight: Int = OUTPUT_SIZE

    private var oesTextureId: Int = 0
    private var cameraSurfaceTexture: SurfaceTexture? = null
    @Volatile
    var cameraInputSurface: Surface? = null
        private set

    private var glProgram: Int = 0
    private var aPositionLoc: Int = -1
    private var aTexCoordLoc: Int = -1
    private var uMvpMatrixLoc: Int = -1
    private var uTexMatrixLoc: Int = -1

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(QUAD_VERTICES)
            position(0)
        }
    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUAD_TEX_COORDS.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(QUAD_TEX_COORDS)
            position(0)
        }

    private val texMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    @Volatile
    private var camWidth: Int = 640
    @Volatile
    private var camHeight: Int = 480
    @Volatile
    private var sensorOrientation: Int = 270
    @Volatile
    private var isFrontCamera: Boolean = true
    @Volatile
    private var hasReceivedFirstFrame: Boolean = false
    @Volatile
    private var isSwitchingCamera: Boolean = false
    @Volatile
    private var isDestroyed: Boolean = false

    // MediaCodec / MediaMuxer state
    private var outputFile: File? = null
    private var videoEncoder: MediaCodec? = null
    private var encoderInputSurface: Surface? = null
    private var audioEncoder: MediaCodec? = null
    private var audioRecord: AudioRecord? = null
    private var mediaMuxer: MediaMuxer? = null

    private var videoDrainThread: Thread? = null
    private var audioCaptureThread: Thread? = null
    private var audioDrainThread: Thread? = null

    private val muxerLock = Object()
    @Volatile
    private var videoTrackIndex: Int = -1
    @Volatile
    private var audioTrackIndex: Int = -1
    @Volatile
    private var muxerStarted: Boolean = false
    @Volatile
    private var isRecordingActive: Boolean = false
    @Volatile
    private var audioExpected: Boolean = false
    @Volatile
    private var recordStartNs: Long = 0L
    @Volatile
    private var lastFrameDrawNs: Long = 0L

    private val pendingSamples = mutableListOf<PendingSample>()

    private data class PendingSample(
        val isVideo: Boolean,
        val data: ByteBuffer,
        val info: MediaCodec.BufferInfo
    )

    private val switchHoldRunnable = object : Runnable {
        override fun run() {
            if (!isDestroyed && isSwitchingCamera && hasReceivedFirstFrame) {
                drawCurrentFrame(updateTex = false)
                glHandler?.postDelayed(this, 33L)
            }
        }
    }

    fun initializeSync(): Surface? {
        if (glThread != null && cameraInputSurface != null) return cameraInputSurface
        isDestroyed = false
        val latch = CountDownLatch(1)
        val thread = HandlerThread("VideoCircleGlThread").apply { start() }
        glThread = thread
        val handler = Handler(thread.looper)
        glHandler = handler

        handler.post {
            runCatching {
                initEgl()
                initGlProgram()
                Matrix.setIdentityM(texMatrix, 0)
                oesTextureId = createOesTexture()
                val st = SurfaceTexture(oesTextureId)
                st.setDefaultBufferSize(camWidth, camHeight)
                st.setOnFrameAvailableListener({
                    if (!isDestroyed) {
                        isSwitchingCamera = false
                        glHandler?.removeCallbacks(switchHoldRunnable)
                        drawCurrentFrame(updateTex = true)
                    }
                }, handler)
                cameraSurfaceTexture = st
                cameraInputSurface = Surface(st)
            }
            latch.countDown()
        }
        runCatching { latch.await(2, TimeUnit.SECONDS) }
        return cameraInputSurface
    }

    fun attachPreviewSurface(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        val handler = glHandler ?: return
        handler.post {
            if (isDestroyed) return@post
            previewSurfaceTexture = surfaceTexture
            previewWidth = width.coerceAtLeast(1)
            previewHeight = height.coerceAtLeast(1)
            destroyPreviewEglSurface()
            runCatching {
                surfaceTexture.setDefaultBufferSize(OUTPUT_SIZE, OUTPUT_SIZE)
                previewEglSurface = EGL14.eglCreateWindowSurface(
                    eglDisplay,
                    eglConfig,
                    surfaceTexture,
                    intArrayOf(EGL14.EGL_NONE),
                    0
                )
            }
            if (hasReceivedFirstFrame) {
                drawCurrentFrame(updateTex = false)
            }
        }
    }

    fun updatePreviewSize(width: Int, height: Int) {
        val handler = glHandler ?: return
        handler.post {
            previewWidth = width.coerceAtLeast(1)
            previewHeight = height.coerceAtLeast(1)
        }
    }

    fun detachPreviewSurface(surfaceTexture: SurfaceTexture) {
        val handler = glHandler ?: return
        handler.post {
            if (previewSurfaceTexture === surfaceTexture) {
                previewSurfaceTexture = null
                destroyPreviewEglSurface()
            }
        }
    }

    fun configureCameraParams(width: Int, height: Int, sensorOrientationDeg: Int, front: Boolean) {
        val handler = glHandler ?: return
        handler.post {
            camWidth = width.coerceAtLeast(1)
            camHeight = height.coerceAtLeast(1)
            sensorOrientation = sensorOrientationDeg
            isFrontCamera = front
            runCatching {
                cameraSurfaceTexture?.setDefaultBufferSize(camWidth, camHeight)
            }
        }
    }

    fun markSwitchingCamera(switching: Boolean) {
        val handler = glHandler ?: return
        handler.post {
            isSwitchingCamera = switching
            handler.removeCallbacks(switchHoldRunnable)
            if (switching && hasReceivedFirstFrame) {
                handler.postDelayed(switchHoldRunnable, 33L)
            }
        }
    }

    fun startRecording(file: File): Boolean {
        val handler = glHandler ?: return false
        if (isRecordingActive) return true
        val latch = CountDownLatch(1)
        var startedOk = false
        handler.post {
            startedOk = startRecordingOnGlThread(file)
            latch.countDown()
        }
        runCatching { latch.await(2, TimeUnit.SECONDS) }
        return startedOk
    }

    @SuppressLint("MissingPermission")
    private fun startRecordingOnGlThread(file: File): Boolean {
        return try {
            outputFile = file
            videoTrackIndex = -1
            audioTrackIndex = -1
            muxerStarted = false
            synchronized(muxerLock) { pendingSamples.clear() }
            recordStartNs = System.nanoTime()
            lastFrameDrawNs = 0L

            val vFormat = MediaFormat.createVideoFormat(VIDEO_MIME, OUTPUT_SIZE, OUTPUT_SIZE).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val vCodec = MediaCodec.createEncoderByType(VIDEO_MIME)
            vCodec.configure(vFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val encSurf = vCodec.createInputSurface()
            encoderInputSurface = encSurf
            encoderEglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay,
                eglConfig,
                encSurf,
                intArrayOf(EGL14.EGL_NONE),
                0
            )
            vCodec.start()
            videoEncoder = vCodec

            mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val minBuf = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val ar = runCatching {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                ).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            }.getOrNull()

            if (ar != null) {
                val aCodec = runCatching {
                    MediaFormat.createAudioFormat(AUDIO_MIME, AUDIO_SAMPLE_RATE, 1).apply {
                        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                        setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE)
                        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBuf * 2)
                    }.let { fmt ->
                        MediaCodec.createEncoderByType(AUDIO_MIME).apply {
                            configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                            start()
                        }
                    }
                }.getOrNull()

                if (aCodec != null) {
                    audioRecord = ar
                    audioEncoder = aCodec
                    audioExpected = true
                } else {
                    runCatching { ar.release() }
                    audioExpected = false
                }
            } else {
                audioExpected = false
            }

            isRecordingActive = true

            videoDrainThread = Thread({ drainVideoLoop(vCodec) }, "CircleVideoDrain").apply { start() }
            if (audioExpected && audioRecord != null && audioEncoder != null) {
                val rec = audioRecord!!
                val aEnc = audioEncoder!!
                audioCaptureThread = Thread({ captureAudioLoop(rec, aEnc, minBuf) }, "CircleAudioCap").apply { start() }
                audioDrainThread = Thread({ drainAudioLoop(aEnc) }, "CircleAudioDrain").apply { start() }
            }
            true
        } catch (_: Exception) {
            cleanupRecordingResources()
            false
        }
    }

    fun stopRecordingSync(deleteIfTooShort: Boolean = false): File? {
        if (!isRecordingActive && videoEncoder == null) return null
        isRecordingActive = false

        val vCodec = videoEncoder
        val aCodec = audioEncoder
        val ar = audioRecord
        val file = outputFile

        runCatching { ar?.stop() }
        runCatching { audioCaptureThread?.join(1200) }
        runCatching { audioDrainThread?.join(1200) }

        val eosLatch = CountDownLatch(1)
        val handler = glHandler
        if (handler != null) {
            handler.post {
                runCatching { vCodec?.signalEndOfInputStream() }
                destroyEncoderEglSurface()
                eosLatch.countDown()
            }
            runCatching { eosLatch.await(1, TimeUnit.SECONDS) }
        } else {
            runCatching { vCodec?.signalEndOfInputStream() }
        }

        runCatching { videoDrainThread?.join(2000) }

        cleanupRecordingResources()

        if (file == null || !file.exists()) return null
        val minBytes = if (deleteIfTooShort) 8192L else 4096L
        if (file.length() < minBytes) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    private fun cleanupRecordingResources() {
        isRecordingActive = false
        videoDrainThread = null
        audioCaptureThread = null
        audioDrainThread = null

        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { audioEncoder?.stop() }
        runCatching { audioEncoder?.release() }
        audioEncoder = null
        runCatching { videoEncoder?.stop() }
        runCatching { videoEncoder?.release() }
        videoEncoder = null

        val latch = CountDownLatch(1)
        val handler = glHandler
        if (handler != null && !isDestroyed) {
            handler.post {
                destroyEncoderEglSurface()
                latch.countDown()
            }
            runCatching { latch.await(500, TimeUnit.MILLISECONDS) }
        }
        runCatching { encoderInputSurface?.release() }
        encoderInputSurface = null

        synchronized(muxerLock) {
            pendingSamples.clear()
            if (muxerStarted) {
                runCatching { mediaMuxer?.stop() }
            }
            runCatching { mediaMuxer?.release() }
            mediaMuxer = null
            muxerStarted = false
            videoTrackIndex = -1
            audioTrackIndex = -1
        }
        outputFile = null
    }

    fun release() {
        isDestroyed = true
        isSwitchingCamera = false
        glHandler?.removeCallbacksAndMessages(null)
        stopRecordingSync(deleteIfTooShort = false)

        val latch = CountDownLatch(1)
        val handler = glHandler
        if (handler != null) {
            handler.post {
                destroyPreviewEglSurface()
                destroyEncoderEglSurface()
                runCatching { cameraInputSurface?.release() }
                cameraInputSurface = null
                runCatching { cameraSurfaceTexture?.release() }
                cameraSurfaceTexture = null
                if (oesTextureId != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
                    oesTextureId = 0
                }
                if (glProgram != 0) {
                    GLES20.glDeleteProgram(glProgram)
                    glProgram = 0
                }
                releaseEgl()
                latch.countDown()
            }
            runCatching { latch.await(1500, TimeUnit.MILLISECONDS) }
        }
        glThread?.quitSafely()
        glThread = null
        glHandler = null
    }

    private fun drawCurrentFrame(updateTex: Boolean) {
        if (isDestroyed || eglDisplay === EGL14.EGL_NO_DISPLAY) return
        val st = cameraSurfaceTexture ?: return
        if (updateTex) {
            runCatching {
                makePbufferCurrentIfNeeded()
                st.updateTexImage()
                st.getTransformMatrix(texMatrix)
                hasReceivedFirstFrame = true
            }
        }
        if (!hasReceivedFirstFrame) return

        val nowNs = System.nanoTime()

        if (previewEglSurface !== EGL14.EGL_NO_SURFACE) {
            if (EGL14.eglMakeCurrent(eglDisplay, previewEglSurface, previewEglSurface, eglContext)) {
                GLES20.glViewport(0, 0, previewWidth, previewHeight)
                renderQuad(previewWidth, previewHeight)
                EGL14.eglSwapBuffers(eglDisplay, previewEglSurface)
            }
        }

        if (isRecordingActive && encoderEglSurface !== EGL14.EGL_NO_SURFACE) {
            val ptsNs = (nowNs - recordStartNs).coerceAtLeast(0L)
            if (ptsNs - lastFrameDrawNs >= 16_000_000L) {
                lastFrameDrawNs = ptsNs
                if (EGL14.eglMakeCurrent(eglDisplay, encoderEglSurface, encoderEglSurface, eglContext)) {
                    GLES20.glViewport(0, 0, OUTPUT_SIZE, OUTPUT_SIZE)
                    renderQuad(OUTPUT_SIZE, OUTPUT_SIZE)
                    EGLExt.eglPresentationTimeANDROID(eglDisplay, encoderEglSurface, ptsNs)
                    EGL14.eglSwapBuffers(eglDisplay, encoderEglSurface)
                }
            }
        }
    }

    private fun renderQuad(surfW: Int, surfH: Int) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(glProgram)

        val rotated90or270 = (sensorOrientation == 90 || sensorOrientation == 270)
        val rotW = (if (rotated90or270) camHeight else camWidth).toFloat().coerceAtLeast(1f)
        val rotH = (if (rotated90or270) camWidth else camHeight).toFloat().coerceAtLeast(1f)
        val srcAspect = rotW / rotH
        val dstAspect = surfW.toFloat().coerceAtLeast(1f) / surfH.toFloat().coerceAtLeast(1f)

        var scaleX = if (srcAspect > dstAspect) srcAspect / dstAspect else 1f
        val scaleY = if (srcAspect > dstAspect) 1f else dstAspect / srcAspect
        if (isFrontCamera) {
            scaleX = -scaleX
        }

        val scaleMat = FloatArray(16)
        val rotMat = FloatArray(16)
        Matrix.setIdentityM(scaleMat, 0)
        Matrix.scaleM(scaleMat, 0, scaleX, scaleY, 1f)
        Matrix.setIdentityM(rotMat, 0)
        Matrix.rotateM(rotMat, 0, -sensorOrientation.toFloat(), 0f, 0f, 1f)
        Matrix.multiplyMM(mvpMatrix, 0, scaleMat, 0, rotMat, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)

        GLES20.glUniformMatrix4fv(uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        texCoordBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
    }

    private fun drainVideoLoop(codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val idx = try {
                codec.dequeueOutputBuffer(info, 10_000L)
            } catch (_: Exception) {
                break
            }
            if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                onTrackFormatChanged(isVideo = true, format = codec.outputFormat)
            } else if (idx >= 0) {
                val buf = codec.getOutputBuffer(idx)
                if (buf != null) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        info.size = 0
                    }
                    if (info.size > 0) {
                        writeOrQueueSample(isVideo = true, buf = buf, info = info)
                    }
                }
                runCatching { codec.releaseOutputBuffer(idx, false) }
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
            } else if (!isRecordingActive && idx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // Allow EOS to drain cleanly; loop breaks once EOS flag arrives
            }
        }
    }

    private fun captureAudioLoop(ar: AudioRecord, codec: MediaCodec, minBuf: Int) {
        val pcmBuf = ByteArray(minBuf.coerceAtLeast(2048))
        runCatching { ar.startRecording() }
        while (isRecordingActive) {
            val read = runCatching { ar.read(pcmBuf, 0, pcmBuf.size) }.getOrDefault(-1)
            if (read <= 0) continue
            val inIdx = try {
                codec.dequeueInputBuffer(10_000L)
            } catch (_: Exception) {
                -1
            }
            if (inIdx >= 0) {
                val inBuf = codec.getInputBuffer(inIdx) ?: continue
                inBuf.clear()
                val copyLen = read.coerceAtMost(inBuf.remaining())
                inBuf.put(pcmBuf, 0, copyLen)
                val ptsUs = ((System.nanoTime() - recordStartNs) / 1000L).coerceAtLeast(0L)
                runCatching { codec.queueInputBuffer(inIdx, 0, copyLen, ptsUs, 0) }
            }
        }
        val eosIdx = runCatching { codec.dequeueInputBuffer(100_000L) }.getOrDefault(-1)
        if (eosIdx >= 0) {
            val ptsUs = ((System.nanoTime() - recordStartNs) / 1000L).coerceAtLeast(0L)
            runCatching { codec.queueInputBuffer(eosIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM) }
        }
    }

    private fun drainAudioLoop(codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val idx = try {
                codec.dequeueOutputBuffer(info, 10_000L)
            } catch (_: Exception) {
                break
            }
            if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                onTrackFormatChanged(isVideo = false, format = codec.outputFormat)
            } else if (idx >= 0) {
                val buf = codec.getOutputBuffer(idx)
                if (buf != null) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        info.size = 0
                    }
                    if (info.size > 0) {
                        writeOrQueueSample(isVideo = false, buf = buf, info = info)
                    }
                }
                runCatching { codec.releaseOutputBuffer(idx, false) }
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
            }
        }
    }

    private fun onTrackFormatChanged(isVideo: Boolean, format: MediaFormat) {
        synchronized(muxerLock) {
            val muxer = mediaMuxer ?: return
            if (muxerStarted) return
            if (isVideo) {
                if (videoTrackIndex < 0) videoTrackIndex = muxer.addTrack(format)
            } else {
                if (audioTrackIndex < 0) audioTrackIndex = muxer.addTrack(format)
            }
            val ready = videoTrackIndex >= 0 && (!audioExpected || audioTrackIndex >= 0)
            if (ready) {
                muxer.start()
                muxerStarted = true
                for (sample in pendingSamples) {
                    val trackIdx = if (sample.isVideo) videoTrackIndex else audioTrackIndex
                    if (trackIdx >= 0) {
                        runCatching { muxer.writeSampleData(trackIdx, sample.data, sample.info) }
                    }
                }
                pendingSamples.clear()
            }
        }
    }

    private fun writeOrQueueSample(isVideo: Boolean, buf: ByteBuffer, info: MediaCodec.BufferInfo) {
        synchronized(muxerLock) {
            val muxer = mediaMuxer ?: return
            buf.position(info.offset)
            buf.limit(info.offset + info.size)
            if (muxerStarted) {
                val trackIdx = if (isVideo) videoTrackIndex else audioTrackIndex
                if (trackIdx >= 0) {
                    runCatching { muxer.writeSampleData(trackIdx, buf, info) }
                }
            } else {
                if (pendingSamples.size < 180) {
                    val copy = ByteBuffer.allocateDirect(info.size).order(ByteOrder.nativeOrder())
                    copy.put(buf)
                    copy.flip()
                    val copyInfo = MediaCodec.BufferInfo().apply {
                        set(0, info.size, info.presentationTimeUs, info.flags)
                    }
                    pendingSamples.add(PendingSample(isVideo, copy, copyInfo))
                }
            }
        }
    }

    private fun initEgl() {
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
        eglConfig = configs[0]

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        pbufferSurface = EGL14.eglCreatePbufferSurface(
            eglDisplay,
            eglConfig,
            intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
            0
        )
        EGL14.eglMakeCurrent(eglDisplay, pbufferSurface, pbufferSurface, eglContext)
    }

    private fun makePbufferCurrentIfNeeded() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY && pbufferSurface !== EGL14.EGL_NO_SURFACE) {
            if (EGL14.eglGetCurrentContext() != eglContext) {
                EGL14.eglMakeCurrent(eglDisplay, pbufferSurface, pbufferSurface, eglContext)
            }
        }
    }

    private fun destroyPreviewEglSurface() {
        if (previewEglSurface !== EGL14.EGL_NO_SURFACE) {
            makePbufferCurrentIfNeeded()
            EGL14.eglDestroySurface(eglDisplay, previewEglSurface)
            previewEglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun destroyEncoderEglSurface() {
        if (encoderEglSurface !== EGL14.EGL_NO_SURFACE) {
            makePbufferCurrentIfNeeded()
            EGL14.eglDestroySurface(eglDisplay, encoderEglSurface)
            encoderEglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    private fun releaseEgl() {
        if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            if (pbufferSurface !== EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, pbufferSurface)
                pbufferSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext !== EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
    }

    private fun initGlProgram() {
        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        glProgram = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vShader)
            GLES20.glAttachShader(prog, fShader)
            GLES20.glLinkProgram(prog)
        }
        GLES20.glDeleteShader(vShader)
        GLES20.glDeleteShader(fShader)

        aPositionLoc = GLES20.glGetAttribLocation(glProgram, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(glProgram, "aTexCoord")
        uMvpMatrixLoc = GLES20.glGetUniformLocation(glProgram, "uMvpMatrix")
        uTexMatrixLoc = GLES20.glGetUniformLocation(glProgram, "uTexMatrix")
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createOesTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return tex[0]
    }
}
