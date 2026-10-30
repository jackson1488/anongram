package com.anongram.app.features.chats.messages.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.chats.data.ChatListRepository
import com.anongram.app.features.chats.data.InMemoryChatListRepository
import com.anongram.app.features.chats.messages.data.ChatMessagesRepository
import com.anongram.app.features.chats.messages.data.InMemoryChatMessagesRepository
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.matchesQuery
import com.anongram.app.features.chats.messages.domain.LinkPreview
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * State-холдер экрана чата: вся логика ТЗ (доставка с ошибками и повтором,
 * ответы и правки, пересылка, закрепление, реакции, выделение, поиск, запись
 * голоса и кружка, самоуничтожение, черновики, paging). UI только рендерит.
 */
class ChatMessagesViewModel(
    private val sessionId: String,
    private val repository: ChatMessagesRepository,
    private val chatsRepository: ChatListRepository
) : ViewModel() {

    constructor(sessionId: String, repository: ChatMessagesRepository) : this(
        sessionId, repository, InMemoryChatListRepository()
    )

    private val _uiState = MutableStateFlow(ChatMessagesUiState())
    val uiState: StateFlow<ChatMessagesUiState> = _uiState.asStateFlow()

    private var draftJob: Job? = null
    private var recordingJob: Job? = null
    private var selfDestructJob: Job? = null
    private var highlightJob: Job? = null

    init {
        viewModelScope.launch {
            val drafts = chatsRepository.loadDrafts()
            val loaded = repository.loadMessages(sessionId)
            val initialPinned = if (sessionId == "2" && loaded.size > 2) listOf(loaded[1].id, loaded[2].id) else emptyList<String>()
            _uiState.update {
                it.copy(
                    messages = loaded,
                    input = drafts[sessionId] ?: "",
                    firstUnreadId = loaded.firstOrNull { message -> message.isUnread }?.id,
                    pinnedIds = initialPinned,
                    pinnedId = initialPinned.lastOrNull()
                )
            }
        }
    }

    // --- Поле ввода и черновики ---

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(600)
            chatsRepository.saveDraft(sessionId, value.trim())
        }
    }

    fun appendEmoji(symbol: String) = onInputChange(_uiState.value.input + symbol)

    // --- Отправка, доставка, правки ---

    fun send() {
        val state = _uiState.value
        val text = state.input.trim()
        if (text.isEmpty()) return
        val editing = state.editing
        if (editing != null) {
            editMessage(editing.id, text)
            return
        }
        val message = ChatMessage(
            id = "out_${System.nanoTime()}",
            direction = MessageDirection.OUTGOING,
            text = text,
            epochMs = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            replyToId = state.replyTo?.id,
            linkPreview = buildLinkPreview(text)
        )
        viewModelScope.launch {
            chatsRepository.saveDraft(sessionId, "")
            repository.addMessage(sessionId, message)
            _uiState.update { s ->
                s.copy(messages = s.messages + message, input = "", replyTo = null)
            }
            runDeliveryLifecycle(message.id)
        }
    }

    fun sendAttachment(attachment: MessageAttachment, caption: String = "") {
        val message = ChatMessage(
            id = "out_${System.nanoTime()}",
            direction = MessageDirection.OUTGOING,
            text = caption,
            epochMs = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            attachment = attachment
        )
        viewModelScope.launch {
            repository.addMessage(sessionId, message)
            _uiState.update { s -> s.copy(messages = s.messages + message) }
            runDeliveryLifecycle(message.id)
        }
    }

    fun sendSticker(symbol: String, colorSeed: Int) =
        sendAttachment(MessageAttachment.Sticker(symbol, colorSeed))

    fun retry(messageId: String) {
        viewModelScope.launch {
            setStatus(messageId, MessageStatus.SENDING)
            runDeliveryLifecycle(messageId)
        }
    }

    /** Отправитель сам строит превью ссылки — без сервера. */
    private fun buildLinkPreview(text: String): LinkPreview? {
        val url = Regex("https?://\\S+").find(text)?.value ?: return null
        val domain = url.removePrefix("https://").removePrefix("http://").substringBefore('/')
        return LinkPreview(
            url = url,
            domain = domain,
            title = "Ссылка от собеседника",
            description = "Материал на $domain — превью собрано локально"
        )
    }

    private suspend fun runDeliveryLifecycle(messageId: String) {
        delay(600)
        // FakeRepository имитирует сбой сети: красный «!», тап = повторить.
        if (Random.nextInt(100) < 12) {
            setStatus(messageId, MessageStatus.FAILED)
            return
        }
        setStatus(messageId, MessageStatus.SENT)
        delay(1_100)
        setStatus(messageId, MessageStatus.READ)
        val voiceReply = Random.nextInt(100) < 25
        _uiState.update {
            it.copy(
                isPeerTyping = !voiceReply,
                peerActivity = if (voiceReply) "записывает голосовое…" else null
            )
        }
        delay(if (voiceReply) 2_200 else 1_800)
        val reply = repository.generateAutoReply(sessionId)
        _uiState.update { s ->
            s.copy(
                isPeerTyping = false,
                peerActivity = null,
                messages = reply?.let { s.messages + it } ?: s.messages,
                unreadBadge = s.unreadBadge + (if (reply != null) 1 else 0)
            )
        }
    }

    private suspend fun setStatus(messageId: String, status: MessageStatus) {
        repository.updateStatus(sessionId, messageId, status)
        _uiState.update { state ->
            state.copy(messages = state.messages.map {
                if (it.id == messageId) it.copy(status = status) else it
            })
        }
    }

    fun startEdit(message: ChatMessage) {
        _uiState.update {
            it.copy(editing = message, input = message.text, replyTo = null, actionMenuFor = null)
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editing = null, input = "") }
    }

    private fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            val edited = _uiState.value.messages.firstOrNull { it.id == messageId }
                ?.copy(text = newText, isEdited = true) ?: return@launch
            repository.updateMessage(sessionId, edited)
            _uiState.update { s ->
                s.copy(
                    messages = s.messages.map { if (it.id == messageId) edited else it },
                    editing = null,
                    input = ""
                )
            }
        }
    }

    // --- Ответ свайпом / из меню ---

    fun startReply(message: ChatMessage) =
        _uiState.update { it.copy(replyTo = message, editing = null, actionMenuFor = null) }

    fun cancelReply() = _uiState.update { it.copy(replyTo = null) }

    // --- Меню действий с сообщением (долгий тап) ---

    fun openActionMenu(message: ChatMessage) =
        _uiState.update { it.copy(actionMenuFor = message) }

    fun closeActionMenu() = _uiState.update { it.copy(actionMenuFor = null) }

    // --- Реакции (шаг 11) ---

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            val updated = _uiState.value.messages.firstOrNull { it.id == messageId }
                ?: return@launch
            val (reactions, my) = if (updated.myReaction == emoji) {
                val remaining = updated.reactions[emoji]?.minus(1) ?: 0
                val map = updated.reactions.toMutableMap()
                if (remaining <= 0) map.remove(emoji) else map[emoji] = remaining
                map to null
            } else {
                val map = updated.reactions.toMutableMap()
                updated.myReaction?.let { old ->
                    val oldCount = (map[old] ?: 1) - 1
                    if (oldCount <= 0) map.remove(old) else map[old] = oldCount
                }
                map[emoji] = (map[emoji] ?: 0) + 1
                map to emoji
            }
            val next = updated.copy(reactions = reactions, myReaction = my)
            repository.updateMessage(sessionId, next)
            _uiState.update { s ->
                s.copy(messages = s.messages.map { if (it.id == messageId) next else it })
            }
        }
    }

    // --- Закреплённое сообщение (шаг 12) ---

    fun pinMessage(messageId: String) {
        val state = _uiState.value
        if (!state.pinnedIds.contains(messageId) && state.pinnedIds.size >= MAX_PINNED_LIMIT) {
            showLimitWarning("Нельзя закрепить больше 99 сообщений. Достигнут лимит")
            _uiState.update { it.copy(actionMenuFor = null) }
            return
        }
        _uiState.update { s ->
            val pinned = s.pinnedIds.toMutableList()
            if (!pinned.contains(messageId)) pinned.add(messageId)
            s.copy(
                pinnedIds = pinned,
                pinnedId = messageId,
                pinnedIndex = pinned.indexOf(messageId).coerceAtLeast(0),
                actionMenuFor = null
            )
        }
    }

    fun unpinMessage(messageId: String) {
        _uiState.update { state ->
            val pinned = state.pinnedIds.filter { it != messageId }
            val nextIndex = state.pinnedIndex.coerceAtMost((pinned.size - 1).coerceAtLeast(0))
            state.copy(
                pinnedIds = pinned,
                pinnedId = pinned.getOrNull(nextIndex) ?: pinned.lastOrNull(),
                pinnedIndex = nextIndex
            )
        }
    }

    fun unpinAllMessages() {
        _uiState.update { it.copy(pinnedIds = emptyList(), pinnedId = null, pinnedIndex = 0, isPinnedMessagesScreenOpen = false) }
    }

    fun cycleNextPinnedMessage(): String? {
        val state = _uiState.value
        if (state.pinnedIds.isEmpty()) return null
        val nextIndex = (state.pinnedIndex + 1) % state.pinnedIds.size
        val nextId = state.pinnedIds[nextIndex]
        _uiState.update { it.copy(pinnedIndex = nextIndex, pinnedId = nextId) }
        return nextId
    }

    fun showLimitWarning(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(limitWarning = message) }
            delay(2800)
            _uiState.update { if (it.limitWarning == message) it.copy(limitWarning = null) else it }
        }
    }

    fun dismissLimitWarning() {
        _uiState.update { it.copy(limitWarning = null) }
    }

    fun triggerDemoPinnedLimitWarning() {
        showLimitWarning("Нельзя закрепить больше 99 сообщений. Достигнут лимит")
    }

    fun openPinnedMessages() = _uiState.update { it.copy(isPinnedMessagesScreenOpen = true) }

    fun closePinnedMessages() = _uiState.update { it.copy(isPinnedMessagesScreenOpen = false) }

    // --- Режим выделения (шаг 10 + поштучный выбор элементов внутри альбома, группы файлов или музыки) ---

    fun enterSelection(messageId: String) =
        _uiState.update { it.copy(selection = setOf(messageId), actionMenuFor = null) }

    fun toggleSelection(messageId: String) {
        val msg = _uiState.value.messages.firstOrNull { it.id == messageId }
        when (val att = msg?.attachment) {
            is MessageAttachment.MediaAlbum -> {
                toggleWholeGroupSelection(messageId, "photo", att.items.size)
                return
            }
            is MessageAttachment.FileGroup -> {
                toggleWholeGroupSelection(messageId, "file", att.items.size)
                return
            }
            is MessageAttachment.MusicGroup -> {
                toggleWholeGroupSelection(messageId, "music", att.items.size)
                return
            }
            else -> Unit
        }
        _uiState.update { state ->
            val next = state.selection.toMutableSet()
            if (!next.add(messageId)) {
                next.remove(messageId)
            } else if (next.size > 100) {
                return@update state
            }
            state.copy(selection = next)
        }
    }

    /** Поштучный выбор конкретного фото внутри альбома */
    fun toggleAlbumPhotoSelection(messageId: String, photoIndex: Int, totalPhotos: Int) =
        toggleGroupItemSelection(messageId, "photo", photoIndex, totalPhotos)

    /** Поштучный выбор конкретного файла внутри группы файлов */
    fun toggleFileGroupItemSelection(messageId: String, fileIndex: Int, totalFiles: Int) =
        toggleGroupItemSelection(messageId, "file", fileIndex, totalFiles)

    /** Поштучный выбор конкретного трека внутри группы музыки */
    fun toggleMusicGroupItemSelection(messageId: String, musicIndex: Int, totalTracks: Int) =
        toggleGroupItemSelection(messageId, "music", musicIndex, totalTracks)

    fun toggleGroupItemSelection(messageId: String, prefix: String, itemIndex: Int, totalItems: Int) {
        _uiState.update { state ->
            val next = state.selection.toMutableSet()
            if (next.remove(messageId)) {
                for (i in 0 until totalItems) {
                    next.add("${messageId}#${prefix}_$i")
                }
            }
            val key = "${messageId}#${prefix}_$itemIndex"
            if (!next.add(key)) {
                next.remove(key)
            } else if (next.size > 100) {
                return@update state
            }
            state.copy(selection = next, actionMenuFor = null)
        }
    }

    /** Переключение выбора всех фото альбома целиком (по кружку слева от пузыря) */
    fun toggleWholeAlbumSelection(messageId: String, totalPhotos: Int) =
        toggleWholeGroupSelection(messageId, "photo", totalPhotos)

    fun toggleWholeGroupSelection(messageId: String, prefix: String, totalItems: Int) {
        _uiState.update { state ->
            val next = state.selection.toMutableSet()
            val itemKeys = (0 until totalItems).map { "${messageId}#${prefix}_$it" }
            val allSelected = (messageId in next) || itemKeys.all { it in next }
            next.remove(messageId)
            if (allSelected) {
                next.removeAll(itemKeys.toSet())
            } else {
                for (k in itemKeys) {
                    if (next.size < 100) next.add(k)
                }
            }
            state.copy(selection = next, actionMenuFor = null)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selection = emptySet()) }

    // --- Пересылка (в выбранный чат или «Избранное») ---

    fun forwardMessages(messageIds: Collection<String>, authorName: String) {
        forwardMessagesToTargetChat(sessionId, messageIds, authorName)
    }

    fun forwardMessagesToTargetChat(
        targetSessionId: String,
        messageIds: Collection<String>,
        authorName: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val fullIds = messageIds.filter { "#" !in it }.toSet()

            fun parseSubIndices(prefix: String): Map<String, List<Int>> =
                messageIds
                    .filter { "#${prefix}_" in it }
                    .mapNotNull { key ->
                        val parts = key.split("#${prefix}_")
                        val mId = parts.getOrNull(0) ?: return@mapNotNull null
                        val idx = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                        mId to idx
                    }
                    .groupBy({ it.first }, { it.second })

            val photoIndicesByMsgId = parseSubIndices("photo")
            val fileIndicesByMsgId = parseSubIndices("file")
            val musicIndicesByMsgId = parseSubIndices("music")

            val copies = mutableListOf<ChatMessage>()
            for (original in _uiState.value.messages) {
                val fwdAuthor = if (targetSessionId == "0") null else (original.forwardedFrom ?: authorName)
                if (original.id in fullIds) {
                    copies.add(
                        original.copy(
                            id = "fwd_${System.nanoTime()}_${original.id}",
                            direction = MessageDirection.OUTGOING,
                            epochMs = now,
                            status = MessageStatus.SENT,
                            forwardedFrom = fwdAuthor,
                            reactions = emptyMap(),
                            myReaction = null,
                            isUnread = false,
                            replyToId = null
                        )
                    )
                } else if (original.id in photoIndicesByMsgId) {
                    val selectedIndices = photoIndicesByMsgId[original.id].orEmpty().toSet()
                    val album = original.attachment as? MessageAttachment.MediaAlbum
                    if (album != null) {
                        val chosenItems = album.items.filterIndexed { idx, _ -> idx in selectedIndices }
                        if (chosenItems.size == 1) {
                            val item = chosenItems[0]
                            copies.add(
                                original.copy(
                                    id = "fwd_${System.nanoTime()}_${original.id}",
                                    direction = MessageDirection.OUTGOING,
                                    epochMs = now,
                                    status = MessageStatus.SENT,
                                    forwardedFrom = fwdAuthor,
                                    attachment = MessageAttachment.Photo(
                                        label = "",
                                        colorSeed = 1,
                                        isVideo = item.isVideo,
                                        uri = item.uri,
                                        hasSpoiler = item.hasSpoiler
                                    ),
                                    reactions = emptyMap(),
                                    myReaction = null,
                                    isUnread = false,
                                    replyToId = null
                                )
                            )
                        } else if (chosenItems.size > 1) {
                            copies.add(
                                original.copy(
                                    id = "fwd_${System.nanoTime()}_${original.id}",
                                    direction = MessageDirection.OUTGOING,
                                    epochMs = now,
                                    status = MessageStatus.SENT,
                                    forwardedFrom = fwdAuthor,
                                    attachment = album.copy(
                                        items = chosenItems,
                                        hasSpoiler = chosenItems.any { it.hasSpoiler }
                                    ),
                                    reactions = emptyMap(),
                                    myReaction = null,
                                    isUnread = false,
                                    replyToId = null
                                )
                            )
                        }
                    }
                } else if (original.id in fileIndicesByMsgId) {
                    val selectedIndices = fileIndicesByMsgId[original.id].orEmpty().toSet()
                    val fg = original.attachment as? MessageAttachment.FileGroup
                    if (fg != null) {
                        val chosenItems = fg.items.filterIndexed { idx, _ -> idx in selectedIndices }
                        if (chosenItems.size == 1) {
                            copies.add(
                                original.copy(
                                    id = "fwd_${System.nanoTime()}_${original.id}",
                                    direction = MessageDirection.OUTGOING,
                                    epochMs = now,
                                    status = MessageStatus.SENT,
                                    forwardedFrom = fwdAuthor,
                                    attachment = chosenItems[0],
                                    reactions = emptyMap(),
                                    myReaction = null,
                                    isUnread = false,
                                    replyToId = null
                                )
                            )
                        } else if (chosenItems.size > 1) {
                            copies.add(
                                original.copy(
                                    id = "fwd_${System.nanoTime()}_${original.id}",
                                    direction = MessageDirection.OUTGOING,
                                    epochMs = now,
                                    status = MessageStatus.SENT,
                                    forwardedFrom = fwdAuthor,
                                    attachment = MessageAttachment.FileGroup(chosenItems),
                                    reactions = emptyMap(),
                                    myReaction = null,
                                    isUnread = false,
                                    replyToId = null
                                )
                            )
                        }
                    }
                } else if (original.id in musicIndicesByMsgId) {
                    val selectedIndices = musicIndicesByMsgId[original.id].orEmpty().toSet()
                    val mg = original.attachment as? MessageAttachment.MusicGroup
                    if (mg != null) {
                        val chosenItems = mg.items.filterIndexed { idx, _ -> idx in selectedIndices }
                        if (chosenItems.size == 1) {
                            copies.add(
                                original.copy(
                                    id = "fwd_${System.nanoTime()}_${original.id}",
                                    direction = MessageDirection.OUTGOING,
                                    epochMs = now,
                                    status = MessageStatus.SENT,
                                    forwardedFrom = fwdAuthor,
                                    attachment = chosenItems[0],
                                    reactions = emptyMap(),
                                    myReaction = null,
                                    isUnread = false,
                                    replyToId = null
                                )
                            )
                        } else if (chosenItems.size > 1) {
                            copies.add(
                                original.copy(
                                    id = "fwd_${System.nanoTime()}_${original.id}",
                                    direction = MessageDirection.OUTGOING,
                                    epochMs = now,
                                    status = MessageStatus.SENT,
                                    forwardedFrom = fwdAuthor,
                                    attachment = MessageAttachment.MusicGroup(chosenItems),
                                    reactions = emptyMap(),
                                    myReaction = null,
                                    isUnread = false,
                                    replyToId = null
                                )
                            )
                        }
                    }
                }
            }

            // Гарантируем, что история целевого чата инициализирована перед добавлением
            repository.loadMessages(targetSessionId)
            for (copy in copies) {
                repository.addMessage(targetSessionId, copy)
            }
            if (targetSessionId == sessionId) {
                _uiState.update { s ->
                    s.copy(messages = s.messages + copies, selection = emptySet(), actionMenuFor = null)
                }
            } else {
                _uiState.update { s ->
                    s.copy(selection = emptySet(), actionMenuFor = null)
                }
            }
            onDone()
        }
    }

    // --- Удаление (у меня / у всех, включая удаление отдельных элементов из альбома/группы файлов/музыки) ---

    fun requestDelete(messageIds: List<String>) =
        _uiState.update { it.copy(deleteDialogFor = messageIds, actionMenuFor = null) }

    fun confirmDelete(forEveryone: Boolean) {
        val targets = _uiState.value.deleteDialogFor ?: return
        viewModelScope.launch {
            val fullDeleteIds = targets.filter { "#" !in it }.toMutableSet()

            fun parseDeleteSubIndices(prefix: String): Map<String, List<Int>> =
                targets
                    .filter { "#${prefix}_" in it }
                    .mapNotNull { key ->
                        val parts = key.split("#${prefix}_")
                        val mId = parts.getOrNull(0) ?: return@mapNotNull null
                        val idx = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                        mId to idx
                    }
                    .groupBy({ it.first }, { it.second })

            val photoDeletesByMsgId = parseDeleteSubIndices("photo")
            val fileDeletesByMsgId = parseDeleteSubIndices("file")
            val musicDeletesByMsgId = parseDeleteSubIndices("music")

            val updatedMessages = mutableListOf<ChatMessage>()
            for (msg in _uiState.value.messages) {
                if (msg.id in fullDeleteIds) {
                    repository.deleteMessage(sessionId, msg.id)
                    continue
                }
                val photoIndicesToDelete = photoDeletesByMsgId[msg.id]?.toSet()
                val fileIndicesToDelete = fileDeletesByMsgId[msg.id]?.toSet()
                val musicIndicesToDelete = musicDeletesByMsgId[msg.id]?.toSet()

                when (val att = msg.attachment) {
                    is MessageAttachment.MediaAlbum -> {
                        if (photoIndicesToDelete != null) {
                            val remaining = att.items.filterIndexed { idx, _ -> idx !in photoIndicesToDelete }
                            when {
                                remaining.isEmpty() -> {
                                    fullDeleteIds.add(msg.id)
                                    repository.deleteMessage(sessionId, msg.id)
                                }
                                remaining.size == 1 -> {
                                    val single = remaining[0]
                                    val modified = msg.copy(
                                        attachment = MessageAttachment.Photo(
                                            label = "",
                                            colorSeed = 1,
                                            isVideo = single.isVideo,
                                            uri = single.uri,
                                            hasSpoiler = single.hasSpoiler
                                        )
                                    )
                                    repository.updateMessage(sessionId, modified)
                                    updatedMessages.add(modified)
                                }
                                else -> {
                                    val modified = msg.copy(
                                        attachment = att.copy(
                                            items = remaining,
                                            hasSpoiler = remaining.any { it.hasSpoiler }
                                        )
                                    )
                                    repository.updateMessage(sessionId, modified)
                                    updatedMessages.add(modified)
                                }
                            }
                        } else {
                            updatedMessages.add(msg)
                        }
                    }
                    is MessageAttachment.FileGroup -> {
                        if (fileIndicesToDelete != null) {
                            val remaining = att.items.filterIndexed { idx, _ -> idx !in fileIndicesToDelete }
                            when {
                                remaining.isEmpty() -> {
                                    fullDeleteIds.add(msg.id)
                                    repository.deleteMessage(sessionId, msg.id)
                                }
                                remaining.size == 1 -> {
                                    val modified = msg.copy(attachment = remaining[0])
                                    repository.updateMessage(sessionId, modified)
                                    updatedMessages.add(modified)
                                }
                                else -> {
                                    val modified = msg.copy(attachment = MessageAttachment.FileGroup(remaining))
                                    repository.updateMessage(sessionId, modified)
                                    updatedMessages.add(modified)
                                }
                            }
                        } else {
                            updatedMessages.add(msg)
                        }
                    }
                    is MessageAttachment.MusicGroup -> {
                        if (musicIndicesToDelete != null) {
                            val remaining = att.items.filterIndexed { idx, _ -> idx !in musicIndicesToDelete }
                            when {
                                remaining.isEmpty() -> {
                                    fullDeleteIds.add(msg.id)
                                    repository.deleteMessage(sessionId, msg.id)
                                }
                                remaining.size == 1 -> {
                                    val modified = msg.copy(attachment = remaining[0])
                                    repository.updateMessage(sessionId, modified)
                                    updatedMessages.add(modified)
                                }
                                else -> {
                                    val modified = msg.copy(attachment = MessageAttachment.MusicGroup(remaining))
                                    repository.updateMessage(sessionId, modified)
                                    updatedMessages.add(modified)
                                }
                            }
                        } else {
                            updatedMessages.add(msg)
                        }
                    }
                    else -> {
                        updatedMessages.add(msg)
                    }
                }
            }

            _uiState.update { state ->
                state.copy(
                    messages = updatedMessages,
                    deleteDialogFor = null,
                    selection = emptySet(),
                    pinnedId = state.pinnedId?.takeUnless { it in fullDeleteIds }
                )
            }
        }
    }

    fun dismissDeleteDialog() =
        _uiState.update { it.copy(deleteDialogFor = null) }

    // --- История и удаление чата ---

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory(sessionId)
            _uiState.update {
                it.copy(messages = emptyList(), pinnedId = null, firstUnreadId = null, unreadBadge = 0)
            }
        }
    }

    // --- Paging истории (шаг 13) ---

    fun loadOlder() {
        val state = _uiState.value
        if (state.isLoadingOlder || !state.hasMoreOlder || state.messages.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOlder = true) }
            val oldest = state.messages.minByOrNull { it.epochMs } ?: return@launch
            if (!repository.hasMoreOlder(sessionId)) {
                _uiState.update { it.copy(isLoadingOlder = false, hasMoreOlder = false) }
                return@launch
            }
            val page = repository.loadOlder(sessionId, oldest.epochMs)
            _uiState.update { s ->
                s.copy(messages = page + s.messages, isLoadingOlder = false)
            }
        }
    }

    // --- Непрочитанные (шаг 13) ---

    fun clearUnreadBadge() {
        if (_uiState.value.unreadBadge != 0) {
            _uiState.update { it.copy(unreadBadge = 0) }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead(sessionId)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { if (it.isUnread) it.copy(isUnread = false) else it },
                    firstUnreadId = null
                )
            }
        }
    }

    // --- Поиск в чате (шаг 17) ---

    fun openSearch() = _uiState.update {
        it.copy(isSearchOpen = true, isAttachOpen = false, isEmojiOpen = false)
    }

    fun closeSearch() = _uiState.update {
        it.copy(isSearchOpen = false, searchQuery = "", searchMatches = emptyList(), searchIndex = 0)
    }

    fun onSearchQueryChange(query: String) {
        val trimmed = query.trim()
        val matches = if (trimmed.isEmpty()) {
            emptyList()
        } else {
            // Новейшие совпадения первыми (снизу вверх по чату)
            _uiState.value.messages.asReversed()
                .filter { it.matchesQuery(trimmed) }
                .map { it.id }
        }
        _uiState.update {
            it.copy(searchQuery = query, searchMatches = matches, searchIndex = 0)
        }
    }

    fun highlightMessage(id: String) {
        highlightJob?.cancel()
        highlightJob = viewModelScope.launch {
            // Сбрасываем в null, чтобы гарантировать повторное срабатывание анимации даже при повторном клике того же id
            _uiState.update { it.copy(highlightedMessageId = null) }
            delay(32)
            _uiState.update { it.copy(highlightedMessageId = id) }
            delay(2500)
            _uiState.update {
                if (it.highlightedMessageId == id) it.copy(highlightedMessageId = null) else it
            }
        }
    }

    fun jumpToSearchMatch(index: Int) {
        val state = _uiState.value
        if (state.searchMatches.isEmpty()) return
        val clamped = index.coerceIn(0, state.searchMatches.lastIndex)
        val matchId = state.searchMatches[clamped]
        _uiState.update { it.copy(searchIndex = clamped) }
        highlightMessage(matchId)
    }

    /** Стрелка ВВЕРХ (↑): переход к более старому найденному сообщению (вверх по истории чата). */
    fun searchMoveUp() = cycleSearch(+1)

    /** Стрелка ВНИЗ (↓): переход к более новому найденному сообщению (вниз по истории чата). */
    fun searchMoveDown() = cycleSearch(-1)

    fun nextSearchMatch() = searchMoveDown()

    fun previousSearchMatch() = searchMoveUp()

    private fun cycleSearch(delta: Int) {
        val state = _uiState.value
        if (state.searchMatches.isEmpty()) return
        val size = state.searchMatches.size
        val next = (state.searchIndex + delta + size) % size
        val matchId = state.searchMatches[next]
        _uiState.update { it.copy(searchIndex = next) }
        highlightMessage(matchId)
    }

    // --- Запись голоса и кружка (шаг 6) ---

    fun startRecording(mode: RecordMode) {
        if (_uiState.value.recording.mode != RecordMode.NONE) return
        _uiState.update { it.copy(recording = RecordingState(mode = mode)) }
        recordingJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                val recording = _uiState.value.recording
                if (recording.mode == RecordMode.NONE) break
                val nextTenths = recording.elapsedTenths + 1
                _uiState.update { s ->
                    s.copy(recording = s.recording.copy(elapsedTenths = nextTenths))
                }
                // Лимит кружка — 60 секунд, автоотправка.
                if (recording.mode == RecordMode.CIRCLE && nextTenths >= 600) {
                    finishRecording(send = true)
                    break
                }
            }
        }
    }

    fun lockRecording() {
        if (_uiState.value.recording.mode == RecordMode.VOICE) {
            _uiState.update { s -> s.copy(recording = s.recording.copy(isLocked = true)) }
        }
    }

    fun cancelRecording() = finishRecording(send = false)

    /** Палец убран: незаблокированная голосовая уходит в чат, заблокированная ждёт. */
    fun endPress() {
        val recording = _uiState.value.recording
        when {
            recording.mode == RecordMode.VOICE && !recording.isLocked ->
                finishRecording(send = true)

            recording.mode == RecordMode.CIRCLE && !recording.isLocked ->
                finishRecording(send = true)
        }
    }

    fun stopLockedRecording() = finishRecording(send = true)

    fun toggleCameraMode() =
        _uiState.update { it.copy(isCameraMode = !it.isCameraMode) }

    private fun finishRecording(send: Boolean) {
        recordingJob?.cancel()
        recordingJob = null
        val recording = _uiState.value.recording
        _uiState.update { it.copy(recording = RecordingState()) }
        if (!send || recording.mode == RecordMode.NONE) return
        if (recording.elapsedTenths < 5) return // короче полсекунды — не отправляем

        val attachment = if (recording.mode == RecordMode.VOICE) {
            MessageAttachment.Voice(
                durationLabel = recording.durationLabel,
                waveform = List(24) { Random.nextInt(3, 15) }
            )
        } else {
            MessageAttachment.VideoCircle(
                durationLabel = recording.durationLabel,
                colorSeed = Random.nextInt(0, 5)
            )
        }
        sendAttachment(attachment)
    }

    // --- Вложения / панели / обои / безопасность (шаги 3–5, 7, 18) ---

    fun openAttach() = _uiState.update {
        it.copy(isAttachOpen = true, isEmojiOpen = false)
    }

    fun closeAttach() = _uiState.update { it.copy(isAttachOpen = false) }

    fun toggleEmojiPanel() =
        _uiState.update { it.copy(isEmojiOpen = !it.isEmojiOpen, isAttachOpen = false) }

    fun closeEmojiPanel() = _uiState.update { it.copy(isEmojiOpen = false) }

    fun openGeoPicker() = _uiState.update { it.copy(isGeoOpen = true, isAttachOpen = false) }

    fun closeGeoPicker() = _uiState.update { it.copy(isGeoOpen = false) }

    fun openKeyCheck() = _uiState.update { it.copy(isKeyCheckOpen = true) }

    fun closeKeyCheck() = _uiState.update { it.copy(isKeyCheckOpen = false) }

    fun openNotificationsMuteDialog() = _uiState.update { it.copy(isNotificationsMuteDialogOpen = true) }

    fun closeNotificationsMuteDialog() = _uiState.update { it.copy(isNotificationsMuteDialogOpen = false) }

    fun openNotifications() = _uiState.update { it.copy(isNotificationsOpen = true, isNotificationsMuteDialogOpen = false) }

    fun closeNotifications() = _uiState.update { it.copy(isNotificationsOpen = false) }

    fun cycleWallpaper() =
        _uiState.update { it.copy(wallpaperStyle = (it.wallpaperStyle + 1) % 3) }

    fun setSelfDestruct(value: SelfDestruct) {
        _uiState.update { it.copy(selfDestruct = value) }
        selfDestructJob?.cancel()
        if (value.ms > 0L) {
            selfDestructJob = viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    val deadline = System.currentTimeMillis() - value.ms
                    val expired = _uiState.value.messages.filter { it.epochMs < deadline }
                    if (expired.isNotEmpty()) {
                        for (message in expired) repository.deleteMessage(sessionId, message.id)
                        _uiState.update { s ->
                            s.copy(messages = s.messages.filterNot { it.epochMs < deadline })
                        }
                    }
                }
            }
        }
    }

    fun toggleFlagSecure() =
        _uiState.update { it.copy(isFlagSecure = !it.isFlagSecure) }

    companion object {
        const val MAX_PINNED_LIMIT = 99
    }
}
