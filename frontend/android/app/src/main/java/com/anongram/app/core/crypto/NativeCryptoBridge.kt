package com.anongram.app.core.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Реализация [ICryptoBridge] поверх нативного Rust-ядра (`libanongram_ffi.so`).
 * Загружается лениво при первом обращении; при отсутствии `.so` (например, в UI-превью)
 * безопасно переходит в режим `isEngineReady = false`.
 */
object NativeCryptoBridge : ICryptoBridge {

    private val loaded: Boolean = try {
        System.loadLibrary("anongram_ffi")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    private val sessionKeys = ConcurrentHashMap<String, ByteArray>()

    override val isEngineReady: Boolean
        get() = loaded

    override val engineVersion: String
        get() = if (loaded) {
            runCatching { nativeVersion() }.getOrElse { "0.1.0-fallback" }
        } else {
            "unavailable"
        }

    override suspend fun deriveFingerprint(publicKey: ByteArray): String =
        withContext(Dispatchers.Default) {
            if (loaded) {
                runCatching { nativeDeriveFingerprint(publicKey) }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: fallbackFingerprint(publicKey)
            } else {
                fallbackFingerprint(publicKey)
            }
        }

    override suspend fun encryptMessage(sessionId: String, plaintext: ByteArray): CipherEnvelope? =
        withContext(Dispatchers.Default) {
            if (!loaded) return@withContext null
            val key = sessionKeys[sessionId] ?: return@withContext null
            val aad = sessionId.toByteArray(Charsets.UTF_8)
            val sealed = nativeAeadSeal(key, aad, plaintext) ?: return@withContext null
            if (sealed.size < 24) return@withContext null
            val nonce = sealed.copyOfRange(0, 24)
            CipherEnvelope(
                ciphertext = sealed,
                nonce = nonce,
                algorithm = "XChaCha20-Poly1305"
            )
        }

    override suspend fun decryptMessage(sessionId: String, envelope: CipherEnvelope): ByteArray? =
        withContext(Dispatchers.Default) {
            if (!loaded) return@withContext null
            val key = sessionKeys[sessionId] ?: return@withContext null
            val aad = sessionId.toByteArray(Charsets.UTF_8)
            nativeAeadOpen(key, aad, envelope.ciphertext)
        }

    override suspend fun wipeSessionKeys(sessionId: String): Boolean =
        withContext(Dispatchers.Default) {
            sessionKeys.remove(sessionId)?.fill(0)
            true
        }

    override suspend fun stripImageMetadata(data: ByteArray): ByteArray =
        withContext(Dispatchers.Default) {
            if (!loaded || data.isEmpty()) return@withContext data
            runCatching { nativeStripImageMetadata(data) }.getOrNull() ?: data
        }

    override suspend fun sealMedia(key: ByteArray, mediaType: Int, data: ByteArray): ByteArray? =
        withContext(Dispatchers.Default) {
            if (!loaded || key.size != 32) return@withContext null
            runCatching { nativeSealMedia(key, mediaType, data) }.getOrNull()
        }

    override suspend fun openMedia(key: ByteArray, sealed: ByteArray): ByteArray? =
        withContext(Dispatchers.Default) {
            if (!loaded || key.size != 32) return@withContext null
            runCatching { nativeOpenMedia(key, sealed) }.getOrNull()
        }

    fun registerSessionKey(sessionId: String, key32: ByteArray) {
        if (key32.size == 32) {
            sessionKeys[sessionId] = key32.copyOf()
        }
    }

    private fun fallbackFingerprint(publicKey: ByteArray): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(publicKey)
        return String.format(
            "0x%02X%02X…%02X%02X",
            hash[0].toInt() and 0xFF,
            hash[1].toInt() and 0xFF,
            hash[30].toInt() and 0xFF,
            hash[31].toInt() and 0xFF
        )
    }

    private external fun nativeVersion(): String
    private external fun nativeDeriveFingerprint(publicKey: ByteArray): String
    private external fun nativeAeadSeal(key: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray?
    private external fun nativeAeadOpen(key: ByteArray, aad: ByteArray, cipher: ByteArray): ByteArray?
    private external fun nativeStripImageMetadata(data: ByteArray): ByteArray?
    private external fun nativeSealMedia(key: ByteArray, mediaType: Int, data: ByteArray): ByteArray?
    private external fun nativeOpenMedia(key: ByteArray, sealed: ByteArray): ByteArray?
}
