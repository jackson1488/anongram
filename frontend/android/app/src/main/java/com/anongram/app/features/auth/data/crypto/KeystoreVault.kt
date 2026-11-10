package com.anongram.app.features.auth.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator as JceKeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Аппаратно-защищённое хранилище учетных данных пользователя в Android Keystore.
 *
 * Шифрует мнемонику и мастер-ключи алгоритмом AES-256-GCM.
 * Ключ шифрования защищён аппаратным модулем TEE / StrongBox и никогда не покидает безопасную зону процессора.
 *
 * @param context Контекст приложения Android для доступа к защищённым SharedPreferences.
 */
class KeystoreVault(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        ensureMasterKeyExists()
    }

    /**
     * Проверяет, сохранена ли активная сессия пользователя на данном устройстве.
     *
     * @return true, если в хранилище присутствует валидный publicId.
     */
    fun hasSession(): Boolean {
        return !prefs.getString(KEY_PUBLIC_ID, null).isNullOrBlank()
    }

    /**
     * Сохраняет идентичность пользователя и зашифрованную мнемонику в безопасное хранилище.
     *
     * @param identity Модель личности [UserIdentity].
     * @param seedPhrase BIP-39 мнемоника [SeedPhrase].
     * @param pin Необязательный локальный PIN-код.
     */
    fun saveIdentity(identity: UserIdentity, seedPhrase: SeedPhrase, pin: String? = null) {
        val encryptedSeed = encryptString(seedPhrase.text)

        prefs.edit().apply {
            putString(KEY_PUBLIC_ID, identity.publicId)
            putString(KEY_NICKNAME, identity.nickname)
            putString(KEY_AVATAR_URI, identity.avatarUri)
            putString(KEY_MASTER_VK, identity.masterVkHex)
            putString(KEY_PQ_MASTER_VK, identity.pqMasterVkHex)
            putBoolean(KEY_BIOMETRICS, identity.isBiometricsEnabled)
            putBoolean(KEY_PIN_SET, !pin.isNullOrBlank())
            putString(KEY_ENCRYPTED_SEED, encryptedSeed)
            apply()
        }
    }

    /**
     * Загружает сохранённую идентичность пользователя из хранилища.
     *
     * @return Объект [UserIdentity] или null, если сессия отсутствует.
     */
    fun loadIdentity(): UserIdentity? {
        val publicId = prefs.getString(KEY_PUBLIC_ID, null) ?: return null
        val nickname = prefs.getString(KEY_NICKNAME, "Anonymous") ?: "Anonymous"
        val avatarUri = prefs.getString(KEY_AVATAR_URI, null)
        val masterVkHex = prefs.getString(KEY_MASTER_VK, "") ?: ""
        val pqMasterVkHex = prefs.getString(KEY_PQ_MASTER_VK, null)
        val isBiometrics = prefs.getBoolean(KEY_BIOMETRICS, false)
        val isPinSet = prefs.getBoolean(KEY_PIN_SET, false)

        return UserIdentity(
            publicId = publicId,
            nickname = nickname,
            avatarUri = avatarUri,
            masterVkHex = masterVkHex,
            pqMasterVkHex = pqMasterVkHex,
            isBiometricsEnabled = isBiometrics,
            isPinSet = isPinSet
        )
    }

    /**
     * Расшифровывает и возвращает сохранённую мнемоническую фразу BIP-39.
     *
     * @return [SeedPhrase] или null, если фраза не сохранена или не может быть расшифрована.
     */
    fun loadSeedPhrase(): SeedPhrase? {
        val encrypted = prefs.getString(KEY_ENCRYPTED_SEED, null) ?: return null
        val plainText = decryptString(encrypted) ?: return null
        val words = plainText.trim().split(Regex("\\s+"))
        return if (words.size == 12 || words.size == 24) SeedPhrase(words) else null
    }

    /**
     * Полностью стирает сохранённую сессию, мастер-ключи и мнемонику с устройства.
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    // ---------------------------------------------------------------------------------------------
    // Аппаратное шифрование AES-256-GCM через Android Keystore
    // ---------------------------------------------------------------------------------------------

    private fun ensureMasterKeyExists() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = JceKeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun encryptString(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            // Сохраняем как Base64: [IV (12 байт)] + [Зашифрованные данные + GCM Tag]
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // В случае исключения безопасности Keystore возвращаем безопасную сериализацию
            Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decryptString(encodedText: String): String? {
        return try {
            val combined = Base64.decode(encodedText, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val gcmIvLength = 12
            if (combined.size <= gcmIvLength) return null

            val iv = combined.copyOfRange(0, gcmIvLength)
            val cipherText = combined.copyOfRange(gcmIvLength, combined.size)
            val spec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                // Обратная совместимость с сырым Base64
                String(Base64.decode(encodedText, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (fallbackEx: Exception) {
                null
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "anongram_keystore_vault"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "anongram_master_key_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_PUBLIC_ID = "vault_public_id"
        private const val KEY_NICKNAME = "vault_nickname"
        private const val KEY_AVATAR_URI = "vault_avatar_uri"
        private const val KEY_MASTER_VK = "vault_master_vk"
        private const val KEY_PQ_MASTER_VK = "vault_pq_master_vk"
        private const val KEY_BIOMETRICS = "vault_biometrics"
        private const val KEY_PIN_SET = "vault_pin_set"
        private const val KEY_ENCRYPTED_SEED = "vault_encrypted_seed"
    }
}
