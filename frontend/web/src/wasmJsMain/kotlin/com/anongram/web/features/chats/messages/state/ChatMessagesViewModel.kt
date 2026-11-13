package com.anongram.web.features.chats.messages.state

import com.anongram.web.features.chats.data.ChatListRepository
import com.anongram.web.features.chats.data.InMemoryChatListRepository
import com.anongram.web.features.chats.messages.data.ChatMessagesRepository
import com.anongram.web.features.chats.messages.domain.ChatMessage
import com.anongram.web.features.chats.messages.domain.LinkPreview
import com.anongram.web.features.chats.messages.domain.MessageAttachment
import com.anongram.web.features.chats.messages.domain.MessageDirection
import com.anongram.web.features.chats.messages.domain.MessageStatus
import com.anongram.web.features.chats.messages.domain.matchesQuery
import com.anongram.web.features.chats.messages.domain.resolveSubItem
import com.anongram.web.platform.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatMessagesViewModel(
    private val sessionId: String,
    private val repository: ChatMessagesRepository,
    private val chatsRepository: ChatListRepository = InMemoryChatListRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _uiState = MutableStateFlow(ChatMessagesUiState())
    val uiState: StateFlow<ChatMessagesUiState> = _uiState.asStateFlow()

    private var highlightJob: Job? = null
    private var localCounter = 1L

    init {
        scope.launch {
            val drafts = chatsRepository.loadDrafts()
            val loaded = repository.loadMessages(sessionId)
            val initialPinned = if (sessionId == "2" && loaded.size > 2) listOf(loaded[1].id, loaded[2].id) else emptyList()
            _uiState.update {
                it.copy(
                    messages = loaded,
                    input = drafts[sessionId] ?: "",
                    firstUnreadId = loaded.firstOrNull { message -> message.isUnread }?.id,
                    pinnedIds = initialPinned,
                    pinnedId = initialPinned.lastOrNull(),
                    hasMoreOlder = repository.hasMoreOlder(sessionId)
                )
            }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
        scope.launch { chatsRepository.saveDraft(sessionId, value) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val editing = state.editing
        if (editing != null) {
            val trimmed = state.input.trim()
            if (trimmed.isEmpty() && editing.attachment == null) return
            val updated = editing.copy(text = trimmed, isEdited = true)
            _uiState.update {
                it.copy(
                    messages = it.messages.map { m -> if (m.id == updated.id) updated else m },
                    input = "",
                    editing = null,
                    editingSubIndex = null
                )
            }
            scope.launch {
                repository.updateMessage(sessionId, updated)
                chatsRepository.saveDraft(sessionId, "")
            }
            return
        }

        val text = state.input.trim()
        if (text.isEmpty()) return

        val preview = extractLinkPreview(text)
        val now = currentTimeMillis()
        localCounter += 1
        val msg = ChatMessage(
            id = "out_${sessionId}_${now}_$localCounter",
            direction = MessageDirection.OUTGOING,
            text = text,
            epochMs = now,
            status = MessageStatus.SENDING,
            replyToId = state.replyTo?.id,
            replyToSubIndex = state.replyToSubIndex,
            linkPreview = preview
        )
        _uiState.update {
            it.copy(
                messages = it.messages + msg,
                input = "",
                replyTo = null,
                replyToSubIndex = null,
                isAttachOpen = false,
                isEmojiOpen = false
            )
        }
        scope.launch {
            chatsRepository.saveDraft(sessionId, "")
            repository.addMessage(sessionId, msg)
            chatsRepository.updateLastMessage(sessionId, msg.text, msg.epochMs, msg.attachment)
            delay(250)
            repository.updateStatus(sessionId, msg.id, MessageStatus.SENT)
            _uiState.update { s ->
                s.copy(messages = s.messages.map { if (it.id == msg.id) it.copy(status = MessageStatus.SENT) else it })
            }
            delay(450)
            repository.updateStatus(sessionId, msg.id, MessageStatus.READ)
            _uiState.update { s ->
                s.copy(messages = s.messages.map { if (it.id == msg.id) it.copy(status = MessageStatus.READ) else it })
            }
            _uiState.update { it.copy(isPeerTyping = true, peerActivity = "печатает...") }
            delay(900)
            val reply = repository.generateAutoReply(sessionId)
            _uiState.update { it.copy(isPeerTyping = false, peerActivity = null) }
            if (reply != null) {
                repository.addMessage(sessionId, reply)
                _uiState.update { s -> s.copy(messages = s.messages + reply) }
                val replyPreview = reply.text.ifBlank { if (reply.attachment is MessageAttachment.Voice) "Голосовое сообщение" else "Вложение" }
                chatsRepository.updateLastMessage(sessionId, replyPreview, reply.epochMs, reply.attachment)
            }
        }
    }

    fun sendAttachment(attachment: MessageAttachment, caption: String = "") {
        val now = currentTimeMillis()
        localCounter += 1
        val msg = ChatMessage(
            id = "att_${sessionId}_${now}_$localCounter",
            direction = MessageDirection.OUTGOING,
            text = caption.trim(),
            epochMs = now,
            status = MessageStatus.SENT,
            replyToId = _uiState.value.replyTo?.id,
            replyToSubIndex = _uiState.value.replyToSubIndex,
            attachment = attachment
        )
        _uiState.update {
            it.copy(
                messages = it.messages + msg,
                replyTo = null,
                replyToSubIndex = null,
                isAttachOpen = false,
                isEmojiOpen = false
            )
        }
        scope.launch {
            repository.addMessage(sessionId, msg)
            val preview = msg.text.ifBlank {
                when (attachment) {
                    is MessageAttachment.Photo -> "Фото"
                    is MessageAttachment.File -> attachment.name
                    is MessageAttachment.Music -> "${attachment.artist} — ${attachment.title}"
                    is MessageAttachment.Voice -> "Голосовое сообщение"
                    is MessageAttachment.Sticker -> attachment.symbol
                    is MessageAttachment.Geo -> "Геопозиция"
                    is MessageAttachment.Contact -> "Контакт: ${attachment.name}"
                    else -> "Вложение"
                }
            }
            chatsRepository.updateLastMessage(sessionId, preview, msg.epochMs, attachment)
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val target = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val nextReactions = target.reactions.toMutableMap()
        val prev = target.myReaction
        if (prev != null) {
            val count = (nextReactions[prev] ?: 1) - 1
            if (count <= 0) nextReactions.remove(prev) else nextReactions[prev] = count
        }
        val nextMy = if (prev == emoji) null else {
            nextReactions[emoji] = (nextReactions[emoji] ?: 0) + 1
            emoji
        }
        val updated = target.copy(reactions = nextReactions, myReaction = nextMy)
        _uiState.update { s -> s.copy(messages = s.messages.map { if (it.id == messageId) updated else it }, actionMenuFor = null) }
        scope.launch { repository.updateMessage(sessionId, updated) }
    }

    fun startReply(message: ChatMessage, subIndex: Int? = null) {
        val resolved = message.resolveSubItem(subIndex)
        _uiState.update { it.copy(replyTo = resolved, replyToSubIndex = subIndex, editing = null, actionMenuFor = null) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyTo = null, replyToSubIndex = null) }
    }

    fun startEditing(message: ChatMessage) {
        if (message.direction != MessageDirection.OUTGOING) return
        _uiState.update { it.copy(editing = message, input = message.text, replyTo = null, actionMenuFor = null, selection = emptySet()) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editing = null, editingSubIndex = null, input = "") }
    }

    fun openActionMenu(message: ChatMessage) {
        _uiState.update { it.copy(actionMenuFor = message) }
    }

    fun dismissActionMenu() {
        _uiState.update { it.copy(actionMenuFor = null) }
    }

    fun toggleSelection(messageId: String) {
        _uiState.update { s ->
            val next = s.selection.toMutableSet()
            if (!next.add(messageId)) next.remove(messageId)
            s.copy(selection = next, actionMenuFor = null)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selection = emptySet()) }
    }

    fun requestDelete(ids: List<String>) {
        _uiState.update { it.copy(deleteDialogFor = ids, actionMenuFor = null) }
    }

    fun confirmDelete() {
        val ids = _uiState.value.deleteDialogFor ?: return
        _uiState.update { s ->
            s.copy(
                messages = s.messages.filterNot { it.id in ids },
                selection = s.selection - ids.toSet(),
                pinnedIds = s.pinnedIds - ids.toSet(),
                deleteDialogFor = null
            )
        }
        scope.launch {
            ids.forEach { repository.deleteMessage(sessionId, it) }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteDialogFor = null) }
    }

    fun togglePin(messageId: String) {
        _uiState.update { s ->
            val next = if (messageId in s.pinnedIds) s.pinnedIds - messageId else s.pinnedIds + messageId
            s.copy(
                pinnedIds = next,
                pinnedId = next.lastOrNull(),
                pinnedIndex = (next.size - 1).coerceAtLeast(0),
                actionMenuFor = null
            )
        }
    }

    fun cyclePinned() {
        _uiState.update { s ->
            if (s.pinnedIds.isEmpty()) s
            else s.copy(pinnedIndex = (s.pinnedIndex + 1) % s.pinnedIds.size)
        }
    }

    fun openSearch() {
        _uiState.update { it.copy(isSearchOpen = true) }
    }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchOpen = false, searchQuery = "", searchMatches = emptyList(), searchIndex = 0) }
    }

    fun onSearchQueryChange(q: String) {
        _uiState.update { s ->
            val matches = if (q.isBlank()) emptyList() else s.messages.filter { it.matchesQuery(q) }.map { it.id }
            s.copy(searchQuery = q, searchMatches = matches, searchIndex = (matches.size - 1).coerceAtLeast(0))
        }
    }

    fun prevSearchMatch() {
        _uiState.update { s ->
            if (s.searchMatches.isEmpty()) s
            else s.copy(searchIndex = (s.searchIndex - 1 + s.searchMatches.size) % s.searchMatches.size)
        }
    }

    fun nextSearchMatch() {
        _uiState.update { s ->
            if (s.searchMatches.isEmpty()) s
            else s.copy(searchIndex = (s.searchIndex + 1) % s.searchMatches.size)
        }
    }

    fun highlightMessage(messageId: String) {
        highlightJob?.cancel()
        _uiState.update { it.copy(highlightedMessageId = messageId) }
        highlightJob = scope.launch {
            delay(1600)
            _uiState.update { it.copy(highlightedMessageId = null) }
        }
    }

    fun toggleAttach() {
        _uiState.update { it.copy(isAttachOpen = !it.isAttachOpen, isEmojiOpen = false) }
    }

    fun toggleEmoji() {
        _uiState.update { it.copy(isEmojiOpen = !it.isEmojiOpen, isAttachOpen = false) }
    }

    fun closePopups() {
        _uiState.update { it.copy(isAttachOpen = false, isEmojiOpen = false) }
    }

    fun setWallpaperStyle(style: Int) {
        _uiState.update { it.copy(wallpaperStyle = style) }
    }

    fun setSelfDestruct(sd: SelfDestruct) {
        _uiState.update { it.copy(selfDestruct = sd) }
    }

    fun toggleKeyCheck(open: Boolean) {
        _uiState.update { it.copy(isKeyCheckOpen = open) }
    }

    fun loadOlder() {
        val state = _uiState.value
        if (state.isLoadingOlder || !state.hasMoreOlder) return
        val oldest = state.messages.firstOrNull()?.epochMs ?: return
        _uiState.update { it.copy(isLoadingOlder = true) }
        scope.launch {
            val older = repository.loadOlder(sessionId, oldest)
            val hasMore = repository.hasMoreOlder(sessionId)
            _uiState.update { s ->
                s.copy(
                    messages = older + s.messages,
                    isLoadingOlder = false,
                    hasMoreOlder = hasMore
                )
            }
        }
    }

    fun markAllRead() {
        scope.launch {
            repository.markAllRead(sessionId)
            chatsRepository.markAsRead(listOf(sessionId))
            _uiState.update { s ->
                s.copy(
                    messages = s.messages.map { it.copy(isUnread = false) },
                    firstUnreadId = null,
                    unreadBadge = 0
                )
            }
        }
    }

    private fun extractLinkPreview(text: String): LinkPreview? {
        val idx = text.indexOf("https://").takeIf { it >= 0 } ?: text.indexOf("http://")
        if (idx < 0) return null
        val url = text.substring(idx).substringBefore(" ").trim()
        val domain = url.substringAfter("://").substringBefore("/")
        return LinkPreview(
            url = url,
            domain = domain,
            title = domain,
            description = "Защищённое превью без утечки IP"
        )
    }
}
