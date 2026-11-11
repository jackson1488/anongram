package com.anongram.app.features.settings.qr.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.anongram.app.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File

/**
 * Рендерер реальных HD-изображений (PNG) карточки QR-кода профиля AnonGram.
 *
 * Отрисовывает:
 * - Фоновый градиент выбранной пользователем темы;
 * - Белую скруглённую карточку с мягкой тенью;
 * - Модули и маркеры РЕАЛЬНОГО QR-кода (ZXing ErrorCorrectionLevel.H);
 * - Фирменную центральную Стелс-иконку приложения (Cryptographic Shield + Stealth Plane);
 * - Хэндл профиля пользователя (@HANDLE);
 * - Нижний брендинг «AnonGram • Анонимный мессенджер».
 */
object QrImageRenderer {

    fun generateQrCardBitmap(
        context: Context,
        handle: String,
        qrColor: Color,
        gradientColors: List<Color>,
        qrPayload: String = "anongram://user?handle=${handle.removePrefix("@").uppercase()}"
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
        val cardMarginHoriz = 105f
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

        // 3. Реальный QR-код через ZXing
        val qrSize = cardWidth * 0.74f
        val qrLeft = cardRect.left + (cardWidth - qrSize) / 2f
        val qrTop = cardRect.top + cardWidth * 0.11f

        val bitMatrix = runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 0,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            QRCodeWriter().encode(qrPayload, BarcodeFormat.QR_CODE, 0, 0, hints)
        }.getOrNull()

        val gridSize = bitMatrix?.width ?: 29
        val cellSize = qrSize / gridSize

        val qrPaint = Paint().apply {
            isAntiAlias = true
            color = effectiveQrColorInt
            style = Paint.Style.FILL
        }
        val qrStrokePaint = Paint().apply {
            isAntiAlias = true
            color = effectiveQrColorInt
            style = Paint.Style.STROKE
            strokeWidth = (qrSize / gridSize) * 0.95f
        }

        // Поисковые маркеры (Finder patterns)
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

        // Модули данных
        val centerCol = gridSize / 2f
        val centerRow = gridSize / 2f
        val centerCutoutRadius = gridSize * 0.20f

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val inTopLeft = row < 8 && col < 8
                val inTopRight = row < 8 && col >= gridSize - 8
                val inBottomLeft = row >= gridSize - 8 && col < 8

                val dx = col + 0.5f - centerCol
                val dy = row + 0.5f - centerRow
                val inCenterBadge = (dx * dx + dy * dy) <= (centerCutoutRadius * centerCutoutRadius)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenterBadge) {
                    val isModule = bitMatrix?.get(col, row) ?: false
                    if (isModule) {
                        val mx = qrLeft + col * cellSize + 1.2f
                        val my = qrTop + row * cellSize + 1.2f
                        val modRect = RectF(mx, my, mx + cellSize - 2.4f, my + cellSize - 2.4f)
                        canvas.drawRoundRect(modRect, cellSize * 0.35f, cellSize * 0.35f, qrPaint)
                    }
                }
            }
        }

        // 4. Фирменный центральный значок со стелс-иконкой приложения
        val cx = qrLeft + qrSize / 2f
        val cy = qrTop + qrSize / 2f
        val badgeRadius = cellSize * 2.8f

        // Белая обводка значка
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, badgeRadius + 6f, borderPaint)

        // Цветной круг
        canvas.drawCircle(cx, cy, badgeRadius, qrPaint)

        // Стелс-иконка приложения (Cryptographic Shield + Stealth Plane)
        val stealthIcon = ContextCompat.getDrawable(context, R.drawable.ic_stat_anongram)
        if (stealthIcon != null) {
            val iconHalf = (badgeRadius * 0.58f).toInt()
            stealthIcon.setBounds(
                (cx - iconHalf).toInt(),
                (cy - iconHalf).toInt(),
                (cx + iconHalf).toInt(),
                (cy + iconHalf).toInt()
            )
            stealthIcon.setTint(android.graphics.Color.WHITE)
            stealthIcon.draw(canvas)
        }

        // 5. Текст @HANDLE
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = effectiveQrColorInt
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = qrTop + qrSize + 98f
        canvas.drawText("@$cleanHandle", cx, textY, textPaint)

        // 6. Нижняя подпись бренда под карточкой
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(215, 255, 255, 255)
            textSize = 34f
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
        gradientColors: List<Color>,
        qrPayload: String = "anongram://user?handle=${handle.removePrefix("@").uppercase()}"
    ) {
        val bitmap = generateQrCardBitmap(context, handle, qrColor, gradientColors, qrPayload)
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
        gradientColors: List<Color>,
        qrPayload: String = "anongram://user?handle=${handle.removePrefix("@").uppercase()}"
    ): Boolean {
        return try {
            val bitmap = generateQrCardBitmap(context, handle, qrColor, gradientColors, qrPayload)
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
