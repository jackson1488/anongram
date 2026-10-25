package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.chats.common.MuteDuration

/**
 * Bottom-лист выбора длительности мьюта: 1 час / 8 часов / 1 сутки / 2 дня /
 * навсегда. Открывается только при ВКЛЮЧЕНИИ «без звука»; повторное нажатие
 * на замьюченном чате снимает мьют мгновенно, минуя этот лист.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MuteDurationSheet(
    onSelect: (MuteDuration) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Отключить уведомления",
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )

            MuteDuration.entries.forEach { duration ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(duration) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = duration.label,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (duration == MuteDuration.FOREVER) {
                            Icons.Outlined.AllInclusive
                        } else {
                            Icons.Outlined.Schedule
                        },
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
