package com.anongram.web.features.settings.chat_settings.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.ChatTextSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.safeNavigationBarPadding
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.chats.input.i18n.LocalInputStrings
import com.anongram.web.features.settings.chat_settings.ui.components.ChatTextSizeCard
import com.anongram.web.features.settings.chat_settings.ui.components.ThemeSelectorCard

@Composable
fun ChatAppearanceSettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        ChatTextSettingsStore.ensureInitialized()
    }

    val textSizeSp by ChatTextSettingsStore.messageTextSizeSp.collectAsState()
    val registeredUserName by ChatTextSettingsStore.registeredUserName.collectAsState()

    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
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
            ChatTextSizeCard(
                textSizeSp = textSizeSp,
                userName = registeredUserName,
                onTextSizeChange = { newSize ->
                    ChatTextSettingsStore.setMessageTextSizeSp(newSize)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            ThemeSelectorCard(
                currentThemeId = currentThemeId,
                onThemeSelect = onThemeSelect
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
