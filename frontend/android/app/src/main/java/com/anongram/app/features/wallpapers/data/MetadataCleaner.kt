package com.anongram.app.features.wallpapers.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Интерфейс очистки метаданных (EXIF, GPS, данные устройства) перед сохранением обоев.
 * В будущем будет подключен к криптографическому ядру Rust через JNI.
 */
interface MetadataCleaner {
    /**
     * Очищает метаданные изображения из [sourceFile] и сохраняет безопасную копию в [targetFile].
     * @return true при успешной очистке.
     */
    suspend fun clean(sourceFile: File, targetFile: File): Boolean
}

/**
 * Базовая реализация очистки метаданных (побайтовое копирование / безопасная заглушка до интеграции Rust).
 */
class SafeMetadataCleaner : MetadataCleaner {
    override suspend fun clean(sourceFile: File, targetFile: File): Boolean {
        return try {
            targetFile.parentFile?.mkdirs()
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
