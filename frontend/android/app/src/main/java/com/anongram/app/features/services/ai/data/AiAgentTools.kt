package com.anongram.app.features.services.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Встроенные read-only веб-инструменты для агентского режима (web_search, web_fetch).
 */
object AiAgentTools {

    fun toolDefinitionsJson(): JSONArray {
        val searchTool = JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "web_search")
                put("description", "Search the web for up-to-date information, documentation, news, or facts. Returns titles, URLs, and snippets.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("query", JSONObject().apply {
                            put("type", "string")
                            put("description", "Search query")
                        })
                    })
                    put("required", JSONArray().put("query"))
                })
            })
        }

        val fetchTool = JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", "web_fetch")
                put("description", "Fetch and extract readable text content from a public http/https URL.")
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("url", JSONObject().apply {
                            put("type", "string")
                            put("description", "The http:// or https:// URL to fetch")
                        })
                    })
                    put("required", JSONArray().put("url"))
                })
            })
        }

        return JSONArray().apply {
            put(searchTool)
            put(fetchTool)
        }
    }

    suspend fun executeTool(name: String, argumentsJson: String): String = withContext(Dispatchers.IO) {
        val args = runCatching { JSONObject(argumentsJson) }.getOrElse { JSONObject() }
        when (name) {
            "web_search" -> {
                val query = args.optString("query").trim()
                if (query.isEmpty()) "Error: empty search query" else runWebSearch(query)
            }
            "web_fetch" -> {
                val url = args.optString("url").trim()
                if (url.isEmpty()) "Error: empty URL" else runWebFetch(url)
            }
            else -> "Error: unknown tool '$name'"
        }
    }

    private fun runWebSearch(query: String): String {
        return runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://html.duckduckgo.com/html/?q=$encoded")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) return "Search HTTP error $code"
                val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseDuckDuckGoResults(html)
            } finally {
                conn.disconnect()
            }
        }.getOrElse { e ->
            "Search failed: ${e.message ?: "network error"}"
        }
    }

    private fun parseDuckDuckGoResults(html: String): String {
        val linkRegex = Regex("<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val snippetRegex = Regex("<a[^>]*class=\"[^\"]*result__snippet[^\"]*\"[^>]*>(.*?)</a>|<div[^>]*class=\"[^\"]*result__snippet[^\"]*\"[^>]*>(.*?)</div>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

        val links = linkRegex.findAll(html).take(6).toList()
        val snippets = snippetRegex.findAll(html).take(6).map { m ->
            stripHtml(m.groupValues[1].ifEmpty { m.groupValues[2] })
        }.toList()

        if (links.isEmpty()) {
            return "No search results found."
        }

        val sb = StringBuilder()
        links.forEachIndexed { idx, match ->
            val rawHref = match.groupValues[1]
            val resolvedUrl = resolveDuckDuckGoHref(rawHref)
            val title = stripHtml(match.groupValues[2])
            val snippet = snippets.getOrNull(idx).orEmpty()
            sb.append("${idx + 1}. $title\n")
            sb.append("URL: $resolvedUrl\n")
            if (snippet.isNotBlank()) sb.append("Snippet: $snippet\n")
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    private fun resolveDuckDuckGoHref(href: String): String {
        val uddgMatch = Regex("[?&]uddg=([^&]+)").find(href)
        if (uddgMatch != null) {
            return runCatching {
                java.net.URLDecoder.decode(uddgMatch.groupValues[1], "UTF-8")
            }.getOrDefault(href)
        }
        return if (href.startsWith("//")) "https:$href" else href
    }

    private fun runWebFetch(rawUrl: String): String {
        val normalized = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
        return runCatching {
            val url = URL(normalized)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) return "Fetch HTTP error $code for $normalized"
                val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val buf = CharArray(120_000)
                    val read = reader.read(buf)
                    if (read <= 0) "" else String(buf, 0, read)
                }
                val contentType = conn.contentType.orEmpty().lowercase()
                val text = if (contentType.contains("html") || raw.contains("<html", ignoreCase = true)) {
                    stripHtml(raw)
                } else {
                    raw
                }
                text.take(16_000).ifBlank { "(Empty page content)" }
            } finally {
                conn.disconnect()
            }
        }.getOrElse { e ->
            "Fetch failed for $normalized: ${e.message ?: "network error"}"
        }
    }

    private fun stripHtml(html: String): String {
        var cleaned = html
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), " ")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), " ")
            .replace(Regex("<noscript[^>]*>.*?</noscript>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), " ")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</(p|div|li|h[1-6]|tr)>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
        cleaned = cleaned
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
        return cleaned
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
