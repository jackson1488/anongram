package com.anongram.app.features.attach.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Вкладки окна вложений (ТЗ, шаг 1):
 * Галерея, Файл, Геопозиция, Контакт, Музыка.
 */
enum class AttachTab(val title: String, val icon: ImageVector) {
    GALLERY("Галерея", Icons.Outlined.Image),
    FILE("Файл", Icons.Outlined.Description),
    LOCATION("Геопозиция", Icons.Outlined.LocationOn),
    CONTACT("Контакт", Icons.Outlined.Person),
    MUSIC("Музыка", Icons.Outlined.Audiotrack)
}
