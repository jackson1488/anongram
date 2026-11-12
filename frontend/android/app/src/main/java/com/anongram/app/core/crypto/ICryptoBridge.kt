package com.anongram.app.core.crypto

/**
 * Единственная точка контакта feature-модулей с криптографическим ядром (Rust FFI).
 *
 * Контракт ГЛАВНОГО ЗАКОНА МОДУЛЬНОСТИ: UI и ViewModel-слои никогда не обращаются
 * к JNI/NDK напрямую — только через этот интерфейс. Реализация поставляется мостом
 * к Rust-ядру (Double Ratchet, ML-KEM-1024, ChaCha20-Poly1305, физические OTP-пады);
 * до подключения NDK-моста feature-модули работают на локальных репозиториях-данных.
 */
interface ICryptoBridge {

    /** true, если нативное Rust-ядро загружено и инициализировано. */
    val isEngineReady: Boolean

    /** Версия Rust-ядра для экрана «О приложении» и проверки совместимости протокола. */
    val engineVersion: String

    /** Отпечаток открытого ключа собеседника в формате UI: "0x7F2A…9C". */
    suspend fun deriveFingerprint(publicKey: ByteArray): String

    /** Шифрование исходящего сообщения в защищённый конверт. */
    suspend fun encryptMessage(sessionId: String, plaintext: ByteArray): CipherEnvelope?

    /** Расшифровка входящего конверта; null при нарушении тега аутентичности Poly1305. */
    suspend fun decryptMessage(sessionId: String, envelope: CipherEnvelope): ByteArray?

    /**
     * Необратимое уничтожение ключей сессии (ratchet-цепочка + локальная история).
     * Возвращает true при успешном wipe — используется при burn-жесте OTP-чатов.
     */
    suspend fun wipeSessionKeys(sessionId: String): Boolean

    /** Удаление EXIF/GPS/метаданных изображения через Rust media-пайплайн. */
    suspend fun stripImageMetadata(data: ByteArray): ByteArray

    /** Зачистка метаданных, Zstd-сжатие и AEAD-упаковка медиа в бинарный контейнер. */
    suspend fun sealMedia(key: ByteArray, mediaType: Int, data: ByteArray): ByteArray?

    /** Расшифровка и распаковка медиа-контейнера. */
    suspend fun openMedia(key: ByteArray, sealed: ByteArray): ByteArray?
}

/** Защищённый конверт сообщения: шифротекст, одноразовый nonce, идентификатор алгоритма. */
data class CipherEnvelope(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val algorithm: String
)
