package com.anongram.app.features.wallpapers.model

import androidx.compose.ui.graphics.Color

/**
 * Предустановленная тема оформления обоев для чата (по макету chat_wallpaper_presets_mockup).
 */
data class ThemePreset(
    val id: String,
    val title: String,
    val emojiBadge: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color = secondaryColor,
    val patternStyle: Int = 0,
    val isAiMode: Boolean = false,
    val adaptsToTheme: Boolean = false
) {
    companion object {
        val AdaptiveDefault = ThemePreset(
            id = "default",
            title = "Default (Тема)",
            emojiBadge = "⚡",
            primaryColor = Color(0xFF7C5CFF),
            secondaryColor = Color(0xFF2FF2FF),
            tertiaryColor = Color(0xFF0E1322),
            patternStyle = 0,
            adaptsToTheme = true
        )

        val Aurora = ThemePreset(
            id = "aurora",
            title = "Aurora",
            emojiBadge = "✨",
            primaryColor = Color(0xFF5DE0E6),
            secondaryColor = Color(0xFF004AAD),
            tertiaryColor = Color(0xFF8F5BFF),
            patternStyle = 0
        )

        val Sunset = ThemePreset(
            id = "sunset",
            title = "Sunset",
            emojiBadge = "🌅",
            primaryColor = Color(0xFFFF8A5B),
            secondaryColor = Color(0xFFFF4D6D),
            tertiaryColor = Color(0xFF7A4DFF),
            patternStyle = 2
        )

        val Forest = ThemePreset(
            id = "forest",
            title = "Forest",
            emojiBadge = "🌲",
            primaryColor = Color(0xFF1E3C72),
            secondaryColor = Color(0xFF2A5298),
            tertiaryColor = Color(0xFF1F9D8B),
            patternStyle = 1
        )

        val Graphite = ThemePreset(
            id = "graphite",
            title = "Graphite",
            emojiBadge = "🪨",
            primaryColor = Color(0xFF0E111A),
            secondaryColor = Color(0xFF151C2E),
            tertiaryColor = Color(0xFF1A2438),
            patternStyle = 1
        )

        val AiStudy = ThemePreset(
            id = "ai_study",
            title = "Учёба",
            emojiBadge = "🎓",
            primaryColor = Color(0xFF1B2333),
            secondaryColor = Color(0xFF263246),
            tertiaryColor = Color(0xFF303A4F),
            patternStyle = 0,
            isAiMode = true
        )

        val AiPsych = ThemePreset(
            id = "ai_psych",
            title = "Психолог",
            emojiBadge = "🫧",
            primaryColor = Color(0xFF132532),
            secondaryColor = Color(0xFF1D2B45),
            tertiaryColor = Color(0xFF2D2550),
            patternStyle = 2,
            isAiMode = true
        )

        val AiCoding = ThemePreset(
            id = "ai_coding",
            title = "Кодинг",
            emojiBadge = "⌨️",
            primaryColor = Color(0xFF07110F),
            secondaryColor = Color(0xFF0B1A17),
            tertiaryColor = Color(0xFF0D1E1B),
            patternStyle = 1,
            isAiMode = true
        )

        val MainPresets = listOf(
            AdaptiveDefault,
            Aurora,
            Sunset,
            Forest,
            Graphite
        )

        val AiPresets = listOf(
            AiStudy,
            AiPsych,
            AiCoding
        )

        val AllPresets = MainPresets + AiPresets

        fun find(id: String): ThemePreset = when (id) {
            "crypto_mesh" -> AdaptiveDefault
            "neon_matrix" -> Forest
            "cyber_sunset" -> Sunset
            "midnight_hex" -> Aurora
            "cosmic_flow" -> Graphite
            else -> AllPresets.firstOrNull { it.id == id } ?: AdaptiveDefault
        }
    }
}
