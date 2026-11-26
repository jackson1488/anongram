package com.anongram.app.features.profile.appearance

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.AnonGramThemes
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.LocalThemeController
import com.anongram.app.core.theme.rememberThemeRevealLauncher
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.profile.avatar.ProfileAvatarStore
import com.anongram.app.features.profile.avatar.ProfileAvatarView
import com.anongram.app.features.profile.i18n.ProfileI18n

/**
 * Экран «Изменить цвет профиля» (Telegram-style):
 * - Переключатель «Профиль | Имя» и предпросмотр светлой/тёмной шапки;
 * - Живой предпросмотр обложки с аватаром и фоновым эмодзи;
 * - 2 ряда по 8 цветовых кружков (сплошные и двухцветные градиенты);
 * - Выбор фонового эмодзи, сброс цвета и кнопка «Применить стиль».
 */
@Composable
fun ProfileColorEditorScreen(
    displayName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val context = LocalContext.current

    val savedAppearance by ProfileAppearanceStore.myAppearance.collectAsState()
    val avatar by ProfileAvatarStore.avatar.collectAsState()

    var draft by remember(savedAppearance) { mutableStateOf(savedAppearance) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Профиль, 1 = Имя
    val themeController = LocalThemeController.current
    val revealThemeLauncher = rememberThemeRevealLauncher()
    var lastDarkThemeId by remember {
        mutableStateOf(if (scheme.isDark) scheme.id else AnonGramThemes.CyberStealth.id)
    }
    androidx.compose.runtime.LaunchedEffect(scheme.id, scheme.isDark) {
        if (scheme.isDark) lastDarkThemeId = scheme.id
    }
    var previewDark by remember(scheme.isDark) { mutableStateOf(scheme.isDark) }
    var themeBtnCenter by remember { mutableStateOf(Offset.Zero) }
    var emojiPickerOpen by remember { mutableStateOf(false) }

    BackHandler {
        if (emojiPickerOpen) emojiPickerOpen = false else onBack()
    }

    val activePresetId = if (activeTab == 0) draft.colorPresetId else draft.nameColorPresetId

    AnonGramScreenSurface(
        applySystemBarsPadding = false,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .safeNavigationBarPadding()
                    .padding(bottom = 92.dp)
            ) {
                // 1. Живой предпросмотр hero-шапки профиля
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(256.dp)
                ) {
                    ProfileCoverBackdrop(
                        appearance = draft,
                        previewDarkOverride = previewDark,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Верхний бар предпросмотра: Назад | Профиль/Имя | День/Ночь
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .safeStatusBarPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.24f))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(strings.tabProfile, strings.tabName).forEachIndexed { idx, label ->
                                val selected = activeTab == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(17.dp))
                                        .background(if (selected) Color.White else Color.Transparent)
                                        .clickable { activeTab = idx }
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) Color(0xFF12141A) else Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.24f))
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    themeBtnCenter = Offset(
                                        pos.x + coords.size.width / 2f,
                                        pos.y + coords.size.height / 2f
                                    )
                                }
                                .clickable {
                                    if (themeController != null) {
                                        val targetId = if (scheme.isDark) {
                                            AnonGramThemes.MonochromePaper.id
                                        } else {
                                            lastDarkThemeId
                                        }
                                        revealThemeLauncher(themeBtnCenter) {
                                            themeController.selectTheme(targetId)
                                        }
                                    } else {
                                        previewDark = !previewDark
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (previewDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Центр предпросмотра: круглый аватар + имя + статус
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val ringColor = draft.preset?.primary ?: scheme.accent
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, ringColor.copy(alpha = 0.85f), CircleShape)
                                .padding(3.dp)
                                .clip(CircleShape)
                        ) {
                            ProfileAvatarView(
                                avatar = avatar,
                                displayName = displayName,
                                monogramFontSize = 30.sp,
                                emojiFontSize = 36.sp,
                                playVideo = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = displayName,
                            color = draft.namePreset?.primary?.takeIf { activeTab == 1 } ?: Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.onlineStatus,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Карточка палитры (2 ряда по 8 цветов + строка «Фоновый эмодзи»)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorSwatchRow(
                        presets = ProfileColorPresets.solidPresets,
                        selectedId = activePresetId,
                        onSelect = { id ->
                            draft = if (activeTab == 0) {
                                draft.copy(colorPresetId = id)
                            } else {
                                draft.copy(nameColorPresetId = id)
                            }
                        }
                    )
                    ColorSwatchRow(
                        presets = ProfileColorPresets.gradientPresets,
                        selectedId = activePresetId,
                        onSelect = { id ->
                            draft = if (activeTab == 0) {
                                draft.copy(colorPresetId = id)
                            } else {
                                draft.copy(nameColorPresetId = id)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { emojiPickerOpen = true }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.backgroundEmoji,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = draft.backgroundEmoji ?: strings.none,
                            color = scheme.accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = strings.previewNameHint,
                    color = scheme.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                )

                // 3. Кнопка «Сбросить цвет профиля»
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.card)
                        .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
                        .clickable {
                            draft = ProfileAppearanceState()
                        }
                        .padding(horizontal = 18.dp, vertical = 15.dp)
                ) {
                    Text(
                        text = strings.resetProfileColor,
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 4. Нижняя кнопка «Применить стиль»
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(scheme.background.copy(alpha = 0.92f))
                    .safeNavigationBarPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(scheme.accent)
                        .clickable {
                            ProfileAppearanceStore.updateMyAppearance(context, draft)
                            Toast.makeText(context, strings.styleApplied, Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.applyStyle,
                        color = scheme.onAccent,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Модальный выбор фонового эмодзи
            if (emojiPickerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.overlayScrim)
                        .clickable { emojiPickerOpen = false },
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.backgroundEmoji,
                                color = scheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.none,
                                color = scheme.accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        draft = draft.copy(backgroundEmoji = null)
                                        emojiPickerOpen = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(220.dp)
                        ) {
                            items(ProfileColorPresets.emojiOptions) { emoji ->
                                val selected = draft.backgroundEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) scheme.accentSoft else scheme.surface)
                                        .border(
                                            width = if (selected) 1.5.dp else 0.8.dp,
                                            color = if (selected) scheme.accent else scheme.cardBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            draft = draft.copy(backgroundEmoji = emoji)
                                            emojiPickerOpen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    presets: List<ProfileColorPreset>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { preset ->
            val selected = preset.id == selectedId
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected) {
                            Modifier
                                .border(2.dp, scheme.textPrimary, CircleShape)
                                .padding(3.dp)
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .clickable { onSelect(preset.id) }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = preset.primary)
                    if (preset.isGradient) {
                        val triangle = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path = triangle, color = preset.secondary)
                    }
                }
            }
        }
    }
}
