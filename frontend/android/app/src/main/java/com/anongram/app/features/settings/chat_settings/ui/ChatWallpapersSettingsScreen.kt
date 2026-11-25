package com.anongram.app.features.settings.chat_settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.AnonGramThemes
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.rememberOutgoingBubbleBrush
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import com.anongram.app.features.settings.common.ui.SettingsGlassScaffold
import com.anongram.app.features.wallpapers.api.WallpaperModule
import com.anongram.app.features.wallpapers.logic.EditorViewModel
import com.anongram.app.features.wallpapers.logic.PhotoPickerViewModel
import com.anongram.app.features.wallpapers.model.ResolvedWallpaper
import com.anongram.app.features.wallpapers.model.ThemePreset
import com.anongram.app.features.wallpapers.model.Wallpaper
import com.anongram.app.features.wallpapers.model.WallpaperSource
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import com.anongram.app.features.wallpapers.ui.WallpaperEditorScreen
import com.anongram.app.features.wallpapers.ui.components.CustomGalleryPickerSheet
import kotlinx.coroutines.launch

private const val GLOBAL_PREVIEW_CHAT_ID = "__common__"

/**
 * Экран настройки глобальных обоев чата в разделе «Настройки чатов».
 * Сочетается с персональными обоями чата: глобальные обои применяются ко всем чатам,
 * где пользователь не выбрал индивидуальный фон.
 */
