package com.anongram.app.features.services.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class WebSearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

/**
 * Лёгкий клиент живого веб-поиска (без сторонних библиотек и API-ключей):
 * 1) DuckDuckGo Lite HTML (основной)
 * 2) Wikipedia OpenSearch + Summary (резервный)
 */
object WebSearchClient {

    suspend fun search(query: String, maxResults: Int = 5): List<WebSearchResult> = withContext(Dispatchers.IO) {
        val clean = query.trim().take(200)
        if (clean.isEmpty()) return@withContext emptyList()

        val ddgResults = runCatching { searchDuckDuckGoLite(clean, maxResults) }.getOrDefault(emptyList())
        if (ddgResults.size >= 2) return@withContext ddgResults

        val wikiResults = runCatching { searchWikipedia(clean, maxResults) }.getOrDefault(emptyList())
        (ddgResults + wikiResults).distinctBy { it.url }.take(maxResults)
    }

    private fun searchDuckDuckGoLite(query: String, maxResults: Int): List<WebSearchResult> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val conn = (URL("https://lite.duckduckgo.com/lite/?q=$encoded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 8000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val linkRegex = Regex(
                """<a[^>]+class=['"]result-link['"][^>]*href=['"]([^'"]+)['"][^>]*>(.*?)</a>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val snippetRegex = Regex(
                """<td[^>]+class=['"]result-snippet['"][^>]*>(.*?)</td>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )

            val links = linkRegex.findAll(html).toList()
            val snippets = snippetRegex.findAll(html).toList()
            val results = mutableListOf<WebSearchResult>()
            for (i in links.indices) {
                if (results.size >= maxResults) break
                val rawUrl = decodeHtml(links[i].groupValues[1])
                val url = normalizeDdgUrl(rawUrl)
                val title = stripHtml(links[i].groupValues[2])
                val snippet = snippets.getOrNull(i)?.let { stripHtml(it.groupValues[1]) }.orEmpty()
                if (url.startsWith("http") && title.isNotBlank()) {
                    results.add(WebSearchResult(title = title, url = url, snippet = snippet))
                }
            }
            results
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
            connectTimeout = 6000
            readTimeout = 7000
            setRequestProperty("User-Agent", "AnonGram/1.0")
        }
        return try {
            if (conn.responseCode !in 200..299) return emptyList()
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
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

    private fun normalizeDdgUrl(raw: String): String {
        val idx = raw.indexOf("uddg=")
        if (idx >= 0) {
            val encoded = raw.substring(idx + 5).substringBefore('&')
            return runCatching { java.net.URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(raw)
        }
        return if (raw.startsWith("//")) "https:$raw" else raw
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
