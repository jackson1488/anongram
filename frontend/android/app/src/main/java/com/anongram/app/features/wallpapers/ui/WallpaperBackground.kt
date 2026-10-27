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
import com.anongram.app.core.theme.ObsidianBlack
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
            .background(ObsidianBlack)
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
                // Чистый пустой тёмный фон
                Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack))
            }

            is WallpaperSource.Theme -> {
                val preset = ThemePreset.find(source.themeId)
                ThemePatternCanvas(
                    preset = preset,
                    modifier = motionModifier
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
        Box(modifier = modifier.background(ObsidianBlack))
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
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = ObsidianBlack)

        when (preset.patternStyle) {
            0 -> {
                // Точечная сотовая сетка + диагональный крипто-mesh
                val gridStep = 28.dp.toPx()
                val rowStep = gridStep * 0.86f
                val dotRadius = 1.2.dp.toPx()
                val dotColor = Color(0xFF141923)

                var row = 0
                var y = 0f
                while (y <= size.height + rowStep) {
                    val offset = if (row % 2 == 0) 0f else gridStep / 2f
                    var x = offset
                    while (x <= size.width + gridStep) {
                        drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                        x += gridStep
                    }
                    y += rowStep
                    row += 1
                }

                val meshColor = preset.primaryColor.copy(alpha = 0.06f)
                val stroke = 1.dp.toPx()
                val meshStep = 96.dp.toPx()

                var d = -size.height
                while (d <= size.width) {
                    drawLine(
                        color = meshColor,
                        start = Offset(d, 0f),
                        end = Offset(d + size.height, size.height),
                        strokeWidth = stroke
                    )
                    d += meshStep
                }
                var u = 0f
                while (u <= size.width + size.height) {
                    drawLine(
                        color = preset.secondaryColor.copy(alpha = 0.04f),
                        start = Offset(u, 0f),
                        end = Offset(u - size.height, size.height),
                        strokeWidth = stroke
                    )
                    u += meshStep
                }
            }

            1 -> {
                // Гексагональные узлы и связи
                val step = 44.dp.toPx()
                val nodeColor = preset.primaryColor.copy(alpha = 0.09f)
                val lineColor = preset.secondaryColor.copy(alpha = 0.04f)

                var y = 0f
                var row = 0
                while (y <= size.height + step) {
                    val offsetX = if (row % 2 == 0) 0f else step / 2f
                    var x = offsetX
                    while (x <= size.width + step) {
                        drawCircle(color = nodeColor, radius = 2.dp.toPx(), center = Offset(x, y))
                        drawLine(
                            color = lineColor,
                            start = Offset(x, y),
                            end = Offset(x + step, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        x += step
                    }
                    y += step * 0.86f
                    row += 1
                }
            }

            2 -> {
                // Вертикальные потоковые линии (космос/кибер-закат)
                val lineStep = 32.dp.toPx()
                val stroke = 1.2.dp.toPx()
                var x = 0f
                var i = 0
                while (x <= size.width) {
                    val alpha = if (i % 2 == 0) 0.06f else 0.03f
                    val col = if (i % 3 == 0) preset.primaryColor else preset.secondaryColor
                    drawLine(
                        color = col.copy(alpha = alpha),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = stroke
                    )
                    x += lineStep
                    i++
                }
            }
        }
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
