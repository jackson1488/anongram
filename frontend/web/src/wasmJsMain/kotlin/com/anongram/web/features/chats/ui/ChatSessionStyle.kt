package com.anongram.web.features.chats.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.anongram.web.core.theme.CyberCyan
import com.anongram.web.core.theme.FlameOrange
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.theme.NeonEmerald
import com.anongram.web.features.chats.common.ChatSecurityTier
import com.anongram.web.features.chats.common.IChatSession

val IChatSession.accentColor: Color
    @Composable get() = when (tier) {
        ChatSecurityTier.STANDARD -> LocalAnonGramScheme.current.accent
        ChatSecurityTier.POST_QUANTUM -> NeonEmerald
        ChatSecurityTier.OTP_PAD -> FlameOrange
    }
