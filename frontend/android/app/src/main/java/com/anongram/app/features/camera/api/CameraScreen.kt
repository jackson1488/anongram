package com.anongram.app.features.camera.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anongram.app.features.camera.ui.CameraScreenContent

/**
 * Полноэкранный экран камеры CameraX (ТЗ, camera: CameraScreen(onResult, onClose)).
 * Тап: фото. Удержание: видео до 60 сек. Вспышка, переворот, тап-фокус.
 */
@Composable
fun CameraScreen(
    onResult: (CapturedMedia) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    CameraScreenContent(
        onResult = onResult,
        onClose = onClose,
        modifier = modifier
    )
}
