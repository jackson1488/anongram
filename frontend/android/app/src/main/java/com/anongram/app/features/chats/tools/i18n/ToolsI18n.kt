package com.anongram.app.features.chats.tools.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

/**
 * Словарь локализации для модуля инструментов чата (`features/chats/tools/`):
 * выделение, поиск внутри сообщений, закреплённые, пересылка, контекстное меню, удаление и разделитель непрочитанных.
 */
data class ToolsI18nStrings(
    val isRussian: Boolean,
    // Общие
    val back: String,
    val cancel: String,
    val clear: String,
    val copy: String,
    val forward: String,
    val share: String,
    val delete: String,
    val save: String,
    val youLabel: String,
    // SelectionPanel
    val selectedCancel: String,
    val editOrReplace: String,
    val saveToGallery: String,
    val saveAlbumToGallery: String,
    // ChatSearchPanel
    val closeSearch: String,
    val searchPlaceholder: String,
    val clearQuery: String,
    val prevMatch: String,
    val nextMatch: String,
    val zeroOfZero: String,
    val viewInChat: String,
    val viewAsList: String,
    val formatResultsCount: (Int) -> String,
    val nothingFound: String,
    val stickerLabel: (String) -> String,
    val photoLabel: String,
    val voiceMessageLabel: String,
    // PinnedBar & PinnedMessagesScreen
    val videoType: String,
    val photoType: String,
    val filesCount: (Int) -> String,
    val audiosCount: (Int) -> String,
    val voiceWithDuration: (String) -> String,
    val videoMessageType: String,
    val contactPrefix: (String) -> String,
    val locationType: String,
    val stickerWithSymbol: (String) -> String,
    val albumCount: (Int) -> String,
    val messageFallback: String,
    val pinnedHeaderMulti: (Int, Int) -> String,
    val pinnedHeaderSingle: String,
    val allPinnedMessages: String,
    val pinnedCountTitle: (Int) -> String,
    val noPinnedMessages: String,
    val unpinAllMessages: String,
    // ForwardTargetPickerScreen
    val forwardTitle: String,
    val searchChatsPlaceholder: String,
    val savedMessagesTitle: String,
    val savedMessagesSubtitle: String,
    val globalServerSearchHeader: String,
    val globalServerOnlineSubtitle: String,
    // MessageContextMenu
    val saveToGalleryCount: (Int) -> String,
    val saveToDownloads: String,
    val saveFilesCount: (Int) -> String,
    val saveToMusic: String,
    val saveTracksCount: (Int) -> String,
    val moreReactions: String,
    val reply: String,
    val edit: String,
    val pin: String,
    val unpin: String,
    // DeleteMessageDialog
    val deleteMultipleTitle: (Int) -> String,
    val deleteSingleTitle: String,
    val deleteMultiplePrompt: String,
    val deleteSinglePrompt: String,
    val alsoDeleteForPeer: (String) -> String,
    val alsoDeleteForEveryone: String,
    // ReactionChips / UnreadDivider
    val unreadMessages: String
)

private val RuToolsStrings = ToolsI18nStrings(
    isRussian = true,
    back = "Назад",
    cancel = "Отмена",
    clear = "Очистить",
    copy = "Копировать",
    forward = "Переслать",
    share = "Поделиться",
    delete = "Удалить",
    save = "Сохранить",
    youLabel = "Вы",
    selectedCancel = "Снять выделение",
    editOrReplace = "Редактировать / Заменить",
    saveToGallery = "Сохранить в галерею",
    saveAlbumToGallery = "Сохранить альбом в галерею",
    closeSearch = "Закрыть поиск",
    searchPlaceholder = "Поиск",
    clearQuery = "Очистить запрос",
    prevMatch = "Предыдущее совпадение",
    nextMatch = "Следующее совпадение",
    zeroOfZero = "0 из 0",
    viewInChat = "В чате",
    viewAsList = "Списком",
    formatResultsCount = { count ->
        val rem100 = count % 100
        val rem10 = count % 10
        val word = when {
            rem100 in 11..19 -> "результатов"
            rem10 == 1 -> "результат"
            rem10 in 2..4 -> "результата"
            else -> "результатов"
        }
        "$count $word"
    },
    nothingFound = "Ничего не найдено",
    stickerLabel = { symbol -> "$symbol Стикер" },
    photoLabel = "📷 Фотография",
    voiceMessageLabel = "🎙 Голосовое сообщение",
    videoType = "Видеозапись",
    photoType = "Фотография",
    filesCount = { n -> "Файлы ($n шт.)" },
    audiosCount = { n -> "Аудиозаписи ($n шт.)" },
    voiceWithDuration = { dur -> "Голосовое сообщение ($dur)" },
    videoMessageType = "Видеосообщение",
    contactPrefix = { name -> "Контакт: $name" },
    locationType = "Геопозиция",
    stickerWithSymbol = { symbol -> "Стикер $symbol" },
    albumCount = { n -> "Альбом ($n медиа)" },
    messageFallback = "Сообщение",
    pinnedHeaderMulti = { idx, total -> "#${idx + 1} Закреплённое сообщение (${idx + 1}/$total)" },
    pinnedHeaderSingle = "#1 Закреплённое сообщение",
    allPinnedMessages = "Все закреплённые сообщения",
    pinnedCountTitle = { count ->
        when {
            count % 10 == 1 && count % 100 != 11 -> "$count закреплённое сообщение"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "$count закреплённых сообщения"
            else -> "$count закреплённых сообщений"
        }
    },
    noPinnedMessages = "Нет закреплённых сообщений",
    unpinAllMessages = "Открепить все сообщения",
    forwardTitle = "Переслать...",
    searchChatsPlaceholder = "Поиск чатов",
    savedMessagesTitle = "Избранное",
    savedMessagesSubtitle = "Сохранить на память",
    globalServerSearchHeader = "Глобальный поиск по серверу",
    globalServerOnlineSubtitle = "Глобальный сервер AnonGram • В сети",
    saveToGalleryCount = { n -> "Сохранить в галерею ($n)" },
    saveToDownloads = "Сохранить в Загрузки",
    saveFilesCount = { n -> "Сохранить файлы ($n)" },
    saveToMusic = "Сохранить в Музыку",
    saveTracksCount = { n -> "Сохранить треки ($n)" },
    moreReactions = "Ещё реакции",
    reply = "Ответить",
    edit = "Изменить",
    pin = "Закрепить",
    unpin = "Открепить",
    deleteMultipleTitle = { count -> "Удалить $count сообщений" },
    deleteSingleTitle = "Удалить сообщение",
    deleteMultiplePrompt = "Вы точно хотите удалить эти сообщения?",
    deleteSinglePrompt = "Вы точно хотите удалить это сообщение?",
    alsoDeleteForPeer = { peer -> "Также удалить для $peer" },
    alsoDeleteForEveryone = "Также удалить для всех",
    unreadMessages = "Непрочитанные сообщения"
)

