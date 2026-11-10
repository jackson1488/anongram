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

/**
 * Менеджер камеры Camera2 и записи круглых видеосообщений.
 * Обеспечивает живое превью 1:1 в круглом видоискателе и запись H.264/AAC в файл MP4.
 */
class VideoCircleCameraManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentVideoFile: File? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    var isFrontCamera: Boolean = true
        private set

    var isFlashOn: Boolean = false
        private set

    var isRecording: Boolean = false
        private set

    private var surfaceTexture: SurfaceTexture? = null

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("VideoCircleCameraThread").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
        } catch (_: Exception) {}
        backgroundThread = null
        backgroundHandler = null
    }

    @SuppressLint("MissingPermission")
    fun startPreview(surfaceTexture: SurfaceTexture, front: Boolean = true) {
        this.surfaceTexture = surfaceTexture
        this.isFrontCamera = front
        startBackgroundThread()

        try {
            closeCamera()

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (front) facing == CameraCharacteristics.LENS_FACING_FRONT
                else facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return

            surfaceTexture.setDefaultBufferSize(720, 720)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (_: Exception) {}
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
        surfaceTexture?.let {
            startPreview(it, front = !isFrontCamera)
        }
    }

    fun toggleFlash() {
        if (isFrontCamera) return
        isFlashOn = !isFlashOn
        createPreviewSession()
    }

    @SuppressLint("MissingPermission")
    fun startRecording(): File? {
        val camera = cameraDevice ?: return null
        val st = surfaceTexture ?: return null

        try {
            val file = File(context.cacheDir, "video_circle_${System.currentTimeMillis()}.mp4")
            currentVideoFile = file

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            fun configureRecorder(recorder: MediaRecorder, size: Int, bitRate: Int) {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(file.absolutePath)
                    setVideoEncodingBitRate(bitRate)
                    setVideoFrameRate(30)
                    setVideoSize(size, size)
                    setOrientationHint(if (isFrontCamera) 270 else 90)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(64000)
                }
            }

            try {
                configureRecorder(mr, 720, 2_000_000)
                mr.prepare()
            } catch (_: Exception) {
                mr.reset()
                configureRecorder(mr, 480, 1_200_000)
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

            return file
        } catch (_: Exception) {
            isRecording = false
            return null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        isRecording = false

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
        }

        val file = currentVideoFile
        currentVideoFile = null
        createPreviewSession()
        return if (file != null && file.exists() && file.length() > 0) file else null
    }

    fun cancelRecording() {
        if (isRecording) {
            isRecording = false
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (_: Exception) {
            } finally {
                mediaRecorder = null
            }
        }
        currentVideoFile?.delete()
        currentVideoFile = null
        createPreviewSession()
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (_: Exception) {}
    }

    fun release() {
        cancelRecording()
        closeCamera()
        stopBackgroundThread()
    }
}
