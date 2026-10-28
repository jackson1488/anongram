package com.anongram.app.features.attach.ui.tabs

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private val PurpleAccent = Color(0xFF8B5CF6)

@Composable
fun MusicTab(
    onSelectAudio: (AttachmentDraft.Audio) -> Unit,
    onExpandPlayer: () -> Unit,
    onExpandToFullscreen: () -> Unit = {},
    selectedTracks: List<Track> = emptyList(),
    onToggleSelectTrack: (Track) -> Unit = {},
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

    // При закрытии вкладки или окна вложений воспроизведение останавливается (ТЗ, шаг 6)
    DisposableEffect(Unit) {
        onDispose {
            controller.stop()
        }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Поле "Поиск музыки" (ТЗ, шаг 6): тап сразу переводит окно на весь экран
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск музыки", color = Color(0xFF64748B), fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF94A3B8)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            },
            singleLine = true,
            interactionSource = searchInteractionSource,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF131D31),
                unfocusedContainerColor = Color(0xFF131D31),
                focusedBorderColor = PurpleAccent,
                unfocusedBorderColor = Color(0x22FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onExpandToFullscreen()
                    }
                }
        )

        // Мини-плеер СТРОГО под полем поиска (ТЗ, шаг 6): пауза, "Исполнитель - Название", крестик, линия прогресса
        if (playerState.isVisible && playerState.currentTrack != null) {
            Spacer(modifier = Modifier.height(10.dp))
            MiniPlayer(
                onExpand = onExpandPlayer,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 84.dp)
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
        // Play / Pause круглая кнопка
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (isPlaying) PurpleAccent else Color(0xFF1E293B))
                .clickable { onPlayClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = "Play/Pause",
                tint = if (isPlaying) Color.White else PurpleAccent,
                modifier = Modifier.size(26.dp)
            )
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

        Spacer(modifier = Modifier.width(10.dp))

        // Кружок выбора справа (ТЗ, шаг 12)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) PurpleAccent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) PurpleAccent else Color(0x66FFFFFF),
                    shape = CircleShape
                )
                .clickable { onToggleSelect() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Выбран",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
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
