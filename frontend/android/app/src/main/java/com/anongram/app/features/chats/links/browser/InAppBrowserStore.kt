package com.anongram.app.features.chats.links.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import com.anongram.app.core.theme.ChatTextSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class BrowserSearchEngine(val label: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p="),
    BRAVE("Brave Search", "https://search.brave.com/search?q=")
}

data class BrowserPageEntry(
    val url: String,
    val title: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class InAppBrowserState(
    val activeUrl: String? = null,
    val pageTitle: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val isMinimized: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isHistoryOpen: Boolean = false,
    val isBookmarksOpen: Boolean = false,
    val history: List<BrowserPageEntry> = emptyList(),
    val bookmarks: List<BrowserPageEntry> = emptyList(),
    val excludedDomains: List<String> = emptyList(),
    val searchEngine: BrowserSearchEngine = BrowserSearchEngine.GOOGLE,
    val cookieSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val reloadNonce: Long = 0L
)

object InAppBrowserStore {
    private const val PREFS_NAME = "anongram_in_app_browser"
    private const val KEY_HISTORY = "history_json"
    private const val KEY_BOOKMARKS = "bookmarks_json"
    private const val KEY_EXCLUDED = "excluded_domains_json"
    private const val KEY_SEARCH_ENGINE = "search_engine"

    private val _state = MutableStateFlow(InAppBrowserState())
    val state: StateFlow<InAppBrowserState> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        initialized = true
        val appCtx = context.applicationContext
        ChatTextSettingsStore.ensureInitialized(appCtx)
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = parseEntries(prefs.getString(KEY_HISTORY, null))
        val bookmarks = parseEntries(prefs.getString(KEY_BOOKMARKS, null))
        val excluded = parseStrings(prefs.getString(KEY_EXCLUDED, null))
        val engineName = prefs.getString(KEY_SEARCH_ENGINE, BrowserSearchEngine.GOOGLE.name)
        val engine = BrowserSearchEngine.entries.firstOrNull { it.name == engineName } ?: BrowserSearchEngine.GOOGLE

        _state.update {
            it.copy(
                history = history,
                bookmarks = bookmarks,
                excludedDomains = excluded,
                searchEngine = engine
            )
        }
        refreshStorageStats(appCtx)
    }

    fun openUrl(context: Context, rawUrl: String) {
        ensureInitialized(context)
        val url = normalizeUrl(rawUrl)
        val host = extractHost(url)
        val useInApp = ChatTextSettingsStore.inAppBrowserEnabled.value
        val isExcluded = _state.value.excludedDomains.any { dom ->
            host == dom || host.endsWith(".$dom")
        }
        if (!useInApp || isExcluded) {
            openInDefaultBrowser(context, url)
            return
        }
        _state.update {
            it.copy(
                activeUrl = url,
                pageTitle = host.ifBlank { url },
                progress = 10,
                isLoading = true,
                isMinimized = false,
                isHistoryOpen = false,
                isBookmarksOpen = false
            )
        }
        recordHistory(context, url, host.ifBlank { url })
    }

    fun openInDefaultBrowser(context: Context, rawUrl: String) {
        val url = normalizeUrl(rawUrl)
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun updatePageProgress(url: String?, title: String?, progress: Int) {
        _state.update { st ->
            if (st.activeUrl == null) st
            else {
                val cleanTitle = title?.takeIf { it.isNotBlank() } ?: st.pageTitle
                st.copy(
                    activeUrl = url?.takeIf { it.startsWith("http") } ?: st.activeUrl,
                    pageTitle = cleanTitle,
                    progress = progress.coerceIn(0, 100),
                    isLoading = progress in 1..99
                )
            }
        }
    }

    fun onPageFinished(context: Context, url: String?, title: String?) {
        val finalUrl = url?.takeIf { it.startsWith("http") } ?: _state.value.activeUrl ?: return
        val finalTitle = title?.takeIf { it.isNotBlank() } ?: extractHost(finalUrl).ifBlank { finalUrl }
        _state.update {
            it.copy(
                activeUrl = finalUrl,
                pageTitle = finalTitle,
                progress = 100,
                isLoading = false
            )
        }
        recordHistory(context, finalUrl, finalTitle)
        refreshStorageStats(context)
    }

    fun minimize() = _state.update { it.copy(isMinimized = true, isHistoryOpen = false, isBookmarksOpen = false, isSettingsOpen = false) }
    fun expand() = _state.update { it.copy(isMinimized = false) }
    fun close() = _state.update {
        it.copy(
            activeUrl = null,
            pageTitle = "",
            progress = 0,
            isLoading = false,
            isMinimized = false,
            isHistoryOpen = false,
            isBookmarksOpen = false
        )
    }

    fun triggerReload() = _state.update { it.copy(reloadNonce = it.reloadNonce + 1L) }

    fun openSettings(context: Context) {
        ensureInitialized(context)
        refreshStorageStats(context)
        _state.update { it.copy(isSettingsOpen = true, isHistoryOpen = false, isBookmarksOpen = false) }
    }

    fun closeSettings() = _state.update { it.copy(isSettingsOpen = false) }

    fun openHistory(context: Context) {
        ensureInitialized(context)
        _state.update { it.copy(isHistoryOpen = true, isBookmarksOpen = false) }
    }

    fun closeHistory() = _state.update { it.copy(isHistoryOpen = false) }

    fun openBookmarks(context: Context) {
        ensureInitialized(context)
        _state.update { it.copy(isBookmarksOpen = true, isHistoryOpen = false) }
    }

    fun closeBookmarks() = _state.update { it.copy(isBookmarksOpen = false) }

    fun toggleCurrentBookmark(context: Context) {
        val url = _state.value.activeUrl ?: return
        val title = _state.value.pageTitle.ifBlank { extractHost(url) }
        val current = _state.value.bookmarks
        val exists = current.any { it.url == url }
        val updated = if (exists) {
            current.filterNot { it.url == url }
        } else {
            listOf(BrowserPageEntry(url = url, title = title)) + current.take(199)
        }
        _state.update { it.copy(bookmarks = updated) }
        saveEntries(context, KEY_BOOKMARKS, updated)
    }

    fun removeBookmark(context: Context, url: String) {
        val updated = _state.value.bookmarks.filterNot { it.url == url }
        _state.update { it.copy(bookmarks = updated) }
        saveEntries(context, KEY_BOOKMARKS, updated)
    }

    fun clearHistory(context: Context) {
        _state.update { it.copy(history = emptyList()) }
        saveEntries(context, KEY_HISTORY, emptyList())
    }

    fun addExcludedDomain(context: Context, rawDomain: String) {
        val clean = extractHost(normalizeUrl(rawDomain))
        if (clean.isBlank()) return
        val updated = (_state.value.excludedDomains + clean).distinct()
        _state.update { it.copy(excludedDomains = updated) }
        saveStrings(context, KEY_EXCLUDED, updated)
    }

    fun removeExcludedDomain(context: Context, domain: String) {
        val updated = _state.value.excludedDomains.filterNot { it == domain }
        _state.update { it.copy(excludedDomains = updated) }
        saveStrings(context, KEY_EXCLUDED, updated)
    }

    fun setSearchEngine(context: Context, engine: BrowserSearchEngine) {
        _state.update { it.copy(searchEngine = engine) }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SEARCH_ENGINE, engine.name)
            .apply()
    }

    fun clearCookies(context: Context) {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        refreshStorageStats(context)
    }

    fun clearCache(context: Context) {
        runCatching {
            WebView(context.applicationContext).apply {
                clearCache(true)
                destroy()
            }
            val webCacheDir = File(context.applicationContext.cacheDir, "WebView")
            if (webCacheDir.exists()) webCacheDir.deleteRecursively()
        }
        refreshStorageStats(context)
    }

    fun refreshStorageStats(context: Context) {
        val appCtx = context.applicationContext
        val webViewDir = File(appCtx.dataDir, "app_webview")
        val cookiesFile = File(webViewDir, "Default/Cookies")
        val cookieBytes = if (cookiesFile.exists()) cookiesFile.length() else 0L
        val cacheDir = File(appCtx.cacheDir, "WebView")
        val cacheBytes = dirSize(cacheDir) + dirSize(File(webViewDir, "Default/Cache"))
        _state.update { it.copy(cookieSizeBytes = cookieBytes, cacheSizeBytes = cacheBytes) }
    }

    private fun recordHistory(context: Context, url: String, title: String) {
        val current = _state.value.history
        val deduped = current.filterNot { it.url == url }
        val updated = (listOf(BrowserPageEntry(url = url, title = title)) + deduped).take(200)
        _state.update { it.copy(history = updated) }
        saveEntries(context, KEY_HISTORY, updated)
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }

    private fun normalizeUrl(raw: String): String {
        val t = raw.trim()
        return if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
    }

    fun extractHost(url: String): String =
        runCatching { Uri.parse(url).host?.lowercase()?.removePrefix("www.") }.getOrNull().orEmpty()

    private fun saveEntries(context: Context, key: String, list: List<BrowserPageEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("url", e.url)
                    put("title", e.title)
                    put("ts", e.timestampMs)
                }
            )
        }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, arr.toString())
            .apply()
    }

    private fun parseEntries(raw: String?): List<BrowserPageEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val url = o.optString("url")
                if (url.isBlank()) null
                else BrowserPageEntry(url, o.optString("title", url), o.optLong("ts"))
            }
        }.getOrDefault(emptyList())
    }

    private fun saveStrings(context: Context, key: String, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, arr.toString())
            .apply()
    }

    private fun parseStrings(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
        }.getOrDefault(emptyList())
    }
}
