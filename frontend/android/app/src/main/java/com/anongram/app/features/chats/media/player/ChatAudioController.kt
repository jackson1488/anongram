package com.anongram.app.features.chats.media.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
 * Изолированный контроллер воспроизведения музыки чата AnonGram.
 */
class ChatAudioController private constructor(
    private val appContext: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val _state = MutableStateFlow(ChatPlayerState())
    val state: StateFlow<ChatPlayerState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    fun play(queue: List<ChatTrack>, track: ChatTrack) {
        if (queue.isEmpty()) return

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
        AudioPlayerService.start(appContext)
    }

    private fun startPlayback(track: ChatTrack) {
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
                    simulatePlayback()
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            simulatePlayback()
        }
    }

    private fun simulatePlayback() {
        startProgressTracker()
        _state.update { it.copy(isPlaying = true) }
    }

    fun toggle() {
        val current = _state.value
        if (current.currentTrack == null && current.queue.isNotEmpty()) {
            play(current.queue, current.queue.first())
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

    fun togglePlayPause() = toggle()

    fun seek(ms: Long) {
        val targetMs = ms.coerceIn(0L, _state.value.durationMs.coerceAtLeast(1L))
        mediaPlayer?.seekTo(targetMs.toInt())
        _state.update { it.copy(positionMs = targetMs) }
    }

    fun next() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val nextIndex = (currentIndex + 1) % queue.size
        play(queue, queue[nextIndex])
    }

    fun prev() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _state.value.currentTrack?.id }
        val prevIndex = if (currentIndex <= 0) queue.lastIndex else currentIndex - 1
        play(queue, queue[prevIndex])
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
        releasePlayer()
        _state.update { it.copy(isPlaying = false, isVisible = false, positionMs = 0L) }
    }

    fun openSheet() {
        _state.update { it.copy(isSheetOpen = true) }
    }

    fun closeSheet() {
        _state.update { it.copy(isSheetOpen = false) }
    }

    private fun handleTrackCompletion() {
        when (_state.value.repeatMode) {
            ChatRepeatMode.ONE -> {
                seek(0L)
                mediaPlayer?.start()
                _state.update { it.copy(isPlaying = true) }
            }
            ChatRepeatMode.ALL -> next()
            ChatRepeatMode.OFF -> {
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
        private var instance: ChatAudioController? = null

        fun getInstance(context: Context): ChatAudioController =
            instance ?: synchronized(this) {
                instance ?: ChatAudioController(context.applicationContext).also { instance = it }
            }
    }
}
