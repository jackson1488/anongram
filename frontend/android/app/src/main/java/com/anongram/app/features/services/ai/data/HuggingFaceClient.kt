package com.anongram.app.features.services.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object HuggingFaceClient {

    suspend fun fetchRepoGgufFiles(repoId: String): Result<List<HfModelFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanRepo = repoId.trim().removePrefix("https://huggingface.co/").trim('/')
            val apiUrl = "https://huggingface.co/api/models/$cleanRepo?blobs=true"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "AnonGram-Android/1.0")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonText)
            val siblings = root.optJSONArray("siblings") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until siblings.length()) {
                    val item = siblings.optJSONObject(i) ?: continue
                    val rfilename = item.optString("rfilename", "")
                    if (!rfilename.endsWith(".gguf", ignoreCase = true)) continue
                    val sizeBytes = item.optLong("size", 0L).takeIf { it > 0L }
                        ?: item.optJSONObject("lfs")?.optLong("size", 0L)
                        ?: 0L
                    add(
                        HfModelFile(
                            filename = rfilename,
                            sizeBytes = sizeBytes,
                            downloadUrl = "https://huggingface.co/$cleanRepo/resolve/main/$rfilename"
                        )
                    )
                }
            }.sortedBy { it.sizeBytes }
        }
    }

    private fun openWithRedirects(initialUrl: String, existingBytes: Long): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < 6) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 45_000
                setRequestProperty("User-Agent", "AnonGram-Android/1.0")
                if (existingBytes > 0L) {
                    setRequestProperty("Range", "bytes=$existingBytes-")
                }
            }
            val code = conn.responseCode
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect HTTP $code without Location")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
                redirects++
                continue
            }
            return conn
        }
        throw IllegalStateException("Too many redirects")
    }

    suspend fun downloadFile(
        url: String,
        destFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            destFile.parentFile?.mkdirs()
            val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
            var existingBytes = if (tmpFile.exists()) tmpFile.length() else 0L

            var conn = openWithRedirects(url, existingBytes)
            var code = conn.responseCode
            if (code == 416 && existingBytes > 0L) {
                conn.disconnect()
                tmpFile.delete()
                existingBytes = 0L
                conn = openWithRedirects(url, 0L)
                code = conn.responseCode
            }

            val append = code == 206
            if (code !in 200..299) {
                conn.disconnect()
                throw IllegalStateException("HTTP $code")
            }

            val contentLen = conn.contentLengthLong
            val totalBytes = if (append && contentLen > 0L) existingBytes + contentLen else contentLen
            var downloaded = if (append) existingBytes else 0L

            try {
                conn.inputStream.use { input ->
                    FileOutputStream(tmpFile, append).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (isActive) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, totalBytes)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }

            if (!isActive) {
                throw java.util.concurrent.CancellationException("Download cancelled")
            }

            // Verify GGUF magic header ("GGUF")
            val magic = ByteArray(4)
            val readMagic = tmpFile.inputStream().use { it.read(magic) }
            if (readMagic < 4 || magic[0] != 'G'.code.toByte() || magic[1] != 'G'.code.toByte() ||
                magic[2] != 'U'.code.toByte() || magic[3] != 'F'.code.toByte()
            ) {
                tmpFile.delete()
                throw IllegalStateException("Invalid GGUF file")
            }

            if (destFile.exists()) destFile.delete()
            if (!tmpFile.renameTo(destFile)) {
                tmpFile.copyTo(destFile, overwrite = true)
                tmpFile.delete()
            }
            destFile
        }
    }
}
