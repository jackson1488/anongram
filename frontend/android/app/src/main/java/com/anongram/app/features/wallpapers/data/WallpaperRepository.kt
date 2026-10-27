package com.anongram.app.features.wallpapers.data

import com.anongram.app.features.wallpapers.api.WallpaperProvider
import com.anongram.app.features.wallpapers.model.ResolvedWallpaper
import com.anongram.app.features.wallpapers.model.Wallpaper
import com.anongram.app.features.wallpapers.model.WallpaperSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Интерфейс репозитория обоев, реализующий публичный контракт [WallpaperProvider].
 */
interface WallpaperRepository : WallpaperProvider {
    /**
     * Получить сырую запись обоев по chatId (null = общие).
     */
    suspend fun getWallpaper(chatId: String?): Wallpaper?

    /**
     * Сохранить персональные обои для чата или общие обои.
     */
    suspend fun saveWallpaper(wallpaper: Wallpaper)

    /**
     * Получить разрешённые обои однократно (с учётом приоритетов).
     */
    suspend fun getResolved(chatId: String): ResolvedWallpaper

    /**
     * Установить временные обои для мгновенного живого превью за окном выбора (не сохраняя в БД).
     * Передача null сбрасывает живое превью к сохранённым обоям.
     */
    fun setPreview(chatId: String, preview: ResolvedWallpaper?)
}

/**
 * Локальная реализация репозитория обоев.
 * Обеспечивает строгие приоритеты:
 * 1. Обои конкретного чата.
 *    - Если выбрано «None» (Без темы) -> явно пустой фон, общие обои НЕ применяются.
 * 2. Общие обои приложения (chatId == null).
 * 3. Дефолтные обои (ResolvedWallpaper.Default).
 */
class LocalWallpaperRepository(
    private val storage: WallpaperStorage,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : WallpaperRepository {

    private val mutex = Mutex()
    // Карта кэша: chatId (или "__common__" для null) -> Wallpaper
    private val _wallpapersState = MutableStateFlow<Map<String, Wallpaper>>(emptyMap())
    // Карта живого превью для открытых экранов/диалогов выбора
    private val _previewState = MutableStateFlow<Map<String, ResolvedWallpaper>>(emptyMap())

    init {
        scope.launch {
            loadInitial()
        }
    }

    private suspend fun loadInitial() = mutex.withLock {
        val list = storage.getAll()
        val map = list.associateBy { it.chatId ?: KEY_COMMON }
        _wallpapersState.value = map
    }

    override fun setPreview(chatId: String, preview: ResolvedWallpaper?) {
        val next = _previewState.value.toMutableMap()
        if (preview == null) {
            next.remove(chatId)
        } else {
            next[chatId] = preview
        }
        _previewState.value = next
    }

    override fun observe(chatId: String): Flow<ResolvedWallpaper> {
        return kotlinx.coroutines.flow.combine(_wallpapersState, _previewState) { wallpapers, previews ->
            previews[chatId] ?: resolveFromMap(chatId, wallpapers)
        }
    }

    override suspend fun getResolved(chatId: String): ResolvedWallpaper {
        val preview = _previewState.value[chatId]
        if (preview != null) return preview
        val map = _wallpapersState.value
        return resolveFromMap(chatId, map)
    }

    override suspend fun getWallpaper(chatId: String?): Wallpaper? {
        val key = chatId ?: KEY_COMMON
        return _wallpapersState.value[key] ?: storage.getWallpaper(chatId)
    }

    override suspend fun saveWallpaper(wallpaper: Wallpaper) {
        mutex.withLock {
            storage.saveWallpaper(wallpaper)
            val key = wallpaper.chatId ?: KEY_COMMON
            val next = _wallpapersState.value.toMutableMap()
            next[key] = wallpaper
            _wallpapersState.value = next
        }
    }

    override suspend fun reset(chatId: String) {
        mutex.withLock {
            storage.deleteForChat(chatId)
            val next = _wallpapersState.value.toMutableMap()
            next.remove(chatId)
            _wallpapersState.value = next
        }
    }

    private fun resolveFromMap(chatId: String, map: Map<String, Wallpaper>): ResolvedWallpaper {
        // 1. Приоритет: Обои конкретного чата
        val chatWp = map[chatId]
        if (chatWp != null) {
            return when (chatWp.source) {
                is WallpaperSource.None -> ResolvedWallpaper.Empty // Явно пустой фон, общие не применять!
                else -> chatWp.toResolved()
            }
        }

        // 2. Приоритет: Общие обои (chatId = null)
        val commonWp = map[KEY_COMMON]
        if (commonWp != null) {
            return when (commonWp.source) {
                is WallpaperSource.None -> ResolvedWallpaper.Empty
                else -> commonWp.toResolved()
            }
        }

        // 3. Дефолт
        return ResolvedWallpaper.Default
    }

    companion object {
        private const val KEY_COMMON = "__common__"
    }
}
