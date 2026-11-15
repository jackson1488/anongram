package com.anongram.app.features.services.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val pageExcerpt: String = ""
)

/**
 * Умный клиент живого веб-поиска:
 * 1) Публичные инстансы SearXNG JSON API (реальная выдача веб-поиска)
 * 2) Google News RSS (свежие новости и события)
 * 3) Wikipedia OpenSearch (резерв, если первые два недоступны)
 * + загрузка текста топ-страниц (fetchPageExcerpt) для глубокого осмысления моделью.
 */
object WebSearchClient {

    private val searxInstances = listOf(
        "https://searx.be",
        "https://paulgo.io",
        "https://searx.work",
        "https://search.bus-hit.me"
    )

    suspend fun search(query: String, maxResults: Int = 5, fetchTopPages: Int = 1): List<WebSearchResult> =
        withContext(Dispatchers.IO) {
            val clean = query.trim().take(220)
            if (clean.isEmpty()) return@withContext emptyList()

            val results = mutableListOf<WebSearchResult>()

            // 1. DuckDuckGo HTML (живой веб-поиск без API-ключей)
            val ddg = runCatching { searchDuckDuckGoHtml(clean, maxResults) }.getOrDefault(emptyList())
            results.addAll(ddg)

            // 2. SearXNG JSON (резерв, если DuckDuckGo не ответил)
            if (results.size < 2) {
                for (base in searxInstances) {
                    val found = runCatching { searchSearxJson(base, clean, maxResults) }.getOrDefault(emptyList())
                    if (found.isNotEmpty()) {
                        results.addAll(found)
                        break
                    }
                }
            }

            // 3. Google News RSS (свежие новости, если выдачи мало)
            if (results.size < 2) {
                val news = runCatching { searchGoogleNewsRss(clean, maxResults) }.getOrDefault(emptyList())
                results.addAll(news)
            }

            // 4. Wikipedia (последний резерв)
            if (results.isEmpty()) {
                val wiki = runCatching { searchWikipedia(clean, maxResults) }.getOrDefault(emptyList())
                results.addAll(wiki)
            }

            val deduped = results.distinctBy { it.url }.take(maxResults)

            deduped.mapIndexed { idx, item ->
                if (idx < fetchTopPages && !item.url.contains("news.google.com/rss/articles")) {
                    val excerpt = runCatching { fetchPageExcerpt(item.url, 1500) }.getOrNull().orEmpty()
                    if (excerpt.isNotBlank()) item.copy(pageExcerpt = excerpt) else item
                } else {
                    item
                }
            }
        }

