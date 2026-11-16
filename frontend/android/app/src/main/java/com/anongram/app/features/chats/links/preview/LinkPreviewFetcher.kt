package com.anongram.app.features.chats.links.preview

import android.content.Context
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Умный загрузчик предпросмотра ссылок:
 * - Скачивает только первые 64 КБ HTML (<head>)
 * - Извлекает OpenGraph / Twitter / standard meta теги
 * - Пропускает .onion и локальные адреса ради приватности
 * - Кэширует в RAM (LruCache) и на диске (cacheDir), ограничивая параллельность (Semaphore(3)).
 */
object LinkPreviewFetcher {

    private const val MAX_HEAD_BYTES = 65_536
    private const val DISK_CACHE_FILE = "anongram_link_previews_v2.json"
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private val videoExtensions = setOf("mp4", "webm", "mov", "m4v", "mkv")
    private val urlRegex = Regex(
        """((?:https?://|www\.)[^\s<>\]\[)"'«»]+)""",
        RegexOption.IGNORE_CASE
    )

    private val ramCache = LruCache<String, LinkPreviewData>(128)
    private val failedCache = LruCache<String, Long>(128)
    private val diskLock = Any()
    private val networkSemaphore = Semaphore(3)

    @Volatile
    private var diskMap: MutableMap<String, LinkPreviewData>? = null

    fun extractAllUrls(text: String): List<Pair<IntRange, String>> {
        if (text.isBlank()) return emptyList()
        return urlRegex.findAll(text).mapNotNull { match ->
            val raw = match.value.trimEnd('.', ',', ';', ':', '!', '?', ')')
            val end = match.range.first + raw.length - 1
            if (end < match.range.first) null
            else (match.range.first..end) to normalizeUrl(raw)
        }.toList()
    }

    fun extractFirstUrl(text: String): String? = extractAllUrls(text).firstOrNull()?.second

    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun isPrivateOrOnion(url: String): Boolean {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull().orEmpty()
        if (host.isBlank()) return true
        if (host.endsWith(".onion") || host == "localhost" || host.endsWith(".local")) return true
        if (host.startsWith("127.") || host.startsWith("10.") || host.startsWith("192.168.")) return true
        return false
    }

    suspend fun fetch(context: Context, rawUrl: String): LinkPreviewData? {
        val url = normalizeUrl(rawUrl)
        if (isPrivateOrOnion(url)) return null

        ramCache.get(url)?.let { return it }
        val lastFail = failedCache.get(url)
        if (lastFail != null && System.currentTimeMillis() - lastFail < 5 * 60_000L) return null

        loadDiskCache(context)[url]?.let { cached ->
            ramCache.put(url, cached)
            return cached
        }

        detectDirectMediaByExtension(url)?.let { direct ->
            ramCache.put(url, direct)
            saveToDiskCache(context, direct)
            return direct
        }

        return withContext(Dispatchers.IO) {
            networkSemaphore.withPermit {
                ramCache.get(url)?.let { return@withPermit it }
                val fetched = runCatching { fetchMetaFromHead(url) }.getOrNull()
                if (fetched != null && (fetched.title.isNotBlank() || fetched.description.isNotBlank() || fetched.mediaType != LinkMediaType.NONE)) {
                    ramCache.put(url, fetched)
                    saveToDiskCache(context, fetched)
                    fetched
                } else {
                    failedCache.put(url, System.currentTimeMillis())
                    null
                }
            }
        }
    }

