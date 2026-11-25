package com.anongram.app.features.wallpapers.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.wallpapers.api.WallpaperModule
import com.anongram.app.features.wallpapers.model.ResolvedWallpaper
import com.anongram.app.features.wallpapers.model.ThemePreset
import com.anongram.app.features.wallpapers.model.WallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Глобальный кэш декодированных Bitmap для предотвращения лагов и повторных чтений с диска при скролле.
 */
private object WallpaperBitmapCache {
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun get(key: String): ImageBitmap? = cache[key]

    fun put(key: String, bitmap: ImageBitmap) {
        if (cache.size > 20) cache.clear()
        cache[key] = bitmap
    }
}

/**
 * Фоновый слой обоев для экрана чата.
 * Размещается непосредственно ПОД списком сообщений.
 *
 * @param chatId Идентификатор сессии чата.
 * @param modifier Модификатор размера и расположения.
 * @param previewWallpaper Опциональные обои для мгновенного живого превью в пикере/редакторе.
 */
@Composable
fun WallpaperBackground(
    chatId: String,
    modifier: Modifier = Modifier,
    previewWallpaper: ResolvedWallpaper? = null
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val provider = remember(context) { WallpaperModule.getProvider(context) }

    // Если передан previewWallpaper (например, в окне выбора темы), используем его для живого превью
    val resolvedState = remember(provider, chatId) { provider.observe(chatId) }
        .collectAsState(initial = ResolvedWallpaper.Default)
    val resolved = previewWallpaper ?: resolvedState.value

    // Параллакс-движение при наклоне устройства
    val (tiltX, tiltY) = rememberAccelerometerTilt(enabled = resolved.motion)
    val animatedTiltX by animateFloatAsState(targetValue = tiltX, animationSpec = tween(150), label = "tiltX")
    val animatedTiltY by animateFloatAsState(targetValue = tiltY, animationSpec = tween(150), label = "tiltY")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        val motionModifier = if (resolved.motion) {
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = animatedTiltX
                    translationY = animatedTiltY
                    scaleX = 1.08f
                    scaleY = 1.08f
                }
        } else {
            Modifier.fillMaxSize()
        }

        when (val source = resolved.source) {
            is WallpaperSource.None -> {
                // Чистый пустой фон текущей темы
                Box(modifier = Modifier.fillMaxSize().background(scheme.background))
            }

            is WallpaperSource.Theme -> {
                val preset = ThemePreset.find(source.themeId)
                val themeModifier = if (resolved.blur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    motionModifier.blur(18.dp)
                } else {
                    motionModifier
                }
                ThemePatternCanvas(
                    preset = preset,
                    modifier = themeModifier
                )
            }

            is WallpaperSource.Photo -> {
                PhotoWallpaper(
                    path = source.path,
                    blur = resolved.blur,
                    modifier = motionModifier
                )
            }
        }

        // Слой затемнения (dim 0..100)
        if (resolved.dim > 0) {
            val dimAlpha = (resolved.dim.coerceIn(0, 100) / 100f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }
    }
}

/**
 * Отрисовка фото с кэшированием и поддержкой блюра.
 */
@Composable
private fun PhotoWallpaper(
    path: String,
    blur: Boolean,
    modifier: Modifier = Modifier
) {
    var imageBitmap by remember(path) {
        mutableStateOf(WallpaperBitmapCache.get(path))
    }

    LaunchedEffect(path) {
        if (imageBitmap == null && path.isNotEmpty()) {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    val file = File(path)
                    if (file.exists()) {
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
                        bmp?.asImageBitmap()
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            if (loaded != null) {
                WallpaperBitmapCache.put(path, loaded)
                imageBitmap = loaded
            }
        }
    }

    val finalBmp = imageBitmap
    if (finalBmp != null) {
        val blurModifier = if (blur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modifier.blur(20.dp)
        } else {
            modifier
        }

        Image(
            bitmap = finalBmp,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = blurModifier
        )
    } else {
        // Запасной фон до завершения загрузки фото
        val scheme = LocalAnonGramScheme.current
        Box(modifier = modifier.background(scheme.background))
    }
}

/**
 * Отрисовка узоров предустановленных тем на Canvas.
 */
@Composable
fun ThemePatternCanvas(
    preset: ThemePreset,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val dotColor = if (scheme.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
    val gradientStops = when {
        preset.adaptsToTheme && scheme.isRainbow -> {
            com.anongram.app.core.theme.AnonGramThemes.rainbowColors.map {
                it.copy(alpha = if (scheme.isDark) 0.42f else 0.34f)
            }
        }
        preset.adaptsToTheme -> listOf(
            scheme.accent.copy(alpha = if (scheme.isDark) 0.36f else 0.24f),
            scheme.accentDim.copy(alpha = if (scheme.isDark) 0.32f else 0.20f),
            scheme.background
        )
        else -> listOf(
            preset.primaryColor.copy(alpha = 0.92f),
            preset.secondaryColor.copy(alpha = 0.88f),
            preset.tertiaryColor.copy(alpha = 0.86f)
        )
    }
    val overlayColor = if (scheme.isDark) Color(0x3806090E) else Color(0x38FFFFFF)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = scheme.background)
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = gradientStops,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
        drawRect(color = overlayColor)
    }
}

/**
 * Чтение акселерометра для параллакс-эффекта (наклон устройства смещает фон).
 */
@Composable
private fun rememberAccelerometerTilt(enabled: Boolean): Pair<Float, Float> {
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    DisposableEffect(enabled) {
        if (!enabled) {
            tiltX = 0f
            tiltY = 0f
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                // event.values[0] = X (наклон влево-вправо), event.values[1] = Y (наклон вперед-назад)
                val rawX = event.values[0]
                val rawY = event.values[1]
                // Ограничиваем диапазон и плавно масштабируем до +/- 24px
                tiltX = (-rawX * 4f).coerceIn(-24f, 24f)
                tiltY = (rawY * 4f).coerceIn(-24f, 24f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return tiltX to tiltY
}
