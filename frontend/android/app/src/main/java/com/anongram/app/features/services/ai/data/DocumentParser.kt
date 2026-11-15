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
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.nio.charset.Charset
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume

/**
 * Офлайн-парсер документов и изображений для локального ИИ (+0 КБ к APK):
 * - Office Open XML: .docx, .xlsx, .pptx
 * - OpenDocument / книги / разметка: .odt, .epub, .fb2, .rtf
 * - PDF: прямое извлечение текстового слоя (FlateDecode + BT/ET) + резервный PdfRenderer + ML Kit OCR
 * - Изображения: ML Kit OCR (для текста на фото/скриншотах)
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
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val isPdf = mimeType.equals("application/pdf", ignoreCase = true) || ext == "pdf"
        return when {
            isPdf -> extractPdfText(context, uri)
            ext == "docx" -> extractDocxText(context, uri)
            ext == "xlsx" -> extractXlsxText(context, uri)
            ext == "pptx" -> extractPptxText(context, uri)
            ext == "odt" -> extractOdtText(context, uri)
            ext == "epub" -> extractEpubText(context, uri)
            ext == "fb2" -> extractFb2Text(context, uri)
            ext == "rtf" -> extractRtfText(context, uri)
            isImage -> extractImageText(context, uri)
            else -> null
        }
    }

    private fun readZipEntries(context: Context, uri: Uri, filter: (String) -> Boolean): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && filter(name)) {
                            out[name] = zis.readBytes()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        }
        return out
    }

    private fun extractXmlParagraphText(xml: String, paragraphTags: Set<String>, textTags: Set<String>? = null): String {
        val sb = StringBuilder()
        runCatching {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            var insideText = textTags == null
            while (event != XmlPullParser.END_DOCUMENT && sb.length < MAX_TEXT_CHARS) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name?.substringAfter(':') ?: ""
                        if (textTags != null && name in textTags) insideText = true
                        if (name == "br" || name == "cr") sb.append('\n')
                        if (name == "tab") sb.append('\t')
                    }
                    XmlPullParser.TEXT -> {
                        if (insideText) {
                            val t = parser.text
                            if (!t.isNullOrEmpty()) sb.append(t)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name?.substringAfter(':') ?: ""
                        if (textTags != null && name in textTags) insideText = false
                        if (name in paragraphTags && sb.isNotEmpty() && sb.last() != '\n') {
                            sb.append('\n')
                        }
                    }
                }
                event = parser.next()
            }
        }
        return sb.toString().replace(Regex("\n{3,}"), "\n\n").trim().take(MAX_TEXT_CHARS)
    }

    private fun extractDocxText(context: Context, uri: Uri): String? {
        val entries = readZipEntries(context, uri) { it == "word/document.xml" }
        val bytes = entries["word/document.xml"] ?: return null
        val text = extractXmlParagraphText(
            xml = bytes.toString(Charsets.UTF_8),
            paragraphTags = setOf("p", "tr"),
            textTags = setOf("t", "delText", "instrText")
        )
        return text.takeIf { it.isNotBlank() }
    }

    private fun extractXlsxText(context: Context, uri: Uri): String? {
        val entries = readZipEntries(context, uri) {
            it == "xl/sharedStrings.xml" || (it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml"))
        }
        if (entries.isEmpty()) return null

        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { ssBytes ->
            runCatching {
                val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                val parser = factory.newPullParser()
                parser.setInput(StringReader(ssBytes.toString(Charsets.UTF_8)))
                var event = parser.eventType
                val cellBuf = StringBuilder()
                var inSi = false
                var inT = false
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name?.substringAfter(':')) {
                                "si" -> { inSi = true; cellBuf.clear() }
                                "t" -> if (inSi) inT = true
                            }
                        }
                        XmlPullParser.TEXT -> if (inSi && inT) cellBuf.append(parser.text.orEmpty())
                        XmlPullParser.END_TAG -> {
                            when (parser.name?.substringAfter(':')) {
                                "t" -> inT = false
                                "si" -> { sharedStrings.add(cellBuf.toString()); inSi = false }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
        }

        val sb = StringBuilder()
        val sheetKeys = entries.keys.filter { it.startsWith("xl/worksheets/sheet") }.sorted()
        for (sheetName in sheetKeys) {
            if (sb.length >= MAX_TEXT_CHARS) break
            val xml = entries[sheetName]?.toString(Charsets.UTF_8) ?: continue
            sb.append("--- ").append(sheetName.substringAfterLast('/')).append(" ---\n")
            runCatching {
                val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                val parser = factory.newPullParser()
                parser.setInput(StringReader(xml))
                var event = parser.eventType
                var cellType = ""
                var inV = false
                var inT = false
                var rowHasValues = false
                while (event != XmlPullParser.END_DOCUMENT && sb.length < MAX_TEXT_CHARS) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name?.substringAfter(':')) {
                                "row" -> rowHasValues = false
                                "c" -> cellType = parser.getAttributeValue(null, "t").orEmpty()
                                "v" -> inV = true
                                "t" -> inT = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            val rawVal = parser.text.orEmpty()
                            if (inV && rawVal.isNotBlank()) {
                                val resolved = if (cellType == "s") {
                                    rawVal.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: rawVal
                                } else rawVal
                                if (rowHasValues) sb.append(" | ")
                                sb.append(resolved)
                                rowHasValues = true
                            } else if (inT && rawVal.isNotBlank()) {
                                if (rowHasValues) sb.append(" | ")
                                sb.append(rawVal)
                                rowHasValues = true
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name?.substringAfter(':')) {
                                "v" -> inV = false
                                "t" -> inT = false
                                "row" -> if (rowHasValues) sb.append('\n')
                            }
                        }
                    }
                    event = parser.next()
                }
            }
            sb.append('\n')
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }?.take(MAX_TEXT_CHARS)
    }

    private fun extractPptxText(context: Context, uri: Uri): String? {
        val entries = readZipEntries(context, uri) {
            it.startsWith("ppt/slides/slide") && it.endsWith(".xml")
        }
        if (entries.isEmpty()) return null
        val sb = StringBuilder()
        entries.keys.sortedBy {
            Regex("""slide(\d+)\.xml""").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 999
        }.forEachIndexed { idx, key ->
            if (sb.length >= MAX_TEXT_CHARS) return@forEachIndexed
            val xml = entries[key]?.toString(Charsets.UTF_8) ?: return@forEachIndexed
            val slideText = extractXmlParagraphText(xml, setOf("p"), setOf("t"))
            if (slideText.isNotBlank()) {
                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append("--- Slide ${idx + 1} ---\n").append(slideText)
            }
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }?.take(MAX_TEXT_CHARS)
    }

    private fun extractOdtText(context: Context, uri: Uri): String? {
        val entries = readZipEntries(context, uri) { it == "content.xml" }
        val bytes = entries["content.xml"] ?: return null
        val text = extractXmlParagraphText(
            xml = bytes.toString(Charsets.UTF_8),
            paragraphTags = setOf("p", "h", "list-item")
        )
        return text.takeIf { it.isNotBlank() }
    }

    private fun extractEpubText(context: Context, uri: Uri): String? {
        val entries = readZipEntries(context, uri) {
            it.endsWith(".xhtml", ignoreCase = true) || it.endsWith(".html", ignoreCase = true)
        }
        if (entries.isEmpty()) return null
        val sb = StringBuilder()
        for (key in entries.keys.sorted()) {
            if (sb.length >= MAX_TEXT_CHARS) break
            val html = entries[key]?.toString(Charsets.UTF_8) ?: continue
            val clean = html
                .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (clean.isNotBlank()) {
                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append(clean)
            }
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }?.take(MAX_TEXT_CHARS)
    }

    private fun extractFb2Text(context: Context, uri: Uri): String? {
        val xml = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        val head = xml.take(256).toByteArray().toString(Charsets.ISO_8859_1)
        val charset = if (head.contains("windows-1251", ignoreCase = true)) {
            Charset.forName("windows-1251")
        } else {
            Charsets.UTF_8
        }
        val text = extractXmlParagraphText(
            xml = xml.toString(charset),
            paragraphTags = setOf("p", "v", "subtitle", "title")
        )
        return text.takeIf { it.isNotBlank() }
    }

    private fun extractRtfText(context: Context, uri: Uri): String? {
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.ISO_8859_1)?.use { it.readText() }
        }.getOrNull() ?: return null
        val cp1251 = Charset.forName("windows-1251")
        val withoutDestinations = raw
            .replace(Regex("""\{\\\*(?:[^{}]|\{[^{}]*\})*\}"""), " ")
            .replace(Regex("""\{\\(?:fonttbl|colortbl|stylesheet|info|pict|object)(?:[^{}]|\{[^{}]*\})*\}"""), " ")
        val hexDecoded = Regex("""\\'([0-9a-fA-F]{2})""").replace(withoutDestinations) { m ->
            val b = m.groupValues[1].toInt(16).toByte()
            String(byteArrayOf(b), cp1251)
        }
        val unicodeDecoded = Regex("""\\u(-?\d+)\??""").replace(hexDecoded) { m ->
            val code = m.groupValues[1].toIntOrNull() ?: return@replace ""
            val ch = if (code < 0) (code + 65536).toChar() else code.toChar()
            ch.toString()
        }
        val clean = unicodeDecoded
            .replace(Regex("""\\par\b|\\line\b"""), "\n")
            .replace(Regex("""\\tab\b"""), "\t")
            .replace(Regex("""\\[a-zA-Z]+-?\d*\s?"""), "")
            .replace("{", "")
            .replace("}", "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        return clean.takeIf { it.isNotBlank() }?.take(MAX_TEXT_CHARS)
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
        val rawBytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()

        if (rawBytes != null) {
            val embedded = extractEmbeddedPdfText(rawBytes)
            if (!embedded.isNullOrBlank()) return embedded
        }

        val tempFile = File.createTempFile("ai_pdf_", ".pdf", context.cacheDir)
        return try {
            if (rawBytes != null) {
                tempFile.writeBytes(rawBytes)
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return null
            }

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

    private fun extractEmbeddedPdfText(pdfBytes: ByteArray): String? {
        val rawLatin = pdfBytes.toString(Charsets.ISO_8859_1)
        val sb = StringBuilder()
        val streamRegex = Regex("""stream\r?\n(.*?)\r?\nendstream""", RegexOption.DOT_MATCHES_ALL)
        for (match in streamRegex.findAll(rawLatin)) {
            if (sb.length >= MAX_TEXT_CHARS) break
            val streamText = match.groupValues[1]
            val streamBytes = streamText.toByteArray(Charsets.ISO_8859_1)
            val decoded = inflatePdfStream(streamBytes) ?: streamBytes
            val content = decoded.toString(Charsets.ISO_8859_1)
            if (!content.contains("BT") || (!content.contains("Tj") && !content.contains("TJ"))) continue
            val extracted = extractTextFromPdfContentStream(content)
            if (extracted.isNotBlank()) {
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(extracted)
            }
        }
        val result = sb.toString().replace(Regex("\\s+"), " ").trim()
        return result.takeIf { it.length >= 20 }?.take(MAX_TEXT_CHARS)
    }

    private fun inflatePdfStream(bytes: ByteArray): ByteArray? = runCatching {
        val inflater = Inflater()
        inflater.setInput(bytes)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!inflater.finished()) {
            val count = inflater.inflate(buf)
            if (count == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
            } else {
                out.write(buf, 0, count)
            }
        }
        inflater.end()
        out.toByteArray().takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun extractTextFromPdfContentStream(content: String): String {
        val sb = StringBuilder()
        val tjRegex = Regex("""\(((?:\\.|[^\\)])*)\)\s*Tj""")
        val tjArrRegex = Regex("""\[(.*?)\]\s*TJ""", RegexOption.DOT_MATCHES_ALL)
        val innerParenRegex = Regex("""\(((?:\\.|[^\\)])*)\)""")

        val tokens = mutableListOf<Pair<Int, String>>()
        tjRegex.findAll(content).forEach { m ->
            tokens.add(m.range.first to unescapePdfString(m.groupValues[1]))
        }
        tjArrRegex.findAll(content).forEach { m ->
            val joined = innerParenRegex.findAll(m.groupValues[1])
                .joinToString("") { unescapePdfString(it.groupValues[1]) }
            if (joined.isNotBlank()) tokens.add(m.range.first to joined)
        }
        tokens.sortBy { it.first }
        tokens.forEach { (_, text) ->
            val cleaned = text.filter { it >= ' ' || it == '\n' }
            if (cleaned.isNotBlank()) sb.append(cleaned).append(' ')
        }
        return sb.toString().trim()
    }

    private fun unescapePdfString(s: String): String = s
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\(", "(")
        .replace("\\)", ")")
        .replace("\\\\", "\\")

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
