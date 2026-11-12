package com.anongram.app.features.chats.viewer.video

import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.features.chats.viewer.i18n.ViewerI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Stable
internal class ViewerVideoPlaybackState {
    var currentPositionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var isPlaying by mutableStateOf(false)
    internal var onSeekRequest: ((Long) -> Unit)? = null

    fun seekTo(positionMs: Long) {
        onSeekRequest?.invoke(positionMs)
    }
}

/**
 * Полноэкранная страница видеоплеера в стиле Telegram (подпапка `viewer/video/`):
 * - TextureView + MediaPlayer (совместим со свайпом вверх/вниз и прозрачностью без артефактов)
 * - Адаптивные пропорции под вертикальное и горизонтальное видео + кнопка `[ ]`
 * - По центру только Пауза/Плей (без кнопок назад/вперёд), появляется по тапу
 * - Жесты как в Telegram:
 *   - двойной тап слева/справа: перемотка ±5с с накоплением (5, 10, 15...)
 *   - удержание справа: ускорение 2x >> до отпускания
 *   - удержание слева: пошаговая перемотка назад со счётчиком секунд (1, 2, 3...) до отпускания
 */
@Composable
internal fun ViewerVideoPage(
    uriString: String,
    isActivePage: Boolean,
    showControls: Boolean,
    playbackSpeed: Float,
    isLooping: Boolean,
    playbackState: ViewerVideoPlaybackState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = ViewerI18n.current
    val parsedUri = remember(uriString) { runCatching { Uri.parse(uriString) }.getOrNull() }

    var videoAspect by remember(uriString) { mutableFloatStateOf(16f / 9f) }
    var isFillExpanded by remember(uriString) { mutableStateOf(false) }

    var mediaPlayer by remember(uriString) { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember(uriString) { mutableStateOf(false) }
    var isPlaying by remember(uriString) { mutableStateOf(false) }

    // Жестовые состояния Telegram
    var doubleTapSide by remember { mutableIntStateOf(0) } // -1 = left, +1 = right, 0 = none
    var doubleTapAccumSec by remember { mutableIntStateOf(0) }
    var doubleTapBumpToken by remember { mutableIntStateOf(0) }

    var holdMode by remember { mutableIntStateOf(0) } // -1 = left rewind, +1 = right 2x, 0 = none
    var rewindHoldSec by remember { mutableIntStateOf(0) }

    // Определение реальных пропорций видео (учитывая rotation)
    LaunchedEffect(parsedUri) {
        if (parsedUri != null) {
            val aspect = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, parsedUri)
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    if (w > 0 && h > 0) {
                        val realW = if (rot == 90 || rot == 270) h else w
                        val realH = if (rot == 90 || rot == 270) w else h
                        (realW.toFloat() / realH.toFloat()).coerceIn(0.35f, 3.0f)
                    } else 16f / 9f
                } catch (_: Exception) {
                    16f / 9f
                } finally {
                    runCatching { retriever.release() }
                }
            }
            videoAspect = aspect
        }
    }

    // Сброс накопленных секунд двойного тапа через 900 мс после последнего тапа
    LaunchedEffect(doubleTapBumpToken) {
        if (doubleTapBumpToken > 0) {
            delay(900)
            doubleTapSide = 0
            doubleTapAccumSec = 0
        }
    }

    // Пошаговая перемотка назад при удержании слева (-1с каждые 300мс)
    LaunchedEffect(holdMode, isPrepared) {
        if (holdMode == -1 && isPrepared) {
            rewindHoldSec = 0
            val player = mediaPlayer ?: return@LaunchedEffect
            val wasPlaying = player.isPlaying
            if (wasPlaying) {
                runCatching { player.pause() }
            }
            while (isActive && holdMode == -1) {
                delay(300)
                if (holdMode != -1) break
                rewindHoldSec += 1
                val cur = runCatching { player.currentPosition }.getOrDefault(0)
                val target = (cur - 1000).coerceAtLeast(0)
                runCatching { player.seekTo(target) }
                playbackState.currentPositionMs = target.toLong()
            }
            if (isActivePage && isPlaying) {
                runCatching { player.start() }
            }
        }
    }

    // Применение скорости и зацикливания
    LaunchedEffect(playbackSpeed, isLooping, holdMode, isPrepared, mediaPlayer) {
        val player = mediaPlayer ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        runCatching {
            player.isLooping = isLooping
        }
        val effectiveSpeed = if (holdMode == 1) 2.0f else playbackSpeed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val params = (player.playbackParams ?: PlaybackParams()).setSpeed(effectiveSpeed)
                player.playbackParams = params
            }
        }
    }

    // Автопауза / автостарт при перелистывании страниц HorizontalPager
    LaunchedEffect(isActivePage, isPrepared, mediaPlayer) {
        val player = mediaPlayer ?: return@LaunchedEffect
        if (!isPrepared) return@LaunchedEffect
        if (isActivePage) {
            if (!player.isPlaying) {
                runCatching { player.start() }
            }
            isPlaying = true
        } else {
            if (player.isPlaying) {
                runCatching { player.pause() }
            }
            isPlaying = false
        }
    }

    // Обновление прогресса воспроизведения
    LaunchedEffect(isActivePage, isPrepared, mediaPlayer) {
        if (isActivePage) {
            playbackState.onSeekRequest = { targetMs ->
                val player = mediaPlayer
                if (player != null && isPrepared) {
                    val clamped = targetMs.toInt().coerceIn(0, playbackState.durationMs.toInt().coerceAtLeast(0))
                    runCatching { player.seekTo(clamped) }
                    playbackState.currentPositionMs = clamped.toLong()
                }
            }
        }
        while (isActive && isActivePage) {
            val player = mediaPlayer
            if (player != null && isPrepared) {
                val pos = runCatching { player.currentPosition.toLong() }.getOrDefault(playbackState.currentPositionMs)
                val dur = runCatching { player.duration.toLong() }.getOrDefault(playbackState.durationMs)
                val playingNow = runCatching { player.isPlaying }.getOrDefault(false)
                playbackState.currentPositionMs = pos
                if (dur > 0) playbackState.durationMs = dur
                playbackState.isPlaying = playingNow
            }
            delay(120)
        }
    }

    DisposableEffect(uriString) {
        onDispose {
            runCatching {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
            mediaPlayer = null
            isPrepared = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        if (player.isPlaying) {
            runCatching { player.pause() }
            isPlaying = false
            playbackState.isPlaying = false
        } else {
            runCatching { player.start() }
            isPlaying = true
            playbackState.isPlaying = true
        }
    }

    fun performDoubleTapSeek(side: Int) {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        val deltaSec = if (side > 0) 5 else -5
        doubleTapAccumSec = if (doubleTapSide == side) doubleTapAccumSec + 5 else 5
        doubleTapSide = side
        doubleTapBumpToken += 1

        val dur = runCatching { player.duration }.getOrDefault(0).coerceAtLeast(0)
        val cur = runCatching { player.currentPosition }.getOrDefault(0)
        val target = (cur + deltaSec * 1000).coerceIn(0, dur)
        runCatching { player.seekTo(target) }
        playbackState.currentPositionMs = target.toLong()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uriString, isPrepared) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tapOffset ->
                        val isRight = tapOffset.x >= size.width / 2f
                        performDoubleTapSeek(if (isRight) 1 else -1)
                    },
                    onLongPress = { pressOffset ->
                        val isRight = pressOffset.x >= size.width / 2f
                        holdMode = if (isRight) 1 else -1
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (holdMode != 0) {
                            holdMode = 0
                            rewindHoldSec = 0
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Видео-поверхность (адаптивная под пропорции видео)
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val containerAspect = if (maxHeight > 0.dp) maxWidth / maxHeight else 0.56f
            val videoBoxModifier = if (isFillExpanded) {
                Modifier.fillMaxSize()
            } else if (videoAspect >= containerAspect) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoAspect)
            } else {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(videoAspect)
            }

            Box(modifier = videoBoxModifier) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                    if (parsedUri == null) return
                                    val surface = Surface(st)
                                    val mp = MediaPlayer()
                                    runCatching {
                                        mp.setDataSource(ctx, parsedUri)
                                        mp.setSurface(surface)
                                        mp.isLooping = isLooping
                                        mp.setOnPreparedListener { preparedMp ->
                                            val vw = preparedMp.videoWidth
                                            val vh = preparedMp.videoHeight
                                            if (vw > 0 && vh > 0) {
                                                videoAspect = (vw.toFloat() / vh.toFloat()).coerceIn(0.35f, 3.0f)
                                            }
                                            playbackState.durationMs = preparedMp.duration.toLong().coerceAtLeast(0L)
                                            isPrepared = true
                                            mediaPlayer = preparedMp
                                            if (isActivePage) {
                                                preparedMp.start()
                                                isPlaying = true
                                                playbackState.isPlaying = true
                                            }
                                        }
                                        mp.setOnCompletionListener {
                                            if (!isLooping) {
                                                isPlaying = false
                                                playbackState.isPlaying = false
                                            }
                                        }
                                        mp.prepareAsync()
                                    }
                                }

                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit

                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    runCatching {
                                        mediaPlayer?.stop()
                                        mediaPlayer?.release()
                                    }
                                    mediaPlayer = null
                                    isPrepared = false
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Кнопка разворота [ ] в правом нижнем углу самого видео (как на скрине 2)
                AnimatedVisibility(
                    visible = showControls && holdMode == 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = { isFillExpanded = !isFillExpanded },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.42f))
                    ) {
                        Icon(
                            imageVector = if (isFillExpanded) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                            contentDescription = strings.toggleFill,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Центральная кнопка Пауза / Плей (только по тапу, без кнопок назад/вперёд)
        AnimatedVisibility(
            visible = showControls && holdMode == 0 && doubleTapSide == 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { togglePlayPause() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) strings.pause else strings.play,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Индикатор двойного тапа (±5, ±10, ±15...)
        AnimatedVisibility(
            visible = doubleTapSide != 0 && doubleTapAccumSec > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(if (doubleTapSide < 0) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (doubleTapSide < 0) {
                    Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        text = "-${doubleTapAccumSec} ${strings.secondsUnit}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "+${doubleTapAccumSec} ${strings.secondsUnit}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Индикатор удержания: справа 2x >>, слева пошаговая перемотка назад -1, -2, -3...
        AnimatedVisibility(
            visible = holdMode != 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (holdMode > 0) {
                    Text(
                        text = "2x",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "-${rewindHoldSec} ${strings.secondsUnit}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
