package com.anongram.app.features.services.ai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Лёгкий парсер изображений и PDF-документов:
 * - Для изображений использует динамический модуль Play Services ML Kit OCR (скачивается извне).
 * - Для PDF использует встроенный PdfRenderer + ML Kit OCR по страницам (до 10 страниц).
 */
object DocumentParser {

    private const val MAX_PDF_PAGES = 10
    private const val MAX_TEXT_CHARS = 12000

    suspend fun extractText(
        context: Context,
        uri: Uri,
        isImage: Boolean,
        mimeType: String,
        fileName: String
    ): String? {
        val isPdf = mimeType.equals("application/pdf", ignoreCase = true) ||
            fileName.endsWith(".pdf", ignoreCase = true)
        return when {
            isPdf -> extractPdfText(context, uri)
            isImage -> extractImageText(context, uri)
            else -> null
        }
    }

    private suspend fun extractImageText(context: Context, uri: Uri): String? {
        val bitmap = decodeScaledBitmap(context, uri) ?: return null
        return try {
            recognizeBitmap(bitmap)?.takeIf { it.isNotBlank() }?.take(MAX_TEXT_CHARS)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun extractPdfText(context: Context, uri: Uri): String? {
        val tempFile = File.createTempFile("ai_pdf_", ".pdf", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
                    val sb = StringBuilder()
                    for (i in 0 until pageCount) {
                        if (sb.length >= MAX_TEXT_CHARS) break
                        renderer.openPage(i).use { page ->
                            val scale = 2
                            val w = (page.width * scale).coerceIn(400, 1800)
                            val h = (page.height * scale).coerceIn(400, 2400)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            try {
                                Canvas(bmp).drawColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val pageText = recognizeBitmap(bmp)
                                if (!pageText.isNullOrBlank()) {
                                    if (sb.isNotEmpty()) sb.append("\n\n")
                                    sb.append("--- Page ${i + 1} ---\n").append(pageText.trim())
                                }
                            } finally {
                                bmp.recycle()
                            }
                        }
                    }
                    sb.toString().trim().takeIf { it.isNotEmpty() }?.take(MAX_TEXT_CHARS)
                }
            }
        } catch (_: Throwable) {
            null
        } finally {
            tempFile.delete()
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val optsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, optsBounds)
            }
            var sample = 1
            val maxDim = maxOf(optsBounds.outWidth, optsBounds.outHeight)
            while (maxDim / sample > 1800) sample *= 2

            val optsDecode = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, optsDecode)
            }
        }.getOrNull()
    }

    private suspend fun recognizeBitmap(bitmap: Bitmap): String? =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (cont.isActive) cont.resume(result.text)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }
}
