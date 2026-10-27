package com.anongram.app.features.wallpapers.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.wallpapers.data.WallpaperRepository
import com.anongram.app.features.wallpapers.data.WallpaperStorage
import com.anongram.app.features.wallpapers.model.ResolvedWallpaper
import com.anongram.app.features.wallpapers.model.WallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Описание готового кураторского фона для быстрого выбора.
 */
data class CuratedPhoto(
    val id: String,
    val title: String,
    val topColor: Long,
    val bottomColor: Long,
    val accentColor: Long
)

/**
 * Состояние экрана выбора фона из фото.
 */
data class PhotoPickerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val curatedWallpapers: List<CuratedPhoto> = listOf(
        CuratedPhoto("neon_cyan", "Кибер-Неон", 0xFF0D1B2A, 0xFF00E5FF, 0xFF7A4DF3),
        CuratedPhoto("deep_space", "Космос", 0xFF050510, 0xFF240046, 0xFF7B2CBF),
        CuratedPhoto("sunset_glow", "Кибер-закат", 0xFF1F1135, 0xFFFF5722, 0xFFFFC107),
        CuratedPhoto("emerald_matrix", "Изумруд", 0xFF00120B, 0xFF00E676, 0xFF1DE9B6),
        CuratedPhoto("violet_aura", "Аура", 0xFF140D2B, 0xFF7A4DF3, 0xFFE040FB),
        CuratedPhoto("obsidian_slate", "Сланец", 0xFF0B0E14, 0xFF2A324B, 0xFF6C757D)
    )
)

/**
 * ViewModel для экрана выбора фото/галереи.
 */
class PhotoPickerViewModel(
    private val chatId: String,
    private val repository: WallpaperRepository,
    private val storage: WallpaperStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoPickerUiState())
    val uiState: StateFlow<PhotoPickerUiState> = _uiState.asStateFlow()

    /**
     * Импортировать изображение из внешнего системного Picker Uri в приватную память приложения.
     */
    fun importFromUri(context: Context, uri: Uri, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val path = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Не удалось открыть изображение")
                    stream.use { s ->
                        storage.importPhoto(s)
                    }
                }
                repository.setPreview(chatId, ResolvedWallpaper(
                    source = WallpaperSource.Photo(path),
                    dim = 20,
                    blur = false,
                    motion = false
                ))
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(path)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Ошибка импорта фото") }
            }
        }
    }

    /**
     * Сгенерировать и импортировать кураторский фон.
     */
    fun selectCurated(photo: CuratedPhoto, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val path = withContext(Dispatchers.IO) {
                    val width = 1080
                    val height = 1920
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, 0f, height.toFloat(),
                            photo.topColor.toInt(),
                            photo.bottomColor.toInt(),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = photo.accentColor.toInt()
                        alpha = 80
                    }
                    canvas.drawCircle(width * 0.5f, height * 0.45f, width * 0.65f, glowPaint)

                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
                    val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                    storage.importPhoto(inputStream)
                }

                repository.setPreview(chatId, ResolvedWallpaper(
                    source = WallpaperSource.Photo(path),
                    dim = 20,
                    blur = false,
                    motion = false
                ))
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(path)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Ошибка создания обоев") }
            }
        }
    }
}
