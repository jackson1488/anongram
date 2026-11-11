package com.anongram.app.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Полная цветовая схема AnonGram.
 *
 * Изолированный единый источник всех цветов интерфейса:
 * - фоны экранов и поверхности (`background`, `surface`, `card`, `islandBg`);
 * - ввод, редактор и эмодзи (`inputBg`, `editorBg`, `emojiPanelBg`, `bannerBg`);
 * - пузыри сообщений (`outgoingBubbleStart/End`, `incomingBubbleBg`, `replyStrip*`, `selectedGlow`);
 * - акценты и автоконтраст (`accent`, `accentDim`, `onAccent`, `textPrimary`).
 */
@Immutable
data class AnonGramScheme(
    val id: String,
    val displayName: String,
    val accent: Color,
    val accentDim: Color,
    val isDark: Boolean = true,
    val spec: ThemeSpec = ThemeSpec(),
    val background: Color = Color(0xFF05070A),
    val surface: Color = Color(0xFF0A0F18),
    val card: Color = Color(0xFF10141E),
    val cardBorder: Color = Color(0x1AFFFFFF),
    val islandBg: Color = Color(0xFF10131B),
    val islandBorder: Color = Color(0x24FFFFFF),
    val bannerBg: Color = Color(0xF01C1F26),
    val overlayScrim: Color = Color(0xCC040508),
    val inputBg: Color = Color(0xFF10131B),
    val inputBorder: Color = Color(0x24FFFFFF),
    val editorBg: Color = Color(0xFF05070A),
    val editorSurface: Color = Color(0xFF141419),
    val emojiPanelBg: Color = Color(0xFF0A0F18),
    val accentSoft: Color = accent.copy(alpha = 0.16f),
    val onAccent: Color = Color(0xFF0A0C10),
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xFF94A3B8),
    val textMuted: Color = Color(0xFF64748B),
    val divider: Color = Color(0x18FFFFFF),
    val outgoingBubbleStart: Color = Color(0xFF281C3D),
    val outgoingBubbleEnd: Color = Color(0xFF201633),
    val outgoingBubbleBorder: Color = accent.copy(alpha = 0.40f),
    val outgoingText: Color = Color(0xFFFFFFFF),
    val incomingBubbleBg: Color = Color(0xFF10141E),
    val incomingBubbleBorder: Color = Color(0x18FFFFFF),
    val incomingText: Color = Color(0xFFFFFFFF),
    val replyStripIncoming: Color = Color(0xFF0B121E),
    val replyStripOutgoing: Color = Color(0xFF081A2C),
    val selectedBubbleBg: Color = Color(0xFF3F2B66),
    val selectedGlow: Color = Color(0xFF8B5CF6)
)

/**
 * Каталог кураторских тем (Тёмные, Монохром Ч/Б и Б/Ч, Светлые, Полная ОС палитра).
 * Для сотен пользовательских комбинаций также поддерживает `custom:<ThemeSpec.encode()>`.
 */
object AnonGramThemes {

    private fun preset(
        id: String,
        name: String,
        polarity: ThemePolarity,
        tintMode: ThemeTintMode,
        bubbleStyle: BubbleColorStyle,
        hue: Float,
        sat: Float = 0.86f
    ): AnonGramScheme = ThemePaletteEngine.buildScheme(
        id = id,
        displayName = name,
        spec = ThemeSpec(
            polarity = polarity,
            tintMode = tintMode,
            bubbleStyle = bubbleStyle,
            hue = hue,
            saturation = sat
        )
    )

    // --- Базовые строгие Ч/Б и Б/Ч ---
    val MonochromeNoir = preset(
        "monochrome_noir", "Ч/Б Noir", ThemePolarity.DARK, ThemeTintMode.MONOCHROME, BubbleColorStyle.OUTLINE, 0f, 0f
    )
    val MonochromePaper = preset(
        "monochrome_paper", "Б/Ч Paper", ThemePolarity.LIGHT, ThemeTintMode.MONOCHROME, BubbleColorStyle.GRADIENT, 0f, 0f
    )

