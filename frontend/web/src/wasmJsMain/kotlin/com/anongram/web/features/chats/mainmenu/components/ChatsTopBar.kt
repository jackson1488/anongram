package com.anongram.web.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.theme.NeonEmerald
import com.anongram.web.core.ui.safeStatusBarPadding

@Composable
internal fun ChatsTopBar(
    onOpenSearch: () -> Unit,
    onEnterSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val language by LanguageSettingsStore.language.collectAsState()
    val isRu = language == AppLanguage.RU
    val onionBg = if (scheme.isDark) Color(0xFF0F261E) else Color(0xFFE6F7F0)
    val onionTint = if (scheme.isDark) NeonEmerald else Color(0xFF059669)
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeStatusBarPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "AnonGram",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = scheme.textPrimary
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(onionBg)
                    .border(1.dp, onionTint.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Hub,
                        contentDescription = null,
                        tint = onionTint,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Onion v3",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = onionTint
                    )
                }
            }
        }

        IconButton(onClick = onOpenSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = if (isRu) "Поиск" else "Search",
                tint = scheme.textPrimary
            )
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = if (isRu) "Меню" else "Menu",
                    tint = scheme.textPrimary
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = scheme.surface
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isRu) "Выбрать чаты" else "Select chats",
                            color = scheme.textPrimary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.SelectAll,
                            contentDescription = null,
                            tint = scheme.textPrimary
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onEnterSelection()
                    }
                )
            }
        }
    }
}
