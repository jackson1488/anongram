package com.anongram.web.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WebAnonGramScheme(
    val background: Color = Color(0xFF05070A),
    val surface: Color = Color(0xFF0A0F18),
    val card: Color = Color(0xFF10141E),
    val cardBorder: Color = Color(0x1AFFFFFF),
    val islandBg: Color = Color(0xFF10131B),
    val islandBorder: Color = Color(0x24FFFFFF),
    val accent: Color = Color(0xFF00E5FF),
    val onAccent: Color = Color(0xFF05070A),
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xFF94A3B8),
    val textMuted: Color = Color(0xFF64748B),
    val divider: Color = Color(0x18FFFFFF),
    val outgoingBubbleStart: Color = Color(0xFF004D5A),
    val outgoingBubbleEnd: Color = Color(0xFF003842),
    val outgoingBubbleBorder: Color = Color(0x6600E5FF),
    val outgoingText: Color = Color(0xFFFFFFFF),
    val incomingBubbleBg: Color = Color(0xFF10141E),
    val incomingBubbleBorder: Color = Color(0x18FFFFFF),
    val incomingText: Color = Color(0xFFFFFFFF)
)

val LocalWebScheme = staticCompositionLocalOf { WebAnonGramScheme() }
