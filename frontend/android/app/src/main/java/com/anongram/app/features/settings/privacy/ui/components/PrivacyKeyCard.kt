package com.anongram.app.features.settings.privacy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Карточка криптографических ключей и параметров приватности AnonGram.
 * Изолированный компонент модуля `features/settings/privacy/`.
 */
@Composable
fun PrivacyKeyCard(
    publicKeyHex: String,
    hideOnlineStatus: Boolean,
    onHideOnlineStatusChange: (Boolean) -> Unit,
    anonymousForwardingAlwaysOn: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val keyColor = if (scheme.isDark) Color(0xFF4ADE80) else Color(0xFF059669)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = keyColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Публичный ключ Ed25519",
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = publicKeyHex.ifBlank { "ed25519:8f91c2a0b4...e391" },
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Скрывать время в сети",
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Никто не увидит точное время последнего входа",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = hideOnlineStatus,
                onCheckedChange = onHideOnlineStatusChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.onAccent,
                    checkedTrackColor = scheme.accent,
                    uncheckedThumbColor = scheme.textSecondary,
                    uncheckedTrackColor = scheme.surface,
                    uncheckedBorderColor = scheme.cardBorder
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "100% анонимная пересылка",
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (anonymousForwardingAlwaysOn) {
                        "Протокол AnonGram всегда очищает метаданные автора при пересылке"
                    } else "",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
