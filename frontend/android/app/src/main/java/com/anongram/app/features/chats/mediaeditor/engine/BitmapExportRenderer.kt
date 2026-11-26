package com.anongram.app.features.chats.mediaeditor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.anongram.app.features.chats.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.chats.mediaeditor.api.EditedMedia
import com.anongram.app.features.chats.mediaeditor.model.BrushStroke
import com.anongram.app.features.chats.mediaeditor.model.BrushType
import com.anongram.app.features.chats.mediaeditor.model.PhotoEditState
import com.anongram.app.features.chats.mediaeditor.model.TextAlignOption
import com.anongram.app.features.chats.mediaeditor.model.TextBackgroundStyle
import com.anongram.app.features.chats.mediaeditor.model.TextLayerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object BitmapExportRenderer {

    suspend fun exportAll(
        context: Context,
        items: List<EditableMediaItem>
    ): List<EditedMedia> = withContext(Dispatchers.IO) {
        items.map { item ->
            if (item.isVideo) {
                exportVideoItem(context, item)
            } else {
                exportPhotoItem(context, item)
            }
        }
    }

    private fun exportVideoItem(context: Context, item: EditableMediaItem): EditedMedia {
        val targetFile = File(
            context.cacheDir,
            "edited_video_${System.currentTimeMillis()}_${item.id.take(4)}.mp4"
        )

        val needsRemuxEdit = item.isMuted ||
            item.trimStartMs > 0L ||
            (item.trimEndMs > 0L && (item.durationMs <= 0L || item.trimEndMs < item.durationMs - 150L)) ||
            item.editState.crop.baseRotation90 % 360 != 0 ||
            item.rotationDegrees % 360 != 0

        var remuxSucceeded = false
        if (needsRemuxEdit) {
            remuxSucceeded = runCatching {
                remuxVideoWithEdits(context, item, targetFile)
            }.getOrDefault(false)
        }

        if (!remuxSucceeded) {
            runCatching {
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        val finalUri = if (targetFile.exists() && targetFile.length() > 0) Uri.fromFile(targetFile) else item.uri

        // Register full-video edits (strokes, text, crop, adjustments, curves, blur) in VideoLiveEditRegistry
        // so that EVERY frame of the playing video in chat bubbles and fullscreen player renders them live!
        val hasVisualEdits = item.editState.crop.isModified ||
            item.editState.hasAnyAdjustments ||
            item.editState.hasAnyPaintOrText

        var transparentOverlayBmp: Bitmap? = null
        if (item.editState.hasAnyPaintOrText) {
            runCatching {
                val emptyCanvasBmp = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
                transparentOverlayBmp = renderOverlayLayers(context, emptyCanvasBmp, item.editState)
            }
        }

        // Only bake a static coverUri if the user explicitly picked a custom cover (item.coverUri != null),
        // and update the quick thumbnail cache for instant 0ms first-frame display before video autoplay starts.
        var finalCoverUri = item.coverUri
        var blurMaskBmp: Bitmap? = null
        if (hasVisualEdits || item.coverUri != null) {
            runCatching {
                val baseFrame = if (item.coverUri != null) {
                    decodeBitmapWithExif(context, item.coverUri, maxDim = 1280)
                } else {
                    val retriever = android.media.MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, item.uri)
                        val tUs = (item.coverTimeMs ?: item.trimStartMs) * 1000L
                        retriever.getFrameAtTime(tUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.frameAtTime
                    } finally {
                        runCatching { retriever.release() }
                    }
                }
                if (baseFrame != null) {
                    var working = PhotoFilterEngine.applyCropAndTransform(baseFrame, item.editState)
                    if (item.editState.blur.isModified) {
                        blurMaskBmp = PhotoFilterEngine.buildSelectiveBlurOverlayBitmap(working, item.editState)
                    }
                    if (PhotoFilterEngine.needsPixelPipeline(item.editState)) {
                        working = PhotoFilterEngine.applyPixelPipeline(working, item.editState)
                    }
                    if (item.editState.strokes.isNotEmpty() || item.editState.textLayers.isNotEmpty()) {
                        working = renderOverlayLayers(context, working, item.editState)
                    }
                    // Cache the edited preview thumbnail and cropped dimensions for finalUri so gallery/chat bubbles size adaptively in 0ms
                    com.anongram.app.features.chats.attach.tabs.gallery.thumbnailCache.put(finalUri, working)
                    com.anongram.app.features.chats.attach.tabs.gallery.putMediaDimensions(
                        finalUri.toString(),
                        working.width,
                        working.height
                    )

                    if (item.coverUri != null) {
                        val coverFile = File(
                            context.cacheDir,
                            "edited_video_cover_${System.currentTimeMillis()}_${item.id.take(4)}.jpg"
                        )
                        FileOutputStream(coverFile).use { fos ->
                            working.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                        }
                        if (coverFile.exists() && coverFile.length() > 0) {
                            finalCoverUri = Uri.fromFile(coverFile)
                            com.anongram.app.features.chats.attach.tabs.gallery.thumbnailCache.put(finalCoverUri!!, working)
                        }
                    }
                }
            }
        }

        // Preserve extra live rotation only if remux didn't already bake the rotation into the MP4 file
        val liveStateForPlayback = if (remuxSucceeded && item.editState.crop.baseRotation90 % 360 != 0) {
            item.editState.copy(crop = item.editState.crop.copy(baseRotation90 = 0))
        } else {
            item.editState
        }
        com.anongram.app.features.chats.mediaeditor.model.VideoLiveEditRegistry.put(
            uriString = finalUri.toString(),
            state = liveStateForPlayback,
            overlayBitmap = transparentOverlayBmp,
            blurMaskBitmap = blurMaskBmp
        )
        com.anongram.app.features.chats.mediaeditor.model.VideoLiveEditRegistry.put(
            uriString = item.uri.toString(),
            state = item.editState,
            overlayBitmap = transparentOverlayBmp,
            blurMaskBitmap = blurMaskBmp
        )

        return EditedMedia(
            uri = finalUri,
            isVideo = true,
            caption = item.caption,
            hd = item.videoQuality.maxHeight >= 720,
            ttl = item.ttl,
            trimStartMs = item.trimStartMs,
            trimEndMs = item.trimEndMs,
            isMuted = item.isMuted,
            coverUri = finalCoverUri,
            quality = item.videoQuality.label,
            originalId = item.id
        )
    }

    private fun remuxVideoWithEdits(
        context: Context,
        item: EditableMediaItem,
        outFile: File
    ): Boolean {
        val extractor = android.media.MediaExtractor()
        var muxer: android.media.MediaMuxer? = null
        return try {
            extractor.setDataSource(context, item.uri, null)
            var origRotation = 0
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, item.uri)
                origRotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }

            val extraRot = if (item.editState.crop.baseRotation90 != 0) item.editState.crop.baseRotation90 else item.rotationDegrees
            val finalRotation = ((origRotation + extraRot) % 360 + 360) % 360

            muxer = android.media.MediaMuxer(outFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(finalRotation)

            val trackMap = HashMap<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i)
                    trackMap[i] = muxer.addTrack(format)
                } else if (mime.startsWith("audio/") && !item.isMuted) {
                    extractor.selectTrack(i)
                    trackMap[i] = muxer.addTrack(format)
                }
            }

            if (trackMap.isEmpty()) {
                muxer.release()
                return false
            }

            muxer.start()
            val startUs = (item.trimStartMs.coerceAtLeast(0L)) * 1000L
            val endUs = if (item.trimEndMs > item.trimStartMs) item.trimEndMs * 1000L else Long.MAX_VALUE
            if (startUs > 0L) {
                extractor.seekTo(startUs, android.media.MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            val buffer = java.nio.ByteBuffer.allocate(512 * 1024)
            val info = android.media.MediaCodec.BufferInfo()
            var basePtsUs = -1L

            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break
                if (sampleTimeUs >= startUs || basePtsUs >= 0L) {
                    if (basePtsUs < 0L) basePtsUs = sampleTimeUs
                    info.presentationTimeUs = (sampleTimeUs - basePtsUs).coerceAtLeast(0L)
                    info.flags = extractor.sampleFlags
                    val dstTrack = trackMap[extractor.sampleTrackIndex]
                    if (dstTrack != null) {
                        muxer.writeSampleData(dstTrack, buffer, info)
                    }
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            outFile.exists() && outFile.length() > 0L
        } catch (_: Exception) {
            runCatching { muxer?.release() }
            runCatching { if (outFile.exists()) outFile.delete() }
            false
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun exportPhotoItem(context: Context, item: EditableMediaItem): EditedMedia {
        val targetFile = File(
            context.cacheDir,
            "edited_photo_${System.currentTimeMillis()}_${item.id.take(4)}.jpg"
        )
        var outWidth = 0
        var outHeight = 0

        runCatching {
            val decoded = decodeBitmapWithExif(context, item.uri, maxDim = if (item.hd) 2560 else 1280)
            if (decoded != null) {
                val effectiveState = item.editState.let { st ->
                    if (item.rotationDegrees != 0 && st.crop.baseRotation90 == 0) {
                        st.copy(crop = st.crop.copy(baseRotation90 = item.rotationDegrees))
                    } else st
                }

                // 1. Crop / Rotate / Mirror
                var working = PhotoFilterEngine.applyCropAndTransform(decoded, effectiveState)

                // 2. Color adjustments, Curves, Blur
                if (PhotoFilterEngine.needsPixelPipeline(effectiveState)) {
                    working = PhotoFilterEngine.applyPixelPipeline(working, effectiveState)
                }

                // 3. Strokes (including Blur brush) + Text layers
                if (effectiveState.strokes.isNotEmpty() || effectiveState.textLayers.isNotEmpty()) {
                    working = renderOverlayLayers(context, working, effectiveState)
                }

                outWidth = working.width
                outHeight = working.height

                // Compress to JPEG (EXIF stripped automatically)
                FileOutputStream(targetFile).use { fos ->
                    working.compress(Bitmap.CompressFormat.JPEG, if (item.hd) 94 else 84, fos)
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    val outUri = Uri.fromFile(targetFile)
                    com.anongram.app.features.chats.attach.tabs.gallery.thumbnailCache.put(outUri, working)
                    com.anongram.app.features.chats.attach.tabs.gallery.putMediaDimensions(
                        outUri.toString(),
                        working.width,
                        working.height
                    )
                }
            }
        }

        val finalUri = if (targetFile.exists() && targetFile.length() > 0) Uri.fromFile(targetFile) else item.uri
        return EditedMedia(
            uri = finalUri,
            isVideo = false,
            caption = item.caption,
            hd = item.hd,
            ttl = item.ttl,
            width = outWidth,
            height = outHeight,
            quality = if (item.hd) "HD" else "SD",
            originalId = item.id
        )
    }

    fun decodeBitmapWithExif(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (origW / sampleSize > maxDim * 2 || origH / sampleSize > maxDim * 2) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        // Read EXIF orientation
        val exifOrientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        if (!matrix.isIdentity) {
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }

        // Scale down to maxDim if needed
        val maxCurrent = maxOf(bmp.width, bmp.height)
        if (maxCurrent > maxDim) {
            val scale = maxDim.toFloat() / maxCurrent
            bmp = Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).toInt().coerceAtLeast(1),
                (bmp.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
        return bmp
    }

    private fun renderOverlayLayers(
        context: Context,
        baseBitmap: Bitmap,
        state: PhotoEditState
    ): Bitmap {
        val w = baseBitmap.width
        val h = baseBitmap.height
        val mutable = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val minDim = minOf(w, h).toFloat().coerceAtLeast(1f)

        // Prepare strongly blurred version of baseBitmap if any BLUR brush stroke exists
        var blurredShader: BitmapShader? = null
        if (state.strokes.any { it.type == BrushType.BLUR }) {
            val pixels = IntArray(w * h)
            baseBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val pass1 = PhotoFilterEngine.fastBoxBlur(pixels, w, h, radius = 22)
            val pass2 = PhotoFilterEngine.fastBoxBlur(pass1, w, h, radius = 22)
            val blurredBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            blurredBmp.setPixels(pass2, 0, w, 0, 0, w, h)
            blurredShader = BitmapShader(blurredBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        // Unified stroke layer (all brushes including BLUR are drawn here so ERASER erases BLUR too)
        val strokeLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val strokeCanvas = Canvas(strokeLayer)

        for (stroke in state.strokes) {
            if (stroke.points.isEmpty()) continue
            val strokePx = (stroke.strokeWidthNorm * minDim).coerceAtLeast(3f)
            val path = buildPathFromNormalized(stroke, w.toFloat(), h.toFloat())

            when (stroke.type) {
                BrushType.BLUR -> {
                    // Composite current baseBitmap + any strokes drawn so far, blur it, and paint onto strokeCanvas
                    val compositeBmp = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    Canvas(compositeBmp).drawBitmap(strokeLayer, 0f, 0f, null)
                    val px = IntArray(w * h)
                    compositeBmp.getPixels(px, 0, w, 0, 0, w, h)
                    val pass1 = PhotoFilterEngine.fastBoxBlur(px, w, h, radius = 22)
                    val pass2 = PhotoFilterEngine.fastBoxBlur(pass1, w, h, radius = 22)
                    val blurredComposite = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    blurredComposite.setPixels(pass2, 0, w, 0, 0, w, h)
                    val dynamicShader = BitmapShader(blurredComposite, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = strokePx * 2.2f
                        shader = dynamicShader
                        maskFilter = BlurMaskFilter((strokePx * 0.45f).coerceAtLeast(4f), BlurMaskFilter.Blur.NORMAL)
                    }
                    strokeCanvas.drawPath(path, p)
                }
                BrushType.ERASER -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = strokePx * 1.3f
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    }
                    strokeCanvas.drawPath(path, p)
                }
                BrushType.MARKER -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.SQUARE
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = strokePx * 1.5f
                        color = stroke.colorArgb.toInt()
                        alpha = 128
                    }
                    strokeCanvas.drawPath(path, p)
                }
                BrushType.NEON -> {
                    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = strokePx * 1.9f
                        color = stroke.colorArgb.toInt()
                        maskFilter = BlurMaskFilter(strokePx * 0.9f, BlurMaskFilter.Blur.NORMAL)
                    }
                    val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = strokePx * 0.6f
                        color = android.graphics.Color.WHITE
                    }
                    strokeCanvas.drawPath(path, glow)
                    strokeCanvas.drawPath(path, core)
                }
                BrushType.PEN -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = strokePx
                        color = stroke.colorArgb.toInt()
                    }
                    strokeCanvas.drawPath(path, p)
                }
                BrushType.ARROW -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = strokePx
                        color = stroke.colorArgb.toInt()
                    }
                    strokeCanvas.drawPath(path, p)
                    drawArrowHead(strokeCanvas, stroke, w.toFloat(), h.toFloat(), strokePx, p)
                }
            }
        }

        // Composite strokes onto photo
        canvas.drawBitmap(strokeLayer, 0f, 0f, null)

        // Draw text layers
        for (layer in state.textLayers) {
            drawTextLayer(context, canvas, layer, w.toFloat(), h.toFloat(), minDim)
        }

        return mutable
    }

    private fun buildPathFromNormalized(stroke: BrushStroke, w: Float, h: Float): Path {
        val path = Path()
        val first = stroke.points.first()
        path.moveTo(first.x * w, first.y * h)
        if (stroke.points.size == 1) {
            path.lineTo(first.x * w + 0.5f, first.y * h + 0.5f)
            return path
        }
        for (i in 1 until stroke.points.size) {
            val prev = stroke.points[i - 1]
            val curr = stroke.points[i]
            val midX = (prev.x + curr.x) * 0.5f * w
            val midY = (prev.y + curr.y) * 0.5f * h
            path.quadTo(prev.x * w, prev.y * h, midX, midY)
        }
        val last = stroke.points.last()
        path.lineTo(last.x * w, last.y * h)
        return path
    }

    private fun drawArrowHead(
        canvas: Canvas,
        stroke: BrushStroke,
        w: Float,
        h: Float,
        strokePx: Float,
        paint: Paint
    ) {
        if (stroke.points.size < 2) return
        val end = stroke.points.last()
        val refIdx = (stroke.points.size - 5).coerceAtLeast(0)
        val prev = stroke.points[refIdx]
        val ex = end.x * w
        val ey = end.y * h
        val px = prev.x * w
        val py = prev.y * h
        val angle = atan2((ey - py).toDouble(), (ex - px).toDouble())
        val headLen = strokePx * 3.4f
        val wingAngle = Math.toRadians(28.0)

        val x1 = (ex - headLen * cos(angle - wingAngle)).toFloat()
        val y1 = (ey - headLen * sin(angle - wingAngle)).toFloat()
        val x2 = (ex - headLen * cos(angle + wingAngle)).toFloat()
        val y2 = (ey - headLen * sin(angle + wingAngle)).toFloat()

        val headPath = Path().apply {
            moveTo(x1, y1)
            lineTo(ex, ey)
            lineTo(x2, y2)
        }
        canvas.drawPath(headPath, paint)
    }

    @Suppress("DEPRECATION")
    private fun drawTextLayer(
        context: Context,
        canvas: Canvas,
        layer: TextLayerItem,
        w: Float,
        h: Float,
        minDim: Float
    ) {
        if (layer.text.isBlank()) return
        val typeface = EditorFontResolver.resolveTypeface(context, layer.fontSpec)
        val textSizePx = (minDim * 0.065f * layer.scale).coerceIn(18f, minDim * 0.35f)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.colorArgb.toInt()
            textSize = textSizePx
            this.typeface = typeface
        }

        val maxTextWidth = (w * 0.85f).toInt().coerceAtLeast(100)
        val alignment = when (layer.align) {
            TextAlignOption.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlignOption.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignOption.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val layout = StaticLayout.Builder
            .obtain(layer.text, 0, layer.text.length, textPaint, maxTextWidth)
            .setAlignment(alignment)
            .setIncludePad(false)
            .build()

        var maxLineW = 0f
        for (i in 0 until layout.lineCount) {
            maxLineW = maxOf(maxLineW, layout.getLineWidth(i))
        }
        val boxW = maxLineW.coerceAtLeast(textSizePx)
        val boxH = layout.height.toFloat()

        canvas.save()
        canvas.translate(layer.normX * w, layer.normY * h)
        canvas.rotate(layer.rotationDegrees)

        val padH = textSizePx * 0.45f
        val padV = textSizePx * 0.28f
        val bgRect = RectF(-boxW / 2f - padH, -boxH / 2f - padV, boxW / 2f + padH, boxH / 2f + padV)

        when (layer.bgStyle) {
            TextBackgroundStyle.FILLED -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (layer.colorArgb == 0xFF000000L) 0xFFFFFFFF.toInt() else layer.colorArgb.toInt()
                }
                if (layer.colorArgb != 0xFF000000L) {
                    textPaint.color = android.graphics.Color.BLACK
                }
                canvas.drawRoundRect(bgRect, textSizePx * 0.32f, textSizePx * 0.32f, bgPaint)
            }
            TextBackgroundStyle.SEMI -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x88000000.toInt()
                }
                canvas.drawRoundRect(bgRect, textSizePx * 0.32f, textSizePx * 0.32f, bgPaint)
            }
            TextBackgroundStyle.OUTLINE -> {
                val strokePaint = TextPaint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = textSizePx * 0.14f
                    color = if (layer.colorArgb == 0xFF000000L) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                }
                canvas.save()
                canvas.translate(-maxTextWidth / 2f, -boxH / 2f)
                val outlineLayout = StaticLayout.Builder
                    .obtain(layer.text, 0, layer.text.length, strokePaint, maxTextWidth)
                    .setAlignment(alignment)
                    .setIncludePad(false)
                    .build()
                outlineLayout.draw(canvas)
                canvas.restore()
            }
            TextBackgroundStyle.NONE -> Unit
        }

        canvas.translate(-maxTextWidth / 2f, -boxH / 2f)
        layout.draw(canvas)
        canvas.restore()
    }
}
