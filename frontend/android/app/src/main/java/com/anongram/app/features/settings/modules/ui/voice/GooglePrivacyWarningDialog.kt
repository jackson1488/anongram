package com.anongram.app.features.settings.modules.ui.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.modules.i18n.ModulesI18n

/**
 * Красивое модальное предупреждение безопасности при выборе «Системный / Google»
 * (закрытый исходный код — нельзя провести аудит и гарантировать отсутствие утечки аудио).
 */
@Composable
fun GooglePrivacyWarningDialog(
    onKeepLocal: () -> Unit,
    onAcceptAnyway: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current

    Dialog(
        onDismissRequest = onKeepLocal,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.card)
                .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CrimsonKillSwitch.copy(alpha = 0.14f))
                    .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.GppMaybe,
                    contentDescription = null,
                    tint = CrimsonKillSwitch,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CrimsonKillSwitch.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.notRecommendedBadge.uppercase(),
                    color = CrimsonKillSwitch,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.googleWarningTitle,
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.googleWarningBody,
                color = scheme.textSecondary,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.accent)
                        .clickable(onClick = onKeepLocal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.googleWarningKeepLocal,
                        color = scheme.onAccent,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CrimsonKillSwitch.copy(alpha = 0.12f))
                        .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onAcceptAnyway),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.googleWarningAcceptAnyway,
                        color = CrimsonKillSwitch,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
