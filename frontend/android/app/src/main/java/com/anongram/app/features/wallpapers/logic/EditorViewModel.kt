package com.anongram.app.features.wallpapers.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.wallpapers.data.WallpaperRepository
import com.anongram.app.features.wallpapers.model.ResolvedWallpaper
import com.anongram.app.features.wallpapers.model.Wallpaper
import com.anongram.app.features.wallpapers.model.WallpaperSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Состояние редактора обоев.
 */
data class EditorUiState(
    val photoPath: String? = null,
    val dim: Int = 20, // 0..100
    val blur: Boolean = false,
    val motion: Boolean = false,
    val isSaving: Boolean = false
)

/**
 * ViewModel для экрана редактирования и настройки обоев (затемнение, размытие, параллакс).
 */
class EditorViewModel(
    private val chatId: String,
    private val repository: WallpaperRepository,
    initialPhotoPath: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditorUiState(
            photoPath = initialPhotoPath,
            dim = 20,
            blur = false,
            motion = false
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        updateLivePreview()
    }

    fun setPhotoPath(path: String) {
        _uiState.update { it.copy(photoPath = path) }
        updateLivePreview()
    }

    fun setDim(dim: Int) {
        val clamped = dim.coerceIn(0, 100)
        _uiState.update { it.copy(dim = clamped) }
        updateLivePreview()
    }

    fun toggleBlur() {
        _uiState.update { it.copy(blur = !it.blur) }
        updateLivePreview()
    }

    fun toggleMotion() {
        _uiState.update { it.copy(motion = !it.motion) }
        updateLivePreview()
    }

    private fun updateLivePreview() {
        val s = _uiState.value
        val path = s.photoPath ?: return
        repository.setPreview(
            chatId = chatId,
            preview = ResolvedWallpaper(
                source = WallpaperSource.Photo(path),
                dim = s.dim,
                blur = s.blur,
                motion = s.motion
            )
        )
    }

    /**
     * Сохранить настроенные обои для текущего чата в БД и применить.
     */
    fun applyWallpaper(onSuccess: () -> Unit) {
        val s = _uiState.value
        val path = s.photoPath ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val wallpaper = Wallpaper(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                source = WallpaperSource.Photo(path),
                dim = s.dim,
                blur = s.blur,
                motion = s.motion
            )
            repository.saveWallpaper(wallpaper)
            repository.setPreview(chatId, null) // Сбрасываем временное превью, теперь активны сохраненные обои
            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    fun cancelPreview() {
        repository.setPreview(chatId, null)
    }

    override fun onCleared() {
        super.onCleared()
        cancelPreview()
    }
}
