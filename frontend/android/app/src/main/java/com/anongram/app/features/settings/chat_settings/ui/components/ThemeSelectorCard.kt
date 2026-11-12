package com.anongram.app.features.settings.chat_settings.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.AnonGramScheme
import com.anongram.app.core.theme.AnonGramThemes
import com.anongram.app.core.theme.BubbleColorStyle
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.LocalThemeController
import com.anongram.app.core.theme.ThemePaletteEngine
import com.anongram.app.core.theme.ThemePolarity
import com.anongram.app.core.theme.ThemeSpec
import com.anongram.app.core.theme.ThemeTintMode
import com.anongram.app.features.chats.input.i18n.InputI18nStrings
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import java.util.Locale

private enum class ThemeStudioTab {
    PRESETS,
    WHEEL,
    SAVED
}

private enum class PresetFilter {
    ALL,
    DARK,
    LIGHT
}

/**
 * Карточка выбора цветовой темы и конструктора палитр с живым превью чата,
 * цветовым кругом (Color Wheel), полярностью Светлая/Тёмная (включая Ч/Б и Б/Ч)
 * и разделом «Мои темы».
 */
@Composable
fun ThemeSelectorCard(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAnonGramScheme.current
    val controller = LocalThemeController.current
    val strings = LocalInputStrings.current
    val clipboard = LocalClipboardManager.current

    var activeTab by remember { mutableStateOf(ThemeStudioTab.PRESETS) }
    var presetFilter by remember { mutableStateOf(PresetFilter.ALL) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var draftSpec by remember(activeTheme.id) { mutableStateOf(activeTheme.spec) }
    var customName by remember { mutableStateOf("") }
    var importCodeInput by remember { mutableStateOf("") }
    val savedThemes by (controller?.customThemes?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })

    fun applyDraft(newSpec: ThemeSpec) {
        draftSpec = newSpec
        statusMessage = null
        controller?.applyCustomSpec(newSpec, name = "Custom")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(activeTheme.card)
            .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = strings.themeCardTitle,
            color = activeTheme.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.themeCardSubtitle,
            color = activeTheme.textSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Интерактивное живое превью чата (доказывает, что фон, островки и пузыри разделены)
        LiveChatThemeMiniPreview(scheme = activeTheme, strings = strings)

        Spacer(modifier = Modifier.height(14.dp))

        // Вкладки: Пресеты | Цветовой круг | Мои темы
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(activeTheme.surface)
                .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                ThemeStudioTab.PRESETS to strings.themeTabPresets,
                ThemeStudioTab.WHEEL to strings.themeTabWheel,
                ThemeStudioTab.SAVED to strings.themeTabSaved
            ).forEach { (tab, label) ->
                val isSelected = tab == activeTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) activeTheme.accent else Color.Transparent)
                        .clickable {
                            activeTab = tab
                            statusMessage = null
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) activeTheme.onAccent else activeTheme.textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (activeTab) {
            ThemeStudioTab.PRESETS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PresetFilter.ALL to strings.themeFilterAll,
                        PresetFilter.DARK to strings.themeFilterDark,
                        PresetFilter.LIGHT to strings.themeFilterLight
                    ).forEach { (filter, label) ->
                        val selected = presetFilter == filter
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) activeTheme.accentSoft else activeTheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) activeTheme.accent else activeTheme.cardBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { presetFilter = filter }
                        ) {
                            Text(
                                text = label,
                                color = if (selected) activeTheme.accent else activeTheme.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val filtered = when (presetFilter) {
                    PresetFilter.ALL -> AnonGramThemes.all
                    PresetFilter.DARK -> AnonGramThemes.all.filter { it.isDark }
                    PresetFilter.LIGHT -> AnonGramThemes.all.filter { !it.isDark }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filtered.forEach { theme ->
                        ThemeItemRow(
                            theme = theme,
                            activeScheme = activeTheme,
                            isSelected = theme.id == currentThemeId,
                            onSelect = {
                                draftSpec = theme.spec
                                onThemeSelect(theme.id)
                            }
                        )
                    }
                }
            }

            ThemeStudioTab.WHEEL -> {
                // Полярность: Тёмная / Светлая
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PolarityChip(
                        icon = Icons.Outlined.DarkMode,
                        label = strings.themePolarityDark,
                        selected = draftSpec.polarity == ThemePolarity.DARK,
                        scheme = activeTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { applyDraft(draftSpec.copy(polarity = ThemePolarity.DARK)) }
                    )
                    PolarityChip(
                        icon = Icons.Outlined.LightMode,
                        label = strings.themePolarityLight,
                        selected = draftSpec.polarity == ThemePolarity.LIGHT,
                        scheme = activeTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { applyDraft(draftSpec.copy(polarity = ThemePolarity.LIGHT)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Цветовой круг + HEX + контраст
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HsvColorWheel(
                        hue = draftSpec.hue,
                        saturation = draftSpec.saturation,
                        onColorChange = { newHue, newSat ->
                            val nextTint = if (draftSpec.tintMode == ThemeTintMode.MONOCHROME) {
                                ThemeTintMode.FULL_OS
                            } else {
                                draftSpec.tintMode
                            }
                            applyDraft(
                                draftSpec.copy(
                                    hue = newHue,
                                    saturation = newSat,
                                    tintMode = nextTint
                                )
                            )
                        },
                        wheelSize = 166.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val hex = remember(activeTheme.accent) {
                            String.format(Locale.US, "#%06X", activeTheme.accent.toArgb() and 0xFFFFFF)
                        }
                        val contrast = remember(activeTheme.background, activeTheme.textPrimary) {
                            ThemePaletteEngine.contrastRatio(activeTheme.background, activeTheme.textPrimary)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(activeTheme.surface)
                                .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = strings.themeAccentHex,
                                color = activeTheme.textMuted,
                                fontSize = 11.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(activeTheme.accent)
                                )
                                Text(
                                    text = hex,
                                    color = activeTheme.textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "WCAG ${String.format(Locale.US, "%.1f", contrast)}:1 ✓",
                                color = activeTheme.accent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Кнопка копирования кода AG1:...
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = activeTheme.surface,
                            border = BorderStroke(1.dp, activeTheme.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    clipboard.setText(AnnotatedString(draftSpec.encode()))
                                    statusMessage = strings.themeCodeCopied
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = activeTheme.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.themeCopyCode,
                                    color = activeTheme.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Глубина цвета экрана (Ч/Б -> Акцент -> Мягкая -> Как ОС)
                Text(
                    text = strings.themeTintDepth,
                    color = activeTheme.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ThemeTintMode.MONOCHROME to strings.themeTintMonochrome,
                        ThemeTintMode.AMOLED_ACCENT to strings.themeTintAmoled,
                        ThemeTintMode.SOFT_OS to strings.themeTintSoftOs,
                        ThemeTintMode.FULL_OS to strings.themeTintFullOs
                    ).forEach { (mode, label) ->
                        val selected = draftSpec.tintMode == mode
                        OptionPill(
                            label = label,
                            selected = selected,
                            scheme = activeTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { applyDraft(draftSpec.copy(tintMode = mode)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Стиль пузырей
                Text(
                    text = strings.themeBubbleStyle,
                    color = activeTheme.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        BubbleColorStyle.GRADIENT to strings.themeBubbleGradient,
                        BubbleColorStyle.TINTED to strings.themeBubbleTinted,
                        BubbleColorStyle.OUTLINE to strings.themeBubbleOutline
                    ).forEach { (style, label) ->
                        val selected = draftSpec.bubbleStyle == style
                        OptionPill(
                            label = label,
                            selected = selected,
                            scheme = activeTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { applyDraft(draftSpec.copy(bubbleStyle = style)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Сохранение в «Мои темы»
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeTheme.surface)
                            .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (customName.isEmpty()) {
                            Text(
                                text = strings.themeCustomNamePlaceholder,
                                color = activeTheme.textMuted,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            singleLine = true,
                            textStyle = TextStyle(color = activeTheme.textPrimary, fontSize = 13.sp),
                            cursorBrush = SolidColor(activeTheme.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeTheme.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val saved = controller?.saveCustomTheme(customName, draftSpec)
                                if (saved != null) {
                                    customName = ""
                                    statusMessage = strings.themeSavedToast
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null,
                                tint = activeTheme.onAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.save,
                                color = activeTheme.onAccent,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            ThemeStudioTab.SAVED -> {
                if (savedThemes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(activeTheme.surface)
                            .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = activeTheme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.themeNoSavedYet,
                                color = activeTheme.textSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedThemes.forEach { saved ->
                            val previewScheme = remember(saved) {
                                ThemePaletteEngine.buildScheme(saved.id, saved.name, saved.spec)
                            }
                            ThemeItemRow(
                                theme = previewScheme,
                                activeScheme = activeTheme,
                                isSelected = saved.id == currentThemeId,
                                onSelect = {
                                    draftSpec = saved.spec
                                    onThemeSelect(saved.id)
                                },
                                onDelete = { controller?.deleteCustomTheme(saved.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = activeTheme.divider)
                Spacer(modifier = Modifier.height(12.dp))

                // Импорт кода темы AG1:...
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeTheme.surface)
                            .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (importCodeInput.isEmpty()) {
                            Text(
                                text = strings.themeImportPlaceholder,
                                color = activeTheme.textMuted,
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        BasicTextField(
                            value = importCodeInput,
                            onValueChange = { importCodeInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = activeTheme.textPrimary,
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            cursorBrush = SolidColor(activeTheme.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeTheme.accentSoft,
                        border = BorderStroke(1.dp, activeTheme.accent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val ok = controller?.importThemeCode(importCodeInput) == true
                                statusMessage = if (ok) {
                                    importCodeInput = ""
                                    strings.themeImportApplied
                                } else {
                                    strings.themeImportInvalid
                                }
                            }
                    ) {
                        Text(
                            text = strings.themeImportApply,
                            color = activeTheme.accent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = msg,
                color = activeTheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LiveChatThemeMiniPreview(
    scheme: AnonGramScheme,
    strings: InputI18nStrings
) {
    val animBg by animateColorAsState(scheme.background, tween(260), label = "prevBg")
    val animIsland by animateColorAsState(scheme.islandBg, tween(260), label = "prevIsland")
    val animInBubble by animateColorAsState(scheme.incomingBubbleBg, tween(260), label = "prevIn")
    val animOutStart by animateColorAsState(scheme.outgoingBubbleStart, tween(260), label = "prevOutS")
    val animOutEnd by animateColorAsState(scheme.outgoingBubbleEnd, tween(260), label = "prevOutE")
    val animAccent by animateColorAsState(scheme.accent, tween(260), label = "prevAccent")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(animBg)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Мини-шапка (островок)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(animIsland)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(animAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", color = scheme.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AnonGram",
                    color = scheme.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "ML-DSA-87",
                color = animAccent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Входящее сообщение с цитатой
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 3.dp))
                    .background(animInBubble)
                    .border(1.dp, scheme.incomingBubbleBorder, RoundedCornerShape(14.dp, 14.dp, 14.dp, 3.dp))
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.replyStripIncoming)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(animAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.themePreviewIncomingReply,
                        color = animAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.themePreviewIncoming,
                    color = scheme.incomingText,
                    fontSize = 12.sp
                )
            }
        }

        // Исходящее сообщение
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp, 14.dp, 3.dp, 14.dp))
                    .background(Brush.linearGradient(listOf(animOutStart, animOutEnd)))
                    .border(
                        1.dp,
                        scheme.outgoingBubbleBorder,
                        RoundedCornerShape(14.dp, 14.dp, 3.dp, 14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = strings.themePreviewOutgoing(scheme.displayName),
                    color = scheme.outgoingText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Мини-строка ввода
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(scheme.inputBg)
                    .border(1.dp, scheme.inputBorder, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = strings.themePreviewInputPlaceholder,
                    color = scheme.textMuted,
                    fontSize = 11.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(animAccent),
                contentAlignment = Alignment.Center
            ) {
                Text("➤", color = scheme.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PolarityChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    scheme: AnonGramScheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) scheme.accentSoft else scheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) scheme.accent else scheme.cardBorder
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) scheme.accent else scheme.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) scheme.accent else scheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OptionPill(
    label: String,
    selected: Boolean,
    scheme: AnonGramScheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) scheme.accent else scheme.surface,
        border = BorderStroke(1.dp, if (selected) scheme.accent else scheme.cardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) scheme.onAccent else scheme.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ThemeItemRow(
    theme: AnonGramScheme,
    activeScheme: AnonGramScheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) theme.accent else activeScheme.cardBorder,
        animationSpec = tween(durationMillis = 220),
        label = "themeItemBorder"
    )
    val animatedBackground by animateColorAsState(
        targetValue = if (isSelected) activeScheme.accentSoft else activeScheme.surface,
        animationSpec = tween(durationMillis = 220),
        label = "themeItemBackground"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBackground)
            .border(
                width = if (isSelected) 1.5.dp else 0.8.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Двухцветный превью-кружок: фон темы внутри, акцентная обводка
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(theme.background, theme.outgoingBubbleStart)
                    )
                )
                .border(2.5.dp, theme.accent, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.displayName,
                color = activeScheme.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (theme.isDark) "Dark" else "Light",
                color = activeScheme.textMuted,
                fontSize = 11.sp
            )
        }

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = activeScheme.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(theme.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = theme.onAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
