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

    com.anongram.app.features.settings.common.ui.SettingsGlassScaffold(
        title = strings.chatSettingsTitle,
        onBack = onBack,
        backContentDescription = strings.back,
        modifier = modifier
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

            val browserStrings = com.anongram.app.features.chats.links.browser.InAppBrowserI18n.current
            val linkPreviewsEnabled by ChatTextSettingsStore.linkPreviewsEnabled.collectAsState()
            val hiddenLinkPreviewsEnabled by ChatTextSettingsStore.hiddenLinkPreviewsEnabled.collectAsState()
            val spoilerParticleMotionEnabled by ChatTextSettingsStore.spoilerParticleMotionEnabled.collectAsState()
            val inAppBrowserEnabled by ChatTextSettingsStore.inAppBrowserEnabled.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = browserStrings.linksAndBrowserSection,
                    color = scheme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ChatTextSettingsStore.setLinkPreviewsEnabled(context, !linkPreviewsEnabled)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = browserStrings.linkPreviewsTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = browserStrings.linkPreviewsDesc,
                            color = scheme.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = linkPreviewsEnabled,
                        onCheckedChange = { ChatTextSettingsStore.setLinkPreviewsEnabled(context, it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ChatTextSettingsStore.setHiddenLinkPreviewsEnabled(context, !hiddenLinkPreviewsEnabled)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = browserStrings.hiddenLinkPreviewsTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = browserStrings.hiddenLinkPreviewsDesc,
                            color = scheme.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = hiddenLinkPreviewsEnabled,
                        onCheckedChange = { ChatTextSettingsStore.setHiddenLinkPreviewsEnabled(context, it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ChatTextSettingsStore.setSpoilerParticleMotionEnabled(context, !spoilerParticleMotionEnabled)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = browserStrings.spoilerMotionTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = browserStrings.spoilerMotionDesc,
                            color = scheme.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = spoilerParticleMotionEnabled,
                        onCheckedChange = { ChatTextSettingsStore.setSpoilerParticleMotionEnabled(context, it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ChatTextSettingsStore.setInAppBrowserEnabled(context, !inAppBrowserEnabled)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = browserStrings.useInAppBrowser,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = browserStrings.useInAppBrowserDesc,
                            color = scheme.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = inAppBrowserEnabled,
                        onCheckedChange = { ChatTextSettingsStore.setInAppBrowserEnabled(context, it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable {
                            com.anongram.app.features.chats.links.browser.InAppBrowserStore.openSettings(context)
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = browserStrings.browserSettingsTitle,
                        color = scheme.accent,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        color = scheme.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
}
