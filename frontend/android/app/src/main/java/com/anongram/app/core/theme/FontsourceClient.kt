package com.anongram.app.core.theme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Лёгкий клиент внешнего каталога шрифтов Fontsource API (1700+ открытых шрифтов, без API-ключа).
 */
object FontsourceClient {
    private const val CATALOG_URL = "https://api.fontsource.org/v1/fonts"
    private const val FONT_DETAIL_URL = "https://api.fontsource.org/v1/fonts/"

    suspend fun fetchCatalog(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(CATALOG_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 25_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) error("HTTP $code")
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }

    fun parseCatalogJson(rawJson: String): List<FontCatalogItem> {
        return runCatching {
            val arr = JSONArray(rawJson)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val family = obj.optString("family", id).takeIf { it.isNotBlank() } ?: id
                val category = obj.optString("category", "sans-serif")
                val subsets = obj.optJSONArray("subsets")
                var cyr = false
                if (subsets != null) {
                    for (s in 0 until subsets.length()) {
                        if (subsets.optString(s).equals("cyrillic", ignoreCase = true)) {
                            cyr = true
                            break
                        }
                    }
                }
                FontCatalogItem(
                    id = id,
                    family = family,
                    category = category,
                    hasCyrillic = cyr
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun resolveTtfUrl(fontId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("$FONT_DETAIL_URL$fontId").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) error("HTTP $code")
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(body)
                val variants = root.optJSONObject("variants") ?: error("No font variants")
                val weightObj = variants.optJSONObject("400")
                    ?: variants.keys().asSequence().firstOrNull()?.let { variants.optJSONObject(it) }
                    ?: error("No font weight")
                val styleObj = weightObj.optJSONObject("normal")
                    ?: weightObj.keys().asSequence().firstOrNull()?.let { weightObj.optJSONObject(it) }
                    ?: error("No font style")

                val preferredOrder = listOf("cyrillic", "cyrillic-ext", "latin", "latin-ext")
                for (sub in preferredOrder) {
                    val ttf = styleObj.optJSONObject(sub)?.optJSONObject("url")?.optString("ttf")
                    if (!ttf.isNullOrBlank()) return@runCatching ttf
                }
                val anyKey = styleObj.keys().asSequence().firstOrNull { k ->
                    !styleObj.optJSONObject(k)?.optJSONObject("url")?.optString("ttf").isNullOrBlank()
                } ?: error("No .ttf URL for $fontId")
                styleObj.getJSONObject(anyKey).getJSONObject("url").getString("ttf")
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun downloadFile(
        url: String,
        destFile: File,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) error("HTTP $code")
                val total = conn.contentLengthLong.coerceAtLeast(1L)
                val tmp = File(destFile.parentFile, "${destFile.name}.part")
                conn.inputStream.use { input ->
                    FileOutputStream(tmp).use { out ->
                        val buf = ByteArray(16 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            out.write(buf, 0, n)
                            downloaded += n
                            onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                if (destFile.exists()) destFile.delete()
                tmp.renameTo(destFile)
                Unit
            } finally {
                conn.disconnect()
            }
        }
    }
}
