package com.anongram.app.features.chats.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
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
import android.opengl.Matrix
import android.view.Surface
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Системный аппаратный GPU-транскодер видео (`MediaCodec` Surface-to-Surface + `EGL14` + `MediaMuxer`).
 * Перекодирует только видеопоток в H.264 до целевого разрешения/битрейта, а аудио копирует без пересжатия.
 * Поддерживает плавный прогресс и мгновенную отмену.
 */
internal object HardwareVideoTranscoder {

    suspend fun transcode(
        context: Context,
        sourceUri: Uri,
        outFile: File,
        targetWidth: Int,
        targetHeight: Int,
        targetBitrate: Int,
        rotationHint: Int,
        durationUs: Long,
        onProgress: (Float) -> Unit
    ): Boolean {
        val extractor = MediaExtractor()
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var videoFormat: MediaFormat? = null
        var audioFormat: MediaFormat? = null

        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var eglState: EglInputSurface? = null
        var decodeSurface: OutputOesSurface? = null

        return try {
            extractor.setDataSource(context, sourceUri, null)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (videoTrackIndex < 0 && mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = fmt
                } else if (audioTrackIndex < 0 && mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                }
            }
            if (videoTrackIndex < 0 || videoFormat == null) return false

            val outW = (targetWidth and 1.inv()).coerceAtLeast(16)
            val outH = (targetHeight and 1.inv()).coerceAtLeast(16)
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

            eglState = EglInputSurface(inputSurface)
            eglState.makeCurrent()
            decodeSurface = OutputOesSurface()

            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME) ?: return false
            decoder = MediaCodec.createDecoderByType(videoMime)
            decoder.configure(videoFormat, decodeSurface.surface, null, 0)
            decoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotationHint)

            extractor.selectTrack(videoTrackIndex)

            var muxVideoTrack = -1
            var muxAudioTrack = -1
            var muxerStarted = false

            var inputDone = false
            var decoderDone = false
            var encoderDone = false
            val decInfo = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10_000L
            val safeDurUs = durationUs.coerceAtLeast(1L)

            while (!encoderDone) {
                if (!currentCoroutineContext().isActive) {
                    throw CancellationException("Transcode cancelled")
                }

                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(timeoutUs)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)
                        val sampleSize = if (buf != null) extractor.readSampleData(buf, 0) else -1
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!decoderDone) {
                    val outIdx = decoder.dequeueOutputBuffer(decInfo, timeoutUs)
                    if (outIdx >= 0) {
                        val doRender = decInfo.size > 0
                        decoder.releaseOutputBuffer(outIdx, doRender)
                        if (doRender) {
                            decodeSurface.awaitNewImage()
                            decodeSurface.drawImage()
                            eglState.setPresentationTime(decInfo.presentationTimeUs * 1000L)
                            eglState.swapBuffers()
                            val p = (decInfo.presentationTimeUs.toFloat() / safeDurUs).coerceIn(0f, 0.92f)
                            onProgress(p)
                        }
                        if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
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

            // Быстрое копирование аудиодорожки без перекодирования
            if (muxerStarted && muxAudioTrack >= 0 && audioTrackIndex >= 0) {
                extractor.unselectTrack(videoTrackIndex)
                extractor.selectTrack(audioTrackIndex)
                extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val audioBuf = ByteBuffer.allocate(256 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                while (currentCoroutineContext().isActive) {
                    audioInfo.offset = 0
                    audioInfo.size = extractor.readSampleData(audioBuf, 0)
                    if (audioInfo.size < 0) break
                    audioInfo.presentationTimeUs = extractor.sampleTime
                    audioInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxAudioTrack, audioBuf, audioInfo)
                    extractor.advance()
                }
            }

            onProgress(1f)
            muxerStarted
        } catch (ce: CancellationException) {
            runCatching { if (outFile.exists()) outFile.delete() }
            throw ce
        } catch (_: Throwable) {
            runCatching { if (outFile.exists()) outFile.delete() }
            false
        } finally {
            runCatching { decoder?.stop(); decoder?.release() }
            runCatching { encoder?.stop(); encoder?.release() }
            runCatching { muxer?.stop(); muxer?.release() }
            runCatching { decodeSurface?.release() }
            runCatching { eglState?.release() }
            runCatching { extractor.release() }
        }
    }
}

private class EglInputSurface(private val surface: Surface) {
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

private class OutputOesSurface : SurfaceTexture.OnFrameAvailableListener {
    private val textureId: Int
    private val surfaceTexture: SurfaceTexture
    val surface: Surface
    private val frameSyncObject = Object()
    private var frameAvailable = false

    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val program: Int
    private val vertexBuffer: FloatBuffer
    private val mvpHandle: Int
    private val stHandle: Int
    private val posHandle: Int
    private val texHandle: Int

    init {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(this)
        surface = Surface(surfaceTexture)

        val quad = floatArrayOf(
            -1f, -1f, 0f, 0f, 0f,
             1f, -1f, 0f, 1f, 0f,
            -1f,  1f, 0f, 0f, 1f,
             1f,  1f, 0f, 1f, 1f
        )
        vertexBuffer = ByteBuffer.allocateDirect(quad.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(quad); position(0) }

        val vs = compileShader(
            GLES20.GL_VERTEX_SHADER,
            "uniform mat4 uMVPMatrix; uniform mat4 uSTMatrix; attribute vec4 aPosition; attribute vec4 aTextureCoord; varying vec2 vTextureCoord; void main() { gl_Position = uMVPMatrix * aPosition; vTextureCoord = (uSTMatrix * aTextureCoord).xy; }"
        )
        val fs = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            "#extension GL_OES_EGL_image_external : require\nprecision mediump float; varying vec2 vTextureCoord; uniform samplerExternalOES sTexture; void main() { gl_FragColor = texture2D(sTexture, vTextureCoord); }"
        )
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
        posHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        stHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        Matrix.setIdentityM(mvpMatrix, 0)
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

    fun awaitNewImage() {
        synchronized(frameSyncObject) {
            while (!frameAvailable) {
                frameSyncObject.wait(2500L)
                if (!frameAvailable) break
            }
            frameAvailable = false
        }
        surfaceTexture.updateTexImage()
    }

    fun drawImage() {
        surfaceTexture.getTransformMatrix(stMatrix)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(posHandle)

        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
        GLES20.glEnableVertexAttribArray(texHandle)

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(stHandle, 1, false, stMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFinish()
    }

    fun release() {
        surface.release()
        surfaceTexture.release()
        GLES20.glDeleteProgram(program)
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }
}
