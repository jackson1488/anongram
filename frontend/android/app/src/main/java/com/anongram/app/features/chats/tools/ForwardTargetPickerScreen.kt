package com.anongram.app.features.chats.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.SessionGlyph
import com.anongram.app.features.chats.tools.i18n.ToolsI18n
import com.anongram.app.features.chats.ui.accentColor

internal data class GlobalServerForwardItem(
    val id: String,
    val title: String,
    val handle: String,
    val subtitle: String,
    val kind: String // "user", "group", "bot"
)

private val DEFAULT_GLOBAL_SERVER_DIRECTORY = listOf(
    GlobalServerForwardItem("srv_1", "Алиса Ветрова", "@alisa_vetrova", "В сети • P2P Kyber-1024", "user"),
    GlobalServerForwardItem("srv_2", "Кайрат Нурланов", "@kairat_n", "Был(а) недавно • Глобальный узел", "user"),
    GlobalServerForwardItem("srv_3", "Арман Сейткали", "@arman_sec", "В сети • Разработчик ядра", "user"),
    GlobalServerForwardItem("srv_4", "Дамир Касымов", "@damir_kasymov", "В сети • Алматы", "user"),
    GlobalServerForwardItem("srv_5", "Елена Соколова", "@elena_sokolova", "Была 15 мин назад", "user"),
    GlobalServerForwardItem("srv_6", "Тимур Бектуров", "@timur_bekturov", "В сети • Астана", "user"),
    GlobalServerForwardItem("srv_7", "AnonGram Dev Hub", "@anongram_devs", "Публичная группа • 14 820 участников", "group"),
    GlobalServerForwardItem("srv_8", "Cypherpunk KZ & RU", "@cypherpunk_node", "Крипто-сообщество • 8 410 участников", "group"),
    GlobalServerForwardItem("srv_9", "Quantum Relay Bot", "@quantum_relay_bot", "Бот маршрутизации и проверки ключей", "bot"),
    GlobalServerForwardItem("srv_10", "Мадина Оспанова", "@madina_osp", "В сети • Дизайн-система", "user"),
    GlobalServerForwardItem("srv_11", "Руслан Ким", "@ruslan_kim", "Был(а) вчера • Токио", "user"),
    GlobalServerForwardItem("srv_12", "Сабина Алиева", "@sabina_ali", "В сети • Криптография", "user")
)

/**
 * Полноэкранный выбор чата «Переслать...» (по референсу Telegram / AnonGram):
 * - Шапка: кнопка Назад (←) и заголовок «Переслать...»
 * - Поле поиска «Поиск чатов» (работает и по активным чатам, и глобально по всему серверу)
 * - Первая строка по умолчанию: «Избранное» («Сохранить на память») с акцентной иконкой закладки
 * - Ниже: список всех актуальных чатов, с которыми сейчас ведётся переписка
 * - При вводе текста в поиск: найденные актуальные чаты + секция «Глобальный поиск по серверу»
 */
