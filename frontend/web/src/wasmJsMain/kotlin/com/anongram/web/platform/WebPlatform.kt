package com.anongram.web.platform

import androidx.compose.runtime.Composable
import kotlinx.browser.window

/**
 * Простая обёртка над localStorage браузера — замена SharedPreferences.
 */
object WebStorage {
    fun getString(key: String, default: String? = null): String? =
        try {
            window.localStorage.getItem(key) ?: default
        } catch (_: Throwable) {
            default
        }

    fun putString(key: String, value: String) {
        try {
            window.localStorage.setItem(key, value)
        } catch (_: Throwable) {
        }
    }

    fun getInt(key: String, default: Int): Int =
        getString(key)?.toIntOrNull() ?: default

    fun putInt(key: String, value: Int) =
        putString(key, value.toString())

    fun getBoolean(key: String, default: Boolean): Boolean =
        when (getString(key)) {
            "true" -> true
            "false" -> false
            else -> default
        }

    fun putBoolean(key: String, value: Boolean) =
        putString(key, value.toString())

    fun remove(key: String) {
        try {
            window.localStorage.removeItem(key)
        } catch (_: Throwable) {
        }
    }
}

/** Текущее время в миллисекундах (замена System.currentTimeMillis() для Wasm). */
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

fun currentTimeMillis(): Long = jsDateNow().toLong()

/** Веб-заглушка для BackHandler (в браузере назад обрабатывается кнопками UI). */
@Composable
fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    // В вебе навигация назад управляется кнопками в UI
}
