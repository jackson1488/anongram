package com.anongram.app.features.settings.security.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PasscodeType {
    PIN,
    PASSWORD
}

/**
 * Безопасное хранилище код-пароля, его типа и персистентной защиты от перебора (brute-force).
 * Сам пароль в открытом виде не сохраняется: хранится только PBKDF2-HMAC-SHA256 хэш и случайная соль.
 */
object PasscodeStore {

    private const val PREFS_NAME = "anongram_security_settings"
    private const val KEY_PASSCODE_ENABLED = "key_passcode_enabled"
    private const val KEY_PASSCODE_HASH = "key_passcode_hash"
    private const val KEY_PASSCODE_SALT = "key_passcode_salt"
    private const val KEY_PASSCODE_TYPE = "key_passcode_type"
    private const val KEY_PASSCODE_LENGTH = "key_passcode_length"
    private const val KEY_PASSCODE_ITERATIONS = "key_passcode_iterations"
    private const val KEY_DB_SALT = "key_db_salt"

    private const val KEY_BRUTEFORCE_ENABLED = "key_bruteforce_enabled"
    private const val KEY_BRUTEFORCE_MAX_ATTEMPTS = "key_bruteforce_max_attempts"
    private const val KEY_BRUTEFORCE_BASE_SEC = "key_bruteforce_base_sec"
    private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL_MS = "key_lockout_until_ms"

    private const val VERIFY_ITERATIONS = 15_000
    private const val LEGACY_ITERATIONS = 100_000
    private const val DB_KEY_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256

    fun isPasscodeEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PASSCODE_ENABLED, false) &&
            !prefs.getString(KEY_PASSCODE_HASH, null).isNullOrBlank() &&
            !prefs.getString(KEY_PASSCODE_SALT, null).isNullOrBlank()
    }

    fun getPasscodeType(context: Context): PasscodeType {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PASSCODE_TYPE, PasscodeType.PIN.name)
        return runCatching { PasscodeType.valueOf(raw ?: PasscodeType.PIN.name) }
            .getOrDefault(PasscodeType.PIN)
    }

    fun getPasscodeLength(context: Context): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_PASSCODE_LENGTH, 0)
    }

    fun isBruteForceEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BRUTEFORCE_ENABLED, false)
    }

    fun setBruteForceEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BRUTEFORCE_ENABLED, enabled)
            .apply()
        if (!enabled) {
            resetFailedAttempts(context)
        }
    }

    fun getBruteForceMaxAttempts(context: Context): Int {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BRUTEFORCE_MAX_ATTEMPTS, 5)
            .coerceIn(1, 10)
    }

    fun setBruteForceMaxAttempts(context: Context, attempts: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BRUTEFORCE_MAX_ATTEMPTS, attempts.coerceIn(1, 10))
            .apply()
    }

    fun getBruteForceBaseSec(context: Context): Int {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BRUTEFORCE_BASE_SEC, 30)
            .coerceIn(5, 86_400)
    }

    fun setBruteForceBaseSec(context: Context, sec: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BRUTEFORCE_BASE_SEC, sec.coerceIn(5, 86_400))
            .apply()
    }

    fun getLockoutRemainingSeconds(context: Context): Long {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        val diff = until - System.currentTimeMillis()
        return if (diff > 0L) (diff + 999L) / 1000L else 0L
    }

    fun recordFailedAttempt(context: Context): Long {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val maxAttempts = getBruteForceMaxAttempts(context)
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
        var lockSeconds = 0L
        if (attempts >= maxAttempts && ((attempts - maxAttempts) % maxAttempts == 0 || maxAttempts == 1)) {
            val stage = ((attempts - maxAttempts) / maxAttempts).coerceIn(0, 6)
            val baseSec = getBruteForceBaseSec(context).toLong()
            lockSeconds = (baseSec shl stage).coerceAtMost(86_400L)
            val until = System.currentTimeMillis() + lockSeconds * 1000L
            editor.putLong(KEY_LOCKOUT_UNTIL_MS, until)
        }
        editor.apply()
        return lockSeconds
    }

    fun resetFailedAttempts(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL_MS)
            .apply()
    }

    fun setPasscode(
        context: Context,
        rawPasscode: String,
        type: PasscodeType = PasscodeType.PIN
    ) {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        val hash = deriveHash(rawPasscode.toCharArray(), salt, VERIFY_ITERATIONS)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PASSCODE_ENABLED, true)
            .putString(KEY_PASSCODE_TYPE, type.name)
            .putInt(KEY_PASSCODE_LENGTH, rawPasscode.length)
            .putInt(KEY_PASSCODE_ITERATIONS, VERIFY_ITERATIONS)
            .putString(KEY_PASSCODE_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSCODE_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL_MS)
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
        val storedIterations = prefs.getInt(KEY_PASSCODE_ITERATIONS, -1)

        val chars = rawPasscode.toCharArray()
        return try {
            val matched = if (storedIterations > 0) {
                val actual = deriveHash(chars, salt, storedIterations)
                val matches = MessageDigest.isEqual(expected, actual)
                actual.fill(0)
                matches
            } else {
                val fastActual = deriveHash(chars, salt, VERIFY_ITERATIONS)
                val fastMatch = MessageDigest.isEqual(expected, fastActual)
                fastActual.fill(0)
                if (fastMatch) {
                    prefs.edit()
                        .putInt(KEY_PASSCODE_ITERATIONS, VERIFY_ITERATIONS)
                        .putInt(KEY_PASSCODE_LENGTH, rawPasscode.length)
                        .apply()
                    true
                } else {
                    val legacyActual = deriveHash(chars, salt, LEGACY_ITERATIONS)
                    val legacyMatch = MessageDigest.isEqual(expected, legacyActual)
                    legacyActual.fill(0)
                    if (legacyMatch) {
                        setPasscode(context, rawPasscode, getPasscodeType(context))
                    }
                    legacyMatch
                }
            }
            if (matched && prefs.getInt(KEY_PASSCODE_LENGTH, 0) == 0) {
                prefs.edit().putInt(KEY_PASSCODE_LENGTH, rawPasscode.length).apply()
            }
            matched
        } finally {
            chars.fill('\u0000')
            expected.fill(0)
            salt.fill(0)
        }
    }

    fun deriveDbKeyBytes(context: Context, rawPasscode: String): ByteArray {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingSaltB64 = prefs.getString(KEY_DB_SALT, null)
        val dbSalt = if (existingSaltB64.isNullOrBlank()) {
            ByteArray(32).also {
                SecureRandom().nextBytes(it)
                prefs.edit().putString(KEY_DB_SALT, Base64.encodeToString(it, Base64.NO_WRAP)).apply()
            }
        } else {
            Base64.decode(existingSaltB64, Base64.NO_WRAP)
        }
        val key = deriveHash(rawPasscode.toCharArray(), dbSalt, DB_KEY_ITERATIONS)
        dbSalt.fill(0)
        return key
    }

    fun clearPasscode(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PASSCODE_ENABLED, false)
            .remove(KEY_PASSCODE_HASH)
            .remove(KEY_PASSCODE_SALT)
            .remove(KEY_PASSCODE_TYPE)
            .remove(KEY_PASSCODE_LENGTH)
            .remove(KEY_PASSCODE_ITERATIONS)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL_MS)
            .apply()
    }

    private fun deriveHash(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