    suspend fun fetchPageText(url: String, maxChars: Int = 3500): String = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) return@withContext ""
        runCatching { fetchPageExcerpt(cleanUrl, maxChars).orEmpty() }.getOrDefault("")
    }

    private fun searchDuckDuckGoHtml(query: String, maxResults: Int): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val conn = (URL("https://html.duckduckgo.com/html/?q=$encoded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 5000
            readTimeout = 7000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val titleLinkRegex = Regex(
                """<a[^>]*class="[^"]*result__a[^"]*"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val snippetRegex = Regex(
                """class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</(?:a|div|span)>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val links = titleLinkRegex.findAll(html).toList()
            val snippets = snippetRegex.findAll(html).toList()
            val out = mutableListOf<WebSearchResult>()
            for (i in links.indices) {
                if (out.size >= maxResults) break
                val rawHref = decodeHtml(links[i].groupValues[1].trim())
                val resolvedUrl = resolveDdgRedirect(rawHref)
                val title = stripHtml(links[i].groupValues[2])
                val snippet = snippets.getOrNull(i)?.let { stripHtml(it.groupValues[1]) }.orEmpty()
                if (resolvedUrl.startsWith("http") && title.isNotBlank()) {
                    out.add(WebSearchResult(title = title, url = resolvedUrl, snippet = snippet))
                }
            }
            out
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveDdgRedirect(href: String): String {
        val normalized = if (href.startsWith("//")) "https:$href" else href
        val uddgIdx = normalized.indexOf("uddg=")
        if (uddgIdx >= 0) {
            val encodedPart = normalized.substring(uddgIdx + 5).substringBefore('&')
            return runCatching { java.net.URLDecoder.decode(encodedPart, "UTF-8") }.getOrDefault(normalized)
        }
        return normalized
    }

    private fun searchSearxJson(baseUrl: String, query: String, maxResults: Int): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search?q=$encoded&format=json&language=auto"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4500
            readTimeout = 5500
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val arr = json.optJSONArray("results") ?: return emptyList()
            val out = mutableListOf<WebSearchResult>()
            for (i in 0 until arr.length()) {
                if (out.size >= maxResults) break
                val obj = arr.optJSONObject(i) ?: continue
                val title = stripHtml(obj.optString("title"))
                val link = obj.optString("url").trim()
                val content = stripHtml(obj.optString("content"))
                val date = obj.optString("publishedDate").take(10)
                val snippet = if (date.isNotBlank() && content.isNotBlank()) "[$date] $content" else content
                if (link.startsWith("http") && title.isNotBlank()) {
                    out.add(WebSearchResult(title = title, url = link, snippet = snippet))
                }
            }
            out
        } finally {
            conn.disconnect()
        }
    }

    private fun searchGoogleNewsRss(query: String, maxResults: Int): List<WebSearchResult> {
        val isCyrillic = query.any { it in '\u0400'..'\u04FF' }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val params = if (isCyrillic) "hl=ru&gl=RU&ceid=RU:ru" else "hl=en-US&gl=US&ceid=US:en"
        val conn = (URL("https://news.google.com/rss/search?q=$encoded&$params").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 6000
            setRequestProperty("User-Agent", "AnonGram/1.0")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            val itemRegex = Regex("""<item>(.*?)</item>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val titleRegex = Regex("""<title>(.*?)</title>""", RegexOption.DOT_MATCHES_ALL)
            val linkRegex = Regex("""<link>(.*?)</link>""", RegexOption.DOT_MATCHES_ALL)
            val pubRegex = Regex("""<pubDate>(.*?)</pubDate>""", RegexOption.DOT_MATCHES_ALL)
            val descRegex = Regex("""<description>(.*?)</description>""", RegexOption.DOT_MATCHES_ALL)

            val out = mutableListOf<WebSearchResult>()
            for (match in itemRegex.findAll(xml)) {
                if (out.size >= maxResults) break
                val block = match.groupValues[1]
                val title = stripHtml(titleRegex.find(block)?.groupValues?.get(1).orEmpty())
                val link = linkRegex.find(block)?.groupValues?.get(1)?.trim().orEmpty()
                val pub = pubRegex.find(block)?.groupValues?.get(1)?.trim()?.take(16).orEmpty()
                val desc = stripHtml(descRegex.find(block)?.groupValues?.get(1).orEmpty())
                val snippet = listOf(pub, desc).filter { it.isNotBlank() }.joinToString(" · ")
                if (link.startsWith("http") && title.isNotBlank()) {
                    out.add(WebSearchResult(title = title, url = link, snippet = snippet))
                }
            }
            out
        } finally {
            conn.disconnect()
        }
    }

    private fun searchWikipedia(query: String, maxResults: Int): List<WebSearchResult> {
        val isCyrillic = query.any { it in '\u0400'..'\u04FF' }
        val host = if (isCyrillic) "ru.wikipedia.org" else "en.wikipedia.org"
        val encoded = URLEncoder.encode(query, "UTF-8")
        val conn = (URL("https://$host/w/api.php?action=opensearch&search=$encoded&limit=$maxResults&namespace=0&format=json").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 6000
            setRequestProperty("User-Agent", "AnonGram/1.0")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = org.json.JSONArray(json)
            if (arr.length() < 4) return emptyList()
            val titles = arr.getJSONArray(1)
            val descs = arr.getJSONArray(2)
            val urls = arr.getJSONArray(3)
            val out = mutableListOf<WebSearchResult>()
            for (i in 0 until minOf(titles.length(), urls.length(), maxResults)) {
                val t = titles.optString(i)
                val d = descs.optString(i)
                val u = urls.optString(i)
                if (u.startsWith("http") && t.isNotBlank()) {
                    out.add(WebSearchResult(title = t, url = u, snippet = d))
                }
            }
            out
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchPageExcerpt(url: String, maxChars: Int): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 4000
            readTimeout = 5000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val contentType = conn.contentType.orEmpty()
            if (!contentType.contains("text/html", ignoreCase = true) && !contentType.contains("text/plain", ignoreCase = true)) {
                return null
            }
            val raw = conn.inputStream.bufferedReader().use { reader ->
                val buf = CharArray(32768)
                val n = reader.read(buf)
                if (n > 0) String(buf, 0, n) else ""
            }
            if (raw.isBlank()) return null
            val cleaned = raw
                .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<nav[^>]*>.*?</nav>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<header[^>]*>.*?</header>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<footer[^>]*>.*?</footer>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            stripHtml(cleaned).take(maxChars).takeIf { it.length > 80 }
        } finally {
            conn.disconnect()
        }
    }

    private fun stripHtml(raw: String): String =
        decodeHtml(raw.replace(Regex("<[^>]+>"), " "))
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun decodeHtml(s: String): String = s
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
}
