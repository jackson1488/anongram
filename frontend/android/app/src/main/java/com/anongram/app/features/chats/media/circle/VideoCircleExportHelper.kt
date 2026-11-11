package com.anongram.app.features.chats.media.circle

import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import android.opengl.Matrix
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.view.Surface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Экспорт и шеринг видео-кружков:
 * Конвертирует кружок в квадратное MP4 480x480 1:1 с кругом ровно по центру на чёрном фоне
 * (сохраняя оригинальную аудиодорожку), затем сохраняет в Галерею или открывает системный Share.
 */
internal object VideoCircleExportHelper {

    private const val SIZE = 480
    private const val BITRATE = 1_600_000

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
            vec2 squareUv = gl_FragCoord.xy / 480.0;
            float dist = distance(squareUv, vec2(0.5, 0.5));
            float mask = 1.0 - smoothstep(0.492, 0.497, dist);
            vec4 tex = texture2D(uTexture, vTexCoord);
            gl_FragColor = vec4(tex.rgb * mask, 1.0);
        }
    """

    private val QUAD_VERTICES = floatArrayOf(
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
         1f,  1f
    )

    private val QUAD_TEX = floatArrayOf(
        0f, 0f,
        1f, 0f,
        0f, 1f,
        1f, 1f
    )

    suspend fun saveVideoCircleToGallery(context: Context, uriString: String?): Boolean =
        withContext(Dispatchers.IO) {
            val exported = exportSquareCenteredCircleMp4(context, uriString) ?: return@withContext false
            runCatching {
                val filename = "AnonGram_Circle_${System.currentTimeMillis()}.mp4"
                val resolver = context.contentResolver
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AnonGram")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val destUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false
                    resolver.openOutputStream(destUri)?.use { out ->
                        exported.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(destUri, values, null, null)
                } else {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    }
                    val destUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext false
                    resolver.openOutputStream(destUri)?.use { out ->
                        exported.inputStream().use { input -> input.copyTo(out) }
                    }
                }
                true
            }.getOrDefault(false)
        }

    suspend fun shareVideoCircleSystemChooser(context: Context, uriString: String?): Boolean {
        val exported = withContext(Dispatchers.IO) {
            exportSquareCenteredCircleMp4(context, uriString)
        } ?: return false

        return runCatching {
            val authority = "${context.packageName}.provider"
            val shareUri = FileProvider.getUriForFile(context, authority, exported)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Поделиться видеосообщением").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    suspend fun exportSquareCenteredCircleMp4(context: Context, uriString: String?): File? =
        withContext(Dispatchers.IO) {
            if (uriString.isNullOrBlank()) return@withContext null
            val sourceFile = resolveSourceFile(context, uriString) ?: return@withContext null
            if (!sourceFile.exists() || sourceFile.length() <= 0L) return@withContext null

            val shareDir = File(context.cacheDir, "shared_cache").apply { mkdirs() }
            val cacheKey = "${sourceFile.nameWithoutExtension}_${sourceFile.length()}_${sourceFile.lastModified()}"
            val outFile = File(shareDir, "circle_sq_$cacheKey.mp4")
            if (outFile.exists() && outFile.length() > 4096L) {
                return@withContext outFile
            }
            val tmpFile = File(shareDir, "circle_sq_tmp_${System.currentTimeMillis()}.mp4")
            val ok = runCatching { renderMaskedSquareMp4(sourceFile, tmpFile) }.getOrDefault(false)
            if (ok && tmpFile.exists() && tmpFile.length() > 4096L) {
                if (outFile.exists()) runCatching { outFile.delete() }
                if (tmpFile.renameTo(outFile)) outFile else tmpFile
            } else {
                runCatching { tmpFile.delete() }
                // Fallback to original file if hardware transcoding unavailable
                val fallbackCopy = File(shareDir, "circle_raw_${sourceFile.name}")
                runCatching { sourceFile.copyTo(fallbackCopy, overwrite = true) }.getOrNull()
            }
        }

    private fun resolveSourceFile(context: Context, uriString: String): File? {
        val parsed = Uri.parse(uriString)
        return when (parsed.scheme) {
            null, "file" -> File(parsed.path ?: uriString)
            "content" -> {
                val tmpDir = File(context.cacheDir, "shared_cache").apply { mkdirs() }
                val tmp = File(tmpDir, "src_${uriString.hashCode()}.mp4")
                runCatching {
                    context.contentResolver.openInputStream(parsed)?.use { input ->
                        tmp.outputStream().use { out -> input.copyTo(out) }
                    }
                    tmp
                }.getOrNull()
            }
            else -> File(uriString).takeIf { it.exists() }
        }
    }

    private fun renderMaskedSquareMp4(source: File, dest: File): Boolean {
        val mmr = MediaMetadataRetriever()
        var videoW = SIZE
        var videoH = SIZE
        var rotation = 0
        try {
            mmr.setDataSource(source.absolutePath)
            videoW = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: SIZE
            videoH = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: SIZE
            rotation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        } finally {
            runCatching { mmr.release() }
        }

        val extractor = MediaExtractor()
        extractor.setDataSource(source.absolutePath)
        var videoTrack = -1
        var audioTrack = -1
        var videoFormat: MediaFormat? = null
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
            if (videoTrack < 0 && mime.startsWith("video/")) {
                videoTrack = i
                videoFormat = fmt
            } else if (audioTrack < 0 && mime.startsWith("audio/")) {
                audioTrack = i
                audioFormat = fmt
            }
        }
        if (videoTrack < 0 || videoFormat == null) {
            extractor.release()
            return false
        }

        val cbThread = HandlerThread("CircleExportCallback").apply { start() }
        val cbHandler = Handler(cbThread.looper)

        var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        var encoderInputSurface: Surface? = null
        var decoderSurface: Surface? = null
        var decoderSurfaceTexture: SurfaceTexture? = null
        var encoder: MediaCodec? = null
        var decoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        return try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val ver = IntArray(2)
            EGL14.eglInitialize(eglDisplay, ver, 0, ver, 1)
            val configs = arrayOfNulls<EGLConfig>(1)
            val numCfg = IntArray(1)
            EGL14.eglChooseConfig(
                eglDisplay,
                intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    0x3142, 1,
                    EGL14.EGL_NONE
                ),
                0, configs, 0, 1, numCfg, 0
            )
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )

            val encFmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, SIZE, SIZE).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                configure(encFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }
            encoderInputSurface = encoder.createInputSurface()
            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, configs[0], encoderInputSurface, intArrayOf(EGL14.EGL_NONE), 0
            )
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            encoder.start()

            val oesTex = createOesTexture()
            val frameLock = ReentrantLock()
            val frameCond = frameLock.newCondition()
            var frameAvailable = false

            decoderSurfaceTexture = SurfaceTexture(oesTex).apply {
                setOnFrameAvailableListener({
                    frameLock.withLock {
                        frameAvailable = true
                        frameCond.signalAll()
                    }
                }, cbHandler)
            }
            decoderSurface = Surface(decoderSurfaceTexture)

            val vMime = videoFormat.getString(MediaFormat.KEY_MIME)!!
            decoder = MediaCodec.createDecoderByType(vMime).apply {
                configure(videoFormat, decoderSurface, null, 0)
                start()
            }

            val program = createProgram()
            val aPosLoc = GLES20.glGetAttribLocation(program, "aPosition")
            val aTexLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
            val uMvpLoc = GLES20.glGetUniformLocation(program, "uMvpMatrix")
            val uTexLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")

            val vBuf = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply { put(QUAD_VERTICES); position(0) }
            val tBuf = ByteBuffer.allocateDirect(QUAD_TEX.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply { put(QUAD_TEX); position(0) }

            val stMatrix = FloatArray(16)
            val mvpMatrix = buildMvpMatrix(videoW, videoH, rotation)

            muxer = MediaMuxer(dest.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_FORMAT_MPEG_4)
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var muxerStarted = false

            extractor.selectTrack(videoTrack)
            val decInfo = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var encoderDone = false

            while (!encoderDone) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(inBuf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!outputDone) {
                    val decIdx = decoder.dequeueOutputBuffer(decInfo, 10_000L)
                    if (decIdx >= 0) {
                        val render = decInfo.size > 0
                        decoder.releaseOutputBuffer(decIdx, render)
                        if (render) {
                            var ready = false
                            frameLock.withLock {
                                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500)
                                while (!frameAvailable) {
                                    val rem = deadline - System.nanoTime()
                                    if (rem <= 0) break
                                    frameCond.awaitNanos(rem)
                                }
                                ready = frameAvailable
                                frameAvailable = false
                            }
                            if (ready) {
                                decoderSurfaceTexture.updateTexImage()
                                decoderSurfaceTexture.getTransformMatrix(stMatrix)
                                drawMaskedFrame(
                                    program, oesTex, aPosLoc, aTexLoc, uMvpLoc, uTexLoc,
                                    mvpMatrix, stMatrix, vBuf, tBuf
                                )
                                EGLExt.eglPresentationTimeANDROID(
                                    eglDisplay,
                                    eglSurface,
                                    decInfo.presentationTimeUs * 1000L
                                )
                                EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                            }
                        }
                        if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                            encoder.signalEndOfInputStream()
                        }
                    }
                }

                while (true) {
                    val encIdx = encoder.dequeueOutputBuffer(encInfo, if (outputDone) 10_000L else 0L)
                    if (encIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (encIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            muxerVideoTrack = muxer.addTrack(encoder.outputFormat)
                            if (audioTrack >= 0 && audioFormat != null) {
                                muxerAudioTrack = muxer.addTrack(audioFormat)
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (encIdx >= 0) {
                        val encBuf = encoder.getOutputBuffer(encIdx)
                        if (encBuf != null && (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && encInfo.size > 0 && muxerStarted) {
                            encBuf.position(encInfo.offset)
                            encBuf.limit(encInfo.offset + encInfo.size)
                            muxer.writeSampleData(muxerVideoTrack, encBuf, encInfo)
                        }
                        encoder.releaseOutputBuffer(encIdx, false)
                        if ((encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoderDone = true
                            break
                        }
                    }
                }
            }

            // Прямое копирование оригинальной аудиодорожки без перекодирования
            if (muxerStarted && muxerAudioTrack >= 0) {
                extractor.unselectTrack(videoTrack)
                extractor.selectTrack(audioTrack)
                val audioBuf = ByteBuffer.allocateDirect(256 * 1024)
                val audioInfo = MediaCodec.BufferInfo()
                while (true) {
                    val sampleSize = extractor.readSampleData(audioBuf, 0)
                    if (sampleSize < 0) break
                    audioInfo.set(0, sampleSize, extractor.sampleTime, extractor.sampleFlags)
                    audioBuf.position(0)
                    audioBuf.limit(sampleSize)
                    muxer.writeSampleData(muxerAudioTrack, audioBuf, audioInfo)
                    extractor.advance()
                }
            }

            GLES20.glDeleteTextures(1, intArrayOf(oesTex), 0)
            GLES20.glDeleteProgram(program)
            muxerStarted
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            runCatching { decoderSurface?.release() }
            runCatching { decoderSurfaceTexture?.release() }
            runCatching { encoderInputSurface?.release() }
            if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface !== EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                if (eglContext !== EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
            }
            cbThread.quitSafely()
        }
    }

    private fun buildMvpMatrix(videoW: Int, videoH: Int, rotation: Int): FloatArray {
        val rotated90or270 = (rotation == 90 || rotation == 270)
        val rotW = (if (rotated90or270) videoH else videoW).toFloat().coerceAtLeast(1f)
        val rotH = (if (rotated90or270) videoW else videoH).toFloat().coerceAtLeast(1f)
        val srcAspect = rotW / rotH
        val scaleX = if (srcAspect > 1f) srcAspect else 1f
        val scaleY = if (srcAspect > 1f) 1f else 1f / srcAspect

        val scaleMat = FloatArray(16)
        val rotMat = FloatArray(16)
        val res = FloatArray(16)
        Matrix.setIdentityM(scaleMat, 0)
        Matrix.scaleM(scaleMat, 0, scaleX, scaleY, 1f)
        Matrix.setIdentityM(rotMat, 0)
        Matrix.rotateM(rotMat, 0, -rotation.toFloat(), 0f, 0f, 1f)
        Matrix.multiplyMM(res, 0, scaleMat, 0, rotMat, 0)
        return res
    }

    private fun drawMaskedFrame(
        program: Int,
        oesTex: Int,
        aPosLoc: Int,
        aTexLoc: Int,
        uMvpLoc: Int,
        uTexLoc: Int,
        mvpMatrix: FloatArray,
        stMatrix: FloatArray,
        vBuf: FloatBuffer,
        tBuf: FloatBuffer
    ) {
        GLES20.glViewport(0, 0, SIZE, SIZE)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTex)
        GLES20.glUniformMatrix4fv(uMvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uTexLoc, 1, false, stMatrix, 0)

        vBuf.position(0)
        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 0, vBuf)

        tBuf.position(0)
        GLES20.glEnableVertexAttribArray(aTexLoc)
        GLES20.glVertexAttribPointer(aTexLoc, 2, GLES20.GL_FLOAT, false, 0, tBuf)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosLoc)
        GLES20.glDisableVertexAttribArray(aTexLoc)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)
    }

    private fun createProgram(): Int {
        val vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).also {
            GLES20.glShaderSource(it, VERTEX_SHADER)
            GLES20.glCompileShader(it)
        }
        val fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).also {
            GLES20.glShaderSource(it, FRAGMENT_SHADER)
            GLES20.glCompileShader(it)
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
        }
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
