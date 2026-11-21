package com.anongram.app.features.chats.user_profile.ui

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings

@Composable
internal fun UserProfileTopOverlayBar(
    session: IChatSession,
    isCollapsed: Boolean,
    collapsedSubtitle: String,
    strings: UserProfileStrings,
    onBack: () -> Unit,
    overflowMenu: @Composable () -> Unit,
    onOpenOverflow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isCollapsed) Modifier.background(scheme.surface.copy(alpha = 0.94f))
                else Modifier
            )
            .safeStatusBarPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CircleHeaderButton(
            onClick = onBack,
            content = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }
        )

        if (isCollapsed) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = session.title,
                    color = scheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (collapsedSubtitle.isNotBlank()) {
                    Text(
                        text = collapsedSubtitle,
                        color = scheme.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Box {
            CircleHeaderButton(
                onClick = onOpenOverflow,
                content = {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = strings.moreOptions,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            )
            overflowMenu()
        }
    }
}

@Composable
internal fun UserProfileHeroHeader(
    session: IChatSession,
    isExpandedHero: Boolean,
    strings: UserProfileStrings,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val initials = (session.avatarMonogram ?: session.title.take(2)).uppercase()
    val statusText = if (session.isOnline) strings.online else strings.lastSeenRecently

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isExpandedHero) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                scheme.accent.copy(alpha = 0.85f),
                                scheme.accentSoft,
                                scheme.background
                            )
                        )
                    )
                    .clickable(onClick = onToggleExpand)
            ) {
                Text(
                    text = initials,
                    color = scheme.onAccent.copy(alpha = 0.25f),
                    fontSize = 96.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, scheme.background.copy(alpha = 0.92f))
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = session.title,
                        color = scheme.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = if (session.isOnline) scheme.accent else scheme.textSecondary,
                            fontSize = 13.5.sp
                        )
                        if (!session.isOnline) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(scheme.card)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = strings.whenQuestion,
                                    color = scheme.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.safeStatusBarPadding())
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(scheme.accent, scheme.accent.copy(alpha = 0.72f))
                        )
                    )
                    .clickable(onClick = onToggleExpand),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = scheme.onAccent,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = session.title,
                color = scheme.textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = statusText,
                    color = if (session.isOnline) scheme.accent else scheme.textSecondary,
                    fontSize = 13.sp
                )
                if (!session.isOnline) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.card)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.whenQuestion,
                            color = scheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CircleHeaderButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(scheme.card.copy(alpha = 0.85f))
            .border(0.6.dp, scheme.islandBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
