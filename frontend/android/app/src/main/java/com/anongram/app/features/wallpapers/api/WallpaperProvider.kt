package com.anongram.app.features.wallpapers.api

import com.anongram.app.features.wallpapers.model.ResolvedWallpaper
import kotlinx.coroutines.flow.Flow

/**
 * Публичный контракт модуля обоев с экраном чата.
 * Чат подписывается на поток обоев по chatId и может сбросить их к дефолту.
 */
interface WallpaperProvider {
    /**
     * Подписка на текущие разрешённые обои для указанного чата.
     * Применяет цепочку приоритетов: обои чата -> общие обои -> дефолт.
     * Если для чата выбрано «Без темы», общие обои НЕ подставляются.
     */
    fun observe(chatId: String): Flow<ResolvedWallpaper>

    /**
     * Сброс персональных обоев чата (возврат к общим или дефолтным).
     */
    suspend fun reset(chatId: String)
}
