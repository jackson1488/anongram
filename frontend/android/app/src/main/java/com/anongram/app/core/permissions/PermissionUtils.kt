package com.anongram.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Проверка и утилиты системных разрешений (ТЗ, шаг 2).
 * Запрашивать только при открытии нужной вкладки.
 * MANAGE_EXTERNAL_STORAGE строго не используется.
 */
object PermissionUtils {

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // --- Галерея ---

    fun getGalleryPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasGalleryPermission(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ||
                hasPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO)

        else -> hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** Частичный доступ (Android 14+): плашка «Доступ ограничен, выбрать ещё» */
    fun isGalleryPartialAccess(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            hasPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            !hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)

    // --- Музыка ---

    fun getMusicPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasMusicPermission(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            hasPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
        else -> hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // --- Контакты ---

    fun getContactsPermissions(): Array<String> = arrayOf(Manifest.permission.READ_CONTACTS)

    fun hasContactsPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_CONTACTS)

    // --- Камера и Микрофон ---

    fun getCameraPermissions(): Array<String> = arrayOf(Manifest.permission.CAMERA)

    fun hasCameraPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.CAMERA)

    fun getRecordAudioPermissions(): Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    fun hasRecordAudioPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.RECORD_AUDIO)

    // --- Геопозиция ---

    fun getLocationPermissions(): Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasLocationPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    // --- Переход в настройки при отказе ---

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
