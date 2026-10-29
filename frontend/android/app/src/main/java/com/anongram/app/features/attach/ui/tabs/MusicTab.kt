package com.anongram.app.features.attach.ui.tabs

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.anongram.app.core.ui.safeStatusBarPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.attach.ui.components.PermissionDeniedView
import com.anongram.app.features.player.api.MiniPlayer
import com.anongram.app.features.player.api.Track
import com.anongram.app.features.player.logic.AudioPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachCardDark
import com.anongram.app.features.attach.ui.theme.AttachPillDark
import com.anongram.app.features.attach.ui.theme.AttachSheetBackground
import com.anongram.app.features.attach.ui.theme.AttachSurfaceDark

private val PurpleAccent = Color(0xFF8B5CF6)

@Composable
fun MusicTab(
    onSelectAudio: (AttachmentDraft.Audio) -> Unit,
    onExpandPlayer: () -> Unit,
    onExpandToFullscreen: () -> Unit = {},
    selectedTracks: List<Track> = emptyList(),
    onToggleSelectTrack: (Track) -> Unit = {},
    isFullscreen: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { AudioPlayerController.getInstance(context) }
    val playerState by controller.state.collectAsState()

    var hasPermission by remember {
        mutableStateOf(PermissionUtils.hasMusicPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = PermissionUtils.hasMusicPermission(context)
    }

    if (!hasPermission) {
        PermissionDeniedView(
            message = "Для просмотра и отправки аудиофайлов предоставьте доступ к медиатеке.",
            onRequestPermission = {
                launcher.launch(PermissionUtils.getMusicPermissions())
            },
            modifier = modifier
        )
        return
    }

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        tracks = withContext(Dispatchers.IO) {
            loadDeviceAudio(context)
        }
    }

    val filteredTracks = remember(tracks, searchQuery) {
        if (searchQuery.isBlank()) tracks
        else {
            val q = searchQuery.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
    }

    val searchInteractionSource = remember { MutableInteractionSource() }
    val isPressed by searchInteractionSource.collectIsPressedAsState()
    LaunchedEffect(isPressed) {
        if (isPressed) {
            onExpandToFullscreen()
        }
    }

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isPlayerVisible = playerState.isVisible && playerState.currentTrack != null
    val topOffsetDp = statusBarTopDp + 6.dp
    val headerContentHeight = if (isPlayerVisible) 100.dp else 48.dp
    val totalHeaderHeight = topOffsetDp + headerContentHeight

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Фоновый градиент (scrim) СЗАДИ поиска и мини-плеера (zIndex = 5f)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(totalHeaderHeight + 20.dp)
                .zIndex(5f)
                .background(
                    Brush.verticalGradient(
                        0.0f to AttachSheetBackground,
                        0.8f to AttachSheetBackground.copy(alpha = 0.95f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // Парящая шапка поиска и мини-плеера (zIndex = 10f)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = topOffsetDp
                )
                .zIndex(10f)
        ) {
            var isFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AttachCardDark)
                    .border(1.dp, if (isFocused) PurpleAccent else AttachBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Поиск музыки",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        interactionSource = searchInteractionSource,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(PurpleAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                if (focusState.isFocused && !isFullscreen) {
                                    onExpandToFullscreen()
                                }
                            }
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (isPlayerVisible) {
                Spacer(modifier = Modifier.height(6.dp))
                MiniPlayer(
                    onExpand = onExpandPlayer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Список треков (zIndex = 1f) — скроллится ПОЗАДИ поиска и плеера
        if (filteredTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (tracks.isEmpty()) "Аудиозаписи не найдены" else "Ничего не найдено",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .zIndex(1f),
                contentPadding = PaddingValues(
                    top = totalHeaderHeight + 8.dp,
                    bottom = 84.dp
                )
            ) {
                itemsIndexed(filteredTracks, key = { _, t -> t.id }) { index, track ->
                    val isCurrent = playerState.currentTrack?.id == track.id
                    val isPlaying = isCurrent && playerState.isPlaying
                    val isSelected = selectedTracks.any { it.id == track.id }

                    MusicTrackRow(
                        track = track,
                        isPlaying = isPlaying,
                        isSelected = isSelected,
                        onPlayClick = {
                            if (isCurrent) {
                                controller.toggle()
                            } else {
                                controller.play(filteredTracks, index)
                            }
                        },
                        onToggleSelect = {
                            onToggleSelectTrack(track)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicTrackRow(
    track: Track,
    isPlaying: Boolean,
    isSelected: Boolean,
    onPlayClick: () -> Unit,
    onToggleSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка воспроизведения с оверлеем выбора (не сдвигает название трека)
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (isPlaying) PurpleAccent else AttachSurfaceDark)
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = if (isPlaying) Color.White else PurpleAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Маленькая зеленая галочка в углу проигрывателя
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(1.5.dp, AttachSheetBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Выбран",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) PurpleAccent else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${track.durationLabel}",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun loadDeviceAudio(context: Context): List<Track> {
    val list = mutableListOf<Track>()
    val contentResolver = context.contentResolver

    try {
        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        contentResolver.query(audioUri, projection, selection, null, sortOrder)?.use { c ->
            val idIdx = c.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleIdx = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIdx = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val durIdx = c.getColumnIndex(MediaStore.Audio.Media.DURATION)

            while (c.moveToNext()) {
                val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "Unknown" else "Unknown"
                val artist = if (artistIdx >= 0) c.getString(artistIdx) ?: "Unknown" else "Unknown"
                val duration = if (durIdx >= 0) c.getLong(durIdx) else 0L

                val contentUri = ContentUris.withAppendedId(audioUri, id)
                list.add(
                    Track(
                        id = id.toString(),
                        uri = contentUri,
                        title = title,
                        artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                        durationMs = duration
                    )
                )
            }
        }
    } catch (_: Exception) {}

    return list
}
