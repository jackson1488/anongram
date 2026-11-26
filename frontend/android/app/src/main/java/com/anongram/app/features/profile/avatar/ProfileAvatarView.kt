package com.anongram.app.features.profile.avatar

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Универсальный рендерер аватара профиля (фото, зацикленное видео до 20с,
 * эмодзи на градиенте или монограмма).
 */
@Composable
fun ProfileAvatarView(
    avatar: ProfileAvatar,
    displayName: String,
    modifier: Modifier = Modifier,
    monogramFontSize: TextUnit = 32.sp,
    emojiFontSize: TextUnit = 40.sp,
    playVideo: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current

    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        when (avatar) {
            is ProfileAvatar.None -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(colors = listOf(scheme.islandBg, scheme.card))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        color = scheme.accent,
                        fontSize = monogramFontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            is ProfileAvatar.Emoji -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = listOf(avatar.bgStart, avatar.bgEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar.emoji, fontSize = emojiFontSize)
                }
            }

            is ProfileAvatar.Photo -> {
                var bitmap by remember(avatar.uriString) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(avatar.uriString) {
                    bitmap = withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openInputStream(avatar.uri)?.use { stream ->
                                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                                BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                }
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = avatar.zoom * if (avatar.mirrorHorizontal) -1f else 1f
                                scaleY = avatar.zoom
                                translationX = avatar.panX * size.width
                                translationY = avatar.panY * size.height
                                rotationZ = avatar.rotationDegrees.toFloat()
                            }
                    )
                }
            }

            is ProfileAvatar.Video -> {
                var thumbBitmap by remember(avatar.uriString, avatar.thumbnailFrameMs) {
                    mutableStateOf<ImageBitmap?>(null)
                }
                LaunchedEffect(avatar.uriString, avatar.thumbnailFrameMs) {
                    thumbBitmap = withContext(Dispatchers.IO) {
                        val retriever = MediaMetadataRetriever()
                        runCatching {
                            retriever.setDataSource(context, avatar.uri)
                            val frame = retriever.getFrameAtTime(
                                avatar.thumbnailFrameMs * 1000L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            ) ?: retriever.frameAtTime
                            frame?.asImageBitmap()
                        }.getOrNull().also {
                            runCatching { retriever.release() }
                        }
                    }
                }

                val bmp = thumbBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = avatar.zoom * if (avatar.mirrorHorizontal) -1f else 1f
                                scaleY = avatar.zoom
                                translationX = avatar.panX * size.width
                                translationY = avatar.panY * size.height
                                rotationZ = avatar.rotationDegrees.toFloat()
                            }
                    )
                }

                if (playVideo) {
                    var player by remember(avatar.uriString) { mutableStateOf<MediaPlayer?>(null) }
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(
                                        st: android.graphics.SurfaceTexture,
                                        w: Int,
                                        h: Int
                                    ) {
                                        val mp = MediaPlayer().apply {
                                            runCatching {
                                                setDataSource(ctx, avatar.uri)
                                                setSurface(android.view.Surface(st))
                                                setVolume(0f, 0f)
                                                isLooping = true
                                                setOnPreparedListener { prepared ->
                                                    prepared.seekTo(avatar.trimStartMs.toInt())
                                                    prepared.start()
                                                }
                                                prepareAsync()
                                            }
                                        }
                                        player = mp
                                    }

                                    override fun onSurfaceTextureSizeChanged(
                                        st: android.graphics.SurfaceTexture,
                                        w: Int,
                                        h: Int
                                    ) = Unit

                                    override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                        runCatching { player?.release() }
                                        player = null
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) = Unit
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = avatar.zoom * if (avatar.mirrorHorizontal) -1f else 1f
                                scaleY = avatar.zoom
                                translationX = avatar.panX * size.width
                                translationY = avatar.panY * size.height
                                rotationZ = avatar.rotationDegrees.toFloat()
                            }
                    )

                    LaunchedEffect(player, avatar.trimStartMs, avatar.trimEndMs) {
                        val mp = player ?: return@LaunchedEffect
                        val start = avatar.trimStartMs.toInt()
                        val end = avatar.trimEndMs.coerceAtLeast(avatar.trimStartMs + 500L).toInt()
                        while (true) {
                            delay(150)
                            runCatching {
                                if (mp.isPlaying && mp.currentPosition >= end) {
                                    mp.seekTo(start)
                                }
                            }
                        }
                    }

                    DisposableEffect(avatar.uriString) {
                        onDispose {
                            runCatching { player?.release() }
                            player = null
                        }
                    }
                }
            }
        }
    }
}
