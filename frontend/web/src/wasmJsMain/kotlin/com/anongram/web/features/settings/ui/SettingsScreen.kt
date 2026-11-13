package com.anongram.web.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.TransitionType
import com.anongram.web.core.theme.AnonGramThemes
import com.anongram.web.core.theme.CrimsonKillSwitch
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.AnonGramIslandCard

@Composable
fun SettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onOpenLanguage: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val language by LanguageSettingsStore.language.collectAsState()
    val isRu = language == AppLanguage.RU

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "hdr") {
            Text(
                text = if (isRu) "Настройки" else "Settings",
                fontWeight = FontWeight.Bold,
                color = scheme.textPrimary,
                fontSize = 18.sp
            )
        }

        // Theme palette selector (all 24 AnonGramThemes)
        item(key = "themes") {
            AnonGramIslandCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRu) "Палитра темы (${AnonGramThemes.all.size})" else "Theme Palette (${AnonGramThemes.all.size})",
                            color = scheme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AnonGramThemes.all, key = { it.id }) { theme ->
                            val selected = theme.id == currentThemeId
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) scheme.accent.copy(alpha = 0.14f) else scheme.surface)
                                    .border(1.dp, if (selected) scheme.accent else scheme.divider, RoundedCornerShape(14.dp))
                                    .clickable { onThemeSelect(theme.id) }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(theme.background)
                                        .border(2.dp, theme.accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = theme.displayName,
                                    color = scheme.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transition animation selector
        item(key = "animations") {
            AnonGramIslandCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Animation, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRu) "Анимации переходов" else "Screen Transitions",
                            color = scheme.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TransitionType.entries, key = { it.name }) { type ->
                            val selected = NavigationTransitionConfig.currentType == type
                            Text(
                                text = type.displayName,
                                color = if (selected) scheme.onAccent else scheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) scheme.accent else scheme.surface)
                                    .border(1.dp, if (selected) scheme.accent else scheme.divider, RoundedCornerShape(12.dp))
                                    .clickable { NavigationTransitionConfig.currentType = type }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        item(key = "language") {
            SettingsRowCard(
                icon = Icons.Outlined.Language,
                title = if (isRu) "Язык интерфейса" else "Language",
                subtitle = language.nativeTitle,
                onClick = onOpenLanguage
            )
        }

        item(key = "security") {
            SettingsRowCard(
                icon = Icons.Outlined.Shield,
                title = if (isRu) "Безопасность и приватность" else "Security & Privacy",
                subtitle = if (isRu) "Post-Quantum ML-KEM-1024 · Double Ratchet" else "Post-Quantum ML-KEM-1024 · Double Ratchet",
                onClick = {}
            )
        }

        item(key = "logout") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CrimsonKillSwitch.copy(alpha = 0.12f))
                    .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onLogout)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, tint = CrimsonKillSwitch, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRu) "Сбросить сессию" else "Reset Session",
                    color = CrimsonKillSwitch,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    AnonGramIslandCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, color = scheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = scheme.textSecondary, fontSize = 12.sp)
            }
        }
    }
}
