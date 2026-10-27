package com.anongram.app.features.wallpapers.api

import android.content.Context
import com.anongram.app.features.wallpapers.data.LocalWallpaperRepository
import com.anongram.app.features.wallpapers.data.WallpaperRepository
import com.anongram.app.features.wallpapers.data.WallpaperStorage

/**
 * Точка доступа к синглтону провайдера/репозитория обоев.
 */
object WallpaperModule {
    @Volatile
    private var storageInstance: WallpaperStorage? = null

    @Volatile
    private var repositoryInstance: WallpaperRepository? = null

    /**
     * Получить экземпляр [WallpaperStorage].
     */
    fun getStorage(context: Context): WallpaperStorage {
        return storageInstance ?: synchronized(this) {
            storageInstance ?: WallpaperStorage(context.applicationContext).also { storageInstance = it }
        }
    }

    /**
     * Получить экземпляр [WallpaperRepository].
     */
    fun getRepository(context: Context): WallpaperRepository {
        return repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: LocalWallpaperRepository(
                storage = getStorage(context)
            ).also { repositoryInstance = it }
        }
    }

    /**
     * Получить публичный интерфейс [WallpaperProvider] для чата.
     */
    fun getProvider(context: Context): WallpaperProvider = getRepository(context)
}