private val EnToolsStrings = ToolsI18nStrings(
    isRussian = false,
    back = "Back",
    cancel = "Cancel",
    clear = "Clear",
    copy = "Copy",
    forward = "Forward",
    share = "Share",
    delete = "Delete",
    save = "Save",
    youLabel = "You",
    selectedCancel = "Clear selection",
    editOrReplace = "Edit / Replace",
    saveToGallery = "Save to gallery",
    saveAlbumToGallery = "Save album to gallery",
    closeSearch = "Close search",
    searchPlaceholder = "Search",
    clearQuery = "Clear query",
    prevMatch = "Previous match",
    nextMatch = "Next match",
    zeroOfZero = "0 of 0",
    viewInChat = "In chat",
    viewAsList = "As list",
    formatResultsCount = { count ->
        if (count == 1) "1 result" else "$count results"
    },
    nothingFound = "Nothing found",
    stickerLabel = { symbol -> "$symbol Sticker" },
    photoLabel = "📷 Photo",
    voiceMessageLabel = "🎙 Voice message",
    videoType = "Video",
    photoType = "Photo",
    filesCount = { n -> "Files ($n)" },
    audiosCount = { n -> "Audio tracks ($n)" },
    voiceWithDuration = { dur -> "Voice message ($dur)" },
    videoMessageType = "Video message",
    contactPrefix = { name -> "Contact: $name" },
    locationType = "Location",
    stickerWithSymbol = { symbol -> "Sticker $symbol" },
    albumCount = { n -> "Album ($n media)" },
    messageFallback = "Message",
    pinnedHeaderMulti = { idx, total -> "#${idx + 1} Pinned message (${idx + 1}/$total)" },
    pinnedHeaderSingle = "#1 Pinned message",
    allPinnedMessages = "All pinned messages",
    pinnedCountTitle = { count ->
        if (count == 1) "1 pinned message" else "$count pinned messages"
    },
    noPinnedMessages = "No pinned messages",
    unpinAllMessages = "Unpin all messages",
    forwardTitle = "Forward...",
    searchChatsPlaceholder = "Search chats",
    savedMessagesTitle = "Saved Messages",
    savedMessagesSubtitle = "Save to memory",
    globalServerSearchHeader = "Global server search",
    globalServerOnlineSubtitle = "AnonGram Global Server • Online",
    saveToGalleryCount = { n -> "Save to gallery ($n)" },
    saveToDownloads = "Save to Downloads",
    saveFilesCount = { n -> "Save files ($n)" },
    saveToMusic = "Save to Music",
    saveTracksCount = { n -> "Save tracks ($n)" },
    moreReactions = "More reactions",
    reply = "Reply",
    edit = "Edit",
    pin = "Pin",
    unpin = "Unpin",
    deleteMultipleTitle = { count -> "Delete $count messages" },
    deleteSingleTitle = "Delete message",
    deleteMultiplePrompt = "Are you sure you want to delete these messages?",
    deleteSinglePrompt = "Are you sure you want to delete this message?",
    alsoDeleteForPeer = { peer -> "Also delete for $peer" },
    alsoDeleteForEveryone = "Also delete for everyone",
    unreadMessages = "Unread messages"
)

internal object ToolsI18n {
    val current: ToolsI18nStrings
        @Composable
        get() = if (LanguageSettingsStore.language.collectAsState().value == AppLanguage.RU) {
            RuToolsStrings
        } else {
            EnToolsStrings
        }
}
