package com.anongram.app.features.chats.ui

import androidx.compose.ui.graphics.Color
import com.anongram.app.core.theme.CyberCyan
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.NeonEmerald
import com.anongram.app.features.chats.common.ChatSecurityTier
import com.anongram.app.features.chats.common.IChatSession

/**
 * Единственный маппер «уровень защиты → акцентный цвет» для UI чатов.
 * Доменные стратегии не знают Compose-цветов; связывание живёт только здесь.
 */
val IChatSession.accentColor: Color
    get() = when (tier) {
        ChatSecurityTier.STANDARD -> CyberCyan
        ChatSecurityTier.POST_QUANTUM -> NeonEmerald
        ChatSecurityTier.OTP_PAD -> FlameOrange
    }
