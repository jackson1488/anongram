package com.anongram.web.features.settings.language.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.safeStatusBarPadding

@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val current by LanguageSettingsStore.language.collectAsState()
    val isRu = current == AppLanguage.RU

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeStatusBarPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.textPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRu) "Язык интерфейса" else "Interface Language",
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        AppLanguage.entries.forEach { lang ->
            val selected = lang == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) scheme.accent.copy(alpha = 0.14f) else scheme.card)
                    .border(1.dp, if (selected) scheme.accent else scheme.cardBorder, RoundedCornerShape(18.dp))
                    .clickable { LanguageSettingsStore.setLanguage(lang) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(lang.nativeName, color = scheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(lang.displayName, color = scheme.textSecondary, fontSize = 12.sp)
                }
                if (selected) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = scheme.accent)
                }
            }
        }
    }
}
