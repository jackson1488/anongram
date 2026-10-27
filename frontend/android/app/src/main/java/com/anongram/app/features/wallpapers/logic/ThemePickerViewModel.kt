package com.anongram.app.features.wallpapers.logic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.wallpapers.data.WallpaperRepository
import com.anongram.app.features.wallpapers.model.ResolvedWallpaper
import com.anongram.app.features.wallpapers.model.ThemePreset
import com.anongram.app.features.wallpapers.model.Wallpaper
import com.anongram.app.features.wallpapers.model.WallpaperSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Состояние UI для экрана/окна выбора темы обоев.
 */
data class ThemePickerUiState(
    val selectedSource: WallpaperSource = WallpaperSource.Theme("crypto_mesh"),
    val isDarkVariant: Boolean = true,
    val isApplying: Boolean = false,
    val presets: List<ThemePreset> = ThemePreset.AllPresets
)

/**
 * ViewModel для выбора темы обоев с поддержкой живого превью за окном выбора.
 */
class ThemePickerViewModel(
    private val chatId: String,
    private val repository: WallpaperRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemePickerUiState())
    val uiState: StateFlow<ThemePickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val current = repository.getWallpaper(chatId) ?: repository.getWallpaper(null)
            val initialSource = current?.source ?: WallpaperSource.Theme("crypto_mesh")
            _uiState.update { it.copy(selectedSource = initialSource) }
        }
    }

    /**
     * Выбрать тему и сразу показать живое превью на фоне чата.
     */
    fun selectTheme(preset: ThemePreset) {
        val newSource = WallpaperSource.Theme(preset.id)
        _uiState.update { it.copy(selectedSource = newSource) }
        // Мгновенное живое превью для фона чата
        repository.setPreview(
            chatId = chatId,
            preview = ResolvedWallpaper(
                source = newSource,
                dim = 0,
                blur = false,
                motion = false
            )
        )
    }

    /**
     * Выбрать режим «Без темы» (явно пустой тёмный фон).
     */
    fun selectNone() {
        val newSource = WallpaperSource.None
        _uiState.update { it.copy(selectedSource = newSource) }
        repository.setPreview(
            chatId = chatId,
            preview = ResolvedWallpaper.Empty
        )
    }

    /**
     * Переключить превью светлого / тёмного варианта темы.
     */
    fun toggleVariant() {
        _uiState.update { it.copy(isDarkVariant = !it.isDarkVariant) }
    }

    /**
     * Применить выбранную тему и сохранить в постоянную БД.
     */
    fun applyTheme(onSuccess: () -> Unit) {
        val currentSource = _uiState.value.selectedSource
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true) }
            val wallpaper = Wallpaper(
                chatId = chatId,
                source = currentSource,
                dim = 0,
                blur = false,
                motion = false
            )
            repository.saveWallpaper(wallpaper)
            // Сбрасываем временное превью, так как теперь значение сохранено в постоянном состоянии
            repository.setPreview(chatId, null)
            _uiState.update { it.copy(isApplying = false) }
            onSuccess()
        }
    }

    /**
     * Сбросить живое превью при отмене/закрытии окна без сохранения.
     */
    fun onDismiss() {
        repository.setPreview(chatId, null)
    }

    override fun onCleared() {
        super.onCleared()
        repository.setPreview(chatId, null)
    }
}
