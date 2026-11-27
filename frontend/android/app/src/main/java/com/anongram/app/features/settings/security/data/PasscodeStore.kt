package com.anongram.app.features.settings.security.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Безопасное хранилище код-пароля приложения.
 * Сам пароль в открытом виде не сохраняется: хранится только PBKDF2-HMAC-SHA256 хэш и случайная соль.
 */
object PasscodeStore {

    private const val PREFS_NAME = "anongram_security_settings"
    private const val KEY_PASSCODE_ENABLED = "key_passcode_enabled"
    private const val KEY_PASSCODE_HASH = "key_passcode_hash"
    private const val KEY_PASSCODE_SALT = "key_passcode_salt"
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256

    fun isPasscodeEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PASSCODE_ENABLED, false) &&
            !prefs.getString(KEY_PASSCODE_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PASSCODE_SALT, null).isNullOrBlank()
    }

    fun setPasscode(context: Context, rawPasscode: String) {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        val hash = deriveHash(rawPasscode.toCharArray(), salt)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PASSCODE_ENABLED, true)
            .putString(KEY_PASSCODE_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSCODE_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
        hash.fill(0)
        salt.fill(0)
    }

    fun verifyPasscode(context: Context, rawPasscode: String): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saltB64 = prefs.getString(KEY_PASSCODE_SALT, null) ?: return false
        val expectedHashB64 = prefs.getString(KEY_PASSCODE_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(expectedHashB64, Base64.NO_WRAP)
        val actual = deriveHash(rawPasscode.toCharArray(), salt)
        val matches = MessageDigest.isEqual(expected, actual)
        actual.fill(0)
        expected.fill(0)
        salt.fill(0)
        return matches
    }

    fun clearPasscode(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PASSCODE_ENABLED, false)
            .remove(KEY_PASSCODE_HASH)
            .remove(KEY_PASSCODE_SALT)
            .apply()
    }

    private fun deriveHash(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
