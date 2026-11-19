package com.anongram.app.features.chats.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.archive.i18n.ArchiveI18n
import com.anongram.app.features.settings.common.ui.SettingsGlassScaffold

/**
 * Экран «Настройки архива» (по скриншоту Telegram):
 * - Бесшовный прогрессивный блюр шапки + стеклянная кнопка «Назад»;
 * - Карточка «Чаты с вкл. уведомлениями» -> «Всегда оставлять в архиве»;
 * - Карточка «Новые чаты с незнакомцами» -> «Автоматически архивировать»;
 * - Полная адаптация под темы (`LocalAnonGramScheme`) и i18n (`ArchiveI18n`).
 */
@Composable
fun ArchiveSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = ArchiveI18n.current
    val settings by ArchiveSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        ArchiveSettingsStore.ensureInitialized(context)
    }

    BackHandler(onBack = onBack)

    SettingsGlassScaffold(
        title = strings.archiveSettingsTitle,
        onBack = onBack,
        backContentDescription = strings.back,
        modifier = modifier
    ) {
        ArchiveSettingCard(
            header = strings.chatsWithNotificationsHeader,
            toggleTitle = strings.alwaysKeepArchivedTitle,
            checked = settings.keepChatsArchived,
            onCheckedChange = { ArchiveSettingsStore.setKeepChatsArchived(context, it) },
            description = strings.alwaysKeepArchivedDesc,
            scheme = scheme
        )

        Spacer(modifier = Modifier.height(16.dp))

        ArchiveSettingCard(
            header = strings.newChatsFromStrangersHeader,
            toggleTitle = strings.autoArchiveTitle,
            checked = settings.autoArchiveUnknown,
            onCheckedChange = { ArchiveSettingsStore.setAutoArchiveUnknown(context, it) },
            description = strings.autoArchiveDesc,
            scheme = scheme
        )
    }
}

@Composable
private fun ArchiveSettingCard(
    header: String,
    toggleTitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String,
    scheme: com.anongram.app.core.theme.AnonGramScheme
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.card)
                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = header,
                color = scheme.accent,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = toggleTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = scheme.onAccent,
                        checkedTrackColor = scheme.accent,
                        uncheckedThumbColor = scheme.textSecondary,
                        uncheckedTrackColor = scheme.surface,
                        uncheckedBorderColor = scheme.divider
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            color = scheme.textSecondary,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
    }
}
