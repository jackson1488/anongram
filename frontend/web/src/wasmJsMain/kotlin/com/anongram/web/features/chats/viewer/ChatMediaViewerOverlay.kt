package com.anongram.web.features.chats.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.safeNavigationBarPadding
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.chats.messages.domain.MessageAttachment
import com.anongram.web.features.chats.messages.domain.attachmentColor

@Composable
fun ChatMediaViewerOverlay(
    attachment: MessageAttachment,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val itemsCount = when (attachment) {
        is MessageAttachment.MediaAlbum -> attachment.items.size.coerceAtLeast(1)
        else -> 1
    }
    var pageIndex by remember(attachment) { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (itemsCount > 1) "${pageIndex + 1} / $itemsCount" else "AnonGram Media",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(380.dp)
                .clip(if (attachment is MessageAttachment.VideoCircle) CircleShape else RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            attachmentColor(pageIndex + 1),
                            scheme.accentDim.copy(alpha = 0.65f)
                        )
                    )
                )
                .border(
                    1.5.dp,
                    scheme.accent.copy(alpha = 0.7f),
                    if (attachment is MessageAttachment.VideoCircle) CircleShape else RoundedCornerShape(24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val isVideo = attachment is MessageAttachment.VideoCircle ||
                    (attachment is MessageAttachment.Photo && attachment.isVideo) ||
                    (attachment is MessageAttachment.MediaAlbum && attachment.items.getOrNull(pageIndex)?.isVideo == true)
                Icon(
                    imageVector = if (isVideo) Icons.Default.PlayCircleOutline else Icons.Outlined.Photo,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                val label = when (attachment) {
                    is MessageAttachment.Photo -> attachment.label.ifBlank { "E2EE Media" }
                    is MessageAttachment.VideoCircle -> "Video Note · ${attachment.durationLabel}"
                    is MessageAttachment.MediaAlbum -> "Album Item #${pageIndex + 1}"
                    else -> "Media"
                }
                Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (itemsCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (pageIndex > 0) 0.18f else 0.06f))
                        .clickable(enabled = pageIndex > 0) { pageIndex-- },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (pageIndex < itemsCount - 1) 0.18f else 0.06f))
                        .clickable(enabled = pageIndex < itemsCount - 1) { pageIndex++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