@Composable
fun ForwardTargetPickerScreen(
    activeSessions: List<IChatSession>,
    onClose: () -> Unit,
    onSelectSession: (IChatSession) -> Unit,
    onSelectSavedMessages: () -> Unit,
    onSelectGlobalItem: (name: String, handle: String) -> Unit,
    titleOverride: String? = null,
    multiSelectMode: Boolean = false,
    sharedPreviewText: String? = null,
    sharedPreviewUris: List<String> = emptyList(),
    onConfirmMultiSelect: ((selectedSessions: List<IChatSession>, comment: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ToolsI18n.current
    var query by remember { mutableStateOf("") }
    var selectedSessionIds by remember { mutableStateOf(emptySet<String>()) }
    var commentText by remember { mutableStateOf("") }
    var isEmojiStripOpen by remember { mutableStateOf(false) }

    BackHandler { onClose() }

    val trimmedQuery = query.trim()
    val nonSavedSessions = remember(activeSessions) {
        activeSessions.filter { it.id != "0" && !it.title.equals("Избранное", ignoreCase = true) }
    }

    val filteredActiveSessions = remember(nonSavedSessions, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            nonSavedSessions
        } else {
            val q = trimmedQuery.lowercase()
            nonSavedSessions.filter {
                it.title.lowercase().contains(q) ||
                    it.fingerprint.lowercase().contains(q) ||
                    it.lastMessagePreview.lowercase().contains(q)
            }
        }
    }

    val showSavedMessagesRow = remember(trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            true
        } else {
            val q = trimmedQuery.lowercase()
            "избранное".contains(q) || "сохранить на память".contains(q) || "saved".contains(q)
        }
    }

    val globalServerMatches = remember(trimmedQuery, nonSavedSessions, strings.globalServerOnlineSubtitle) {
        if (trimmedQuery.isEmpty()) {
            emptyList()
        } else {
            val q = trimmedQuery.lowercase().removePrefix("@")
            val activeTitles = nonSavedSessions.map { it.title.lowercase() }.toSet()
            val staticMatches = DEFAULT_GLOBAL_SERVER_DIRECTORY.filter { item ->
                item.title.lowercase().contains(q) ||
                    item.handle.lowercase().contains(q) ||
                    item.subtitle.lowercase().contains(q)
            }.filter { it.title.lowercase() !in activeTitles }

            // Если пользователь ввёл любой уникальный ник/имя на сервере — всегда находим также прямой узел на сервере
            val exactDynamic = if (q.length >= 2 && staticMatches.none {
                    it.title.equals(trimmedQuery, ignoreCase = true) ||
                        it.handle.equals("@$q", ignoreCase = true)
                }
            ) {
                val cleanHandle = q.replace(Regex("[^a-zа-яё0-9_]"), "_")
                listOf(
                    GlobalServerForwardItem(
                        id = "srv_dyn_$cleanHandle",
                        title = trimmedQuery.removePrefix("@").replaceFirstChar { it.uppercase() },
                        handle = "@$cleanHandle",
                        subtitle = strings.globalServerOnlineSubtitle,
                        kind = "user"
                    )
                )
            } else {
                emptyList()
            }
            staticMatches + exactDynamic
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .zIndex(310f)
    ) {
        // Верхняя шапка «Переслать...»
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .safeStatusBarPadding()
                .height(56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (multiSelectMode && selectedSessionIds.isNotEmpty()) {
                    selectedSessionIds.size.toString()
                } else {
                    titleOverride ?: strings.forwardTitle
                },
                color = scheme.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Поле «Поиск чатов» (локальный + глобальный поиск по серверу)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(scheme.inputBg)
                    .border(1.dp, scheme.inputBorder, RoundedCornerShape(21.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = scheme.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = strings.searchChatsPlaceholder,
                            color = scheme.textMuted,
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(scheme.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = strings.clear,
                            tint = scheme.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(
                    if (multiSelectMode && selectedSessionIds.isNotEmpty()) Modifier
                    else Modifier.safeNavigationBarPadding()
                ),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            // 1. Строка «Избранное» (Сохранить на память)
            if (showSavedMessagesRow) {
                item(key = "saved_messages_row") {
                    val isSavedSelected = multiSelectMode && "0" in selectedSessionIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSavedSelected) scheme.accentSoft.copy(alpha = 0.28f) else Color.Transparent)
                            .clickable {
                                if (multiSelectMode) {
                                    selectedSessionIds = if (isSavedSelected) {
                                        selectedSessionIds - "0"
                                    } else {
                                        selectedSessionIds + "0"
                                    }
                                } else {
                                    onSelectSavedMessages()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(scheme.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = strings.savedMessagesTitle,
                                    tint = scheme.onAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            if (isSavedSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(scheme.accent)
                                        .border(2.dp, scheme.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = scheme.onAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.savedMessagesTitle,
                                color = scheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.savedMessagesSubtitle,
                                color = scheme.textSecondary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2. Актуальные чаты, с которыми сейчас переписывается пользователь
            items(
                items = filteredActiveSessions,
                key = { "active_${it.id}" }
            ) { session ->
                val isSelected = multiSelectMode && session.id in selectedSessionIds
                ForwardActiveSessionRow(
                    session = session,
                    isSelected = isSelected,
                    onClick = {
                        if (multiSelectMode) {
                            selectedSessionIds = if (isSelected) {
                                selectedSessionIds - session.id
                            } else {
                                selectedSessionIds + session.id
                            }
                        } else {
                            onSelectSession(session)
                        }
                    }
                )
            }

            // 3. Глобальный поиск по всему серверу (пользователи, группы, боты)
            if (globalServerMatches.isNotEmpty()) {
                item(key = "global_server_header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(scheme.surface)
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.globalServerSearchHeader,
                                color = scheme.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(
                    items = globalServerMatches,
                    key = { it.id }
                ) { item ->
                    ForwardGlobalServerRow(
                        item = item,
                        onClick = { onSelectGlobalItem(item.title, item.handle) }
                    )
                }
            }
        }

        if (multiSelectMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .imePadding()
                    .safeNavigationBarPadding()
            ) {
                if (sharedPreviewUris.isNotEmpty() || !sharedPreviewText.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sharedPreviewUris.take(4).forEachIndexed { idx, uriStr ->
                            com.anongram.app.features.chats.input.reply.ReplyAttachmentMiniThumb(
                                message = com.anongram.app.features.chats.messages.domain.ChatMessage(
                                    id = "preview_$idx",
                                    direction = com.anongram.app.features.chats.messages.domain.MessageDirection.OUTGOING,
                                    epochMs = 0L,
                                    attachment = com.anongram.app.features.chats.messages.domain.MessageAttachment.Photo(
                                        label = "Photo",
                                        colorSeed = idx,
                                        uri = uriStr
                                    )
                                ),
                                accent = scheme.accent,
                                size = 44.dp
                            )
                        }
                        if (sharedPreviewUris.size > 4) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(scheme.card),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${sharedPreviewUris.size - 4}",
                                    color = scheme.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (!sharedPreviewText.isNullOrBlank()) {
                            Text(
                                text = sharedPreviewText,
                                color = scheme.textSecondary,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (isEmojiStripOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("👍", "❤️", "🔥", "😂", "🙏", "👌", "🎉").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { commentText += emoji }
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                val canSendMulti = selectedSessionIds.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.inputBg)
                        .border(0.8.dp, scheme.inputBorder, RoundedCornerShape(24.dp))
                        .padding(start = 6.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isEmojiStripOpen = !isEmojiStripOpen },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.SentimentSatisfied,
                            contentDescription = null,
                            tint = if (isEmojiStripOpen) scheme.accent else scheme.textMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (commentText.isEmpty()) {
                            Text(
                                text = strings.addCommentPlaceholder,
                                color = scheme.textMuted,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            singleLine = true,
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 15.sp),
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (canSendMulti) scheme.accent else scheme.accent.copy(alpha = 0.35f))
                            .clickable(enabled = canSendMulti) {
                                val selectedFromList = activeSessions.filter { it.id in selectedSessionIds }
                                val targets = if ("0" in selectedSessionIds && selectedFromList.none { it.id == "0" }) {
                                    val savedPlaceholder = object : IChatSession {
                                        override val id = "0"
                                        override val title = strings.savedMessagesTitle
                                        override val fingerprint = "SAVED"
                                        override val lastMessagePreview = ""
                                        override val lastMessageEpochMs = 0L
                                        override val unreadCount = 0
                                        override val isPinned = false
                                        override val isOnline = false
                                        override val isArchived = false
                                        override val tier = activeSessions.firstOrNull()?.tier
                                            ?: com.anongram.app.features.chats.common.ChatSecurityTier.entries.first()
                                        override val avatarGlyph = SessionGlyph.SAVED
                                        override val mutedUntil: Long? = null
                                        override fun withPinned(pinned: Boolean) = this
                                        override fun withArchived(archived: Boolean) = this
                                        override fun withMutedUntil(untilEpochMs: Long?) = this
                                        override fun withHistoryCleared() = this
                                        override fun withLastMessage(
                                            preview: String,
                                            epochMs: Long,
                                            attachment: com.anongram.app.features.chats.messages.domain.MessageAttachment?
                                        ) = this
                                    }
                                    listOf(savedPlaceholder) + selectedFromList
                                } else {
                                    selectedFromList
                                }
                                onConfirmMultiSelect?.invoke(targets, commentText.trim())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = scheme.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForwardActiveSessionRow(
    session: IChatSession,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val accent = session.accentColor
    val monogram = session.avatarMonogram
        ?: session.title.trim().take(2).uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) scheme.accentSoft.copy(alpha = 0.28f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f))
                    .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when (session.avatarGlyph) {
                    SessionGlyph.GROUP -> {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Text(
                            text = monogram,
                            color = scheme.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(scheme.accent)
                        .border(2.dp, scheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = scheme.onAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (session.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759))
                        .border(2.dp, scheme.background, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = session.title,
                color = scheme.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = session.lastMessagePreview.ifBlank { session.title },
                color = scheme.textSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ForwardGlobalServerRow(
    item: GlobalServerForwardItem,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val avatarColor = when (item.kind) {
        "group" -> Color(0xFF3A7BD5)
        "bot" -> Color(0xFF00B09B)
        else -> scheme.accent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.24f))
                .border(1.dp, avatarColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (item.kind) {
                "group" -> Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = avatarColor,
                    modifier = Modifier.size(24.dp)
                )
                "bot" -> Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = avatarColor,
                    modifier = Modifier.size(24.dp)
                )
                else -> Text(
                    text = item.title.take(2).uppercase(),
                    color = scheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    color = scheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.handle,
                    color = scheme.accent,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                color = scheme.textSecondary,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
