package com.anongram.web.features.chats.mainmenu.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.ui.accentColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatListItemCard(
    session: IChatSession,
    timeLabel: String,
    selectionMode: Boolean,
    isSelected: Boolean,
    draftText: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val accent = session.accentColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) scheme.accent.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(
            session = session,
            accent = accent,
            showSelectionBadge = selectionMode,
            isSelected = isSelected
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (session.isPinned) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = scheme.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = session.title,
                        fontWeight = FontWeight.Bold,
                        color = scheme.textPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (session.lastMessagePreview.isNotEmpty() || session.lastMessageAttachment != null) {
                    Text(
                        text = timeLabel,
                        color = scheme.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val previewAnnotated = buildAnnotatedString {
                    if (!draftText.isNullOrBlank()) {
                        withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)) {
                            append("Черновик: ")
                        }
                        withStyle(SpanStyle(color = scheme.textSecondary)) {
                            append(draftText)
                        }
                    } else {
                        withStyle(SpanStyle(color = scheme.textSecondary)) {
                            append(session.lastMessagePreview.ifBlank { session.securityLabel })
                        }
                    }
                }

                Text(
                    text = previewAnnotated,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                ChatTrailingBadges(session = session)
            }
        }
    }
}