@Composable
fun ChatWallpapersSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember(context) { WallpaperModule.getRepository(context) }
    val storage = remember(context) { WallpaperModule.getStorage(context) }
    val resolved by repository.observe(GLOBAL_PREVIEW_CHAT_ID).collectAsState(initial = ResolvedWallpaper.Default)

    var showGallerySheet by remember { mutableStateOf(false) }
    var editingPhotoPath by remember { mutableStateOf<String?>(null) }

    val photoViewModel = remember(repository, storage) {
        PhotoPickerViewModel(chatId = GLOBAL_PREVIEW_CHAT_ID, repository = repository, storage = storage)
    }

    fun updateGlobal(
        source: WallpaperSource = resolved.source,
        dim: Int = resolved.dim,
        blur: Boolean = resolved.blur,
        motion: Boolean = resolved.motion
    ) {
        scope.launch {
            repository.saveWallpaper(
                Wallpaper(
                    chatId = null,
                    source = source,
                    dim = dim.coerceIn(0, 100),
                    blur = blur,
                    motion = motion
                )
            )
        }
    }

    val activeBadgeText = when (val src = resolved.source) {
        is WallpaperSource.Theme -> ThemePreset.find(src.themeId).title
        is WallpaperSource.Photo -> strings.chatWallpapersStatusPhoto
        is WallpaperSource.None -> strings.chatWallpapersNoneBtn
    }

    Box(modifier = modifier.fillMaxSize()) {
        SettingsGlassScaffold(
            title = strings.chatWallpapersTitle,
            onBack = onBack,
            backContentDescription = strings.back
        ) {
            // 1. Живое превью обоев с мини-чатом
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, scheme.cardBorder, RoundedCornerShape(24.dp))
            ) {
                WallpaperBackground(
                    chatId = GLOBAL_PREVIEW_CHAT_ID,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(scheme.islandBg.copy(alpha = 0.85f))
                                .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = activeBadgeText,
                                color = scheme.textPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                                .background(scheme.incomingBubbleBg)
                                .border(0.8.dp, scheme.incomingBubbleBorder, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = strings.themePreviewIncoming,
                                color = scheme.incomingText,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val outBrush = rememberOutgoingBubbleBrush(scheme)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                                    .background(outBrush)
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = strings.themePreviewOutgoing(activeBadgeText),
                                    color = scheme.outgoingText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Кнопки: Кастомная шторка галереи + Сбросить
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.accent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { showGallerySheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = scheme.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.chatWallpapersGalleryBtn,
                            color = scheme.onAccent,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.card,
                    border = BorderStroke(1.dp, scheme.cardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            updateGlobal(
                                source = WallpaperSource.Theme(ThemePreset.AdaptiveDefault.id),
                                dim = 0,
                                blur = false,
                                motion = false
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = scheme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.chatWallpapersResetBtn,
                            color = scheme.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Основные узоры и градиенты (3-колоночная сетка)
            Text(
                text = strings.chatWallpapersPresetsTitle,
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            val mainEntries: List<ThemePreset?> = ThemePreset.MainPresets + listOf(null)
            mainEntries.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { preset ->
                        if (preset != null) {
                            val selected = resolved.themeId == preset.id ||
                                (preset.id == "default" && resolved.themeId == "crypto_mesh")
                            WallpaperPresetTile(
                                preset = preset,
                                selected = selected,
                                onClick = { updateGlobal(source = WallpaperSource.Theme(preset.id)) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            val selected = resolved.isNone
                            NoWallpaperTile(
                                label = strings.chatWallpapersNoneBtn,
                                selected = selected,
                                onClick = { updateGlobal(source = WallpaperSource.None) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // AI-режимы из макета
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemePreset.AiPresets.forEach { preset ->
                    WallpaperPresetTile(
                        preset = preset,
                        selected = resolved.themeId == preset.id,
                        onClick = { updateGlobal(source = WallpaperSource.Theme(preset.id)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Эффекты: Затемнение, Размытие, Движение
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.cardBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.chatWallpapersEffectsTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.chatWallpapersDimLabel,
                        color = scheme.textSecondary,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = "${resolved.dim}%",
                        color = scheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = resolved.dim.toFloat(),
                    onValueChange = { updateGlobal(dim = it.toInt()) },
                    valueRange = 0f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = scheme.accent,
                        activeTrackColor = scheme.accent,
                        inactiveTrackColor = scheme.surface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BlurOn,
                            contentDescription = null,
                            tint = scheme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.chatWallpapersBlurLabel,
                            color = scheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = resolved.blur,
                        onCheckedChange = { updateGlobal(blur = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent,
                            uncheckedThumbColor = scheme.textMuted,
                            uncheckedTrackColor = scheme.surface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ScreenRotation,
                            contentDescription = null,
                            tint = scheme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.chatWallpapersMotionLabel,
                            color = scheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = resolved.motion,
                        onCheckedChange = { updateGlobal(motion = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent,
                            uncheckedThumbColor = scheme.textMuted,
                            uncheckedTrackColor = scheme.surface
                        )
                    )
                }
            }
        }

        // Кастомная шторка галереи (штатная GalleryTab с камерой и альбомами)
        if (showGallerySheet) {
            CustomGalleryPickerSheet(
                onPickUri = { uri ->
                    photoViewModel.importFromUri(context, uri) { path ->
                        editingPhotoPath = path
                    }
                },
                onDismiss = { showGallerySheet = false }
            )
        }

        // Редактор выбранного фото (блюр, затемнение, параллакс)
        val photoPath = editingPhotoPath
        if (photoPath != null) {
            val editorViewModel = remember(photoPath, repository) {
                EditorViewModel(
                    chatId = GLOBAL_PREVIEW_CHAT_ID,
                    repository = repository,
                    initialPhotoPath = photoPath
                )
            }
            BackHandler {
                editorViewModel.cancelPreview()
                editingPhotoPath = null
            }
            WallpaperEditorScreen(
                chatId = GLOBAL_PREVIEW_CHAT_ID,
                viewModel = editorViewModel,
                onBack = {
                    editorViewModel.cancelPreview()
                    editingPhotoPath = null
                },
                onChangePhoto = {
                    editorViewModel.cancelPreview()
                    editingPhotoPath = null
                    showGallerySheet = true
                },
                onApplied = {
                    editingPhotoPath = null
                    showGallerySheet = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(40f)
            )
        }
    }
}

@Composable
private fun WallpaperPresetTile(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val gradientColors = when {
        preset.adaptsToTheme && scheme.isRainbow -> AnonGramThemes.rainbowColors
        preset.adaptsToTheme -> listOf(scheme.accent, scheme.accentDim, scheme.background)
        else -> listOf(preset.primaryColor, preset.secondaryColor, preset.tertiaryColor)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) scheme.accent else scheme.cardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(gradientColors))
                .padding(6.dp)
        ) {
            Text(
                text = preset.emojiBadge,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(scheme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Text(
            text = preset.title,
            color = scheme.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoWallpaperTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) scheme.accent else scheme.cardBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surface)
                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = scheme.textMuted,
                modifier = Modifier.size(22.dp)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(scheme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            color = scheme.textPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
