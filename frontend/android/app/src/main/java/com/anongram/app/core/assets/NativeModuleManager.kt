package com.anongram.app.core.assets

import android.content.Context
import com.anongram.llama.LlamaNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Загрузчик опциональных нативных `.so` модулей из репозитория `jackson1488/anongram-assets`.
 * Ядро Rust (`libanongram_ffi.so`) всегда остаётся в APK; тяжёлые модули подгружаются по требованию.
 */
object NativeModuleManager {
    const val LLAMA_SO = "libanongram_llama.so"
    const val WHISPER_SO = "libanongram_whisper.so"
    const val VOSK_SO = "libvosk.so"
    const val TRANSLATE_KIT_SO = "libtranslate-kit.so"

    private const val BASE_URL = "https://raw.githubusercontent.com/jackson1488/anongram-assets/main/"

    fun libsDir(context: Context): File =
        File(context.applicationContext.filesDir, "native_libs/arm64-v8a").apply { mkdirs() }

    fun libFile(context: Context, soName: String): File = File(libsDir(context), soName)

    fun requiredSoForCategory(category: ModuleCategory): String = when (category) {
        ModuleCategory.VOICE_WHISPER -> WHISPER_SO
        ModuleCategory.VOICE_VOSK -> VOSK_SO
        ModuleCategory.TRANSLATION -> TRANSLATE_KIT_SO
    }

    fun isInstalled(context: Context, soName: String): Boolean {
        val f = libFile(context, soName)
        return f.exists() && f.length() > 100_000L
    }

    suspend fun ensureDownloaded(context: Context, soName: String): Boolean = withContext(Dispatchers.IO) {
        val dir = libsDir(context)
        val target = File(dir, soName)
        if (target.exists() && target.length() > 100_000L) return@withContext true

        val part = File(dir, "$soName.part")
        runCatching {
            val conn = (URL("$BASE_URL$soName").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 10_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "AnonGram/1.0")
            }
            try {
                if (conn.responseCode !in 200..299) return@withContext false
                conn.inputStream.use { input ->
                    part.outputStream().use { output -> input.copyTo(output) }
                }
                if (part.length() < 100_000L) {
                    part.delete()
                    return@withContext false
                }
                if (target.exists()) target.delete()
                part.renameTo(target)
            } finally {
                conn.disconnect()
            }
        }.getOrElse {
            part.delete()
            false
        }
    }

    fun loadLibrary(context: Context, soName: String, fallbackLibName: String): Boolean {
        val dir = libsDir(context)
        if (soName == VOSK_SO) {
            System.setProperty("jna.library.path", dir.absolutePath)
        }
        val external = File(dir, soName)
        if (external.exists() && external.length() > 100_000L) {
            return runCatching {
                System.load(external.absolutePath)
                true
            }.getOrDefault(false)
        }
        return runCatching {
            System.loadLibrary(fallbackLibName)
            true
        }.getOrDefault(false)
    }

    suspend fun ensureLlamaReady(context: Context): Boolean {
        if (LlamaNativeBridge.isAvailable) return true
        if (!ensureDownloaded(context, LLAMA_SO)) return false
        val path = File(libsDir(context), LLAMA_SO).absolutePath
        return LlamaNativeBridge.loadFromPath(path)
    }
}
