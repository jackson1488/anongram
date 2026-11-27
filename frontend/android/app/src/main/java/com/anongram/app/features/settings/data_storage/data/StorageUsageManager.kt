package com.anongram.app.features.settings.data_storage.data

import android.content.Context
import java.io.File

data class StorageStats(
    val cacheBytes: Long,
    val dataBytes: Long
) {
    val totalBytes: Long get() = cacheBytes + dataBytes
}

/**
 * Подсчёт реального размера кэша и локальных данных AnonGram на диске и их безопасная очистка.
 */
object StorageUsageManager {

    fun calculateUsage(context: Context): StorageStats {
        val appContext = context.applicationContext
        val cacheDirs = listOfNotNull(
            appContext.cacheDir,
            appContext.codeCacheDir,
            appContext.externalCacheDir
        ).distinctBy { it.absolutePath }

        val cacheBytes = cacheDirs.sumOf { dirSize(it) }
        val dataDir = appContext.filesDir?.parentFile
        val totalAppDataBytes = if (dataDir != null && dataDir.exists()) dirSize(dataDir) else 0L
        val dataBytes = (totalAppDataBytes - cacheBytes).coerceAtLeast(0L)
        return StorageStats(cacheBytes = cacheBytes, dataBytes = dataBytes)
    }

    fun clearCache(context: Context): StorageStats {
        val appContext = context.applicationContext
        listOfNotNull(
            appContext.cacheDir,
            appContext.codeCacheDir,
            appContext.externalCacheDir
        ).forEach { dir ->
            deleteChildren(dir)
        }
        return calculateUsage(appContext)
    }

    fun clearAllLocalData(context: Context): StorageStats {
        val appContext = context.applicationContext
        clearCache(appContext)
        appContext.filesDir?.let { deleteChildren(it) }
        appContext.getExternalFilesDir(null)?.let { deleteChildren(it) }
        val dataDir = appContext.filesDir?.parentFile
        File(dataDir, "databases").takeIf { it.exists() }?.let { deleteChildren(it) }
        File(dataDir, "shared_prefs").takeIf { it.exists() }?.let { prefsDir ->
            prefsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val prefName = file.nameWithoutExtension
                    appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .commit()
                }
            }
            deleteChildren(prefsDir)
        }
        return calculateUsage(appContext)
    }

    fun formatSize(bytes: Long, isRu: Boolean): String {
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(java.util.Locale.US, "%.2f %s", bytes / gb, if (isRu) "ГБ" else "GB")
            bytes >= mb -> String.format(java.util.Locale.US, "%.1f %s", bytes / mb, if (isRu) "МБ" else "MB")
            bytes >= kb -> String.format(java.util.Locale.US, "%.0f %s", bytes / kb, if (isRu) "КБ" else "KB")
            else -> "$bytes ${if (isRu) "Б" else "B"}"
        }
    }

    private fun dirSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        val children = file.listFiles() ?: return 0L
        var sum = 0L
        for (child in children) {
            sum += dirSize(child)
        }
        return sum
    }

    private fun deleteChildren(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteChildren(child)
            }
            child.delete()
        }
    }
}