    private fun detectDirectMediaByExtension(url: String): LinkPreviewData? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = uri.host?.removePrefix("www.").orEmpty()
        val path = uri.path.orEmpty()
        val ext = path.substringAfterLast('.', "").lowercase()
        val fileName = path.substringAfterLast('/', "").ifBlank { host }
        return when {
            ext in imageExtensions -> LinkPreviewData(
                url = url,
                domain = host,
                title = fileName,
                imageUrl = url,
                siteName = host,
                mediaType = LinkMediaType.IMAGE
            )
            ext in videoExtensions -> LinkPreviewData(
                url = url,
                domain = host,
                title = fileName,
                siteName = host,
                mediaType = LinkMediaType.VIDEO,
                embedUrl = url
            )
            else -> null
        }
    }

    private fun extractYoutubeEmbedUrl(url: String): String? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        val videoId = when {
            host == "youtu.be" -> uri.pathSegments.firstOrNull()
            host == "youtube.com" || host == "m.youtube.com" -> {
                val path = uri.path.orEmpty()
                when {
                    path.startsWith("/shorts/") -> uri.pathSegments.getOrNull(1)
                    path.startsWith("/embed/") -> uri.pathSegments.getOrNull(1)
                    else -> uri.getQueryParameter("v")
                }
            }
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return null
        return "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0"
    }

    private fun fetchMetaFromHead(url: String): LinkPreviewData? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 4500
            readTimeout = 4500
            setRequestProperty("Range", "bytes=0-${MAX_HEAD_BYTES - 1}")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml,image/*,video/*")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val contentType = conn.contentType.orEmpty().lowercase()
            val finalUrl = conn.url?.toString() ?: url
            val uri = runCatching { Uri.parse(finalUrl) }.getOrNull()
            val host = uri?.host?.removePrefix("www.").orEmpty()
            val fileName = uri?.path?.substringAfterLast('/', "")?.ifBlank { host } ?: host

            if (contentType.startsWith("image/")) {
                return LinkPreviewData(
                    url = url,
                    domain = host,
                    title = fileName,
                    imageUrl = finalUrl,
                    siteName = host,
                    mediaType = LinkMediaType.IMAGE
                )
            }
            if (contentType.startsWith("video/")) {
                return LinkPreviewData(
                    url = url,
                    domain = host,
                    title = fileName,
                    siteName = host,
                    mediaType = LinkMediaType.VIDEO,
                    embedUrl = finalUrl
                )
            }
            if (contentType.isNotBlank() &&
                !contentType.contains("text/html") &&
                !contentType.contains("xhtml")
            ) {
                return null
            }
            val html = conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(4096)
                var total = 0
                while (total < MAX_HEAD_BYTES) {
                    val read = input.read(buf, 0, minOf(buf.size, MAX_HEAD_BYTES - total))
                    if (read <= 0) break
                    out.write(buf, 0, read)
                    total += read
                }
                out.toString(Charsets.UTF_8.name())
            }
            if (html.isBlank()) return null

            val headSection = html.substringBefore("</head>", html)
            val ogTitle = extractMeta(headSection, "og:title")
                ?: extractMeta(headSection, "twitter:title")
                ?: extractTitleTag(headSection)
                ?: ""
            val ogDesc = extractMeta(headSection, "og:description")
                ?: extractMeta(headSection, "twitter:description")
                ?: extractMeta(headSection, "description")
                ?: ""
            val rawImage = extractMeta(headSection, "og:image")
                ?: extractMeta(headSection, "twitter:image")
                ?: ""
            val rawVideo = extractMeta(headSection, "og:video:secure_url")
                ?: extractMeta(headSection, "og:video:url")
                ?: extractMeta(headSection, "og:video")
                ?: ""
            val siteName = extractMeta(headSection, "og:site_name") ?: host
            val resolvedImage = resolveImageUrl(finalUrl, rawImage)
            val youtubeEmbed = extractYoutubeEmbedUrl(finalUrl) ?: extractYoutubeEmbedUrl(url)
            val resolvedVideo = youtubeEmbed ?: resolveImageUrl(finalUrl, rawVideo)
            val mediaType = if (resolvedVideo.isNotBlank()) LinkMediaType.VIDEO else LinkMediaType.NONE

            if (ogTitle.isBlank() && ogDesc.isBlank() && mediaType == LinkMediaType.NONE) return null
            LinkPreviewData(
                url = url,
                domain = host,
                title = decodeHtml(ogTitle).trim(),
                description = decodeHtml(ogDesc).trim(),
                imageUrl = resolvedImage,
                siteName = decodeHtml(siteName).trim().ifBlank { host },
                mediaType = mediaType,
                embedUrl = resolvedVideo
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun extractMeta(html: String, key: String): String? {
        val esc = Regex.escape(key)
        val p1 = Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']$esc["'][^>]+content\s*=\s*["']([^"']*)["']""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        p1.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it }

        val p2 = Regex(
            """<meta[^>]+content\s*=\s*["']([^"']*)["'][^>]+(?:property|name)\s*=\s*["']$esc["']""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return p2.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    private fun extractTitleTag(html: String): String? {
        val regex = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        return regex.find(html)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ")?.trim()
    }

    private fun resolveImageUrl(baseUrl: String, img: String): String {
        val clean = img.trim()
        if (clean.isBlank()) return ""
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        if (clean.startsWith("//")) return "https:$clean"
        return runCatching { URL(URL(baseUrl), clean).toString() }.getOrDefault("")
    }

    private fun decodeHtml(s: String): String = s
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")

    private fun loadDiskCache(context: Context): Map<String, LinkPreviewData> {
        diskMap?.let { return it }
        synchronized(diskLock) {
            diskMap?.let { return it }
            val loaded = LinkedHashMap<String, LinkPreviewData>()
            runCatching {
                val file = File(context.applicationContext.cacheDir, DISK_CACHE_FILE)
                if (file.exists()) {
                    val root = JSONObject(file.readText())
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val o = root.optJSONObject(k) ?: continue
                        loaded[k] = LinkPreviewData(
                            url = k,
                            domain = o.optString("domain"),
                            title = o.optString("title"),
                            description = o.optString("description"),
                            imageUrl = o.optString("imageUrl"),
                            siteName = o.optString("siteName"),
                            mediaType = runCatching {
                                LinkMediaType.valueOf(o.optString("mediaType", LinkMediaType.NONE.name))
                            }.getOrDefault(LinkMediaType.NONE),
                            embedUrl = o.optString("embedUrl")
                        )
                    }
                }
            }
            diskMap = loaded
            return loaded
        }
    }

    private fun saveToDiskCache(context: Context, item: LinkPreviewData) {
        synchronized(diskLock) {
            val map = (diskMap ?: LinkedHashMap()).apply {
                put(item.url, item)
                if (size > 200) {
                    val firstKey = keys.firstOrNull()
                    if (firstKey != null) remove(firstKey)
                }
            }
            diskMap = map
            runCatching {
                val root = JSONObject()
                map.forEach { (k, v) ->
                    root.put(
                        k,
                        JSONObject().apply {
                            put("domain", v.domain)
                            put("title", v.title)
                            put("description", v.description)
                            put("imageUrl", v.imageUrl)
                            put("siteName", v.siteName)
                            put("mediaType", v.mediaType.name)
                            put("embedUrl", v.embedUrl)
                        }
                    )
                }
                File(context.applicationContext.cacheDir, DISK_CACHE_FILE).writeText(root.toString())
            }
        }
    }
}
