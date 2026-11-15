package com.anongram.app.features.services.ai.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.services.ai.data.AiMessage
import com.anongram.app.features.services.ai.i18n.LocalAiI18nStrings

@Composable
fun LocalAiMessageBubble(
    message: AiMessage,
    isGeneratingThis: Boolean,
    strings: LocalAiI18nStrings,
    onRegenerate: (String) -> Unit,
    onEditThoughtsAndRegenerate: (String, String) -> Unit,
    onEditUserMessage: (String, String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    if (message.isUser) {
        var isEditingUser by remember(message.id) { mutableStateOf(false) }
        var editedUserText by remember(message.id, message.content) { mutableStateOf(message.content) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            if (message.attachments.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    message.attachments.forEach { att ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(scheme.card)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = att.name,
                                color = scheme.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (isEditingUser) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 310.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.accent, RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = editedUserText,
                        onValueChange = { editedUserText = it },
                        textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.5.sp),
                        cursorBrush = SolidColor(scheme.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.thoughtCancel,
                            color = scheme.textMuted,
                            fontSize = 12.5.sp,
                            modifier = Modifier.clickable { isEditingUser = false }
                        )
                        Text(
                            text = strings.regenerateBtn,
                            color = scheme.accent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                isEditingUser = false
                                if (editedUserText.isNotBlank()) {
                                    onEditUserMessage(message.id, editedUserText)
                                }
                            }
                        )
                    }
                }
            } else {
                val bubbleShape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .clip(bubbleShape)
                        .background(Brush.linearGradient(listOf(scheme.outgoingBubbleStart, scheme.outgoingBubbleEnd)))
                        .border(1.dp, scheme.outgoingBubbleBorder, bubbleShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.content,
                        color = scheme.outgoingText,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = strings.copyBtn,
                        tint = scheme.textMuted,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable {
                                clipboard.setText(AnnotatedString(message.content))
                                Toast.makeText(context, strings.copiedToast, Toast.LENGTH_SHORT).show()
                            }
                    )
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = strings.editMessageBtn,
                        tint = scheme.textMuted,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { isEditingUser = true }
                    )
                }
            }
        }
        return
    }

    // Сообщение ИИ
    var searchExpanded by remember(message.id) { mutableStateOf(false) }
    var thoughtsExpanded by remember(message.id) { mutableStateOf(false) }
    var editingThoughts by remember(message.id) { mutableStateOf(false) }
    var editedThoughts by remember(message.id, message.thinking) { mutableStateOf(message.thinking) }

    Column(modifier = Modifier.fillMaxWidth()) {
        val isSearchingNow = !message.searchStatus.isNullOrBlank()
        val hasSearchData = isSearchingNow || message.searchSteps.isNotEmpty() || message.webSources.isNotEmpty()
        if (hasSearchData) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.card)
                    .border(
                        width = 1.dp,
                        color = if (isSearchingNow) scheme.accent.copy(alpha = 0.55f) else scheme.islandBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { searchExpanded = !searchExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSearchingNow) {
                            message.searchStatus!!
                        } else {
                            strings.searchBlockTitle(message.webSources.size, false)
                        },
                        color = if (isSearchingNow) scheme.accent else scheme.textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (searchExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = scheme.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = searchExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.searchSteps.forEach { step ->
                            Text(
                                text = step,
                                color = scheme.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                        if (message.webSources.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            message.webSources.forEach { src ->
                                val host = runCatching { Uri.parse(src.url).host?.removePrefix("www.") }.getOrNull().orEmpty()
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scheme.surface)
                                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(src.url)))
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = src.title.ifBlank { host },
                                        color = scheme.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (host.isNotBlank()) {
                                        Text(
                                            text = host,
                                            color = scheme.textMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (src.snippet.isNotBlank()) {
                                        Text(
                                            text = src.snippet,
                                            color = scheme.textSecondary,
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (message.thinking.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { thoughtsExpanded = !thoughtsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.thoughtBlockTitle(message.thinkingDurationSec),
                        color = scheme.textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (thoughtsExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = scheme.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = thoughtsExpanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (editingThoughts) {
                            BasicTextField(
                                value = editedThoughts,
                                onValueChange = { editedThoughts = it },
                                textStyle = TextStyle(
                                    color = scheme.textPrimary,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                ),
                                cursorBrush = SolidColor(scheme.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(scheme.surface)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = strings.thoughtCancel,
                                    color = scheme.textMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { editingThoughts = false }
                                )
                                Text(
                                    text = strings.thoughtApplyRegen,
                                    color = scheme.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        editingThoughts = false
                                        onEditThoughtsAndRegenerate(message.id, editedThoughts)
                                    }
                                )
                            }
                        } else {
                            Text(
                                text = message.thinking,
                                color = scheme.textSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        editedThoughts = message.thinking
                                        editingThoughts = true
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.thoughtEditBtn,
                                    color = scheme.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = message.content.ifBlank { if (isGeneratingThis) strings.generatingLabel else "" },
            color = if (message.content.isBlank()) scheme.textMuted else scheme.textPrimary,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        if (!isGeneratingThis && message.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionIconLabel(
                    icon = Icons.Outlined.ContentCopy,
                    label = strings.copyBtn,
                    onClick = {
                        clipboard.setText(AnnotatedString(message.content))
                        Toast.makeText(context, strings.copiedToast, Toast.LENGTH_SHORT).show()
                    }
                )
                ActionIconLabel(
                    icon = Icons.Outlined.Refresh,
                    label = strings.regenerateBtn,
                    onClick = { onRegenerate(message.id) }
                )
                ActionIconLabel(
                    icon = Icons.Outlined.Share,
                    label = strings.shareBtn,
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message.content)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, strings.shareBtn))
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionIconLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = scheme.textMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = scheme.textMuted,
            fontSize = 12.sp
        )
    }
}
