package com.anongram.app.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Цветовая схема AnonGram. База AMOLED (#05070A / карточки / текст) неизменна
 * по бренд-буку; темы варьируют акцентную пару. Полная взаимозаменяемость:
 * любая схема подключается через LocalAnonGramScheme без правок компонентов.
 */
@Immutable
data class AnonGramScheme(
    val id: String,
    val displayName: String,
    val accent: Color,
    val accentDim: Color
)

/** Пресеты тем: добавление новой = одна строка в [all]. */
object AnonGramThemes {

    val CyberStealth = AnonGramScheme(
        id = "cyber_stealth",
        displayName = "Cyber Stealth",
        accent = Color(0xFF00F2FE),
        accentDim = Color(0xFF06B6D4)
    )

    val VioletGhost = AnonGramScheme(
        id = "violet_ghost",
        displayName = "Violet Ghost",
        accent = Color(0xFF8B5CF6),
        accentDim = Color(0xFFA78BFA)
    )

    val NeonRose = AnonGramScheme(
        id = "neon_rose",
        displayName = "Neon Rose",
        accent = Color(0xFFEC407A),
        accentDim = Color(0xFFF48FB1)
    )

    val EmeraldNight = AnonGramScheme(
        id = "emerald_night",
        displayName = "Emerald Night",
        accent = Color(0xFF10B981),
        accentDim = Color(0xFF34D399)
    )

    val SolarAmber = AnonGramScheme(
        id = "solar_amber",
        displayName = "Solar Amber",
        accent = Color(0xFFF59E0B),
        accentDim = Color(0xFFFBBF24)
    )

    val RubyCrimson = AnonGramScheme(
        id = "ruby_crimson",
        displayName = "Ruby Crimson",
        accent = Color(0xFFEF4444),
        accentDim = Color(0xFFF87171)
    )

    val ElectricIndigo = AnonGramScheme(
        id = "electric_indigo",
        displayName = "Electric Indigo",
        accent = Color(0xFF6366F1),
        accentDim = Color(0xFF818CF8)
    )

    val MintAurora = AnonGramScheme(
        id = "mint_aurora",
        displayName = "Mint Aurora",
        accent = Color(0xFF14B8A6),
        accentDim = Color(0xFF2DD4BF)
    )

    val SunsetCoral = AnonGramScheme(
        id = "sunset_coral",
        displayName = "Sunset Coral",
        accent = Color(0xFFFF6B6B),
        accentDim = Color(0xFFFFA07A)
    )

    val DeepAmethyst = AnonGramScheme(
        id = "deep_amethyst",
        displayName = "Deep Amethyst",
        accent = Color(0xFF9333EA),
        accentDim = Color(0xFFC084FC)
    )

    val all: List<AnonGramScheme> = listOf(
        CyberStealth,
        VioletGhost,
        NeonRose,
        EmeraldNight,
        SolarAmber,
        RubyCrimson,
        ElectricIndigo,
        MintAurora,
        SunsetCoral,
        DeepAmethyst
    )

    fun byId(id: String): AnonGramScheme = all.firstOrNull { it.id == id } ?: CyberStealth
}
