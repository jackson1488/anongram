package com.anongram.app.features.chats.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.TextSecondary

/** Плавающая кнопка «прокрутить вниз» над полем ввода (как в Telegram). */
@Composable
internal fun ScrollDownButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(CardDark)
                .border(1.dp, CardBorder, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Прокрутить вниз",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
