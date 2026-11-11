package com.anongram.app.features.settings.qr.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Модель альбома галереи для выбора изображения с QR-кодом.
 */
data class GalleryAlbumItem(
    val id: String,
    val title: String,
    val count: Int,
    val accentColors: List<Color>,
    val previewUri: Uri? = null,
    val isCameraFolder: Boolean = false
)

/**
 * Карточка альбома галереи в сетке 2xN (Скриншот 5).
 * Отображает превью альбома, название слева внизу и количество фото справа внизу.
 */
@Composable
fun GalleryAlbumCard(
    album: GalleryAlbumItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(album.accentColors)
            )
            .clickable(onClick = onClick)
    ) {
        // Декоративная иконка альбома по центру
        Icon(
            imageVector = when {
                album.isCameraFolder -> Icons.Outlined.PhotoCamera
                album.id == "all" -> Icons.Outlined.PhotoLibrary
                else -> Icons.Outlined.PhotoAlbum
            },
            contentDescription = album.title,
            tint = Color.White.copy(alpha = 0.28f),
            modifier = Modifier
                .size(58.dp)
                .align(Alignment.Center)
        )

        // Градиентное затемнение снизу для читаемости подписи и счётчика
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        // Нижняя строка: Название альбома и количество фотографий
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = album.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album.count.toString(),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
