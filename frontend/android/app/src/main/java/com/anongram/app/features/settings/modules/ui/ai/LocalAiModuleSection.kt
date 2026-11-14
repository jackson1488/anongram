package com.anongram.app.features.settings.modules.ui.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.anongram.app.features.settings.modules.ui.voice.formatMb

@Composable
fun LocalAiModuleSection(
    activeModelName: String,
    installedCount: Int,
    installedBytes: Long,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current
    val title = if (strings.isRussian) "Локальный ИИ (llama.cpp)" else "Local AI (llama.cpp)"
    val subtitle = if (installedCount > 0) {
        "$activeModelName • ${strings.moduleMemoryLabel(formatMb(installedBytes))}"
    } else {
        if (strings.isRussian) "Hugging Face GGUF • 0 моделей" else "Hugging Face GGUF • 0 models"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = scheme.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = scheme.textMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}
