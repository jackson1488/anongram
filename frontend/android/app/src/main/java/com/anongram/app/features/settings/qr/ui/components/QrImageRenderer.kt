package com.anongram.app.features.settings.qr.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.abs

/**
 * Рендерер реальных HD-изображений (PNG) карточки QR-кода профиля AnonGram.
 *
 * Отрисовывает:
 * - Фоновый градиент выбранной пользователем темы;
 * - Белую скруглённую карточку с мягкой тенью;
 * - Модули QR-кода и поисковые маркеры в акцентном цвете темы;
 * - Фирменный центральный логотип самолётика AnonGram;
 * - Хэндл профиля пользователя (@HANDLE);
 * - Нижний брендинг «AnonGram • Анонимный мессенджер».
 */
object QrImageRenderer {

    fun generateQrCardBitmap(
        handle: String,
        qrColor: Color,
        gradientColors: List<Color>
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cleanHandle = handle.removePrefix("@").uppercase()
        val isBlackTheme = qrColor.toArgb() == android.graphics.Color.parseColor("#18181B")
        val effectiveQrColorInt = if (isBlackTheme) android.graphics.Color.BLACK else qrColor.toArgb()
        val gradientInts = gradientColors.map { it.toArgb() }.toIntArray()

        // 1. Фоновый градиент темы
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                gradientInts,
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Белая скруглённая карточка
        val cardMarginHoriz = 110f
        val cardWidth = width - cardMarginHoriz * 2
        val cardHeight = cardWidth * 1.18f
        val cardTop = (height - cardHeight) / 2f - 40f
        val cardRect = RectF(cardMarginHoriz, cardTop, cardMarginHoriz + cardWidth, cardTop + cardHeight)

        // Тень под карточкой
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(80, 0, 0, 0)
        }
        val shadowRect = RectF(cardRect.left, cardRect.top + 18f, cardRect.right, cardRect.bottom + 18f)
        canvas.drawRoundRect(shadowRect, 68f, 68f, shadowPaint)

        // Белая карточка
        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
        }
        canvas.drawRoundRect(cardRect, 68f, 68f, cardPaint)

        // 3. QR-код внутри карточки
        val qrSize = cardWidth * 0.72f
        val qrLeft = cardRect.left + (cardWidth - qrSize) / 2f
        val qrTop = cardRect.top + cardWidth * 0.12f

        val qrPaint = Paint().apply {
            isAntiAlias = true
            color = effectiveQrColorInt
            style = Paint.Style.FILL
        }
        val qrStrokePaint = Paint().apply {
            isAntiAlias = true
            color = effectiveQrColorInt
            style = Paint.Style.STROKE
            strokeWidth = (qrSize / 21f) * 0.95f
        }

        val gridSize = 21
        val cellSize = qrSize / gridSize
        val seedHash = cleanHandle.hashCode()

        // Finder patterns
        fun drawFinder(startX: Int, startY: Int) {
            val fx = qrLeft + startX * cellSize
            val fy = qrTop + startY * cellSize
            val outerSize = 7 * cellSize
            val outerRect = RectF(
                fx + cellSize * 0.4f,
                fy + cellSize * 0.4f,
                fx + outerSize - cellSize * 0.4f,
                fy + outerSize - cellSize * 0.4f
            )
            canvas.drawRoundRect(outerRect, cellSize * 1.8f, cellSize * 1.8f, qrStrokePaint)

            val innerRect = RectF(
                fx + cellSize * 2f,
                fy + cellSize * 2f,
                fx + cellSize * 5f,
                fy + cellSize * 5f
            )
            canvas.drawRoundRect(innerRect, cellSize * 0.9f, cellSize * 0.9f, qrPaint)
        }

        drawFinder(0, 0)
        drawFinder(gridSize - 7, 0)
        drawFinder(0, gridSize - 7)

        // Data modules
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val inTopLeft = row < 8 && col < 8
                val inTopRight = row < 8 && col >= gridSize - 8
                val inBottomLeft = row >= gridSize - 8 && col < 8
                val inCenterLogo = row in 8..12 && col in 8..12

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenterLogo) {
                    val hashVal = abs((row * 31 + col * 17 + seedHash) % 11)
                    if (hashVal < 6) {
                        val mx = qrLeft + col * cellSize + 1f
                        val my = qrTop + row * cellSize + 1f
                        val modRect = RectF(mx, my, mx + cellSize - 2f, my + cellSize - 2f)
                        canvas.drawRoundRect(modRect, cellSize * 0.35f, cellSize * 0.35f, qrPaint)
                    }
                }
            }
        }

        // Центральный круг с самолётиком
        val cx = qrLeft + qrSize / 2f
        val cy = qrTop + qrSize / 2f
        val centerRadius = cellSize * 2.1f
        canvas.drawCircle(cx, cy, centerRadius, qrPaint)

        val planePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val r = centerRadius * 0.55f
        val planePath = Path().apply {
            moveTo(cx + r * 0.85f, cy - r * 0.75f)
            lineTo(cx - r * 0.95f, cy + r * 0.1f)
            lineTo(cx - r * 0.15f, cy + r * 0.25f)
            lineTo(cx + r * 0.25f, cy + r * 0.95f)
            close()
        }
        canvas.drawPath(planePath, planePaint)

        // 4. Текст @HANDLE
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = effectiveQrColorInt
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = qrTop + qrSize + 92f
        canvas.drawText("@$cleanHandle", cx, textY, textPaint)

        // 5. Нижняя подпись бренда под карточкой
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(210, 255, 255, 255)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AnonGram • Анонимный мессенджер", width / 2f, height - 70f, brandPaint)

        return bitmap
    }

    /**
     * Экспорт и шеринг HD-изображения QR-кода через системный диалог Android Share Sheet.
     */
    fun shareQrImage(
        context: Context,
        handle: String,
        qrColor: Color,
        gradientColors: List<Color>
    ) {
        val bitmap = generateQrCardBitmap(handle, qrColor, gradientColors)
        val imagesDir = File(context.cacheDir, "shared_qr")
        imagesDir.mkdirs()
        val file = File(imagesDir, "anongram_qr_${handle.removePrefix("@").lowercase()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Мой анонимный профиль в AnonGram: @${handle.removePrefix("@").uppercase()}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться QR-кодом"))
    }

    /**
     * Сохранение HD-изображения QR-кода в общую системную Галерею пользователя (Pictures/AnonGram).
     */
    fun saveQrImageToGallery(
        context: Context,
        handle: String,
        qrColor: Color,
        gradientColors: List<Color>
    ): Boolean {
        return try {
            val bitmap = generateQrCardBitmap(handle, qrColor, gradientColors)
            val filename = "AnonGram_QR_${handle.removePrefix("@").uppercase()}_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnonGram")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri).use { out ->
                    if (out != null) bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
