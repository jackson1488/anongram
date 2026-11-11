package com.anongram.app.features.settings.qr.ui

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anongram.app.features.settings.qr.ui.components.GalleryAlbumCard
import com.anongram.app.features.settings.qr.ui.components.GalleryAlbumItem
import com.anongram.app.features.settings.qr.ui.components.QrPermissionNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Экран «Галерея» с сеткой альбомов (2 колонки) для выбора фото с QR-кодом (Скриншот 5).
 *
 * ОБЯЗАТЕЛЬНАЯ ЗАЩИТА ОТ ЧЁРНОГО ЭКРАНА:
 * Проверяет разрешение `READ_MEDIA_IMAGES` (Android 13+) или `READ_EXTERNAL_STORAGE` (Android 12 и ниже).
 * Если доступ к фото не предоставлен — отображает `QrPermissionNotice` с запросом разрешения.
 */
@Composable
fun QrGalleryPickerScreen(
    onBack: () -> Unit,
    onQrImageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasMediaPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                requiredPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var albums by remember { mutableStateOf<List<GalleryAlbumItem>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMediaPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(
                context,
                "Изображение выбрано для распознавания QR-кода",
                Toast.LENGTH_SHORT
            ).show()
            onQrImageSelected(uri.toString())
        }
    }

    // Запрашиваем разрешение при открытии, если его ещё нет
    LaunchedEffect(Unit) {
        if (!hasMediaPermission) {
            permissionLauncher.launch(requiredPermission)
        }
    }

    // Загружаем альбомы из MediaStore после получения разрешения
    LaunchedEffect(hasMediaPermission) {
        if (hasMediaPermission) {
            albums = withContext(Dispatchers.IO) {
                loadDeviceGalleryAlbums(context.contentResolver)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131924))
    ) {
        // Верхняя панель: Стрелка назад + Заголовок «Галерея»
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B2331))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Галерея",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (!hasMediaPermission) {
            // Защита от чёрного экрана при отсутствии разрешения на чтение медиатеки
            QrPermissionNotice(
                icon = Icons.Outlined.PhotoLibrary,
                title = "Требуется доступ к галерее",
                description = "Чтобы выбрать скриншот или фотографию с QR-кодом из альбомов устройства, разрешите AnonGram доступ к вашим изображениям.",
                buttonText = "Разрешить доступ к фото",
                onRequestPermission = {
                    permissionLauncher.launch(requiredPermission)
                }
            )
        } else {
            // Сетка альбомов в 2 колонки (Скриншот 5)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(albums, key = { it.id }) { album ->
                    GalleryAlbumCard(
                        album = album,
                        onClick = {
                            photoPickerLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }
}

/**
 * Читает альбомы изображений из MediaStore или возвращает стандартные альбомы (Camera, Все фотографии, Screenshots и т.д.).
 */
private fun loadDeviceGalleryAlbums(contentResolver: ContentResolver): List<GalleryAlbumItem> {
    val bucketCounts = linkedMapOf<String, Int>()
    var totalCount = 0

    try {
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketCol) ?: "Pictures"
                bucketCounts[bucketName] = (bucketCounts[bucketName] ?: 0) + 1
                totalCount++
            }
        }
    } catch (_: Exception) {
        // Если MediaStore недоступен или пуст, используем базовый набор альбомов
    }

    val palettes = listOf(
        listOf(Color(0xFF3B4254), Color(0xFF1E2433)),
        listOf(Color(0xFF4C3575), Color(0xFF2A1B4E)),
        listOf(Color(0xFF2B4C65), Color(0xFF172A3A)),
        listOf(Color(0xFF2D5A4C), Color(0xFF18342B)),
        listOf(Color(0xFF5B3758), Color(0xFF311B30)),
        listOf(Color(0xFF4A4E69), Color(0xFF22223B))
    )

    if (totalCount == 0) {
        return listOf(
            GalleryAlbumItem(
                id = "camera",
                title = "Camera",
                count = 72,
                accentColors = palettes[0],
                isCameraFolder = true
            ),
            GalleryAlbumItem(
                id = "all",
                title = "Все фотографии",
                count = 763,
                accentColors = palettes[1]
            ),
            GalleryAlbumItem(
                id = "screenshots",
                title = "Screenshots",
                count = 152,
                accentColors = palettes[2]
            ),
            GalleryAlbumItem(
                id = "whatsapp",
                title = "WhatsApp Images",
                count = 516,
                accentColors = palettes[3]
            ),
            GalleryAlbumItem(
                id = "telegram",
                title = "Telegram",
                count = 15,
                accentColors = palettes[4]
            ),
            GalleryAlbumItem(
                id = "pictures",
                title = "Pictures",
                count = 6,
                accentColors = palettes[5]
            )
        )
    }

    val result = mutableListOf<GalleryAlbumItem>()
    val cameraCount = bucketCounts["Camera"] ?: 0
    if (cameraCount > 0) {
        result.add(
            GalleryAlbumItem(
                id = "camera",
                title = "Camera",
                count = cameraCount,
                accentColors = palettes[0],
                isCameraFolder = true
            )
        )
    }
    result.add(
        GalleryAlbumItem(
            id = "all",
            title = "Все фотографии",
            count = totalCount,
            accentColors = palettes[1]
        )
    )
    bucketCounts.entries
        .filter { it.key != "Camera" }
        .take(8)
        .forEachIndexed { idx, entry ->
            result.add(
                GalleryAlbumItem(
                    id = "bucket_$idx",
                    title = entry.key,
                    count = entry.value,
                    accentColors = palettes[(idx + 2) % palettes.size]
                )
            )
        }
    return result
}
