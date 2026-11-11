package com.anongram.app.features.chats.media.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import com.anongram.app.features.player.service.AudioPlayerService
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
 * Изолированный контроллер воспроизведения музыки и цепочки голосовых сообщений чата AnonGram.
 */
class ChatAudioController private constructor(
    private val appContext: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val _state = MutableStateFlow(ChatPlayerState())
    val state: StateFlow<ChatPlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var originalChronologicalQueue: List<ChatTrack> = emptyList()
    private var userPaused: Boolean = false
    val isUserPaused: Boolean get() = userPaused

    // Поверхность TextureView раскрытого видео-кружочка (видео рисуется прямо в пузыре чата)
    private var videoSurface: Surface? = null
    private var videoSurfaceTrackId: String? = null

    /**
     * Видео-кружок отдаёт свою поверхность плееру: звук, шторка Android и картинка идут из одного MediaPlayer.
     */
    fun attachVideoSurface(trackId: String, surface: Surface) {
        videoSurface = surface
        videoSurfaceTrackId = trackId
        if (_state.value.currentTrack?.id == trackId) {
            runCatching { mediaPlayer?.setSurface(surface) }
        }
    }

    fun detachVideoSurface(surface: Surface) {
        if (videoSurface === surface) {
            if (_state.value.currentTrack?.id == videoSurfaceTrackId) {
                runCatching { mediaPlayer?.setSurface(null) }
            }
            videoSurface = null
            videoSurfaceTrackId = null
        }
    }

    /** Остановить и свернуть видео-кружок (тап мимо круга / окончание видео) */
    fun stopVideoCircle() {
        if (_state.value.currentTrack?.isVideoCircle != true) return
        userPaused = true
        releasePlayer()
        _state.update { it.copy(currentTrack = null, queue = emptyList(), isPlaying = false, isVisible = false, positionMs = 0L) }
    }

    fun play(queue: List<ChatTrack>, track: ChatTrack, autoStart: Boolean = true) {
        if (queue.isEmpty()) return

        val effectiveQueue = if (_state.value.isShuffleEnabled) {
            if (originalChronologicalQueue.isEmpty() || originalChronologicalQueue != queue) {
                originalChronologicalQueue = queue
            }
            if (queue != _state.value.queue) {
                listOf(track) + (queue.filter { it.id != track.id }.shuffled())
            } else {
                queue
            }
        } else {
            originalChronologicalQueue = queue
            queue
        }

        userPaused = !autoStart
        _state.update {
            it.copy(
                queue = effectiveQueue,
                currentTrack = track,
                durationMs = track.durationMs,
                positionMs = 0L,
                isPlaying = autoStart,
                isVisible = !track.isVoice
            )
        }

        startPlayback(track, autoStart = autoStart)
        AudioPlayerService.start(appContext)
    }

    /**
     * Воспроизведение цепочки голосовых сообщений (ТЗ: непрерывный автопереход к следующему ГС вниз по чату).
     */
    fun playVoiceSequence(queue: List<ChatTrack>, startTrack: ChatTrack) {
        if (queue.isEmpty()) return

        userPaused = false
        _state.update {
            it.copy(
                queue = queue,
                currentTrack = startTrack,
                durationMs = startTrack.durationMs,
                positionMs = 0L,
                isPlaying = true,
                isVisible = false // Плашка мини-плеера не перекрывает чат при прослушивании ГС
            )
        }

        startPlayback(startTrack, autoStart = true)
        AudioPlayerService.start(appContext)
    }

    private fun startPlayback(track: ChatTrack, autoStart: Boolean = true) {
        releasePlayer()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(if (track.isVoice && !track.isVideoCircle) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (track.isVideoCircle && videoSurfaceTrackId == track.id) {
                    videoSurface?.let { runCatching { setSurface(it) } }
                }
                if (track.uri.scheme == "content" || track.uri.scheme == "file") {
                    setDataSource(appContext, track.uri)
                } else if (track.uri.toString().isNotBlank()) {
                    setDataSource(track.uri.toString())
                }
                setOnPreparedListener { mp ->
                    val actualDur = if (track.durationMs > 0) track.durationMs else mp.duration.toLong().coerceAtLeast(1000L)
                    if (autoStart) {
                        mp.start()
                        _state.update {
                            it.copy(
                                isPlaying = true,
                                durationMs = actualDur
                            )
                        }
                        startProgressTracker()
                    } else {
                        _state.update {
                            it.copy(
                                isPlaying = false,
                                positionMs = 0L,
                                durationMs = actualDur
                            )
                        }
                    }
                }
                setOnCompletionListener {
                    handleTrackCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    simulatePlayback(track, autoStart = autoStart)
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            simulatePlayback(track, autoStart = autoStart)
        }
    }

    private fun simulatePlayback(track: ChatTrack, autoStart: Boolean = true) {
        val simulatedDur = if (track.durationMs > 0) track.durationMs else 15_000L
        _state.update { it.copy(isPlaying = autoStart, durationMs = simulatedDur, positionMs = 0L) }
        if (autoStart) {
            startProgressTracker()
        }
    }

    fun pause() {
        userPaused = true
        mediaPlayer?.pause()
        _state.update { it.copy(isPlaying = false) }
        stopProgressTracker()
    }

    fun resume() {
        userPaused = false
        val current = _state.value
        if (current.currentTrack == null && current.queue.isNotEmpty()) {
            play(current.queue, current.queue.first(), autoStart = true)
            return
        }
        mediaPlayer?.start()
        _state.update { it.copy(isPlaying = true) }
        startProgressTracker()
    }

    fun toggle() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun togglePlayPause() = toggle()

    fun seek(ms: Long) {
        val targetMs = ms.coerceIn(0L, _state.value.durationMs.coerceAtLeast(1L))
        try {
            mediaPlayer?.seekTo(targetMs.toInt())
        } catch (_: Throwable) {}
        _state.update { it.copy(positionMs = targetMs) }
    }

    fun next(autoStart: Boolean = _state.value.isPlaying) {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        if (currentIndex < 0) {
            play(queue, queue.first(), autoStart = autoStart)
            return
        }
        val nextIndex = currentIndex + 1
        if (nextIndex < queue.size) {
            play(queue, queue[nextIndex], autoStart = autoStart)
        } else if (_state.value.repeatMode == ChatRepeatMode.ALL) {
            play(queue, queue[0], autoStart = autoStart)
        } else if (_state.value.repeatMode == ChatRepeatMode.ONE) {
            seek(0L)
            if (autoStart) mediaPlayer?.start()
        }
    }

    fun prev(autoStart: Boolean = _state.value.isPlaying) {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        if (currentIndex <= 0) {
            if (_state.value.repeatMode == ChatRepeatMode.ALL) {
                play(queue, queue.last(), autoStart = autoStart)
            } else {
                seek(0L)
                if (autoStart) mediaPlayer?.start()
            }
        } else {
            play(queue, queue[currentIndex - 1], autoStart = autoStart)
        }
    }

    fun toggleShuffle() {
        val current = _state.value
        val curTrack = current.currentTrack
        if (current.isShuffleEnabled) {
            val restored = if (originalChronologicalQueue.isNotEmpty()) originalChronologicalQueue else current.queue
            _state.update {
                it.copy(
                    isShuffleEnabled = false,
                    queue = restored
                )
            }
        } else {
            if (originalChronologicalQueue.isEmpty()) {
                originalChronologicalQueue = current.queue
            }
            val others = current.queue.filter { it.id != curTrack?.id }.shuffled()
            val shuffled = if (curTrack != null) listOf(curTrack) + others else current.queue.shuffled()
            _state.update {
                it.copy(
                    isShuffleEnabled = true,
                    queue = shuffled
                )
            }
        }
    }

    fun setRepeat(mode: ChatRepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
    }

    fun cycleRepeat() {
        val next = when (_state.value.repeatMode) {
            ChatRepeatMode.OFF -> ChatRepeatMode.ALL
            ChatRepeatMode.ALL -> ChatRepeatMode.ONE
            ChatRepeatMode.ONE -> ChatRepeatMode.OFF
        }
        setRepeat(next)
    }

    fun stop() {
        if (_state.value.currentTrack?.isVideoCircle == true) {
            stopVideoCircle()
            return
        }
        userPaused = true
        releasePlayer()
        _state.update { it.copy(isPlaying = false, isVisible = false, positionMs = 0L) }
    }

    fun openSheet() {
        _state.update { it.copy(isSheetOpen = true) }
    }

    fun closeSheet() {
        _state.update { it.copy(isSheetOpen = false) }
    }

    fun openFullScreen() {
        _state.update { it.copy(isFullScreenOpen = true, isSheetOpen = false) }
    }

    fun closeFullScreen() {
        _state.update { it.copy(isFullScreenOpen = false) }
    }

    private fun handleTrackCompletion() {
        val currentTrack = _state.value.currentTrack
        if (currentTrack?.isVideoCircle == true) {
            // Видео-кружок проигрывается один раз и сворачивается обратно
            stopVideoCircle()
            return
        }
        if (currentTrack?.isVoice == true) {
            // Для цепочки ГС: непрерывный автопереход к следующему ГС в очереди
            val queue = _state.value.queue
            val currentIndex = queue.indexOfFirst { it.id == currentTrack.id }
            if (currentIndex in 0 until queue.lastIndex) {
                playVoiceSequence(queue, queue[currentIndex + 1])
            } else {
                stop()
            }
            return
        }

        // Для музыки: автозапуск ПОЛНОСТЬЮ отключен (только ручной старт по требованию пользователя)
        if (_state.value.repeatMode == ChatRepeatMode.ONE) {
            seek(0L)
            mediaPlayer?.start()
            _state.update { it.copy(isPlaying = true) }
        } else {
            userPaused = true
            mediaPlayer?.pause()
            stopProgressTracker()
            _state.update { it.copy(isPlaying = false, positionMs = 0L) }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                delay(200)
                val currentPos = try {
                    mediaPlayer?.currentPosition?.toLong()
                } catch (_: Throwable) { null } ?: (_state.value.positionMs + 200)

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
        private var instance: ChatAudioController? = null

        fun getInstance(context: Context): ChatAudioController =
            instance ?: synchronized(this) {
                instance ?: ChatAudioController(context.applicationContext).also { instance = it }
            }
    }
}
