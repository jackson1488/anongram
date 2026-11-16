package com.anongram.app.features.chats.links.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

data class InAppBrowserStrings(
    val openInDefaultBrowser: String,
    val refresh: String,
    val findInPage: String,
    val addBookmark: String,
    val removeBookmark: String,
    val share: String,
    val history: String,
    val bookmarks: String,
    val settings: String,
    val findPlaceholder: String,
    val minimizeBrowser: String,
    val expandBrowser: String,
    val closeBrowser: String,
    val browserSettingsTitle: String,
    val useInAppBrowser: String,
    val useInAppBrowserDesc: String,
    val deleteCookies: String,
    val clearCache: String,
    val cookiesCacheDesc: String,
    val viewHistory: String,
    val clearHistory: String,
    val pagesCount: (Int) -> String,
    val neverOpenInBrowser: String,
    val addSite: String,
    val addSiteHint: String,
    val neverOpenDesc: String,
    val searchEngine: String,
    val searchEngineDesc: String,
    val cancel: String,
    val add: String,
    val emptyHistory: String,
    val emptyBookmarks: String,
    val cookiesClearedToast: String,
    val cacheClearedToast: String,
    val linkCopiedToast: String,
    val linksAndBrowserSection: String,
    val linkPreviewsTitle: String,
    val linkPreviewsDesc: String
)

object InAppBrowserI18n {
    private val ru = InAppBrowserStrings(
        openInDefaultBrowser = "Открыть в...",
        refresh = "Обновить",
        findInPage = "Поиск",
        addBookmark = "В закладки",
        removeBookmark = "Убрать из закладок",
        share = "Поделиться",
        history = "История",
        bookmarks = "Закладки",
        settings = "Настройки",
        findPlaceholder = "Найти на странице...",
        minimizeBrowser = "Свернуть",
        expandBrowser = "Развернуть",
        closeBrowser = "Закрыть",
        browserSettingsTitle = "Браузер",
        useInAppBrowser = "Использовать встроенный браузер",
        useInAppBrowserDesc = "Включите, чтобы ссылки открывались в AnonGram, а не в стороннем браузере.",
        deleteCookies = "Удалить файлы cookie",
        clearCache = "Очистить кэш",
        cookiesCacheDesc = "Вы можете удалить все файлы cookie и кэш встроенного браузера.",
        viewHistory = "Просмотреть историю",
        clearHistory = "Удалить историю",
        pagesCount = { n -> "$n страниц" },
        neverOpenInBrowser = "Не открывать во встроенном браузере",
        addSite = "Добавить сайт",
        addSiteHint = "Например: youtube.com",
        neverOpenDesc = "Эти сайты всегда будут открываться в стороннем браузере по умолчанию.",
        searchEngine = "Поисковая система",
        searchEngineDesc = "Вы можете выбрать систему для поиска в интернете и адресной строке.",
        cancel = "Отмена",
        add = "Добавить",
        emptyHistory = "История просмотров пуста",
        emptyBookmarks = "Закладок пока нет",
        cookiesClearedToast = "Файлы cookie удалены",
        cacheClearedToast = "Кэш очищен",
        linkCopiedToast = "Ссылка скопирована",
        linksAndBrowserSection = "Ссылки и браузер",
        linkPreviewsTitle = "Предпросмотр ссылок",
        linkPreviewsDesc = "Показывает заголовок и обложку ссылки под сообщением. Отключите для максимальной приватности."
    )

    private val en = InAppBrowserStrings(
        openInDefaultBrowser = "Open in...",
        refresh = "Refresh",
        findInPage = "Find in page",
        addBookmark = "Add bookmark",
        removeBookmark = "Remove bookmark",
        share = "Share",
        history = "History",
        bookmarks = "Bookmarks",
        settings = "Settings",
        findPlaceholder = "Find on page...",
        minimizeBrowser = "Minimize",
        expandBrowser = "Expand",
        closeBrowser = "Close",
        browserSettingsTitle = "Browser",
        useInAppBrowser = "Use built-in browser",
        useInAppBrowserDesc = "Enable to open links inside AnonGram instead of an external browser.",
        deleteCookies = "Delete cookies",
        clearCache = "Clear cache",
        cookiesCacheDesc = "You can delete all cookies and cache of the built-in browser.",
        viewHistory = "View history",
        clearHistory = "Clear history",
        pagesCount = { n -> "$n pages" },
        neverOpenInBrowser = "Never open in built-in browser",
        addSite = "Add site",
        addSiteHint = "Example: youtube.com",
        neverOpenDesc = "These sites will always open in your default external browser.",
        searchEngine = "Search engine",
        searchEngineDesc = "Choose the search engine used for web queries and address bar.",
        cancel = "Cancel",
        add = "Add",
        emptyHistory = "Browsing history is empty",
        emptyBookmarks = "No bookmarks yet",
        cookiesClearedToast = "Cookies deleted",
        cacheClearedToast = "Cache cleared",
        linkCopiedToast = "Link copied",
        linksAndBrowserSection = "Links & Browser",
        linkPreviewsTitle = "Link previews",
        linkPreviewsDesc = "Shows link title and preview card below messages. Disable for maximum privacy."
    )

    val current: InAppBrowserStrings
        @Composable
        get() {
            val lang by LanguageSettingsStore.language.collectAsState()
            return if (lang == AppLanguage.RU) ru else en
        }
}
