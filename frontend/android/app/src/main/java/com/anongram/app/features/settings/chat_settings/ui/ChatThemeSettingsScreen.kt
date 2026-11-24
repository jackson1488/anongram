package com.anongram.app.features.settings.chat_settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.AnonGramThemes
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.rememberThemeRevealLauncher
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import com.anongram.app.features.settings.chat_settings.ui.components.ThemeSelectorCard
import com.anongram.app.features.settings.common.ui.SettingsGlassScaffold

private enum class PalettePolarityFilter {
    ALL,
    DARK,
    LIGHT
}

/**
 * Отдельный экран «Тема чата»:
 * - Живое превью переписки сверху;
 * - Закруглённые кнопки разделов: «Палитра тем» (выдвижная шторка как в QR),
 *   «Кастомный редактор» (цветовой круг, полярность, стиль пузырей, Мои темы)
 *   и «Тема разговора» (быстрые пресеты оформления и сброс).
 */
@Composable
fun ChatThemeSettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current

    var isPaletteSheetOpen by remember { mutableStateOf(false) }
    var isCustomEditorOpen by remember { mutableStateOf(false) }
    var isConversationSectionOpen by remember { mutableStateOf(false) }
    var polarityFilter by remember { mutableStateOf(PalettePolarityFilter.ALL) }

    BackHandler(enabled = isPaletteSheetOpen) {
        isPaletteSheetOpen = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        SettingsGlassScaffold(
            title = strings.chatThemeTitle,
            onBack = onBack,
            backContentDescription = strings.back
        ) {
            // 1. Живое превью переписки
            val previewBgBrush = if (scheme.isRainbow) {
                com.anongram.app.core.theme.rememberRainbowBackgroundBrush(scheme)
            } else {
                Brush.verticalGradient(colors = listOf(scheme.background, scheme.surface))
            }
            val previewBorderBrush = com.anongram.app.core.theme.rememberRainbowBorderBrush(scheme)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(previewBgBrush)
                    .border(1.dp, previewBorderBrush, RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.chatThemeCurrentLabel,
                        color = scheme.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.accent.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = scheme.displayName,
                            color = scheme.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Входящее сообщение
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.cardBorder, RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = strings.themePreviewIncoming,
                        color = scheme.incomingText,
                        fontSize = 13.5.sp
                    )
                }

                // Исходящее сообщение
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val previewOutgoingBrush = com.anongram.app.core.theme.rememberOutgoingBubbleBrush(scheme)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                            .background(previewOutgoingBrush)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = strings.themePreviewOutgoing(scheme.displayName),
                            color = scheme.outgoingText,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Отдельные закруглённые кнопки разделов
            ChatThemeRoundedButton(
                icon = Icons.Rounded.Palette,
                badgeColor = scheme.accent,
                title = strings.chatThemePaletteBtn,
                subtitle = strings.chatThemePaletteDesc,
                onClick = { isPaletteSheetOpen = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ChatThemeRoundedButton(
                icon = Icons.Rounded.Tune,
                badgeColor = Color(0xFF26A69A),
                title = strings.chatThemeCustomBtn,
                subtitle = strings.chatThemeCustomDesc,
                isExpanded = isCustomEditorOpen,
                onClick = { isCustomEditorOpen = !isCustomEditorOpen }
            )

            AnimatedVisibility(visible = isCustomEditorOpen) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    ThemeSelectorCard(
                        currentThemeId = currentThemeId,
                        onThemeSelect = onThemeSelect
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ChatThemeRoundedButton(
                icon = Icons.Rounded.Forum,
                badgeColor = Color(0xFF42A5F5),
                title = strings.chatThemeTalkBtn,
                subtitle = strings.chatThemeTalkDesc,
                isExpanded = isConversationSectionOpen,
                onClick = { isConversationSectionOpen = !isConversationSectionOpen }
            )

            AnimatedVisibility(visible = isConversationSectionOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.cardBorder, RoundedCornerShape(22.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val quickThemes = AnonGramThemes.all.take(6)
                    val revealLauncher = rememberThemeRevealLauncher()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        quickThemes.forEach { t ->
                            val selected = t.id == currentThemeId
                            var quickOrigin by remember { mutableStateOf(Offset.Zero) }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .onGloballyPositioned { quickOrigin = it.boundsInRoot().center }
                                    .clip(CircleShape)
                                    .then(
                                        if (selected) Modifier.border(2.dp, scheme.accent, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { revealLauncher(quickOrigin) { onThemeSelect(t.id) } },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 32.dp else 38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (t.isRainbow) {
                                                Brush.sweepGradient(AnonGramThemes.rainbowColors)
                                            } else {
                                                Brush.linearGradient(listOf(t.accent, t.accent))
                                            }
                                        )
                                        .border(1.dp, t.background, CircleShape)
                                )
                            }
                        }
                    }

                    var resetBtnOrigin by remember { mutableStateOf(Offset.Zero) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { resetBtnOrigin = it.boundsInRoot().center }
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.surface)
                            .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                revealLauncher(resetBtnOrigin) {
                                    AnonGramThemes.all.firstOrNull()?.let { onThemeSelect(it.id) }
                                }
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.chatThemeResetBtn,
                                color = scheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 3. Выдвижная ровная палитра цветов (в стиле QrColorPaletteSheet)
        if (isPaletteSheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .background(scheme.overlayScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isPaletteSheetOpen = false }
                    )
            )
        }

        AnimatedVisibility(
            visible = isPaletteSheetOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(240)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(21f)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .safeNavigationBarPadding()
        ) {
            val controller = com.anongram.app.core.theme.LocalThemeController.current
            com.anongram.app.features.settings.chat_settings.ui.components.FullOsColorPaletteSheet(
                currentSpec = scheme.spec,
                onSelectSpec = { newSpec ->
                    controller?.applyCustomSpec(newSpec, "Full OS")
                },
                onSelectRainbow = { polarity, isLive ->
                    val target = when {
                        polarity == com.anongram.app.core.theme.ThemePolarity.DARK && isLive ->
                            AnonGramThemes.RainbowLiveDark
                        polarity == com.anongram.app.core.theme.ThemePolarity.DARK ->
                            AnonGramThemes.RainbowStaticDark
                        isLive ->
                            AnonGramThemes.RainbowLiveLight
                        else ->
                            AnonGramThemes.RainbowStaticLight
                    }
                    onThemeSelect(target.id)
                },
                onReset = {
                    AnonGramThemes.all.firstOrNull()?.let { onThemeSelect(it.id) }
                },
                onDone = { isPaletteSheetOpen = false }
            )
        }
    }
}

