package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.AmberWarning
import com.anongram.app.core.theme.LocalAnonGramScheme

/** Баннер недоступности onion-цепочки: сообщения копятся в локальной очереди шифрования. */
@Composable
internal fun OnionOfflineBanner(modifier: Modifier = Modifier) {
    val scheme = LocalAnonGramScheme.current
    val bannerBg = if (scheme.isDark) Color(0xFF241704) else Color(0xFFFFF7ED)
    val warningTint = if (scheme.isDark) AmberWarning else Color(0xFFD97706)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bannerBg)
            .border(1.dp, warningTint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = warningTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Onion-сеть недоступна — исходящие ждут в очереди шифрования",
            color = scheme.textSecondary,
            fontSize = 11.sp
        )
    }
}
