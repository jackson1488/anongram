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

    val EmeraldNight = AnonGramScheme(
        id = "emerald_night",
        displayName = "Emerald Night",
        accent = Color(0xFF10B981),
        accentDim = Color(0xFF34D399)
    )

    val VioletGhost = AnonGramScheme(
        id = "violet_ghost",
        displayName = "Violet Ghost",
        accent = Color(0xFF8B5CF6),
        accentDim = Color(0xFFA78BFA)
    )

    val SolarAmber = AnonGramScheme(
        id = "solar_amber",
        displayName = "Solar Amber",
        accent = Color(0xFFF59E0B),
        accentDim = Color(0xFFFBBF24)
    )

    val all: List<AnonGramScheme> = listOf(CyberStealth, EmeraldNight, VioletGhost, SolarAmber)

    fun byId(id: String): AnonGramScheme = all.firstOrNull { it.id == id } ?: CyberStealth
}
