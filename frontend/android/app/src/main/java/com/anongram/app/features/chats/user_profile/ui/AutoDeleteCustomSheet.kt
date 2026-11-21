package com.anongram.app.features.chats.user_profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.state.SelfDestruct
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings
import com.anongram.app.features.chats.user_profile.model.AutoDeleteWheelOption

@Composable
internal fun AutoDeleteCustomSheet(
    current: SelfDestruct,
    isRu: Boolean,
    strings: UserProfileStrings,
    onDismiss: () -> Unit,
    onApply: (SelfDestruct) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val options = remember { AutoDeleteWheelOption.entries }
    var selectedIdx by remember(current) {
        mutableIntStateOf(options.indexOfFirst { it.mapped == current }.coerceAtLeast(0))
    }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIdx) {
        runCatching { listState.animateScrollToItem((selectedIdx - 1).coerceAtLeast(0)) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(30f)
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(scheme.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = strings.autoDeleteSheetTitle,
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(options) { idx, option ->
                        val isSelected = idx == selectedIdx
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) scheme.card else Color.Transparent
                                )
                                .clickable { selectedIdx = idx }
                        ) {
                            if (isSelected) {
                                HorizontalDivider(color = scheme.accent.copy(alpha = 0.65f), thickness = 1.dp)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRu) option.ruLabel else option.enLabel,
                                    color = if (isSelected) scheme.textPrimary else scheme.textMuted,
                                    fontSize = if (isSelected) 16.sp else 14.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                            if (isSelected) {
                                HorizontalDivider(color = scheme.accent.copy(alpha = 0.65f), thickness = 1.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val chosen = options[selectedIdx].mapped
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.accent)
                    .clickable {
                        onApply(chosen)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (chosen == SelfDestruct.OFF) {
                        strings.autoDeleteDisableBtn
                    } else {
                        strings.autoDeleteEnableBtn
                    },
                    color = scheme.onAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
