package com.anongram.app.features.chats.media.circle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Менеджер камеры Camera2 + непрерывного OpenGL/MediaCodec рекордера (CameraGlRecorder).
 * - Переключение Front <-> Back меняет только сенсор Camera2 поверх единого GL/MediaCodec/AudioRecord пайплайна:
 *   запись видео и звука не прерывается, предыдущий фрагмент сохраняется, чёрного экрана нет
 * - Вспышка при переключении камеры всегда сбрасывается в OFF (без синхронного переноса)
 */
class VideoCircleCameraManager(private val context: Context) {

    private data class CameraMeta(
        val cameraId: String,
        val size: Size,
        val sensorOrientation: Int,
        val fpsRange: android.util.Range<Int>?
    )

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val glRecorder = CameraGlRecorder()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId: String? = null

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
    private var shouldAutoRecord: Boolean = true

    @Volatile
    private var isReleased: Boolean = false

    @Volatile
    private var isOpeningCamera: Boolean = false

    @Volatile
    private var isClosingCamera: Boolean = false

    @Volatile
    private var pendingOpenFront: Boolean? = null

    @Volatile
    var videoSize: Size = Size(CameraGlRecorder.OUTPUT_SIZE, CameraGlRecorder.OUTPUT_SIZE)
        private set

    var onVideoSizeReady: ((width: Int, height: Int) -> Unit)? = null

    private val cameraCache = ConcurrentHashMap<Boolean, CameraMeta>()

