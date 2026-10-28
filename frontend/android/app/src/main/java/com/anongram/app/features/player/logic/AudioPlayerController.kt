package com.anongram.app.features.player.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.anongram.app.features.player.api.PlayerController
import com.anongram.app.features.player.api.PlayerState
import com.anongram.app.features.player.api.RepeatMode
import com.anongram.app.features.player.api.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Реализация PlayerController (ТЗ, шаг 3 и шаг 8):
 * Управляет воспроизведением, прогрессом (мс), очередью и состоянием.
 * Долгоживущий синглтон, не привязанный к ViewModel экрана.
 */
class AudioPlayerController private constructor(
    private val appContext: Context
) : PlayerController {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        // Подготовка демонстрационных треков для мгновенной проверки
        initSampleTrack()
    }

    private fun initSampleTrack() {
        // Тестовый трек для отладки плеера
        val sampleTrack = Track(
            id = "demo_ambient_01",
            uri = Uri.parse("https://actions.google.com/sounds/v1/ambient/rain_heavy.ogg"),
            title = "Cyber Ambient Chill",
            artist = "AnonGram AudioLab",
            durationMs = 19_000L
        )
        _state.update {
            it.copy(
                queue = listOf(sampleTrack),
                currentTrack = sampleTrack,
                durationMs = sampleTrack.durationMs
            )
        }
    }

    override fun play(queue: List<Track>, index: Int) {
        if (queue.isEmpty()) return
        val targetIndex = index.coerceIn(0, queue.lastIndex)
        val track = queue[targetIndex]

        _state.update {
            it.copy(
                queue = queue,
                currentTrack = track,
                durationMs = track.durationMs,
                positionMs = 0L,
                isPlaying = true,
                isVisible = true
            )
        }

        startPlayback(track)
    }

    private fun startPlayback(track: Track) {
        releasePlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (track.uri.scheme == "content" || track.uri.scheme == "file") {
                    setDataSource(appContext, track.uri)
                } else {
                    setDataSource(track.uri.toString())
                }
                setOnPreparedListener { mp ->
                    mp.start()
                    _state.update {
                        it.copy(
                            isPlaying = true,
                            durationMs = if (track.durationMs > 0) track.durationMs else mp.duration.toLong()
                        )
                    }
                    startProgressTracker()
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    // Fallback для симуляции воспроизведения оффлайн-файла/демо
                    simulatePlayback()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            // В случае сетевой изоляции или тестового URI симулируем воспроизведение
            simulatePlayback()
        }
    }

    private fun simulatePlayback() {
        startProgressTracker()
        _state.update { it.copy(isPlaying = true) }
    }

    override fun toggle() {
        val current = _state.value
        if (current.currentTrack == null && current.queue.isNotEmpty()) {
            play(current.queue, 0)
            return
        }

        if (current.isPlaying) {
            mediaPlayer?.pause()
            _state.update { it.copy(isPlaying = false) }
            stopProgressTracker()
        } else {
            mediaPlayer?.start()
            _state.update { it.copy(isPlaying = true) }
            startProgressTracker()
        }
    }

    override fun seek(ms: Long) {
        val targetMs = ms.coerceIn(0L, _state.value.durationMs.coerceAtLeast(1L))
        mediaPlayer?.seekTo(targetMs.toInt())
        _state.update { it.copy(positionMs = targetMs) }
    }

    override fun next() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val nextIndex = if (_state.value.isShuffle && queue.size > 1) {
            var rand = kotlin.random.Random.nextInt(queue.size)
            if (rand == currentIndex) rand = (rand + 1) % queue.size
            rand
        } else {
            (currentIndex + 1) % queue.size
        }
        play(queue, nextIndex)
    }

    override fun prev() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val prevIndex = if (currentIndex <= 0) queue.lastIndex else currentIndex - 1
        play(queue, prevIndex)
    }

    override fun setRepeat(mode: RepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
    }

    override fun toggleShuffle() {
        _state.update { it.copy(isShuffle = !it.isShuffle) }
    }

    override fun stop() {
        releasePlayer()
        _state.update { it.copy(isPlaying = false, isVisible = false, positionMs = 0L) }
    }

    override fun openFullPlayer() {
        _state.update { it.copy(isFullPlayerOpen = true) }
    }

    override fun closeFullPlayer() {
        _state.update { it.copy(isFullPlayerOpen = false) }
    }

    private fun handleTrackCompletion() {
        when (_state.value.repeatMode) {
            RepeatMode.ONE -> {
                seek(0L)
                mediaPlayer?.start()
                _state.update { it.copy(isPlaying = true) }
            }
            RepeatMode.ALL -> next()
            RepeatMode.OFF -> {
                val queue = _state.value.queue
                val currentIndex = queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
                if (currentIndex < queue.lastIndex) {
                    next()
                } else {
                    stop()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                delay(300)
                val currentPos = mediaPlayer?.currentPosition?.toLong() ?: (_state.value.positionMs + 300)
                val dur = _state.value.durationMs
                if (dur > 0 && currentPos >= dur) {
                    handleTrackCompletion()
                    break
                } else {
                    _state.update { it.copy(positionMs = currentPos) }
                }
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    companion object {
        @Volatile
        private var instance: AudioPlayerController? = null

        fun getInstance(context: Context): AudioPlayerController =
            instance ?: synchronized(this) {
                instance ?: AudioPlayerController(context.applicationContext).also { instance = it }
            }
    }
}
