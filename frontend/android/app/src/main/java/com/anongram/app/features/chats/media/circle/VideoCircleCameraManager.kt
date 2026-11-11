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
import android.util.Size
import android.view.Surface
import java.io.File
import kotlin.math.abs

/**
 * Менеджер камеры Camera2 и записи круглых видеосообщений.
 * Обеспечивает живое превью в круглом видоискателе и запись H.264/AAC в файл MP4 без гонки потоков.
 */
class VideoCircleCameraManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentVideoFile: File? = null
    private var currentCameraId: String? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    var isFrontCamera: Boolean = true
        private set

    var isFlashOn: Boolean = false
        private set

    @Volatile
    var isRecording: Boolean = false
        private set

    @Volatile
    private var shouldAutoRecord: Boolean = true

    private var surfaceTexture: SurfaceTexture? = null
    var videoSize: Size = Size(640, 480)
        private set

    @Volatile
    private var isOpeningCamera: Boolean = false

    companion object {
        private val cameraCache = java.util.concurrent.ConcurrentHashMap<Boolean, Pair<String, Size>>()
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("VideoCircleCameraThread").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(400)
        } catch (_: Exception) {}
        backgroundThread = null
        backgroundHandler = null
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

    /**
     * Запускает открытие камеры параллельно с инфлейтом TextureView для мгновенного старта.
     */
    @SuppressLint("MissingPermission")
    fun preOpenCamera(front: Boolean = true, autoStartRecording: Boolean = true) {
        this.isFrontCamera = front
        this.shouldAutoRecord = autoStartRecording
        startBackgroundThread()
        if (autoStartRecording && currentVideoFile == null) {
            currentVideoFile = File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4")
        }
        backgroundHandler?.post {
            if (cameraDevice == null && !isOpeningCamera) {
                openCameraOnBackground(front)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCameraOnBackground(front: Boolean) {
        try {
            val (cameraId, size) = resolveCameraInfo(front) ?: return
            currentCameraId = cameraId
            videoSize = size
            isOpeningCamera = true

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    isOpeningCamera = false
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
                    runCatching { camera.close() }
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    isOpeningCamera = false
                    runCatching { camera.close() }
                    cameraDevice = null
                }
            }, backgroundHandler)
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
        this.surfaceTexture = surfaceTexture
        this.isFrontCamera = front
        this.shouldAutoRecord = autoStartRecording
        startBackgroundThread()

        if (autoStartRecording && currentVideoFile == null) {
            currentVideoFile = File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4")
        }

        backgroundHandler?.post {
            val info = resolveCameraInfo(front) ?: return@post
            videoSize = info.second
            surfaceTexture.setDefaultBufferSize(videoSize.width, videoSize.height)

            val existing = cameraDevice
            if (existing != null && currentCameraId == info.first) {
                if (shouldAutoRecord && !isRecording) {
                    val started = startRecordingInternal(existing)
                    if (!started) createPreviewSession()
                } else if (!isRecording) {
                    createPreviewSession()
                }
            } else if (!isOpeningCamera || currentCameraId != info.first) {
                closeCameraOnly()
                openCameraOnBackground(front)
            }
        }
    }

    private fun createPreviewSession() {
        val camera = cameraDevice ?: return
        val st = surfaceTexture ?: return

        try {
            val previewSurface = Surface(st)
            val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                if (isFlashOn && !isFrontCamera) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                }
            }

            camera.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            session.setRepeatingRequest(
                                previewRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                        } catch (_: Exception) {}
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                backgroundHandler
            )
        } catch (_: Exception) {}
    }

    fun switchCamera() {
        val st = surfaceTexture ?: return
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (_: Exception) {
            } finally {
                mediaRecorder = null
                isRecording = false
            }
        }
        startPreview(st, front = !isFrontCamera, autoStartRecording = true)
    }

    fun toggleFlash() {
        isFlashOn = !isFlashOn
        if (isFrontCamera) {
            // Front camera uses software white screen illumination in UI
            return
        }
        val session = captureSession ?: return
        val camera = cameraDevice ?: return
        val st = surfaceTexture ?: return
        try {
            val previewSurface = Surface(st)
            val recorderSurface = mediaRecorder?.surface
            val template = if (isRecording && recorderSurface != null) {
                CameraDevice.TEMPLATE_RECORD
            } else {
                CameraDevice.TEMPLATE_PREVIEW
            }
            val builder = camera.createCaptureRequest(template).apply {
                addTarget(previewSurface)
                if (isRecording && recorderSurface != null) {
                    addTarget(recorderSurface)
                }
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(
                    CaptureRequest.FLASH_MODE,
                    if (isFlashOn) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                )
            }
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (_: Exception) {}
    }

    fun startRecording(): File? {
        shouldAutoRecord = true
        if (currentVideoFile == null) {
            currentVideoFile = File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4")
        }
        val camera = cameraDevice
        if (camera != null && !isRecording) {
            startRecordingInternal(camera)
        }
        return currentVideoFile
    }

    @SuppressLint("MissingPermission")
    private fun startRecordingInternal(camera: CameraDevice): Boolean {
        val st = surfaceTexture ?: return false
        try {
            val file = currentVideoFile ?: File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4").also {
                currentVideoFile = it
            }

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
                configureRecorder(mr, 640, 480, 1_200_000)
                mr.prepare()
            }

            mediaRecorder = mr

            val previewSurface = Surface(st)
            val recorderSurface = mr.surface

            val surfaces = listOf(previewSurface, recorderSurface)
            val recordRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
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
                        captureSession = session
                        try {
                            session.setRepeatingRequest(
                                recordRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                            mr.start()
                            isRecording = true
                        } catch (_: Exception) {
                            isRecording = false
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        isRecording = false
                    }
                },
                backgroundHandler
            )

            return true
        } catch (_: Exception) {
            isRecording = false
            return false
        }
    }

    fun stopRecording(): File? {
        shouldAutoRecord = false
        val file = currentVideoFile
        currentVideoFile = null

        if (isRecording || mediaRecorder != null) {
            isRecording = false
            try {
                captureSession?.stopRepeating()
            } catch (_: Exception) {}
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {}
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
        }

        return if (file != null && file.exists() && file.length() > 0L) file else null
    }

    /** Останавливает запись и освобождает камеру, оставляя файл для предпросмотра и обрезки */
    fun pauseRecordingForReview(): File? {
        val file = stopRecording()
        closeCameraOnly()
        return file
    }

    fun cancelRecording() {
        shouldAutoRecord = false
        if (isRecording || mediaRecorder != null) {
            isRecording = false
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {}
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
        }
        currentVideoFile?.delete()
        currentVideoFile = null
    }

    private fun closeCameraOnly() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (_: Exception) {}
    }

    fun release() {
        shouldAutoRecord = false
        if (isRecording || mediaRecorder != null) {
            isRecording = false
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {}
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
        }
        closeCameraOnly()
        stopBackgroundThread()
    }
}