    @Synchronized
    private fun ensureBackgroundThread(): Handler? {
        if (isReleased) return null
        if (backgroundThread == null || backgroundThread?.isAlive == false) {
            backgroundThread = HandlerThread("VideoCircleCameraBg").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
        return backgroundHandler
    }

    @Synchronized
    private fun stopBackgroundThreadSafely() {
        val thread = backgroundThread
        backgroundThread = null
        backgroundHandler = null
        thread?.quitSafely()
    }

    private fun resolveCameraMeta(front: Boolean): CameraMeta? {
        cameraCache[front]?.let { return it }
        return try {
            val ids = cameraManager.cameraIdList
            val cameraId = ids.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (front) facing == CameraCharacteristics.LENS_FACING_FRONT
                else facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: ids.firstOrNull() ?: return null
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            val orientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: if (front) 270 else 90
            val size = chooseSupportedVideoSize(cameraId)
            val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val fpsRange = fpsRanges
                ?.filter { it.upper <= 30 }
                ?.maxWithOrNull(compareBy<android.util.Range<Int>> { it.upper }.thenBy { it.lower })
            CameraMeta(cameraId, size, orientation, fpsRange).also { cameraCache[front] = it }
        } catch (_: Exception) {
            null
        }
    }

    private fun chooseSupportedVideoSize(cameraId: String): Size {
        return try {
            val chars = cameraManager.getCameraCharacteristics(cameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(SurfaceTexture::class.java)
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

    private fun notifySquareSize() {
        mainHandler.post {
            onVideoSizeReady?.invoke(CameraGlRecorder.OUTPUT_SIZE, CameraGlRecorder.OUTPUT_SIZE)
        }
    }

    /**
     * Запускает GL-поток, кеш камер и открытие стартовой камеры параллельно с UI.
     */
    @SuppressLint("MissingPermission")
    fun preOpenCamera(front: Boolean = true, autoStartRecording: Boolean = true) {
        isReleased = false
        this.isFrontCamera = front
        this.shouldAutoRecord = autoStartRecording
        val handler = ensureBackgroundThread() ?: return
        handler.post {
            if (isReleased) return@post
            glRecorder.initializeSync()
            if (shouldAutoRecord && !isRecording) {
                val file = File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4")
                if (glRecorder.startRecording(file)) {
                    isRecording = true
                }
            }
            if (cameraDevice == null && !isOpeningCamera && !isClosingCamera) {
                openCameraOnBackground(front)
            } else if (isClosingCamera) {
                pendingOpenFront = front
            }
            resolveCameraMeta(!front)
        }
    }

    fun startPreview(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
        front: Boolean = true,
        autoStartRecording: Boolean = true
    ) {
        isReleased = false
        this.shouldAutoRecord = autoStartRecording
        val handler = ensureBackgroundThread() ?: return
        handler.post {
            if (isReleased) return@post
            glRecorder.initializeSync()
            glRecorder.attachPreviewSurface(surfaceTexture, width, height)
            notifySquareSize()

            if (shouldAutoRecord && !isRecording) {
                val file = File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4")
                if (glRecorder.startRecording(file)) {
                    isRecording = true
                }
            }

            val meta = resolveCameraMeta(front) ?: return@post
            val existing = cameraDevice
            when {
                existing != null && currentCameraId == meta.cameraId && !isClosingCamera -> {
                    if (captureSession == null) {
                        createCameraSession(existing, meta, front)
                    }
                }
                existing != null -> {
                    pendingOpenFront = front
                    captureSession = null
                    closeCameraAsync()
                }
                isClosingCamera -> {
                    pendingOpenFront = front
                }
                !isOpeningCamera -> {
                    openCameraOnBackground(front)
                }
            }
        }
    }

    fun updatePreviewSize(width: Int, height: Int) {
        glRecorder.updatePreviewSize(width, height)
    }

    fun detachPreviewSurface(surfaceTexture: SurfaceTexture) {
        glRecorder.detachPreviewSurface(surfaceTexture)
    }

    @SuppressLint("MissingPermission")
    private fun openCameraOnBackground(front: Boolean) {
        if (isReleased) return
        val handler = backgroundHandler ?: return
        try {
            val meta = resolveCameraMeta(front) ?: return
            currentCameraId = meta.cameraId
            isOpeningCamera = true

            glRecorder.initializeSync()
            glRecorder.configureCameraParams(
                width = meta.size.width,
                height = meta.size.height,
                sensorOrientationDeg = meta.sensorOrientation,
                front = front
            )
            notifySquareSize()

            cameraManager.openCamera(meta.cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    isOpeningCamera = false
                    if (isReleased || (pendingOpenFront != null && pendingOpenFront != front)) {
                        isClosingCamera = true
                        runCatching { camera.close() }
                        return
                    }
                    cameraDevice = camera
                    createCameraSession(camera, meta, front)
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
                    captureSession = null
                    if (cameraDevice === camera) cameraDevice = null
                    val nextFront = pendingOpenFront
                    pendingOpenFront = null
                    if (isReleased) {
                        mainHandler.postDelayed({
                            if (isReleased && !isClosingCamera) {
                                stopBackgroundThreadSafely()
                            }
                        }, 150L)
                    } else if (nextFront != null) {
                        openCameraOnBackground(nextFront)
                    }
                }
            }, handler)
        } catch (_: Exception) {
            isOpeningCamera = false
        }
    }

    private fun createCameraSession(camera: CameraDevice, meta: CameraMeta, front: Boolean) {
        val handler = backgroundHandler ?: return
        val cameraSurface = glRecorder.cameraInputSurface ?: return
        try {
            glRecorder.configureCameraParams(
                width = meta.size.width,
                height = meta.size.height,
                sensorOrientationDeg = meta.sensorOrientation,
                front = front
            )
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(cameraSurface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                meta.fpsRange?.let { set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it) }
                if (isFlashOn && !front) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }

            camera.createCaptureSession(
                listOf(cameraSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (isReleased || cameraDevice !== camera) {
                            runCatching { session.close() }
                            return
                        }
                        captureSession = session
                        runCatching {
                            session.setRepeatingRequest(requestBuilder.build(), null, handler)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                handler
            )
        } catch (_: Exception) {}
    }

    /**
     * Быстрое переключение камеры без stopRepeating() и без остановки GL/MediaCodec/AudioRecord:
     * - Всегда сбрасывает вспышку в OFF
     * - Удерживает последний кадр на время переоткрытия сенсора (без чёрного экрана)
     */
    fun switchCamera() {
        val nextFront = !isFrontCamera
        isFrontCamera = nextFront
        isFlashOn = false
        glRecorder.markSwitchingCamera(true)

        val handler = ensureBackgroundThread() ?: return
        handler.post {
            if (isReleased) return@post
            pendingOpenFront = nextFront
            captureSession = null
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
            return
        }
        val handler = backgroundHandler ?: return
        handler.post {
            val session = captureSession ?: return@post
            val camera = cameraDevice ?: return@post
            val cameraSurface = glRecorder.cameraInputSurface ?: return@post
            try {
                val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                    addTarget(cameraSurface)
                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                    set(
                        CaptureRequest.FLASH_MODE,
                        if (isFlashOn) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                    )
                }
                session.setRepeatingRequest(builder.build(), null, handler)
            } catch (_: Exception) {}
        }
    }

    private fun closeSessionOnly() {
        val session = captureSession
        captureSession = null
        if (session != null) {
            runCatching { session.stopRepeating() }
            runCatching { session.close() }
        }
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

    fun stopRecordingAsync(onResult: (File?) -> Unit) {
        shouldAutoRecord = false
        isRecording = false
        val handler = backgroundHandler
        if (handler == null) {
            onResult(null)
            return
        }
        handler.post {
            val file = glRecorder.stopRecordingSync(deleteIfTooShort = false)
            mainHandler.post { onResult(file) }
        }
    }

    fun pauseRecordingForReviewAsync(onResult: (File?) -> Unit) {
        shouldAutoRecord = false
        isRecording = false
        val handler = backgroundHandler
        if (handler == null) {
            onResult(null)
            return
        }
        handler.post {
            val file = glRecorder.stopRecordingSync(deleteIfTooShort = false)
            closeSessionOnly()
            closeCameraAsync()
            mainHandler.post { onResult(file) }
        }
    }

    fun cancelRecording() {
        shouldAutoRecord = false
        isRecording = false
        pendingOpenFront = null
        val handler = backgroundHandler
        if (handler == null) {
            glRecorder.stopRecordingSync(deleteIfTooShort = false)?.let { runCatching { it.delete() } }
            return
        }
        handler.post {
            glRecorder.stopRecordingSync(deleteIfTooShort = false)?.let { runCatching { it.delete() } }
        }
    }

    fun release() {
        isReleased = true
        shouldAutoRecord = false
        isRecording = false
        pendingOpenFront = null
        val handler = backgroundHandler
        if (handler == null) {
            glRecorder.release()
            return
        }
        handler.post {
            glRecorder.stopRecordingSync(deleteIfTooShort = false)
            closeSessionOnly()
            glRecorder.release()
            if (cameraDevice != null) {
                closeCameraAsync()
            } else if (!isClosingCamera) {
                stopBackgroundThreadSafely()
            }
        }
    }
}
