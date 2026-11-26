package com.anongram.app.features.profile.avatar

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.camera.api.CameraScreen
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryTab
import com.anongram.app.features.profile.appearance.ProfileColorPresets
import com.anongram.app.features.profile.i18n.ProfileI18n

/**
 * Шторка выбора фотографии, видео или эмодзи для аватара профиля.
 * Наследует готовые [GalleryTab] (живая камера [LiveCameraTile] + кэш галереи)
 * и встроенный [CameraScreen] из модуля вложений.
 */
@Composable
fun ProfileAvatarPickerScreen(
    displayName: String,
    subtitleText: String,
    onPickMedia: (uri: Uri, isVideo: Boolean, durationMs: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val context = LocalContext.current
    val currentAvatar by ProfileAvatarStore.avatar.collectAsState()

    var isCameraOpen by remember { mutableStateOf(false) }
    var emojiSheetOpen by remember { mutableStateOf(false) }

    BackHandler {
        when {
            isCameraOpen -> isCameraOpen = false
            emojiSheetOpen -> emojiSheetOpen = false
            else -> onBack()
        }
    }

    AnonGramScreenSurface(
        applySystemBarsPadding = false,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeStatusBarPadding()
                    .safeNavigationBarPadding()
            ) {
                // Верхний бар: Назад + Удалить фото (если установлено)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.card)
                            .border(0.8.dp, scheme.cardBorder, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (currentAvatar !is ProfileAvatar.None) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    ProfileAvatarStore.setAvatar(context, ProfileAvatar.None)
                                    onBack()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = scheme.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.removePhoto,
                                color = scheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Мини-шапка профиля: круглый аватар, имя и подпись
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, scheme.cardBorder, CircleShape)
                    ) {
                        ProfileAvatarView(
                            avatar = currentAvatar,
                            displayName = displayName,
                            monogramFontSize = 26.sp,
                            emojiFontSize = 32.sp,
                            playVideo = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayName,
                        color = scheme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitleText,
                        color = scheme.textSecondary,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = strings.choosePhotoOrVideo,
                    color = scheme.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )

                // Наследуем готовый GalleryTab (с живой 2-рядной камерой + кэшем миниатюр)
                GalleryTab(
                    onLaunchCamera = { isCameraOpen = true },
                    onOpenEditor = { items ->
                        items.firstOrNull()?.let { item ->
                            onPickMedia(item.uri, item.isVideo, item.durationMs)
                        }
                    },
                    showSelectionCircles = false,
                    onOpenViewer = { list, idx ->
                        list.getOrNull(idx)?.let { media ->
                            onPickMedia(media.uri, media.isVideo, media.durationMs)
                        }
                    },
                    leadingExtraTile = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF62C8FA), Color(0xFF2B6FD9))
                                    )
                                )
                                .clickable { emojiSheetOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🦆", fontSize = 34.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = strings.chooseEmoji,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            if (emojiSheetOpen) {
                EmojiAvatarSheet(
                    onSelect = { emoji, start, end ->
                        ProfileAvatarStore.setAvatar(
                            context,
                            ProfileAvatar.Emoji(
                                emoji = emoji,
                                bgStartArgb = start,
                                bgEndArgb = end
                            )
                        )
                        emojiSheetOpen = false
                        onBack()
                    },
                    onDismiss = { emojiSheetOpen = false }
                )
            }

            if (isCameraOpen) {
                CameraScreen(
                    onResult = { captured ->
                        isCameraOpen = false
                        onPickMedia(captured.uri, captured.isVideo, captured.durationMs)
                    },
                    onClose = { isCameraOpen = false }
                )
            }
        }
    }
}

@Composable
private fun EmojiAvatarSheet(
    onSelect: (emoji: String, bgStartArgb: Long, bgEndArgb: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val emojis = remember {
        listOf(
            "🦆", "🦊", "🐺", "🦁", "🐉", "🦅", "🦉", "🐼",
            "😎", "🤖", "👾", "🎭", "🔥", "⚡", "💎", "🛡️"
        )
    }
    var selectedPreset by remember { mutableStateOf(ProfileColorPresets.gradientPresets.first()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.overlayScrim)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(scheme.card)
                .clickable(enabled = false) {}
                .safeNavigationBarPadding()
                .padding(18.dp)
        ) {
            Text(
                text = strings.chooseEmoji,
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileColorPresets.gradientPresets.forEach { p ->
                    val isSel = p.id == selectedPreset.id
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(p.primary, p.secondary)))
                            .then(
                                if (isSel) Modifier.border(2.dp, scheme.textPrimary, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedPreset = p }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(230.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(listOf(selectedPreset.primary, selectedPreset.secondary))
                            )
                            .clickable {
                                val startArgb = (selectedPreset.primary.value shr 32).toLong()
                                val endArgb = (selectedPreset.secondary.value shr 32).toLong()
                                onSelect(emoji, startArgb, endArgb)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 32.sp)
                    }
                }
            }
        }
    }
}
