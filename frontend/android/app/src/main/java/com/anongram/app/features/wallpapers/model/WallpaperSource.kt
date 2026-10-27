package com.anongram.app.features.wallpapers.model

/**
 * Источник обоев для чата в стиле Telegram.
 */
sealed interface WallpaperSource {
    /**
     * Предустановленная тема (узорный фон + цвета пузырей + эмодзи-метка).
     */
    data class Theme(val themeId: String) : WallpaperSource

    /**
     * Пользовательское фото из приватного хранилища приложения.
     */
    data class Photo(val path: String) : WallpaperSource

    /**
     * Без темы: явно пустой фон (общие обои НЕ применяются).
     */
    data object None : WallpaperSource
}

/**
 * Разрешённые обои, готовые к отрисовке в UI чата.
 */
data class ResolvedWallpaper(
    val source: WallpaperSource,
    val dim: Int = 0, // Затемнение: 0..100
    val blur: Boolean = false, // Размытие фона
    val motion: Boolean = false // Параллакс-эффект движения при наклоне
) {
    val isNone: Boolean get() = source is WallpaperSource.None
    val themeId: String? get() = (source as? WallpaperSource.Theme)?.themeId
    val photoPath: String? get() = (source as? WallpaperSource.Photo)?.path

    companion object {
        /**
         * Дефолтные обои (фирменный крипто-меш).
         */
        val Default = ResolvedWallpaper(
            source = WallpaperSource.Theme("crypto_mesh"),
            dim = 0,
            blur = false,
            motion = false
        )

        /**
         * Пустые обои (без темы).
         */
        val Empty = ResolvedWallpaper(
            source = WallpaperSource.None,
            dim = 0,
            blur = false,
            motion = false
        )
    }
}
