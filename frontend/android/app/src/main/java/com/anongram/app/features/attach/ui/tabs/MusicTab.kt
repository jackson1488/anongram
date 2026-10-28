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
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val PurpleAccent = Color(0xFF8B5CF6)

@Composable
fun MusicTab(
    onSelectAudio: (AttachmentDraft.Audio) -> Unit,
    onExpandPlayer: () -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search field
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
        )

        // Mini player right under search field when playing
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
                    .weight(1f)
            ) {
                itemsIndexed(filteredTracks, key = { _, t -> t.id }) { index, track ->
                    val isCurrent = playerState.currentTrack?.id == track.id
                    val isPlaying = isCurrent && playerState.isPlaying

                    MusicTrackRow(
                        track = track,
                        isPlaying = isPlaying,
                        onPlayClick = {
                            if (isCurrent) {
                                controller.toggle()
                            } else {
                                controller.play(filteredTracks, index)
                            }
                        },
                        onRowClick = {
                            onSelectAudio(
                                AttachmentDraft.Audio(
                                    uri = track.uri,
                                    title = track.title,
                                    artist = track.artist,
                                    durationMs = track.durationMs
                                )
                            )
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
    onPlayClick: () -> Unit,
    onRowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onRowClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause round button
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

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist} • ${formatAudioDuration(track.durationMs)}",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatAudioDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}

private fun loadDeviceAudio(context: Context): List<Track> {
    val list = mutableListOf<Track>()
    val contentResolver = context.contentResolver
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    try {
        val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)
        cursor?.use { c ->
            val idIndex = c.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleIndex = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val durationIndex = c.getColumnIndex(MediaStore.Audio.Media.DURATION)

            while (c.moveToNext()) {
                val id = if (idIndex >= 0) c.getLong(idIndex) else 0L
                val title = if (titleIndex >= 0) c.getString(titleIndex) ?: "Без названия" else "Без названия"
                var artist = if (artistIndex >= 0) c.getString(artistIndex) ?: "" else ""
                if (artist.isBlank() || artist == "<unknown>") {
                    artist = "Неизвестный исполнитель"
                }
                val duration = if (durationIndex >= 0) c.getLong(durationIndex) else 0L

                val contentUri = ContentUris.withAppendedId(uri, id)
                list.add(
                    Track(
                        id = id.toString(),
                        uri = contentUri,
                        title = title,
                        artist = artist,
                        durationMs = duration
                    )
                )
            }
        }
    } catch (_: Exception) {}
    return list
}
