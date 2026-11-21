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
import androidx.compose.runtime.getValue
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
    val collapseAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "profileHeaderCollapse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surface.copy(alpha = 0.94f * collapseAlpha))
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

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (collapseAlpha > 0.02f) {
                Text(
                    text = session.title,
                    color = scheme.textPrimary.copy(alpha = collapseAlpha),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (collapsedSubtitle.isNotBlank()) {
                    Text(
                        text = collapsedSubtitle,
                        color = scheme.textSecondary.copy(alpha = collapseAlpha),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
    val avatarResId = session.avatarResId
    val canExpandHero = avatarResId != null
    val effectiveExpanded = canExpandHero && isExpandedHero

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (effectiveExpanded && avatarResId != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clickable(onClick = onToggleExpand)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                    contentDescription = session.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(340.dp)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, scheme.background.copy(alpha = 0.94f))
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
                    .then(
                        if (canExpandHero) Modifier.clickable(onClick = onToggleExpand)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarResId != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = avatarResId),
                        contentDescription = session.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(92.dp)
                    )
                } else {
                    Text(
                        text = initials,
                        color = scheme.onAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
