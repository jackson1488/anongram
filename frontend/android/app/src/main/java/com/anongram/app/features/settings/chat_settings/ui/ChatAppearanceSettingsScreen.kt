package com.anongram.app.features.settings.chat_settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.AnonGramThemes
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.features.settings.chat_settings.ui.components.ChatTextSizeCard
import com.anongram.app.features.settings.chat_settings.ui.components.ThemePickerDialog

/**
 * Экран «Настройки чатов» (`features/settings/chat_settings/`).
 * Управление размером шрифта сообщений (1..30 sp), цветовой палитрой и темой оформления.
 */
@Composable
fun ChatAppearanceSettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ChatTextSettingsStore.ensureInitialized(context)
    }

    val textSizeSp by ChatTextSettingsStore.messageTextSizeSp.collectAsState()
    val registeredUserName by ChatTextSettingsStore.registeredUserName.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    val activeTheme = AnonGramThemes.byId(currentThemeId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131924))
    ) {
        // Верхняя панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B2331))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Настройки чатов",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ChatTextSizeCard(
                textSizeSp = textSizeSp,
                userName = registeredUserName,
                onTextSizeChange = { newSize ->
                    ChatTextSettingsStore.setMessageTextSizeSp(context, newSize)
                }
            )

            // Карточка выбора цветовой темы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B2331))
                    .clickable { showThemeDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Цветовая тема",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeTheme.displayName,
                        color = Color(0xFF8E99A8),
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(activeTheme.accent, activeTheme.accentDim)
                            )
                        )
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentThemeId = currentThemeId,
            onSelect = {
                onThemeSelect(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}
