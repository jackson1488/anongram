package com.anongram.app.core.assets

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

/**
 * Единый менеджер внешних ассетов AnonGram (модели речи и языковые пакеты).
 * Скачивает файлы во внутреннюю директорию `filesDir/anongram_modules/<id>/`,
 * отслеживает прогресс и позволяет удалять пакеты для освобождения памяти.
 */
class ExternalAssetManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val modulesRoot: File
        get() = File(appContext.filesDir, "anongram_modules").apply { mkdirs() }

    private val _states = MutableStateFlow<Map<String, AssetDownloadState>>(emptyMap())
    val states: StateFlow<Map<String, AssetDownloadState>> = _states.asStateFlow()

    init {
        refreshAllStates()
    }

    fun refreshAllStates() {
        val updated = LinkedHashMap<String, AssetDownloadState>()
        for (spec in ModuleAssetManifest.all) {
            if (activeJobs.containsKey(spec.id)) {
                updated[spec.id] = _states.value[spec.id] ?: AssetDownloadState.NotInstalled
                continue
            }
            val installedDir = assetDir(spec.id)
            val size = directorySizeBytes(installedDir)
            updated[spec.id] = if (isInstalled(spec)) {
                AssetDownloadState.Installed(size)
            } else {
                AssetDownloadState.NotInstalled
            }
        }
        _states.value = updated
    }

    fun assetDir(assetId: String): File = File(modulesRoot, assetId)

    /**
     * Возвращает путь к установленному файлу модели (для Whisper — `.bin`)
     * или корневой папке распакованной модели (для Vosk).
     */
    fun getInstalledModelPath(spec: ModuleAssetSpec): File? {
        val dir = assetDir(spec.id)
        if (!dir.exists()) return null
        if (!spec.isZipArchive) {
            val file = File(dir, spec.fileName)
            return if (file.exists() && file.length() > 1024L) file else null
        }
        // Для Vosk-архивов внутри обычно одна вложенная папка (например vosk-model-small-ru-0.22)
        val children = dir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        val modelDir = children.firstOrNull() ?: dir
        return if (directorySizeBytes(modelDir) > 1024L) modelDir else null
    }

    fun isInstalled(spec: ModuleAssetSpec): Boolean = getInstalledModelPath(spec) != null

    fun totalInstalledBytes(): Long = directorySizeBytes(modulesRoot)

    fun startDownload(spec: ModuleAssetSpec) {
        if (activeJobs.containsKey(spec.id)) return
        val job = scope.launch {
            val tempZipOrBin = File(modulesRoot, "${spec.id}.download.tmp")
            val stageDir = File(modulesRoot, "${spec.id}.stage.tmp")
            try {
                updateState(spec.id, AssetDownloadState.Downloading(0f, 0L, spec.approxSizeMb * 1024L * 1024L))
                downloadWithRedirects(spec.downloadUrl, tempZipOrBin) { downloaded, total ->
                    val effectiveTotal = if (total > 0L) total else spec.approxSizeMb * 1024L * 1024L
                    val progress = (downloaded.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 0.99f)
                    updateState(spec.id, AssetDownloadState.Downloading(progress, downloaded, effectiveTotal))
                }

                if (spec.sha256.isNotBlank()) {
                    val actualHash = sha256Hex(tempZipOrBin)
                    if (!actualHash.equals(spec.sha256, ignoreCase = true)) {
                        throw IllegalStateException("SHA-256 mismatch")
                    }
                }

                stageDir.deleteRecursively()
                stageDir.mkdirs()

                if (spec.isZipArchive) {
                    updateState(spec.id, AssetDownloadState.Extracting)
                    unzipSafely(tempZipOrBin, stageDir)
                    tempZipOrBin.delete()
                } else {
                    val target = File(stageDir, spec.fileName)
                    if (!tempZipOrBin.renameTo(target)) {
                        tempZipOrBin.copyTo(target, overwrite = true)
                        tempZipOrBin.delete()
                    }
                }

                val finalDir = assetDir(spec.id)
                finalDir.deleteRecursively()
                if (!stageDir.renameTo(finalDir)) {
                    stageDir.copyRecursively(finalDir, overwrite = true)
                    stageDir.deleteRecursively()
                }

                val finalSize = directorySizeBytes(finalDir)
                updateState(spec.id, AssetDownloadState.Installed(finalSize))
            } catch (_: CancellationException) {
                tempZipOrBin.delete()
                stageDir.deleteRecursively()
                updateState(spec.id, AssetDownloadState.NotInstalled)
            } catch (t: Throwable) {
                tempZipOrBin.delete()
                stageDir.deleteRecursively()
                updateState(spec.id, AssetDownloadState.Error(t.message ?: "Download failed"))
            } finally {
                activeJobs.remove(spec.id)
            }
        }
        activeJobs[spec.id] = job
    }

    fun cancelDownload(assetId: String) {
        activeJobs.remove(assetId)?.cancel()
        updateState(assetId, AssetDownloadState.NotInstalled)
    }

    fun deleteAsset(assetId: String) {
        activeJobs.remove(assetId)?.cancel()
        assetDir(assetId).deleteRecursively()
        updateState(assetId, AssetDownloadState.NotInstalled)
    }

    fun clearAllAssets() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        modulesRoot.deleteRecursively()
        modulesRoot.mkdirs()
        refreshAllStates()
    }

    private fun updateState(assetId: String, state: AssetDownloadState) {
        _states.update { current -> current + (assetId to state) }
    }

    private suspend fun downloadWithRedirects(
        initialUrl: String,
        destFile: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var currentUrl = initialUrl
        var redirects = 0
        var conn: HttpURLConnection
        while (true) {
            conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "AnonGram-Android")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank() || redirects >= 6) {
                    throw IllegalStateException("Too many redirects ($code)")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
                redirects++
                continue
            }
            if (code !in 200..299) {
                conn.disconnect()
                throw IllegalStateException("HTTP $code")
            }
            break
        }

        try {
            val totalBytes = conn.contentLengthLong
            BufferedInputStream(conn.inputStream, 32 * 1024).use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var downloaded = 0L
                    var lastReportMs = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastReportMs >= 120L) {
                            lastReportMs = now
                            onProgress(downloaded, totalBytes)
                        }
                    }
                    output.flush()
                    onProgress(downloaded, totalBytes)
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun unzipSafely(zipFile: File, targetDir: File) {
        val canonicalTarget = targetDir.canonicalPath + File.separator
        ZipInputStream(BufferedInputStream(zipFile.inputStream(), 32 * 1024)).use { zis ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val entry = zis.nextEntry ?: break
                val outFile = File(targetDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalTarget)) {
                    throw SecurityException("Zip slip detected: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        while (true) {
                            val count = zis.read(buffer)
                            if (count == -1) break
                            fos.write(buffer, 0, count)
                        }
                    }
                }
                zis.closeEntry()
            }
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(32 * 1024)
            while (true) {
                val r = input.read(buf)
                if (r == -1) break
                digest.update(buf, 0, r)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun directorySizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    companion object {
        @Volatile
        private var instance: ExternalAssetManager? = null

        fun getInstance(context: Context): ExternalAssetManager =
            instance ?: synchronized(this) {
                instance ?: ExternalAssetManager(context.applicationContext).also { instance = it }
            }
    }
}
