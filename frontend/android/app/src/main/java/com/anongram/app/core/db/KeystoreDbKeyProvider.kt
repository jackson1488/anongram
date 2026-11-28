package com.anongram.app.core.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Поставщик 256-битного ключа шифрования `anongram.db` через Android Keystore (AES-256-GCM).
 *
 * Используется по умолчанию, когда пользователь ещё не включил код-пароль,
 * чтобы база данных всегда оставалась зашифрованной на диске из коробки.
 */
object KeystoreDbKeyProvider {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "anongram_db_master_key"
    private const val PREFS_NAME = "anongram_security_settings"
    private const val PREF_WRAPPED_KEY = "key_db_wrapped_key"
    private const val PREF_WRAPPED_IV = "key_db_wrapped_iv"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getOrCreateKeystoreKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    @Synchronized
    fun getOrCreateKeyBytes(context: Context): ByteArray {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encB64 = prefs.getString(PREF_WRAPPED_KEY, null)
        val ivB64 = prefs.getString(PREF_WRAPPED_IV, null)
        val secretKey = getOrCreateKeystoreKey()

        if (!encB64.isNullOrEmpty() && !ivB64.isNullOrEmpty()) {
            val decrypted = runCatching {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val iv = Base64.decode(ivB64, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                cipher.doFinal(Base64.decode(encB64, Base64.NO_WRAP))
            }.getOrNull()
            if (decrypted != null && decrypted.size == 32) {
                return decrypted
            }
        }

        val rawKey = ByteArray(32)
        SecureRandom().nextBytes(rawKey)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val wrapped = cipher.doFinal(rawKey)
        prefs.edit()
            .putString(PREF_WRAPPED_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .putString(PREF_WRAPPED_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
        return rawKey
    }

    @Synchronized
    fun hasWrappedKey(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(PREF_WRAPPED_KEY, null).isNullOrEmpty() &&
            !prefs.getString(PREF_WRAPPED_IV, null).isNullOrEmpty()
    }

    @Synchronized
    fun clearKey(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(PREF_WRAPPED_KEY)
            .remove(PREF_WRAPPED_IV)
            .apply()
    }

    private const val PREF_BIO_WRAPPED_KEY = "key_bio_db_wrapped_key"
    private const val PREF_BIO_WRAPPED_IV = "key_bio_db_wrapped_iv"

    @Synchronized
    fun saveBioDbKey(context: Context, rawDbKey: ByteArray) {
        if (rawDbKey.isEmpty()) return
        runCatching {
            val secretKey = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val wrapped = cipher.doFinal(rawDbKey)
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_BIO_WRAPPED_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                .putString(PREF_BIO_WRAPPED_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        }
    }

    @Synchronized
    fun getBioDbKeyBytes(context: Context): ByteArray? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encB64 = prefs.getString(PREF_BIO_WRAPPED_KEY, null) ?: return null
        val ivB64 = prefs.getString(PREF_BIO_WRAPPED_IV, null) ?: return null
        return runCatching {
            val secretKey = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            cipher.doFinal(Base64.decode(encB64, Base64.NO_WRAP))
        }.getOrNull()
    }

    @Synchronized
    fun clearBioDbKey(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_BIO_WRAPPED_KEY)
            .remove(PREF_BIO_WRAPPED_IV)
            .apply()
    }
}