@Composable
private fun ChatThemeRoundedButton(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    isExpanded: Boolean? = null,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(badgeColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isExpanded == true) scheme.accent else scheme.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ChatThemeSlidingPaletteSheet(
    currentThemeId: String,
    filter: PalettePolarityFilter,
    onFilterChange: (PalettePolarityFilter) -> Unit,
    onSelectTheme: (String) -> Unit,
    onDone: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val scrollState = rememberScrollState()

    val themes = remember(filter) {
        when (filter) {
            PalettePolarityFilter.ALL -> AnonGramThemes.all
            PalettePolarityFilter.DARK -> AnonGramThemes.all.filter { it.isDark }
            PalettePolarityFilter.LIGHT -> AnongramThemesFilterLight()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(scheme.islandBg.copy(alpha = 0.96f))
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Заголовок + капсулы фильтра + Готово
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        PalettePolarityFilter.ALL to strings.chatThemeFilterAll,
                        PalettePolarityFilter.DARK to strings.themePolarityDark,
                        PalettePolarityFilter.LIGHT to strings.themePolarityLight
                    ).forEach { (itemFilter, label) ->
                        val active = filter == itemFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) scheme.accent else scheme.card)
                                .border(
                                    0.8.dp,
                                    if (active) scheme.accent else scheme.cardBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { onFilterChange(itemFilter) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (active) scheme.onAccent else scheme.textSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onDone)
                ) {
                    Text(
                        text = strings.chatThemeDone,
                        color = scheme.onAccent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ровная сетка круглых палитр (6 в ряд, как QrColorPaletteSheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                themes.chunked(6).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { theme ->
                            val isSelected = theme.id == currentThemeId
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, scheme.textPrimary, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onSelectTheme(theme.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 32.dp else 38.dp)
                                        .clip(CircleShape)
                                        .background(theme.accent)
                                        .border(1.5.dp, theme.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = theme.onAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        repeat(6 - rowItems.size) {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun AnongramThemesFilterLight() = AnonGramThemes.all.filter { !it.isDark }
