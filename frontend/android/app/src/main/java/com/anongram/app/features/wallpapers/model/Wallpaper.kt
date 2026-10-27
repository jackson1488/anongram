package com.anongram.app.features.wallpapers.model

import java.util.UUID

/**
 * Сущность сохранённых обоев.
 *
 * @param id Уникальный идентификатор записи.
 * @param chatId Идентификатор конкретного чата (null = общие обои приложения).
 * @param source Источник фона (тема, пользовательское фото или явное отсутствие темы).
 * @param dim Уровень затемнения фона от 0 до 100%.
 * @param blur Включено ли размытие фона.
 * @param motion Включён ли параллакс при движении/наклоне устройства.
 */
data class Wallpaper(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String? = null,
    val source: WallpaperSource = WallpaperSource.Theme("crypto_mesh"),
    val dim: Int = 0,
    val blur: Boolean = false,
    val motion: Boolean = false
) {
    init {
        require(dim in 0..100) { "Уровень затемнения (dim) должен быть в диапазоне 0..100, получено: $dim" }
    }

    /**
     * Конвертация в ResolvedWallpaper для отображения.
     */
    fun toResolved(): ResolvedWallpaper = ResolvedWallpaper(
        source = source,
        dim = dim,
        blur = blur,
        motion = motion
    )
}
