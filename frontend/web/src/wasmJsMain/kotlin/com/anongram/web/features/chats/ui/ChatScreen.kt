package com.anongram.web.features.chats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.CrimsonKillSwitch
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.theme.NeonEmerald
import com.anongram.web.core.ui.safeNavigationBarPadding
import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.messages.domain.ChatMessage
import com.anongram.web.features.chats.messages.domain.FileKind
import com.anongram.web.features.chats.messages.domain.MessageAttachment
import com.anongram.web.features.chats.messages.domain.MessageDirection
import com.anongram.web.features.chats.messages.domain.MessageStatus
import com.anongram.web.features.chats.messages.domain.attachmentColor
import com.anongram.web.features.chats.messages.domain.epochDateLabel
import com.anongram.web.features.chats.messages.domain.isSameDay
import com.anongram.web.features.chats.messages.domain.timeLabel
import com.anongram.web.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.web.features.chats.messages.state.SelfDestruct

@Composable
fun ChatScreen(
    session: IChatSession,
    viewModel: ChatMessagesViewModel,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scheme = LocalAnonGramScheme.current
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    LaunchedEffect(state.searchIndex, state.searchMatches) {
        val targetId = state.searchMatches.getOrNull(state.searchIndex)
        if (targetId != null) {
            val idx = state.messages.indexOfFirst { it.id == targetId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
                viewModel.highlightMessage(targetId)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ChatWallpaper(style = state.wallpaperStyle)

        Column(modifier = Modifier.fillMaxSize()) {
            ChatTopBar(
                session = session,
                isPeerTyping = state.isPeerTyping,
                peerActivity = state.peerActivity,
                selfDestruct = state.selfDestruct,
                showBackButton = showBackButton,
                onBack = onBack,
                onOpenSearch = viewModel::openSearch,
                onCycleWallpaper = { viewModel.setWallpaperStyle((state.wallpaperStyle + 1) % 3) },
                onOpenKeyCheck = { viewModel.toggleKeyCheck(true) },
                onCycleSelfDestruct = {
                    val entries = SelfDestruct.entries
                    val next = entries[(entries.indexOf(state.selfDestruct) + 1) % entries.size]
                    viewModel.setSelfDestruct(next)
                },
                onClearHistory = { viewModel.requestDelete(state.messages.map { it.id }) }
            )

            // Search bar inside chat
            if (state.isSearchOpen) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = scheme.islandBg,
                    border = BorderStroke(1.dp, scheme.islandBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (state.searchQuery.isEmpty()) {
                                Text("Поиск в чате…", color = scheme.textMuted, fontSize = 13.sp)
                            }
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                textStyle = TextStyle(color = scheme.textPrimary, fontSize = 13.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(scheme.accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (state.searchCounter.isNotEmpty()) {
                            Text(state.searchCounter, color = scheme.textMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                        }
                        IconButton(onClick = viewModel::prevSearchMatch, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Prev", tint = scheme.textPrimary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = viewModel::nextSearchMatch, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Next", tint = scheme.textPrimary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = viewModel::closeSearch, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close", tint = scheme.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Pinned banner
            state.pinnedMessage?.let { pinned ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clickable {
                            val idx = state.messages.indexOfFirst { it.id == pinned.id }
                            if (idx >= 0) viewModel.highlightMessage(pinned.id)
                            viewModel.cyclePinned()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.islandBg,
                    border = BorderStroke(1.dp, scheme.islandBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.PushPin, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Закреплённое сообщение ${if (state.pinnedIds.size > 1) "(${state.pinnedIndex + 1}/${state.pinnedIds.size})" else ""}",
                                color = scheme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = pinned.text.ifBlank { "Вложение" },
                                color = scheme.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.hasMoreOlder) {
                    item(key = "load_older") {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            if (state.isLoadingOlder) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = scheme.accent, strokeWidth = 2.dp)
                            } else {
                                TextButton(onClick = viewModel::loadOlder) {
                                    Text("Загрузить ранние сообщения", color = scheme.accent, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(state.messages, key = { _, m -> m.id }) { index, message ->
                    val prev = state.messages.getOrNull(index - 1)
                    if (prev == null || !isSameDay(prev.epochMs, message.epochMs)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = epochDateLabel(message.epochMs),
                                color = scheme.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(scheme.islandBg)
                                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (state.firstUnreadId == message.id) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Непрочитанные сообщения",
                                color = scheme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val replyTarget = message.replyToId?.let { rid -> state.messages.firstOrNull { it.id == rid } }
                    MessageBubbleRow(
                        message = message,
                        replyTarget = replyTarget,
                        isHighlighted = state.highlightedMessageId == message.id,
                        isSelected = message.id in state.selection,
                        isMenuOpen = state.actionMenuFor?.id == message.id,
                        onClick = {
                            if (state.isSelectionMode) viewModel.toggleSelection(message.id)
                            else viewModel.openActionMenu(message)
                        },
                        onLongClick = { viewModel.toggleSelection(message.id) },
                        onDismissMenu = viewModel::dismissActionMenu,
                        onReply = { viewModel.startReply(message) },
                        onEdit = { viewModel.startEditing(message) },
                        onPin = { viewModel.togglePin(message.id) },
                        onDelete = { viewModel.requestDelete(listOf(message.id)) },
                        onReact = { emoji -> viewModel.toggleReaction(message.id, emoji) }
                    )
                }
            }

            // Attach & Emoji popups
            if (state.isAttachOpen) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.islandBg,
                    border = BorderStroke(1.dp, scheme.islandBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AttachOption(Icons.Outlined.Photo, "Фото") {
                            viewModel.sendAttachment(MessageAttachment.Photo("IMG_Demo", 2), "Зашифрованное фото")
                        }
                        AttachOption(Icons.Outlined.AttachFile, "Файл") {
                            viewModel.sendAttachment(MessageAttachment.File("key_backup.enc", "128 КБ", FileKind.ARCHIVE))
                        }
                        AttachOption(Icons.Outlined.MusicNote, "Музыка") {
                            viewModel.sendAttachment(MessageAttachment.Music("Onion Beat", "AnonGram", "3:18", 1))
                        }
                        AttachOption(Icons.Outlined.LocationOn, "Гео") {
                            viewModel.sendAttachment(MessageAttachment.Geo("Zero-Knowledge Node #4", 42.87, 74.59))
                        }
                        AttachOption(Icons.Outlined.Person, "Контакт") {
                            viewModel.sendAttachment(MessageAttachment.Contact("Боб", "+996 555 01-02-03", "0x1B77…D3A0", true))
                        }
                    }
                }
            }

            if (state.isEmojiOpen) {
                val emojis = listOf("👍", "🔥", "❤️", "⚡", "🛡️", "🔐", "🐉", "🚀", "😎", "🎉")
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = scheme.islandBg,
                    border = BorderStroke(1.dp, scheme.islandBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.onInputChange(state.input + emoji) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // Reply / Edit banner
            val bannerTitle = when {
                state.editing != null -> "Редактирование"
                state.replyTo != null -> "Ответ на сообщение"
                else -> null
            }
            val bannerText = state.editing?.text ?: state.replyTo?.text?.ifBlank { "Вложение" }
            if (bannerTitle != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.islandBg,
                    border = BorderStroke(1.dp, scheme.islandBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(26.dp).background(scheme.accent, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bannerTitle, color = scheme.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(bannerText ?: "", color = scheme.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(
                            onClick = { if (state.editing != null) viewModel.cancelEditing() else viewModel.cancelReply() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Отмена", tint = scheme.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Bottom floating Input Pill
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeNavigationBarPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                shape = RoundedCornerShape(26.dp),
                color = scheme.islandBg,
                border = BorderStroke(1.dp, scheme.islandBorder),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::toggleEmoji) {
                        Icon(Icons.Outlined.EmojiEmotions, contentDescription = "Эмодзи", tint = if (state.isEmojiOpen) scheme.accent else scheme.textSecondary)
                    }
                    Box(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        if (state.input.isEmpty()) {
                            Text("Сообщение…", color = scheme.textMuted, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = state.input,
                            onValueChange = viewModel::onInputChange,
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(scheme.accent),
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                                        viewModel.sendMessage()
                                        true
                                    } else {
                                        false
                                    }
                                }
                        )
                    }
                    IconButton(onClick = viewModel::toggleAttach) {
                        Icon(Icons.Outlined.Add, contentDescription = "Вложение", tint = if (state.isAttachOpen) scheme.accent else scheme.textSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (state.canSend) scheme.accent else scheme.accent.copy(alpha = 0.25f))
                            .clickable(enabled = state.canSend, onClick = viewModel::sendMessage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.editing != null) Icons.Outlined.Check else Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = scheme.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (state.isKeyCheckOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleKeyCheck(false) },
            containerColor = scheme.surface,
            title = { Text("Проверка ключей (SAS)", color = scheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Собеседник: ${session.title}", color = scheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Отпечаток: ${session.fingerprint}", color = scheme.accent, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("Шифрование: ${session.tier.label}", color = NeonEmerald, fontSize = 12.sp)
                    Text("Сравните отпечаток при личной встрече или по доверенному каналу для исключения MITM.", color = scheme.textSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleKeyCheck(false) }) {
                    Text("Готово", color = scheme.accent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (state.deleteDialogFor != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            containerColor = scheme.surface,
            title = { Text("Удалить сообщения", color = scheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Удалить выбранные сообщения (${state.deleteDialogFor!!.size})?", color = scheme.textSecondary, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Удалить", color = CrimsonKillSwitch, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Отмена", color = scheme.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun AttachOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = scheme.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = scheme.textSecondary, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleRow(
    message: ChatMessage,
    replyTarget: ChatMessage?,
    isHighlighted: Boolean,
    isSelected: Boolean,
    isMenuOpen: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val isOut = message.direction == MessageDirection.OUTGOING
    val bubbleColor = if (isOut) scheme.outgoingBubbleStart else scheme.incomingBubbleBg
    val textColor = if (isOut) scheme.outgoingText else scheme.incomingText
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isOut) 18.dp else 4.dp,
        bottomEnd = if (isOut) 4.dp else 18.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected || isHighlighted) scheme.accent.copy(alpha = 0.14f) else Color.Transparent)
            .padding(vertical = 1.dp),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
    ) {
        Box {
            if (message.isStickerOnly && message.attachment is MessageAttachment.Sticker) {
                Text(
                    text = message.attachment.symbol,
                    fontSize = 56.sp,
                    modifier = Modifier
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                        .padding(4.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(min = 84.dp, max = 340.dp)
                        .clip(bubbleShape)
                        .background(bubbleColor)
                        .border(1.dp, scheme.divider.copy(alpha = 0.4f), bubbleShape)
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (replyTarget != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(scheme.background.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(2.5.dp).height(24.dp).background(scheme.accent, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = replyTarget.text.ifBlank { "Вложение" },
                                color = textColor.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    message.attachment?.let { att ->
                        AttachmentBlock(att = att, textColor = textColor)
                        if (message.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (message.text.isNotBlank()) {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }

                    message.linkPreview?.let { lp ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(scheme.background.copy(alpha = 0.25f))
                                .padding(8.dp)
                        ) {
                            Text(lp.domain, color = scheme.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(lp.title, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(lp.description, color = textColor.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 2)
                        }
                    }

                    if (message.reactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            message.reactions.forEach { (emoji, count) ->
                                val isMine = message.myReaction == emoji
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isMine) scheme.accent.copy(alpha = 0.25f) else scheme.background.copy(alpha = 0.25f))
                                        .clickable { onReact(emoji) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$emoji $count", color = textColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.isEdited) {
                            Text("изм.", color = textColor.copy(alpha = 0.6f), fontSize = 10.sp)
                        }
                        Text(message.timeLabel(), color = textColor.copy(alpha = 0.65f), fontSize = 10.sp)
                        if (isOut) {
                            val icon = when (message.status) {
                                MessageStatus.SENDING -> Icons.Outlined.Schedule
                                MessageStatus.SENT -> Icons.Outlined.Check
                                MessageStatus.READ -> Icons.Outlined.DoneAll
                                MessageStatus.FAILED -> Icons.Outlined.Close
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (message.status == MessageStatus.READ) scheme.accent else textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = onDismissMenu,
                containerColor = scheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("👍", "🔥", "❤️", "⚡", "🛡️").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.clickable { onReact(emoji) }
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text("Ответить", color = scheme.textPrimary) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = scheme.textPrimary) },
                    onClick = onReply
                )
                if (isOut) {
                    DropdownMenuItem(
                        text = { Text("Редактировать", color = scheme.textPrimary) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = scheme.textPrimary) },
                        onClick = onEdit
                    )
                }
                DropdownMenuItem(
                    text = { Text("Закрепить", color = scheme.textPrimary) },
                    leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null, tint = scheme.textPrimary) },
                    onClick = onPin
                )
                DropdownMenuItem(
                    text = { Text("Удалить", color = CrimsonKillSwitch) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = CrimsonKillSwitch) },
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun AttachmentBlock(att: MessageAttachment, textColor: Color) {
    val scheme = LocalAnonGramScheme.current
    when (att) {
        is MessageAttachment.Photo -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(attachmentColor(att.colorSeed)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Photo, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(32.dp))
                    if (att.label.isNotBlank()) {
                        Text(att.label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }
        }
        is MessageAttachment.File -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.background.copy(alpha = 0.25f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(scheme.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(att.kind.label, color = scheme.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(att.name, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(att.sizeLabel, color = textColor.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
        is MessageAttachment.Music -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.background.copy(alpha = 0.25f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(scheme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = scheme.onAccent)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(att.title, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${att.artist} · ${att.durationLabel}", color = textColor.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
        is MessageAttachment.Voice -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(scheme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = scheme.onAccent, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    att.waveform.take(20).forEach { bar ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((bar * 1.4f).coerceIn(4f, 22f).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(scheme.accent.copy(alpha = 0.75f))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(att.durationLabel, color = textColor.copy(alpha = 0.75f), fontSize = 11.sp)
            }
        }
        is MessageAttachment.Geo -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.background.copy(alpha = 0.25f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = scheme.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(att.address, color = textColor, fontSize = 12.sp)
            }
        }
        is MessageAttachment.Contact -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.background.copy(alpha = 0.25f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = scheme.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(att.name, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (att.phone.isNotBlank()) Text(att.phone, color = textColor.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
        else -> {}
    }
}
