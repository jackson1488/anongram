package com.anongram.app.features.settings.animations.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.ui.animations.MessageAppearWrapper
import com.anongram.app.features.chats.ui.animations.rememberStreamedMessageText
import com.anongram.app.features.settings.animations.data.MessageAppearStyle
import com.anongram.app.features.settings.animations.data.TokenStreamSpeed
import com.anongram.app.features.settings.animations.i18n.AnimationsI18n

/**
 * Карточка настройки анимаций отправки/получения сообщений, AI-стриминга токенов и живого предпросмотра.
 */
@Composable
fun AnimationsMessagesCard(
    messageAppearEnabled: Boolean,
    onMessageAppearEnabledChange: (Boolean) -> Unit,
    appearStyle: MessageAppearStyle,
    onAppearStyleChange: (MessageAppearStyle) -> Unit,
    tokenStreamSpeed: TokenStreamSpeed,
    onTokenStreamSpeedChange: (TokenStreamSpeed) -> Unit,
    listItemShiftEnabled: Boolean,
    onListItemShiftEnabledChange: (Boolean) -> Unit,
    statusCheckAnimEnabled: Boolean,
    onStatusCheckAnimEnabledChange: (Boolean) -> Unit,
    speedMultiplier: Float,
    reduceMotionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = AnimationsI18n.current
    var previewTrigger by remember { mutableIntStateOf(0) }

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = scheme.onAccent,
        checkedTrackColor = scheme.accent,
        uncheckedThumbColor = scheme.textSecondary,
        uncheckedTrackColor = scheme.surface,
        uncheckedBorderColor = scheme.cardBorder
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = strings.messagesSectionTitle,
            color = scheme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Общий переключатель анимации отправки и получения
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.messagesAppearToggleTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.messagesAppearToggleDesc,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = messageAppearEnabled,
                onCheckedChange = onMessageAppearEnabledChange,
                colors = switchColors
            )
        }

        AnimatedVisibility(visible = messageAppearEnabled) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = scheme.divider)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. Живой предпросмотр выбранной анимации
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.previewHeader,
                        color = scheme.textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.accent.copy(alpha = 0.14f))
                            .clickable { previewTrigger++ }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = scheme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.previewReplayBtn,
                            color = scheme.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val animKey = "${appearStyle.id}_${tokenStreamSpeed.id}_$previewTrigger"
                val shouldAnimatePreview = !reduceMotionEnabled

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surface)
                        .border(0.5.dp, scheme.cardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Входящее сообщение (слева)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        MessageAppearWrapper(
                            animateKey = "in_$animKey",
                            isIncoming = true,
                            shouldAnimate = shouldAnimatePreview,
                            style = appearStyle,
                            speedMultiplier = speedMultiplier
                        ) {
                            val inText = rememberStreamedMessageText(
                                fullText = strings.previewIncomingSample,
                                animateKey = "in_txt_$animKey",
                                shouldStream = shouldAnimatePreview && appearStyle == MessageAppearStyle.TOKEN_STREAM,
                                tokenSpeed = tokenStreamSpeed,
                                speedMultiplier = speedMultiplier
                            )
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                                    .background(scheme.card)
                                    .border(0.5.dp, scheme.cardBorder, RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                                    .padding(horizontal = 11.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = inText,
                                    color = scheme.incomingText,
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    // Исходящее сообщение (справа)
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        MessageAppearWrapper(
                            animateKey = "out_$animKey",
                            isIncoming = false,
                            shouldAnimate = shouldAnimatePreview,
                            style = appearStyle,
                            speedMultiplier = speedMultiplier
                        ) {
                            val outText = rememberStreamedMessageText(
                                fullText = strings.previewOutgoingSample,
                                animateKey = "out_txt_$animKey",
                                shouldStream = shouldAnimatePreview && appearStyle == MessageAppearStyle.TOKEN_STREAM,
                                tokenSpeed = tokenStreamSpeed,
                                speedMultiplier = speedMultiplier
                            )
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clip(RoundedCornerShape(14.dp, 14.dp, 4.dp, 14.dp))
                                    .background(scheme.accent)
                                    .padding(horizontal = 11.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = outText,
                                    color = scheme.outgoingText,
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = strings.messageStyleHeader,
                    color = scheme.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val styleOptions = listOf(
                    Triple(MessageAppearStyle.TOKEN_STREAM, strings.styleStreamTitle, strings.styleStreamDesc),
                    Triple(MessageAppearStyle.SPRING_FLY, strings.styleSpringTitle, strings.styleSpringDesc),
                    Triple(MessageAppearStyle.SLIDE_FADE, strings.styleSlideTitle, strings.styleSlideDesc),
                    Triple(MessageAppearStyle.CORNER_POP, strings.stylePopTitle, strings.stylePopDesc)
                )

                styleOptions.forEachIndexed { idx, (style, title, desc) ->
                    val selected = appearStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onAppearStyleChange(style)
                                previewTrigger++
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = title,
                                color = if (selected) scheme.accent else scheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Text(
                                text = desc,
                                color = scheme.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 15.5.sp
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (idx < styleOptions.lastIndex) {
                        HorizontalDivider(color = scheme.divider.copy(alpha = 0.6f))
                    }
                }

                // 3. Скорость токенов стриминга (только если выбран TOKEN_STREAM)
                AnimatedVisibility(visible = appearStyle == MessageAppearStyle.TOKEN_STREAM) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.streamSpeedHeader,
                            color = scheme.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        val speeds = listOf(
                            TokenStreamSpeed.SLOW to strings.speedSlow,
                            TokenStreamSpeed.MEDIUM to strings.speedMedium,
                            TokenStreamSpeed.FAST to strings.speedFast,
                            TokenStreamSpeed.TURBO to strings.speedTurbo
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            speeds.forEach { (sp, label) ->
                                val isSel = tokenStreamSpeed == sp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) scheme.accent else scheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) scheme.accent else scheme.cardBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            onTokenStreamSpeedChange(sp)
                                            previewTrigger++
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSel) scheme.onAccent else scheme.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 4. Плавный сдвиг ленты
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.listShiftToggleTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.listShiftToggleDesc,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = listItemShiftEnabled,
                onCheckedChange = onListItemShiftEnabledChange,
                colors = switchColors
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = scheme.divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 5. Анимация галочек
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.statusCheckToggleTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.statusCheckToggleDesc,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = statusCheckAnimEnabled,
                onCheckedChange = onStatusCheckAnimEnabledChange,
                colors = switchColors
            )
        }
    }
}
