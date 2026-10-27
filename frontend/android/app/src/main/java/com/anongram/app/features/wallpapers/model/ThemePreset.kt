package com.anongram.app.features.wallpapers.model

import androidx.compose.ui.graphics.Color

/**
 * Предустановленная тема оформления обоев для чата в стиле Telegram.
 */
data class ThemePreset(
    val id: String,
    val title: String,
    val emojiBadge: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val patternStyle: Int
) {
    companion object {
        val CryptoMesh = ThemePreset(
            id = "crypto_mesh",
            title = "Крипто-меш",
            emojiBadge = "🔮",
            primaryColor = Color(0xFF7A4DF3),
            secondaryColor = Color(0xFF00E5FF),
            patternStyle = 0
        )

        val NeonMatrix = ThemePreset(
            id = "neon_matrix",
            title = "Матрица",
            emojiBadge = "⚡",
            primaryColor = Color(0xFF00E676),
            secondaryColor = Color(0xFF00B0FF),
            patternStyle = 1
        )

        val CyberSunset = ThemePreset(
            id = "cyber_sunset",
            title = "Кибер-закат",
            emojiBadge = "🌆",
            primaryColor = Color(0xFFFF4081),
            secondaryColor = Color(0xFF7C4DFF),
            patternStyle = 2
        )

        val MidnightHex = ThemePreset(
            id = "midnight_hex",
            title = "Гексагон",
            emojiBadge = "🛡️",
            primaryColor = Color(0xFF00E5FF),
            secondaryColor = Color(0xFF651FFF),
            patternStyle = 1
        )

        val CosmicFlow = ThemePreset(
            id = "cosmic_flow",
            title = "Космос",
            emojiBadge = "🌌",
            primaryColor = Color(0xFF536DFE),
            secondaryColor = Color(0xFFFFAB00),
            patternStyle = 2
        )

        val AllPresets = listOf(
            CryptoMesh,
            NeonMatrix,
            CyberSunset,
            MidnightHex,
            CosmicFlow
        )

        fun find(id: String): ThemePreset =
            AllPresets.firstOrNull { it.id == id } ?: CryptoMesh
    }
}
