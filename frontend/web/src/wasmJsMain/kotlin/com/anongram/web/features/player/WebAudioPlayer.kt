package com.anongram.web.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationLabel: String
)

object WebAudioPlayer {
    private val _track = MutableStateFlow<PlayerTrack?>(null)
    val track: StateFlow<PlayerTrack?> = _track.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    fun playOrToggle(item: PlayerTrack) {
        if (_track.value?.id == item.id) {
            _isPlaying.value = !_isPlaying.value
        } else {
            _track.value = item
            _progress.value = 0f
            _isPlaying.value = true
        }
    }

    fun togglePlayPause() {
        if (_track.value != null) {
            _isPlaying.value = !_isPlaying.value
        }
    }

    fun tick() {
        if (_isPlaying.value) {
            val next = _progress.value + 0.015f
            if (next >= 1f) {
                _progress.value = 0f
                _isPlaying.value = false
            } else {
                _progress.value = next
            }
        }
    }

    fun close() {
        _isPlaying.value = false
        _progress.value = 0f
        _track.value = null
    }
}

@Composable
fun MiniAudioPlayerBar(modifier: Modifier = Modifier) {
    val scheme = LocalAnonGramScheme.current
    val track by WebAudioPlayer.track.collectAsState()
    val isPlaying by WebAudioPlayer.isPlaying.collectAsState()
    val progress by WebAudioPlayer.progress.collectAsState()

    LaunchedEffect(track?.id, isPlaying) {
        while (track != null && isPlaying) {
            delay(150)
            WebAudioPlayer.tick()
        }
    }

    AnimatedVisibility(
        visible = track != null,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        val current = track ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = scheme.islandBg,
            border = BorderStroke(1.dp, scheme.islandBorder)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable { WebAudioPlayer.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = scheme.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = current.title,
                                color = scheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${current.subtitle} · ${current.durationLabel}",
                            color = scheme.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { WebAudioPlayer.close() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = scheme.accent,
                    trackColor = scheme.divider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                )
            }
        }
    }
}
