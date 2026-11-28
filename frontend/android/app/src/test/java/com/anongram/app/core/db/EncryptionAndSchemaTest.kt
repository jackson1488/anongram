package com.anongram.app.core.db

import com.anongram.app.core.db.schema.auth.AuthContactsSchema
import com.anongram.app.core.db.schema.chats.ChatsTableSchema
import com.anongram.app.core.db.schema.chats.MessagesTableSchema
import com.anongram.app.core.db.schema.core.CoreKvSchema
import com.anongram.app.core.db.schema.media.MediaIndexSchema
import com.anongram.app.core.db.schema.profile.ProfileSchema
import com.anongram.app.features.chats.media.data.MediaVaultCategory
import com.anongram.app.features.drafts.ChatDraft
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionAndSchemaTest {

    @Test
    fun allModularSchemaIdsAreUnique() {
        val modules = listOf(
            CoreKvSchema,
            ChatsTableSchema,
            MessagesTableSchema,
            MediaIndexSchema,
            ProfileSchema,
            AuthContactsSchema
        )
        val ids = modules.map { it.moduleId }
        assertEquals(6, ids.toSet().size)
    }

    @Test
    fun mediaCategoriesCoverAllTypes() {
        val folders = MediaVaultCategory.entries.map { it.folderName }.toSet()
        assertEquals(
            setOf("voice", "music", "video_notes", "video", "photo", "files"),
            folders
        )
    }

    @Test
    fun aes256CtrMediaChunkedRoundtripZeroLossAndBenchmark() {
        val payloadSize = 2 * 1024 * 1024 // 2 MB photo/voice benchmark
        val original = ByteArray(payloadSize).also { SecureRandom().nextBytes(it) }
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)

        val t0 = System.nanoTime()
        val encryptedOut = ByteArrayOutputStream(payloadSize)
        val encCipher = Cipher.getInstance("AES/CTR/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        }
        CipherOutputStream(encryptedOut, encCipher).use { cos ->
            ByteArrayInputStream(original).copyTo(cos, bufferSize = 64 * 1024)
        }
        val encrypted = encryptedOut.toByteArray()
        val encMs = (System.nanoTime() - t0) / 1_000_000.0

        assertEquals(original.size, encrypted.size)
        assertFalse(original.contentEquals(encrypted))

        val t1 = System.nanoTime()
        val decCipher = Cipher.getInstance("AES/CTR/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        }
        val decrypted = CipherInputStream(ByteArrayInputStream(encrypted), decCipher).use { cis ->
            cis.readBytes()
        }
        val decMs = (System.nanoTime() - t1) / 1_000_000.0

        println("[AnonGram Benchmark] 2MB AES-256-CTR encrypt=${"%.2f".format(encMs)}ms, decrypt=${"%.2f".format(decMs)}ms")
        assertArrayEquals(original, decrypted)
    }

    @Test
    fun draftModelZeroLossFlags() {
        val draft = ChatDraft(
            chatId = "alice_01",
            text = "Привет, это тест",
            replyToMessageId = "msg_42",
            voiceUri = "media_enc/voice/alice_01/v1.enc",
            voiceDurationLabel = "0:12",
            voiceWaveform = listOf(10, 40, 80, 30)
        )
        assertFalse(draft.isEmpty)
        assertEquals("Привет, это тест", draft.previewText)
        assertTrue(ChatDraft(chatId = "empty").isEmpty)
    }

    @Test
    fun vaultUriExtractionAndDeletionRegex() {
        val fileId = "550e8400-e29b-41d4-a716-446655440000"
        val vaultUri = "${com.anongram.app.features.chats.media.data.MediaVaultManager.VAULT_URI_PREFIX}$fileId"
        assertEquals(fileId, com.anongram.app.features.chats.media.data.MediaVaultManager.extractFileId(vaultUri))
        assertEquals(null, com.anongram.app.features.chats.media.data.MediaVaultManager.extractFileId("content://media/external/images/1"))

        val payload = """{"attachment":{"type":"photo","uri":"$vaultUri"}}"""
        val regex = Regex("content://com\\.anongram\\.app\\.vault/([a-zA-Z0-9-]+)")
        val ids = regex.findAll(payload).map { it.groupValues[1] }.toList()
        assertEquals(listOf(fileId), ids)
    }
}