    // --- Классические 10 тем (совместимость ID) ---
    val CyberStealth = preset(
        "cyber_stealth", "Cyber Stealth", ThemePolarity.DARK, ThemeTintMode.AMOLED_ACCENT, BubbleColorStyle.TINTED, 186f, 0.96f
    )
    val VioletGhost = preset(
        "violet_ghost", "Violet Ghost", ThemePolarity.DARK, ThemeTintMode.SOFT_OS, BubbleColorStyle.TINTED, 260f, 0.88f
    )
    val NeonRose = preset(
        "neon_rose", "Neon Rose", ThemePolarity.DARK, ThemeTintMode.SOFT_OS, BubbleColorStyle.GRADIENT, 338f, 0.84f
    )
    val EmeraldNight = preset(
        "emerald_night", "Emerald Night", ThemePolarity.DARK, ThemeTintMode.AMOLED_ACCENT, BubbleColorStyle.TINTED, 158f, 0.82f
    )
    val SolarAmber = preset(
        "solar_amber", "Solar Amber", ThemePolarity.DARK, ThemeTintMode.AMOLED_ACCENT, BubbleColorStyle.TINTED, 38f, 0.92f
    )
    val RubyCrimson = preset(
        "ruby_crimson", "Ruby Crimson", ThemePolarity.DARK, ThemeTintMode.SOFT_OS, BubbleColorStyle.TINTED, 0f, 0.84f
    )
    val ElectricIndigo = preset(
        "electric_indigo", "Electric Indigo", ThemePolarity.DARK, ThemeTintMode.SOFT_OS, BubbleColorStyle.GRADIENT, 238f, 0.86f
    )
    val MintAurora = preset(
        "mint_aurora", "Mint Aurora", ThemePolarity.DARK, ThemeTintMode.AMOLED_ACCENT, BubbleColorStyle.TINTED, 173f, 0.80f
    )
    val SunsetCoral = preset(
        "sunset_coral", "Sunset Coral", ThemePolarity.DARK, ThemeTintMode.SOFT_OS, BubbleColorStyle.GRADIENT, 8f, 0.90f
    )
    val DeepAmethyst = preset(
        "deep_amethyst", "Deep Amethyst", ThemePolarity.DARK, ThemeTintMode.FULL_OS, BubbleColorStyle.TINTED, 272f, 0.82f
    )

    // --- Полная ОС палитра (весь экран в одном тоне) ---
    val SakuraOsDark = preset(
        "sakura_os_dark", "Розовая ОС (Тёмная)", ThemePolarity.DARK, ThemeTintMode.FULL_OS, BubbleColorStyle.GRADIENT, 336f, 0.80f
    )
    val CobaltOsDark = preset(
        "cobalt_os_dark", "Кобальт ОС", ThemePolarity.DARK, ThemeTintMode.FULL_OS, BubbleColorStyle.GRADIENT, 214f, 0.88f
    )
    val TelegramNight = preset(
        "telegram_night", "Telegram Ночь", ThemePolarity.DARK, ThemeTintMode.FULL_OS, BubbleColorStyle.TINTED, 210f, 0.72f
    )
    val EmeraldOsDark = preset(
        "emerald_os_dark", "Изумруд ОС", ThemePolarity.DARK, ThemeTintMode.FULL_OS, BubbleColorStyle.GRADIENT, 154f, 0.78f
    )
    val GoldCarbon = preset(
        "gold_carbon", "Золото на Чёрном", ThemePolarity.DARK, ThemeTintMode.AMOLED_ACCENT, BubbleColorStyle.OUTLINE, 45f, 0.94f
    )
    val CrimsonBlood = preset(
        "crimson_blood", "Вишня ОС", ThemePolarity.DARK, ThemeTintMode.FULL_OS, BubbleColorStyle.TINTED, 352f, 0.76f
    )

    // --- Светлые темы ---
    val TelegramDay = preset(
        "telegram_day", "Telegram День", ThemePolarity.LIGHT, ThemeTintMode.SOFT_OS, BubbleColorStyle.GRADIENT, 206f, 0.88f
    )
    val SakuraOsLight = preset(
        "sakura_os_light", "Розовая ОС (Светлая)", ThemePolarity.LIGHT, ThemeTintMode.FULL_OS, BubbleColorStyle.TINTED, 338f, 0.74f
    )
    val LavenderDay = preset(
        "lavender_day", "Лаванда День", ThemePolarity.LIGHT, ThemeTintMode.FULL_OS, BubbleColorStyle.GRADIENT, 265f, 0.78f
    )
    val MintDay = preset(
        "mint_day", "Мята День", ThemePolarity.LIGHT, ThemeTintMode.SOFT_OS, BubbleColorStyle.TINTED, 166f, 0.76f
    )
    val PeachDaylight = preset(
        "peach_daylight", "Персик День", ThemePolarity.LIGHT, ThemeTintMode.FULL_OS, BubbleColorStyle.GRADIENT, 18f, 0.84f
    )
    val ArcticFrost = preset(
        "arctic_frost", "Арктика Свет", ThemePolarity.LIGHT, ThemeTintMode.AMOLED_ACCENT, BubbleColorStyle.OUTLINE, 196f, 0.86f
    )

    val all: List<AnonGramScheme> = listOf(
        CyberStealth,
        MonochromeNoir,
        MonochromePaper,
        SakuraOsDark,
        SakuraOsLight,
        TelegramNight,
        TelegramDay,
        VioletGhost,
        NeonRose,
        CobaltOsDark,
        EmeraldOsDark,
        MintDay,
        LavenderDay,
        GoldCarbon,
        CrimsonBlood,
        PeachDaylight,
        ArcticFrost,
        EmeraldNight,
        SolarAmber,
        RubyCrimson,
        ElectricIndigo,
        MintAurora,
        SunsetCoral,
        DeepAmethyst
    )

    fun byId(id: String): AnonGramScheme {
        if (id.startsWith("custom:")) {
            val code = id.removePrefix("custom:")
            ThemeSpec.decodeOrNull(code)?.let { spec ->
                return ThemePaletteEngine.buildScheme(id = id, displayName = "Custom", spec = spec)
            }
        }
        return all.firstOrNull { it.id == id } ?: CyberStealth
    }
}
