package com.anongram.app.features.settings.chat_settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.chat_settings.ui.components.ChatTextSizeCard
import com.anongram.app.features.settings.chat_settings.ui.components.ThemeSelectorCard

/**
 * Экран «Настройки чатов» (`features/settings/chat_settings/`).
 * Управление размером шрифта сообщений (1..30 sp) и богатой цветовой палитрой с живой анимацией.
 */
@Composable
fun ChatAppearanceSettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ChatTextSettingsStore.ensureInitialized(context)
    }

    val textSizeSp by ChatTextSettingsStore.messageTextSizeSp.collectAsState()
    val registeredUserName by ChatTextSettingsStore.registeredUserName.collectAsState()


    val scheme = com.anongram.app.core.theme.LocalAnonGramScheme.current
    val strings = com.anongram.app.features.chats.input.i18n.LocalInputStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        // Верхняя панель в стиле Telegram с круглой кнопкой возврата
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.islandBg)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = strings.chatSettingsTitle,
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Карточка размера текста с превью
            ChatTextSizeCard(
                textSizeSp = textSizeSp,
                userName = registeredUserName,
                onTextSizeChange = { newSize ->
                    ChatTextSettingsStore.setMessageTextSizeSp(context, newSize)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Карточка выбора цветовой темы с живым превью и вертикальным скроллом
            ThemeSelectorCard(
                currentThemeId = currentThemeId,
                onThemeSelect = onThemeSelect
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
