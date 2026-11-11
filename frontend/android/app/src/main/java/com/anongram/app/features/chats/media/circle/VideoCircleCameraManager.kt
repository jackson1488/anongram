package com.anongram.app.features.chats.media.circle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Range
import android.util.Size
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Менеджер камеры Camera2 + непрерывного OpenGL/MediaCodec рекордера (CameraGlRecorder)
 * по архитектуре Telegram InstantCameraView / Camera2Session:
 * - Выбирает оптимальный 4:3 поток (без аппаратного кропа EIS) для чистого квадратного кропа 1:1 с полным обзором лица
 * - Отключает CONTROL_VIDEO_STABILIZATION_MODE (EIS-зум) и фиксирует zoom = 1.0x, сохраняя OIS при наличии
 * - Поддерживает плавный автофокус, приоритет лиц и ручной tap-to-focus с автовозвратом к следящему фокусу
 * - Быстрое переключение Front <-> Back поверх единого GL/MediaCodec/AudioRecord пайплайна без разрыва записи
 */
class VideoCircleCameraManager(private val context: Context) {

    private data class CameraMeta(
        val cameraId: String,
        val size: Size,
        val sensorOrientation: Int,
        val fpsRange: Range<Int>?,
        val activeArray: Rect?,
        val afMode: Int,
        val availableAfModes: IntArray,
        val maxAfRegions: Int,
        val maxAeRegions: Int,
        val faceDetectMode: Int?,
        val supportsVideoStabOff: Boolean,
        val supportsOis: Boolean
    )

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val glRecorder = CameraGlRecorder()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCameraId: String? = null
    private var activeMeta: CameraMeta? = null

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
    private val resetFocusRunnable = Runnable {
        resetContinuousFocusOnBackground()
    }

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
            val targetFacing = if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
            val cameraId = ids.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == targetFacing
            } ?: ids.firstOrNull() ?: return null

            val chars = cameraManager.getCameraCharacteristics(cameraId)
            val orientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: if (front) 270 else 90
            val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            val size = chooseOptimalSquareFriendlySize(chars)

            val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val fpsRange = chooseBestFpsRange(fpsRanges)

            val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: IntArray(0)
            val afMode = when {
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO in afModes -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE in afModes -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                CaptureRequest.CONTROL_AF_MODE_AUTO in afModes -> CaptureRequest.CONTROL_AF_MODE_AUTO
                else -> CaptureRequest.CONTROL_AF_MODE_OFF
            }

            val maxAfRegions = chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0
            val maxAeRegions = chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 0

            val faceModes = chars.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
            val faceDetectMode = when {
                faceModes == null -> null
                CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE in faceModes -> CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE
                CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL in faceModes -> CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL
                else -> null
            }

            val stabModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            val supportsVideoStabOff = stabModes == null || CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF in stabModes

            val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            val supportsOis = oisModes != null && CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON in oisModes

            CameraMeta(
                cameraId = cameraId,
                size = size,
                sensorOrientation = orientation,
                fpsRange = fpsRange,
                activeArray = activeArray,
                afMode = afMode,
                availableAfModes = afModes,
                maxAfRegions = maxAfRegions,
                maxAeRegions = maxAeRegions,
                faceDetectMode = faceDetectMode,
                supportsVideoStabOff = supportsVideoStabOff,
                supportsOis = supportsOis
            ).also { cameraCache[front] = it }
        } catch (_: Exception) {
            null
        }
    }

    private fun chooseBestFpsRange(ranges: Array<Range<Int>>?): Range<Int>? {
        if (ranges.isNullOrEmpty()) return null
        val upTo30 = ranges.filter { it.upper <= 30 }
        if (upTo30.isEmpty()) return ranges.minByOrNull { it.upper }
        return upTo30.filter { it.upper == 30 && it.lower in 15..24 }
            .maxByOrNull { it.lower }
            ?: upTo30.filter { it.upper == 30 }.minByOrNull { abs(it.lower - 24) }
            ?: upTo30.maxWithOrNull(compareBy<Range<Int>> { it.upper }.thenBy { it.lower })
    }

    /**
     * Подбор оптимального 4:3 (совпадающего с активной матрицей сенсора) размера потока
     * от 480p до 960p/1280p, чтобы кружок 480x480 был чётким и без потери угла обзора.
     */
    private fun chooseOptimalSquareFriendlySize(chars: CameraCharacteristics): Size {
        return try {
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(SurfaceTexture::class.java)
            if (sizes.isNullOrEmpty()) return Size(640, 480)

            val pixelSize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val sensorAspect = if (pixelSize != null && pixelSize.height > 0) {
                pixelSize.width.toFloat() / pixelSize.height.toFloat()
            } else {
                4f / 3f
            }

            val candidates = sizes.filter { s ->
                val minDim = minOf(s.width, s.height)
                val maxDim = maxOf(s.width, s.height)
                minDim >= 480 && maxDim <= 1280
            }

            val aspectMatched = candidates.filter { s ->
                val aspect = s.width.toFloat() / s.height.toFloat().coerceAtLeast(1f)
                abs(aspect - sensorAspect) < 0.08f
            }

            aspectMatched.firstOrNull { it.width == 960 && it.height == 720 }
                ?: aspectMatched.firstOrNull { it.width == 800 && it.height == 600 }
                ?: aspectMatched.firstOrNull { it.width == 1280 && it.height == 960 }
                ?: aspectMatched.firstOrNull { it.width == 640 && it.height == 480 }
                ?: aspectMatched.minByOrNull { abs(it.width - 800) + abs(it.height - 600) }
                ?: candidates.firstOrNull { it.width == 800 && it.height == 600 }
                ?: candidates.firstOrNull { it.width == 640 && it.height == 480 }
                ?: candidates.minByOrNull { abs(it.width - 800) + abs(it.height - 600) }
                ?: sizes.filter { minOf(it.width, it.height) >= 480 }
                    .minByOrNull { it.width * it.height }
                ?: sizes.first()
        } catch (_: Exception) {
            Size(640, 480)
        }
    }

    private fun notifySquareSize() {
        mainHandler.post {
            onVideoSizeReady?.invoke(CameraGlRecorder.OUTPUT_SIZE, CameraGlRecorder.OUTPUT_SIZE)
        }
    }

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
            activeMeta = meta
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

    private fun buildCaptureRequestBuilder(
        camera: CameraDevice,
        meta: CameraMeta,
        front: Boolean,
        cameraSurface: android.view.Surface,
        afModeOverride: Int? = null,
        meteringRect: MeteringRectangle? = null
    ): CaptureRequest.Builder {
        // TEMPLATE_PREVIEW + VIDEO_STABILIZATION_OFF исключает 1.3x кроп EIS на Xiaomi/Qualcomm,
        // сохраняя полный естественный обзор камеры для лица в кружочке.
        return camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(cameraSurface)
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD)
            set(CaptureRequest.CONTROL_AF_MODE, afModeOverride ?: meta.afMode)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO)
            if (meta.supportsVideoStabOff) {
                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
            }
            if (meta.supportsOis) {
                set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                set(CaptureRequest.CONTROL_ZOOM_RATIO, 1.0f)
            }
            meta.faceDetectMode?.let {
                set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, it)
            }
            meta.fpsRange?.let {
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it)
            }
            if (meteringRect != null) {
                if (meta.maxAfRegions > 0) {
                    set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRect))
                }
                if (meta.maxAeRegions > 0) {
                    set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(meteringRect))
                }
            }
            if (isFlashOn && !front) {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
            } else {
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
            }
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
            val requestBuilder = buildCaptureRequestBuilder(camera, meta, front, cameraSurface)

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
        handler.removeCallbacks(resetFocusRunnable)
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
            val meta = activeMeta ?: return@post
            val cameraSurface = glRecorder.cameraInputSurface ?: return@post
            try {
                val builder = buildCaptureRequestBuilder(camera, meta, isFrontCamera, cameraSurface)
                session.setRepeatingRequest(builder.build(), null, handler)
            } catch (_: Exception) {}
        }
    }

    /**
     * Tap-to-focus: переводит координаты точки внутри кружка [0..1, 0..1] в область сенсора Camera2
     * с учётом зеркалирования фронтальной камеры, sensorOrientation и квадратного кропа 4:3 -> 1:1.
     */
    fun focusAtNormalizedPoint(nx: Float, ny: Float) {
        val handler = backgroundHandler ?: return
        val frontSnapshot = isFrontCamera
        handler.removeCallbacks(resetFocusRunnable)
        handler.post {
            val session = captureSession ?: return@post
            val camera = cameraDevice ?: return@post
            val meta = activeMeta ?: return@post
            val activeArray = meta.activeArray ?: return@post
            val cameraSurface = glRecorder.cameraInputSurface ?: return@post
            if (meta.maxAfRegions <= 0 && meta.maxAeRegions <= 0) return@post

            try {
                val u = if (frontSnapshot) (1f - nx.coerceIn(0f, 1f)) else nx.coerceIn(0f, 1f)
                val v = ny.coerceIn(0f, 1f)

                // Учитываем квадратный кроп 4:3 -> 1:1 в центре кадра
                val rot90or270 = (meta.sensorOrientation == 90 || meta.sensorOrientation == 270)
                val rotW = (if (rot90or270) meta.size.height else meta.size.width).toFloat().coerceAtLeast(1f)
                val rotH = (if (rot90or270) meta.size.width else meta.size.height).toFloat().coerceAtLeast(1f)
                val srcAspect = rotW / rotH
                val cropU = if (srcAspect > 1f) 0.5f + (u - 0.5f) / srcAspect else u
                val cropV = if (srcAspect < 1f) 0.5f + (v - 0.5f) * srcAspect else v

                // Переводим портретные нормализованные координаты в координаты ландшафтного сенсора
                val (sx, sy) = when (meta.sensorOrientation) {
                    90 -> cropV to (1f - cropU)
                    270 -> (1f - cropV) to cropU
                    180 -> (1f - cropU) to (1f - cropV)
                    else -> cropU to cropV
                }

                val aw = activeArray.width()
                val ah = activeArray.height()
                val cx = (activeArray.left + sx * aw).roundToInt()
                val cy = (activeArray.top + sy * ah).roundToInt()
                val halfSize = (minOf(aw, ah) * 0.12f).roundToInt().coerceAtLeast(48)

                val left = (cx - halfSize).coerceIn(activeArray.left, activeArray.right - 2)
                val top = (cy - halfSize).coerceIn(activeArray.top, activeArray.bottom - 2)
                val right = (cx + halfSize).coerceIn(left + 1, activeArray.right)
                val bottom = (cy + halfSize).coerceIn(top + 1, activeArray.bottom)

                val meteringRect = MeteringRectangle(
                    left,
                    top,
                    right - left,
                    bottom - top,
                    MeteringRectangle.METERING_WEIGHT_MAX - 100
                )

                val canAutoTrigger = CaptureRequest.CONTROL_AF_MODE_AUTO in meta.availableAfModes
                val triggerAfMode = if (canAutoTrigger) CaptureRequest.CONTROL_AF_MODE_AUTO else meta.afMode

                val repeatingBuilder = buildCaptureRequestBuilder(
                    camera = camera,
                    meta = meta,
                    front = frontSnapshot,
                    cameraSurface = cameraSurface,
                    afModeOverride = triggerAfMode,
                    meteringRect = meteringRect
                )
                session.setRepeatingRequest(repeatingBuilder.build(), null, handler)

                if (canAutoTrigger && meta.maxAfRegions > 0) {
                    val triggerBuilder = buildCaptureRequestBuilder(
                        camera = camera,
                        meta = meta,
                        front = frontSnapshot,
                        cameraSurface = cameraSurface,
                        afModeOverride = CaptureRequest.CONTROL_AF_MODE_AUTO,
                        meteringRect = meteringRect
                    ).apply {
                        set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
                    }
                    session.capture(triggerBuilder.build(), null, handler)
                }

                handler.postDelayed(resetFocusRunnable, 2200L)
            } catch (_: Exception) {}
        }
    }

    private fun resetContinuousFocusOnBackground() {
        if (isReleased) return
        val handler = backgroundHandler ?: return
        val session = captureSession ?: return
        val camera = cameraDevice ?: return
        val meta = activeMeta ?: return
        val cameraSurface = glRecorder.cameraInputSurface ?: return
        try {
            val builder = buildCaptureRequestBuilder(camera, meta, isFrontCamera, cameraSurface)
            session.setRepeatingRequest(builder.build(), null, handler)
        } catch (_: Exception) {}
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
        handler.removeCallbacks(resetFocusRunnable)
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
