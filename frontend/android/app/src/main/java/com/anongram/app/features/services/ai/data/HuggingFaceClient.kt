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

    suspend fun downloadFile(
        url: String,
        destFile: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            destFile.parentFile?.mkdirs()
            val tmpFile = File(destFile.parentFile, "${destFile.name}.part")
            val existingBytes = if (tmpFile.exists()) tmpFile.length() else 0L

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "AnonGram-Android/1.0")
                if (existingBytes > 0L) {
                    setRequestProperty("Range", "bytes=$existingBytes-")
                }
            }

            val code = conn.responseCode
            val append = code == 206
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }

            val contentLen = conn.contentLengthLong
            val totalBytes = if (append && contentLen > 0L) existingBytes + contentLen else contentLen
            var downloaded = if (append) existingBytes else 0L

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

            if (!isActive) {
                throw java.util.concurrent.CancellationException("Download cancelled")
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
