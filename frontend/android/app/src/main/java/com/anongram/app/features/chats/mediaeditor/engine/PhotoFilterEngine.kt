package com.anongram.app.features.chats.mediaeditor.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.anongram.app.features.chats.mediaeditor.model.BlurEffectState
import com.anongram.app.features.chats.mediaeditor.model.BlurMode
import com.anongram.app.features.chats.mediaeditor.model.CurveChannel
import com.anongram.app.features.chats.mediaeditor.model.CurvePoint
import com.anongram.app.features.chats.mediaeditor.model.PhotoAdjustments
import com.anongram.app.features.chats.mediaeditor.model.PhotoEditState
import com.anongram.app.features.chats.mediaeditor.model.ToneCurvesState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object PhotoFilterEngine {

    /**
     * Строит 256-элементную таблицу перекодировки (LUT) по контрольным точкам кривой.
     */
    fun buildCurveLut(points: List<CurvePoint>): IntArray {
        val lut = IntArray(256)
        if (points.size < 2) {
            for (i in 0..255) lut[i] = i
            return lut
        }
        val sorted = points.sortedBy { it.x }
        for (i in 0..255) {
            val x = i / 255f
            val y = evaluatePiecewiseSmooth(sorted, x)
            lut[i] = (y * 255f).toInt().coerceIn(0, 255)
        }
        return lut
    }

    private fun evaluatePiecewiseSmooth(pts: List<CurvePoint>, x: Float): Float {
        if (x <= pts.first().x) return pts.first().y.coerceIn(0f, 1f)
        if (x >= pts.last().x) return pts.last().y.coerceIn(0f, 1f)
        var idx = 0
        while (idx < pts.size - 1 && x > pts[idx + 1].x) idx++
        val p0 = pts[(idx - 1).coerceAtLeast(0)]
        val p1 = pts[idx]
        val p2 = pts[(idx + 1).coerceAtMost(pts.lastIndex)]
        val p3 = pts[(idx + 2).coerceAtMost(pts.lastIndex)]
        val span = (p2.x - p1.x).coerceAtLeast(1e-4f)
        val t = ((x - p1.x) / span).coerceIn(0f, 1f)
        // Catmull-Rom spline for smooth curve response
        val t2 = t * t
        val t3 = t2 * t
        val y = 0.5f * (
            (2f * p1.y) +
                (-p0.y + p2.y) * t +
                (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
            )
        return y.coerceIn(0f, 1f)
    }

    fun needsPixelPipeline(state: PhotoEditState): Boolean {
        return state.adjustments.isModified || state.curves.isModified || state.blur.isModified
    }

    /**
     * Применяет коррекцию, кривые, гладкую кожу, резкость, виньетку, зерно и размытие.
     */
    fun applyPixelPipeline(source: Bitmap, state: PhotoEditState): Bitmap {
        val adj = state.adjustments
        val curves = state.curves
        val blur = state.blur

        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val allLut = buildCurveLut(curves.pointsFor(CurveChannel.ALL))
        val rLut = buildCurveLut(curves.pointsFor(CurveChannel.RED))
        val gLut = buildCurveLut(curves.pointsFor(CurveChannel.GREEN))
        val bLut = buildCurveLut(curves.pointsFor(CurveChannel.BLUE))

        // Precompute adjustments
        val expFactor = 2f.pow(adj.exposure * 1.4f)
        val enhanceContrast = 1f + adj.contrast * 0.65f + adj.enhance * 0.25f
        val satFactor = (1f + adj.saturation + adj.enhance * 0.2f).coerceIn(0f, 2.2f)
        val warmthShift = adj.warmth * 28f
        val fadeLift = adj.fade * 36f
        val highlightsAdj = adj.highlights * 0.45f
        val shadowsAdj = adj.shadows * 0.45f
        val hasVignette = adj.vignette > 0.01f
        val hasGrain = adj.grain > 0.01f
        val rng = if (hasGrain) Random(42) else null

        val halfW = w * 0.5f
        val halfH = h * 0.5f
        val maxDist = hypot(halfW.toDouble(), halfH.toDouble()).toFloat().coerceAtLeast(1f)

        // Step 1: Per-pixel color, curves, tone, vignette, grain
        for (y in 0 until h) {
            val rowOffset = y * w
            val dy = (y - halfH)
            for (x in 0 until w) {
                val idx = rowOffset + x
                val px = pixels[idx]
                val a = (px ushr 24) and 0xFF
                var r = ((px ushr 16) and 0xFF).toFloat()
                var g = ((px ushr 8) and 0xFF).toFloat()
                var b = (px and 0xFF).toFloat()

                // Exposure
                r *= expFactor
                g *= expFactor
                b *= expFactor

                // Highlights & Shadows
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                if (abs(shadowsAdj) > 0.005f) {
                    val shadowWeight = (1f - lum).pow(2f)
                    val delta = shadowsAdj * shadowWeight * 120f
                    r += delta; g += delta; b += delta
                }
                if (abs(highlightsAdj) > 0.005f) {
                    val hiWeight = lum.pow(2f)
                    val delta = highlightsAdj * hiWeight * 120f
                    r += delta; g += delta; b += delta
                }

                // Contrast
                r = (r - 128f) * enhanceContrast + 128f
                g = (g - 128f) * enhanceContrast + 128f
                b = (b - 128f) * enhanceContrast + 128f

                // Warmth
                r += warmthShift
                b -= warmthShift

                // Fade (lift blacks)
                if (fadeLift > 0.1f) {
                    r = r * (1f - adj.fade * 0.12f) + fadeLift
                    g = g * (1f - adj.fade * 0.12f) + fadeLift
                    b = b * (1f - adj.fade * 0.12f) + fadeLift
                }

                // Saturation
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = gray + (r - gray) * satFactor
                g = gray + (g - gray) * satFactor
                b = gray + (b - gray) * satFactor

                // Clamp & apply Tone Curves LUT
                var ri = r.toInt().coerceIn(0, 255)
                var gi = g.toInt().coerceIn(0, 255)
                var bi = b.toInt().coerceIn(0, 255)

                if (curves.isModified) {
                    ri = allLut[rLut[ri]]
                    gi = allLut[gLut[gi]]
                    bi = allLut[bLut[bi]]
                }

                // Vignette
                if (hasVignette) {
                    val dx = (x - halfW)
                    val distNorm = sqrt(dx * dx + dy * dy) / maxDist
                    val vig = (1f - adj.vignette * 0.85f * (distNorm * distNorm)).coerceIn(0.15f, 1f)
                    ri = (ri * vig).toInt()
                    gi = (gi * vig).toInt()
                    bi = (bi * vig).toInt()
                }

                // Grain
                if (rng != null) {
                    val noise = ((rng.nextFloat() - 0.5f) * 2f * adj.grain * 42f).toInt()
                    ri = (ri + noise).coerceIn(0, 255)
                    gi = (gi + noise).coerceIn(0, 255)
                    bi = (bi + noise).coerceIn(0, 255)
                }

                pixels[idx] = (a shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }

        // Step 2: Smooth skin (soft edge-preserving blend)
        if (adj.smoothSkin > 0.02f) {
            applySmoothSkin(pixels, w, h, adj.smoothSkin)
        }

        // Step 3: Sharpen
        if (adj.sharpen > 0.02f) {
            applySharpen(pixels, w, h, adj.sharpen)
        }

        // Step 4: Radial / Linear Blur
        if (blur.mode != BlurMode.OFF && blur.intensity > 0.02f) {
            applySelectiveBlur(pixels, w, h, blur)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun applySmoothSkin(pixels: IntArray, w: Int, h: Int, amount: Float) {
        val blurred = fastBoxBlur(pixels, w, h, radius = 3)
        val strength = amount.coerceIn(0f, 1f) * 0.72f
        for (i in pixels.indices) {
            val orig = pixels[i]
            val blr = blurred[i]
            val a = (orig ushr 24) and 0xFF
            val r0 = (orig ushr 16) and 0xFF
            val g0 = (orig ushr 8) and 0xFF
            val b0 = orig and 0xFF
            val r1 = (blr ushr 16) and 0xFF
            val g1 = (blr ushr 8) and 0xFF
            val b1 = blr and 0xFF
            // Edge-preserving: reduce smoothing on high-contrast edges
            val diff = (abs(r0 - r1) + abs(g0 - g1) + abs(b0 - b1)) / 3f
            val edgeGuard = (1f - (diff / 45f)).coerceIn(0f, 1f)
            val t = strength * edgeGuard
            val r = (r0 + (r1 - r0) * t).toInt().coerceIn(0, 255)
            val g = (g0 + (g1 - g0) * t).toInt().coerceIn(0, 255)
            val b = (b0 + (b1 - b0) * t).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    private fun applySharpen(pixels: IntArray, w: Int, h: Int, amount: Float) {
        val copy = pixels.clone()
        val k = amount.coerceIn(0f, 1f) * 1.1f
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val c = copy[idx]
                val up = copy[idx - w]
                val dn = copy[idx + w]
                val lt = copy[idx - 1]
                val rt = copy[idx + 1]
                val a = (c ushr 24) and 0xFF

                val r = sharpenChannel((c ushr 16) and 0xFF, (up ushr 16) and 0xFF, (dn ushr 16) and 0xFF, (lt ushr 16) and 0xFF, (rt ushr 16) and 0xFF, k)
                val g = sharpenChannel((c ushr 8) and 0xFF, (up ushr 8) and 0xFF, (dn ushr 8) and 0xFF, (lt ushr 8) and 0xFF, (rt ushr 8) and 0xFF, k)
                val b = sharpenChannel(c and 0xFF, up and 0xFF, dn and 0xFF, lt and 0xFF, rt and 0xFF, k)
                pixels[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    private fun sharpenChannel(c: Int, u: Int, d: Int, l: Int, r: Int, k: Float): Int {
        val avg = (u + d + l + r) * 0.25f
        return (c + (c - avg) * k).toInt().coerceIn(0, 255)
    }

    private fun applySelectiveBlur(pixels: IntArray, w: Int, h: Int, blur: BlurEffectState) {
        val radius = (6 + (blur.intensity * 10)).toInt().coerceIn(3, 18)
        val blurred = fastBoxBlur(pixels, w, h, radius = radius)
        val cx = blur.centerX * w
        val cy = blur.centerY * h
        val minDim = minOf(w, h).toFloat().coerceAtLeast(1f)
        val focusR = (blur.radius * minDim).coerceAtLeast(12f)
        val feather = (focusR * 0.45f).coerceAtLeast(16f)
        val rad = Math.toRadians(blur.angleDegrees.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        for (y in 0 until h) {
            val row = y * w
            val dy = y - cy
            for (x in 0 until w) {
                val dx = x - cx
                val dist = when (blur.mode) {
                    BlurMode.RADIAL -> sqrt(dx * dx + dy * dy)
                    BlurMode.LINEAR -> abs(-dx * sinA + dy * cosA)
                    BlurMode.OFF -> 0f
                }
                val t = ((dist - focusR) / feather).coerceIn(0f, 1f)
                if (t > 0.005f) {
                    val idx = row + x
                    val o = pixels[idx]
                    val b = blurred[idx]
                    val a = (o ushr 24) and 0xFF
                    val rr = (((o ushr 16) and 0xFF) * (1f - t) + ((b ushr 16) and 0xFF) * t).toInt().coerceIn(0, 255)
                    val gg = (((o ushr 8) and 0xFF) * (1f - t) + ((b ushr 8) and 0xFF) * t).toInt().coerceIn(0, 255)
                    val bb = ((o and 0xFF) * (1f - t) + (b and 0xFF) * t).toInt().coerceIn(0, 255)
                    pixels[idx] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
                }
            }
        }
    }

    /**
     * Быстрый 2-проходный box-blur (O(W*H) независимо от радиуса).
     */
    fun fastBoxBlur(src: IntArray, w: Int, h: Int, radius: Int): IntArray {
        val r = radius.coerceIn(1, 30)
        val temp = IntArray(w * h)
        val dst = IntArray(w * h)
        val div = r * 2 + 1

        // Horizontal pass
        for (y in 0 until h) {
            val row = y * w
            var rSum = 0; var gSum = 0; var bSum = 0
            for (k in -r..r) {
                val px = src[row + k.coerceIn(0, w - 1)]
                rSum += (px ushr 16) and 0xFF
                gSum += (px ushr 8) and 0xFF
                bSum += px and 0xFF
            }
            for (x in 0 until w) {
                val origA = (src[row + x] ushr 24) and 0xFF
                temp[row + x] = (origA shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                val addPx = src[row + (x + r + 1).coerceIn(0, w - 1)]
                val subPx = src[row + (x - r).coerceIn(0, w - 1)]
                rSum += ((addPx ushr 16) and 0xFF) - ((subPx ushr 16) and 0xFF)
                gSum += ((addPx ushr 8) and 0xFF) - ((subPx ushr 8) and 0xFF)
                bSum += (addPx and 0xFF) - (subPx and 0xFF)
            }
        }

        // Vertical pass
        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0
            for (k in -r..r) {
                val px = temp[k.coerceIn(0, h - 1) * w + x]
                rSum += (px ushr 16) and 0xFF
                gSum += (px ushr 8) and 0xFF
                bSum += px and 0xFF
            }
            for (y in 0 until h) {
                val idx = y * w + x
                val origA = (temp[idx] ushr 24) and 0xFF
                dst[idx] = (origA shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                val addPx = temp[(y + r + 1).coerceIn(0, h - 1) * w + x]
                val subPx = temp[(y - r).coerceIn(0, h - 1) * w + x]
                rSum += ((addPx ushr 16) and 0xFF) - ((subPx ushr 16) and 0xFF)
                gSum += ((addPx ushr 8) and 0xFF) - ((subPx ushr 8) and 0xFF)
                bSum += (addPx and 0xFF) - (subPx and 0xFF)
            }
        }
        return dst
    }

    /**
     * Применяет обрезку/поворот/зеркалирование к Bitmap.
     */
    fun applyCropAndTransform(source: Bitmap, state: PhotoEditState): Bitmap {
        val crop = state.crop
        var work = source

        // 1. Base 90-degree rotation + mirror
        if (crop.baseRotation90 % 360 != 0 || crop.mirrorHorizontal) {
            val m = android.graphics.Matrix()
            if (crop.mirrorHorizontal) m.postScale(-1f, 1f, work.width / 2f, work.height / 2f)
            if (crop.baseRotation90 % 360 != 0) m.postRotate(crop.baseRotation90.toFloat())
            work = Bitmap.createBitmap(work, 0, 0, work.width, work.height, m, true)
        }

        // 2. Straighten (-45..+45) with auto-zoom to avoid black corners
        if (abs(crop.straightenDegrees) > 0.05f) {
            val rad = Math.toRadians(abs(crop.straightenDegrees).toDouble())
            val w = work.width.toFloat()
            val h = work.height.toFloat()
            val scale = (w * cos(rad) + h * sin(rad)) / w.coerceAtLeast(1f)
            val m = android.graphics.Matrix().apply {
                postRotate(crop.straightenDegrees, w / 2f, h / 2f)
                postScale(scale.toFloat(), scale.toFloat(), w / 2f, h / 2f)
            }
            val rotated = Bitmap.createBitmap(work, 0, 0, work.width, work.height, m, true)
            // Center-crop back to original aspect bounds
            val cx = ((rotated.width - work.width) / 2).coerceIn(0, (rotated.width - 1).coerceAtLeast(0))
            val cy = ((rotated.height - work.height) / 2).coerceIn(0, (rotated.height - 1).coerceAtLeast(0))
            val cw = work.width.coerceAtMost(rotated.width - cx).coerceAtLeast(1)
            val ch = work.height.coerceAtMost(rotated.height - cy).coerceAtLeast(1)
            work = Bitmap.createBitmap(rotated, cx, cy, cw, ch)
        }

        // 3. Crop rect
        val rect = crop.cropRect
        if (!rect.isFull) {
            val x = (rect.left * work.width).toInt().coerceIn(0, (work.width - 1).coerceAtLeast(0))
            val y = (rect.top * work.height).toInt().coerceIn(0, (work.height - 1).coerceAtLeast(0))
            val cw = (rect.width * work.width).toInt().coerceIn(1, work.width - x)
            val ch = (rect.height * work.height).toInt().coerceIn(1, work.height - y)
            work = Bitmap.createBitmap(work, x, y, cw, ch)
        }

        return work
    }
}
