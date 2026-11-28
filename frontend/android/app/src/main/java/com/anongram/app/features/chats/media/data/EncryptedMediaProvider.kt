package com.anongram.app.features.chats.media.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

/**
 * Приватный ContentProvider (`content://com.anongram.app.vault/<file_id>`),
 * который потоково расшифровывает медиа из [MediaVaultManager] через pipe в RAM
 * для просмотра в Coil/ExoPlayer, кнопки «Поделиться» и «Сохранить в галерею».
 */
class EncryptedMediaProvider : ContentProvider() {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String {
        val fileId = MediaVaultManager.extractFileId(uri.toString()) ?: return "application/octet-stream"
        return MediaVaultManager.getMimeType(fileId) ?: "application/octet-stream"
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val fileId = MediaVaultManager.extractFileId(uri.toString())
            ?: throw FileNotFoundException("Invalid vault URI: $uri")
        val stream = MediaVaultManager.openDecryptedStream(fileId)
            ?: throw FileNotFoundException("Encrypted media not found or DB locked: $fileId")

        val pipe = ParcelFileDescriptor.createReliablePipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        ioScope.launch {
            runCatching {
                stream.use { input ->
                    ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                }
            }.onFailure {
                runCatching { writeSide.closeWithError(it.message ?: "Decrypt error") }
            }
        }
        return readSide
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
