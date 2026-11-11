package com.anongram.app.features.chats.media.circle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Менеджер камеры Camera2 и записи круглых видеосообщений.
 * - Дожидается фактического закрытия CameraDevice (onClosed) перед открытием другой камеры при переключении
 * - Все операции MediaRecorder.stop()/release() выполняются на фоновом потоке (0 лагов UI)
 * - Освобождает Surface превью без утечек BufferQueue и не возвращает битые пустые MP4-файлы
 */
class VideoCircleCameraManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recorderStopExecutor = Executors.newSingleThreadExecutor()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentVideoFile: File? = null
    private var currentCameraId: String? = null

    @Volatile
    private var pendingRecorderStopFuture: Future<*>? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    @Volatile
    var isFrontCamera: Boolean = true
        private set

    @Volatile
    var isFlashOn: Boolean = false
        private set

    @Volatile
    var isRecording: Boolean = false
        private set

    @Volatile
    private var hasRecorderActuallyStarted: Boolean = false

    @Volatile
    private var shouldAutoRecord: Boolean = true

    @Volatile
    private var isReleased: Boolean = false

    @Volatile
    private var isOpeningCamera: Boolean = false

    @Volatile
    private var isClosingCamera: Boolean = false

    @Volatile
    private var pendingOpenFront: Boolean? = null

    private var surfaceTexture: SurfaceTexture? = null

    @Volatile
    var videoSize: Size = Size(640, 480)
        private set

    var onVideoSizeReady: ((Int, Int) -> Unit)? = null

    companion object {
        private val cameraCache = ConcurrentHashMap<Boolean, Pair<String, Size>>()
    }

    private fun ensureBackgroundThread(): Handler? {
        if (isReleased) return null
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("VideoCircleCameraThread").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
        return backgroundHandler
    }

    private fun stopBackgroundThreadSafely() {
        val thread = backgroundThread
        backgroundThread = null
        backgroundHandler = null
        thread?.quitSafely()
    }

    private fun resolveCameraInfo(front: Boolean): Pair<String, Size>? {
        cameraCache[front]?.let { return it }
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (front) facing == CameraCharacteristics.LENS_FACING_FRONT
                else facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return null
            val size = chooseSupportedVideoSize(cameraId)
            val pair = cameraId to size
            cameraCache[front] = pair
            pair
        } catch (_: Exception) {
            null
        }
    }

    private fun chooseSupportedVideoSize(cameraId: String): Size {
        return try {
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(MediaRecorder::class.java)
            if (sizes.isNullOrEmpty()) {
                Size(640, 480)
            } else {
                sizes.firstOrNull { it.width == 640 && it.height == 480 }
                    ?: sizes.firstOrNull { it.width == 720 && it.height == 480 }
                    ?: sizes.firstOrNull { it.width == 1280 && it.height == 720 }
                    ?: sizes.filter { it.width <= 1280 && it.height <= 720 }
                        .minByOrNull { abs(it.width - 640) + abs(it.height - 480) }
                    ?: sizes.first()
            }
        } catch (_: Exception) {
            Size(640, 480)
        }
    }

    private fun notifyVideoSize(size: Size) {
        mainHandler.post {
            onVideoSizeReady?.invoke(size.width, size.height)
        }
    }

    /**
     * Запускает открытие камеры параллельно с инфлейтом TextureView для мгновенного старта.
     */
    @SuppressLint("MissingPermission")
    fun preOpenCamera(front: Boolean = true, autoStartRecording: Boolean = true) {
        isReleased = false
        this.isFrontCamera = front
        this.shouldAutoRecord = autoStartRecording
        val handler = ensureBackgroundThread() ?: return
        handler.post {
            if (isReleased) return@post
            if (cameraDevice == null && !isOpeningCamera && !isClosingCamera) {
                openCameraOnBackground(front)
            } else if (isClosingCamera) {
                pendingOpenFront = front
            }
            resolveCameraInfo(!front)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCameraOnBackground(front: Boolean) {
        if (isReleased) return
        val handler = backgroundHandler ?: return
        try {
            val (cameraId, size) = resolveCameraInfo(front) ?: return
            currentCameraId = cameraId
            videoSize = size
            notifyVideoSize(size)
            isOpeningCamera = true

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    isOpeningCamera = false
                    if (isReleased) {
                        isClosingCamera = true
                        runCatching { camera.close() }
                        return
                    }
                    if (pendingOpenFront != null && pendingOpenFront != front) {
                        isClosingCamera = true
                        runCatching { camera.close() }
                        return
                    }
                    cameraDevice = camera
                    val st = surfaceTexture
                    if (st != null) {
                        st.setDefaultBufferSize(videoSize.width, videoSize.height)
                        if (shouldAutoRecord && !isRecording) {
                            val started = startRecordingInternal(camera)
                            if (!started) createPreviewSession()
                        } else if (!isRecording) {
                            createPreviewSession()
                        }
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    isOpeningCamera = false
                    isClosingCamera = true
                    if (cameraDevice === camera) cameraDevice = null
                    runCatching { camera.close() }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    isOpeningCamera = false
                    isClosingCamera = true
                    if (cameraDevice === camera) cameraDevice = null
                    runCatching { camera.close() }
                }

                override fun onClosed(camera: CameraDevice) {
                    isOpeningCamera = false
                    isClosingCamera = false
                    if (cameraDevice === camera) cameraDevice = null
                    val nextFront = pendingOpenFront
                    pendingOpenFront = null
                    if (isReleased) {
                        stopBackgroundThreadSafely()
                    } else if (nextFront != null) {
                        openCameraOnBackground(nextFront)
                    }
                }
            }, handler)
        } catch (_: Exception) {
            isOpeningCamera = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startPreview(
        surfaceTexture: SurfaceTexture,
        front: Boolean = true,
        autoStartRecording: Boolean = true
    ) {
        isReleased = false
        this.surfaceTexture = surfaceTexture
        this.isFrontCamera = front
        this.shouldAutoRecord = autoStartRecording
        val handler = ensureBackgroundThread() ?: return

        handler.post {
            if (isReleased) return@post
            val info = resolveCameraInfo(front) ?: return@post
            videoSize = info.second
            notifyVideoSize(videoSize)
            runCatching { surfaceTexture.setDefaultBufferSize(videoSize.width, videoSize.height) }

            val existing = cameraDevice
            when {
                existing != null && currentCameraId == info.first && !isClosingCamera -> {
                    if (shouldAutoRecord && !isRecording) {
                        val started = startRecordingInternal(existing)
                        if (!started) createPreviewSession()
                    } else if (!isRecording) {
                        createPreviewSession()
                    }
                }
                existing != null -> {
                    pendingOpenFront = front
                    closeCameraAsync()
                }
                isClosingCamera -> {
                    pendingOpenFront = front
                }
                isOpeningCamera && currentCameraId != info.first -> {
                    pendingOpenFront = front
                }
                !isOpeningCamera -> {
                    openCameraOnBackground(front)
                }
            }
        }
    }

    private fun releasePreviewSurface() {
        previewSurface?.let { s ->
            runCatching { s.release() }
        }
        previewSurface = null
    }

    private fun createPreviewSession() {
        val camera = cameraDevice ?: return
        val st = surfaceTexture ?: return
        val handler = backgroundHandler ?: return

        try {
            closeSessionOnly()
            releasePreviewSurface()
            st.setDefaultBufferSize(videoSize.width, videoSize.height)
            val pSurface = Surface(st)
            previewSurface = pSurface
            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(pSurface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                if (isFlashOn && !isFrontCamera) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                }
            }

            camera.createCaptureSession(
                listOf(pSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (isReleased || cameraDevice !== camera) {
                            runCatching { session.close() }
                            return
                        }
                        captureSession = session
                        try {
                            session.setRepeatingRequest(
                                previewRequestBuilder.build(),
                                null,
                                handler
                            )
                        } catch (_: Exception) {}
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                handler
            )
        } catch (_: Exception) {}
    }

    /**
     * Переключение камеры во время превью/записи:
     * корректно останавливает текущий MediaRecorder на фоновом потоке, ждёт onClosed текущей камеры
     * и автоматически открывает выбранную камеру с новым файлом записи (без чёрного экрана и Error code 0).
     */
    fun switchCamera() {
        val nextFront = !isFrontCamera
        isFrontCamera = nextFront
        val handler = ensureBackgroundThread() ?: return
        handler.post {
            if (isReleased) return@post
            stopRecorderAsync(deleteIfTooShort = true)
            closeSessionOnly()
            pendingOpenFront = nextFront
            if (cameraDevice != null && !isClosingCamera) {
                closeCameraAsync()
            } else if (!isClosingCamera && !isOpeningCamera) {
                pendingOpenFront = null
                openCameraOnBackground(nextFront)
            }
        }
    }

    fun toggleFlash() {
        isFlashOn = !isFlashOn
        if (isFrontCamera) {
            // Front camera uses software white screen illumination in UI
            return
        }
        val handler = backgroundHandler ?: return
        handler.post {
            val session = captureSession ?: return@post
            val camera = cameraDevice ?: return@post
            val pSurface = previewSurface ?: return@post
            try {
                val recorderSurface = mediaRecorder?.surface
                val template = if (isRecording && recorderSurface != null) {
                    CameraDevice.TEMPLATE_RECORD
                } else {
                    CameraDevice.TEMPLATE_PREVIEW
                }
                val builder = camera.createCaptureRequest(template).apply {
                    addTarget(pSurface)
                    if (isRecording && recorderSurface != null) {
                        addTarget(recorderSurface)
                    }
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    set(
                        CaptureRequest.FLASH_MODE,
                        if (isFlashOn) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                    )
                }
                session.setRepeatingRequest(builder.build(), null, handler)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecordingInternal(camera: CameraDevice): Boolean {
        val st = surfaceTexture ?: return false
        val handler = backgroundHandler ?: return false
        try {
            closeSessionOnly()
            releasePreviewSurface()
            awaitPendingRecorderStop()

            val file = File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4")
            currentVideoFile = file
            hasRecorderActuallyStarted = false

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            fun configureRecorder(recorder: MediaRecorder, width: Int, height: Int, bitRate: Int) {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(file.absolutePath)
                    setVideoEncodingBitRate(bitRate)
                    setVideoFrameRate(30)
                    setVideoSize(width, height)
                    setOrientationHint(if (isFrontCamera) 270 else 90)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(64000)
                }
            }

            try {
                configureRecorder(mr, videoSize.width, videoSize.height, 1_800_000)
                mr.prepare()
            } catch (_: Exception) {
                mr.reset()
                videoSize = Size(640, 480)
                notifyVideoSize(videoSize)
                configureRecorder(mr, 640, 480, 1_200_000)
                mr.prepare()
            }

            mediaRecorder = mr

            st.setDefaultBufferSize(videoSize.width, videoSize.height)
            val pSurface = Surface(st)
            previewSurface = pSurface
            val recorderSurface = mr.surface

            val surfaces = listOf(pSurface, recorderSurface)
            val recordRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(pSurface)
                addTarget(recorderSurface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                if (isFlashOn && !isFrontCamera) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                }
            }

            camera.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (isReleased || cameraDevice !== camera || mediaRecorder !== mr) {
                            runCatching { session.close() }
                            return
                        }
                        captureSession = session
                        try {
                            session.setRepeatingRequest(
                                recordRequestBuilder.build(),
                                null,
                                handler
                            )
                            mr.start()
                            isRecording = true
                            hasRecorderActuallyStarted = true
                        } catch (_: Exception) {
                            isRecording = false
                            hasRecorderActuallyStarted = false
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        isRecording = false
                        hasRecorderActuallyStarted = false
                    }
                },
                handler
            )

            return true
        } catch (_: Exception) {
            isRecording = false
            hasRecorderActuallyStarted = false
            return false
        }
    }

    private fun finalizeMediaRecorder(
        mr: MediaRecorder?,
        wasStarted: Boolean,
        file: File?,
        deleteIfTooShort: Boolean
    ): File? {
        var stopOk = false
        if (mr != null) {
            if (wasStarted) {
                try {
                    mr.stop()
                    stopOk = true
                } catch (_: Exception) {
                    stopOk = false
                }
            }
            try {
                mr.release()
            } catch (_: Exception) {}
        }

        if (file == null || !file.exists()) return null
        val valid = stopOk && file.length() > 4096L
        if (!valid || (deleteIfTooShort && file.length() < 8192L)) {
            runCatching { file.delete() }
            return null
        }
        return file
    }

    private fun awaitPendingRecorderStop() {
        val future = pendingRecorderStopFuture ?: return
        pendingRecorderStopFuture = null
        runCatching { future.get(600, TimeUnit.MILLISECONDS) }
    }

    private fun stopRecorderAsync(deleteIfTooShort: Boolean = false) {
        val file = currentVideoFile
        currentVideoFile = null
        val mr = mediaRecorder
        mediaRecorder = null
        val wasStarted = hasRecorderActuallyStarted
        isRecording = false
        hasRecorderActuallyStarted = false

        try {
            captureSession?.stopRepeating()
        } catch (_: Exception) {}

        if (mr == null && file == null) return
        pendingRecorderStopFuture = runCatching {
            recorderStopExecutor.submit {
                finalizeMediaRecorder(mr, wasStarted, file, deleteIfTooShort)
            }
        }.getOrNull()
    }

    private fun stopRecorderInternal(deleteIfTooShort: Boolean = false): File? {
        awaitPendingRecorderStop()
        val file = currentVideoFile
        currentVideoFile = null
        val mr = mediaRecorder
        mediaRecorder = null
        val wasStarted = hasRecorderActuallyStarted
        isRecording = false
        hasRecorderActuallyStarted = false

        try {
            captureSession?.stopRepeating()
        } catch (_: Exception) {}

        return finalizeMediaRecorder(mr, wasStarted, file, deleteIfTooShort)
    }

    private fun closeSessionOnly() {
        try {
            captureSession?.stopRepeating()
        } catch (_: Exception) {}
        try {
            captureSession?.close()
        } catch (_: Exception) {}
        captureSession = null
    }

    private fun closeCameraAsync() {
        val cam = cameraDevice
        cameraDevice = null
        if (cam != null && !isClosingCamera) {
            isClosingCamera = true
            try {
                cam.close()
            } catch (_: Exception) {
                isClosingCamera = false
            }
        }
    }

    /** Останавливает запись на фоновом потоке (без блокировки UI) и возвращает готовый MP4-файл */
    fun stopRecordingAsync(onResult: (File?) -> Unit) {
        shouldAutoRecord = false
        val handler = backgroundHandler
        if (handler == null) {
            onResult(null)
            return
        }
        handler.post {
            val file = stopRecorderInternal(deleteIfTooShort = false)
            mainHandler.post { onResult(file) }
        }
    }

    /** Останавливает запись и освобождает камеру для перехода в предпросмотр/обрезку */
    fun pauseRecordingForReviewAsync(onResult: (File?) -> Unit) {
        shouldAutoRecord = false
        val handler = backgroundHandler
        if (handler == null) {
            onResult(null)
            return
        }
        handler.post {
            val file = stopRecorderInternal(deleteIfTooShort = false)
            closeSessionOnly()
            closeCameraAsync()
            mainHandler.post { onResult(file) }
        }
    }

    fun cancelRecording() {
        shouldAutoRecord = false
        pendingOpenFront = null
        val handler = backgroundHandler
        if (handler == null) {
            currentVideoFile?.let { runCatching { it.delete() } }
            currentVideoFile = null
            return
        }
        handler.post {
            stopRecorderInternal(deleteIfTooShort = false)?.let { runCatching { it.delete() } }
        }
    }

    fun release() {
        isReleased = true
        shouldAutoRecord = false
        pendingOpenFront = null
        val handler = backgroundHandler
        if (handler == null) {
            awaitPendingRecorderStop()
            releasePreviewSurface()
            runCatching { recorderStopExecutor.shutdown() }
            return
        }
        handler.post {
            stopRecorderInternal(deleteIfTooShort = false)
            closeSessionOnly()
            releasePreviewSurface()
            runCatching { recorderStopExecutor.shutdown() }
            if (cameraDevice != null) {
                closeCameraAsync()
                // Остановка потока произойдёт в StateCallback.onClosed после полного закрытия камеры
            } else if (!isClosingCamera) {
                stopBackgroundThreadSafely()
            }
        }
    }
}
