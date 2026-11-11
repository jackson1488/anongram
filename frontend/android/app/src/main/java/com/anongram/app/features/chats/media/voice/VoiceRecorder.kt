package com.anongram.app.features.chats.media.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Результат завершённой аудиозаписи голосового сообщения.
 */
data class VoiceRecordResult(
    val file: File,
    val durationLabel: String,
    val durationMs: Long,
    val waveform: List<Int>
)

/**
 * Менеджер реальной записи голосовых сообщений через MediaRecorder.
 * Поддерживает мгновенный старт на фоновом потоке (без фриза UI),
 * захват микрофона, вычисление живой волны (maxAmplitude),
 * корректное завершение и очистку временных файлов.
 */
class VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val executor = Executors.newSingleThreadExecutor()
    private val lock = Any()
    @Volatile
    private var cancelledPending: Boolean = false
    private var startFuture: Future<*>? = null

    private val _liveWaveform = MutableStateFlow<List<Int>>(emptyList())
    val liveWaveform: StateFlow<List<Int>> = _liveWaveform.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun start(context: Context): Boolean {
        if (_isRecording.value) return false

        val appContext = context.applicationContext
        val file = File(appContext.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        cancelledPending = false
        currentFile = file
        startTimeMs = System.currentTimeMillis()
        _isRecording.value = true
        _liveWaveform.value = emptyList()

        startFuture = executor.submit {
            synchronized(lock) {
                if (cancelledPending || !_isRecording.value) {
                    file.delete()
                    return@synchronized
                }
                try {
                    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(appContext)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    mr.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(64000)
                        setAudioSamplingRate(44100)
                        setOutputFile(file.absolutePath)
                        prepare()
                        start()
                    }
                    if (cancelledPending) {
                        runCatching { mr.stop() }
                        runCatching { mr.release() }
                        file.delete()
                    } else {
                        recorder = mr
                    }
                } catch (_: Exception) {
                    runCatching { recorder?.release() }
                    recorder = null
                    file.delete()
                }
            }
        }

        // Сбор амплитуд для построения волны в реальном времени
        amplitudeJob = scope.launch {
            val samples = mutableListOf<Int>()
            while (isActive && _isRecording.value) {
                delay(100)
                val rawAmp = try {
                    recorder?.maxAmplitude ?: 0
                } catch (_: Exception) {
                    0
                }
                val normalized = (rawAmp / 32767f * 13f).toInt().coerceIn(0, 13) + 2
                samples.add(normalized)
                _liveWaveform.value = samples.toList()
            }
        }

        return true
    }

    fun stop(): VoiceRecordResult? {
        if (!_isRecording.value) return null
        _isRecording.value = false
        cancelledPending = true
        amplitudeJob?.cancel()
        amplitudeJob = null

        runCatching { startFuture?.get(500, TimeUnit.MILLISECONDS) }
        startFuture = null

        val file: File?
        val elapsedMs = max(0L, System.currentTimeMillis() - startTimeMs)
        synchronized(lock) {
            file = currentFile
            currentFile = null
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {
            } finally {
                recorder = null
            }
        }

        if (file == null || !file.exists() || file.length() == 0L || elapsedMs < 300) {
            file?.delete()
            return null
        }

        val totalSec = elapsedMs / 1000
        val durationLabel = "%02d:%02d".format(totalSec / 60, totalSec % 60)
        val finalWaveform = if (_liveWaveform.value.isNotEmpty()) {
            _liveWaveform.value
        } else {
            List(20) { 4 }
        }

        return VoiceRecordResult(
            file = file,
            durationLabel = durationLabel,
            durationMs = elapsedMs,
            waveform = finalWaveform
        )
    }

    fun cancel() {
        _isRecording.value = false
        cancelledPending = true
        amplitudeJob?.cancel()
        amplitudeJob = null

        val pending = startFuture
        startFuture = null
        executor.execute {
            runCatching { pending?.get(500, TimeUnit.MILLISECONDS) }
            synchronized(lock) {
                try {
                    recorder?.stop()
                    recorder?.release()
                } catch (_: Exception) {
                } finally {
                    recorder = null
                }
                currentFile?.delete()
                currentFile = null
            }
        }
        _liveWaveform.value = emptyList()
    }
}
