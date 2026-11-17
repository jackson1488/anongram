package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlin.math.hypot
import kotlin.random.Random

val LocalChatRevealedSpoilers = compositionLocalOf<MutableMap<String, Set<Int>>?> { null }

private const val REVEAL_DURATION_MS = 360L

private class SpoilerParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radiusPx: Float,
    val alpha: Float,
    var regionIdx: Int
)

private data class SpoilerRevealAnim(
    val center: Offset,
    val maxRadius: Float,
    val startMs: Long
)

@Composable
fun TelegramTextSpoilerCanvas(
    textLayoutResult: TextLayoutResult?,
    spoilerRanges: List<IntRange>,
    revealedIndices: Set<Int>,
    motionEnabled: Boolean,
    onRevealIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    interactionEnabled: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    val layout = textLayoutResult ?: return
    if (spoilerRanges.isEmpty()) return

    val activeRegionsByIdx = remember(layout, spoilerRanges, revealedIndices) {
        buildMap {
            spoilerRanges.forEachIndexed { idx, range ->
                if (idx !in revealedIndices) {
                    val rects = computeSpoilerLineRects(layout, range)
                    if (rects.isNotEmpty()) put(idx, rects)
                }
            }
        }
    }

    val reveals = remember { mutableStateMapOf<Int, SpoilerRevealAnim>() }
    if (activeRegionsByIdx.isEmpty() && reveals.isEmpty()) return

    val particlesByIdx = remember(activeRegionsByIdx) {
        activeRegionsByIdx.mapValues { (_, rects) -> createParticlesForRects(rects) }
    }

    var frameClockMs by remember { mutableLongStateOf(0L) }
    val needsLoop = (motionEnabled && activeRegionsByIdx.isNotEmpty()) || reveals.isNotEmpty()

    LaunchedEffect(needsLoop, activeRegionsByIdx) {
        if (!needsLoop) return@LaunchedEffect
        var lastMs = 0L
        while (true) {
            withFrameMillis { nowMs ->
                val dt = if (lastMs == 0L) 0.016f else ((nowMs - lastMs) / 1000f).coerceIn(0.001f, 0.05f)
                lastMs = nowMs

                if (motionEnabled) {
                    particlesByIdx.forEach { (idx, list) ->
                        val rects = activeRegionsByIdx[idx] ?: return@forEach
                        advanceParticles(list, rects, dt)
                    }
                }

                if (reveals.isNotEmpty()) {
                    val finished = mutableListOf<Int>()
                    reveals.forEach { (idx, anim) ->
                        if (nowMs - anim.startMs >= REVEAL_DURATION_MS) {
                            finished.add(idx)
                        }
                    }
                    finished.forEach { idx ->
                        reveals.remove(idx)
                        onRevealIndex(idx)
                    }
                }
                frameClockMs = nowMs
            }
        }
    }

    val baseColor = scheme.accent.copy(alpha = 0.24f)
    val particleColor = scheme.textPrimary

    Canvas(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .then(
                if (interactionEnabled) {
                    Modifier.pointerInput(activeRegionsByIdx) {
                        detectTapGestures { tapOffset ->
                            activeRegionsByIdx.forEach { (idx, rects) ->
                                if (idx !in reveals && rects.any { it.contains(tapOffset) }) {
                                    val union = rects.reduce { acc, r ->
                                        Rect(
                                            left = minOf(acc.left, r.left),
                                            top = minOf(acc.top, r.top),
                                            right = maxOf(acc.right, r.right),
                                            bottom = maxOf(acc.bottom, r.bottom)
                                        )
                                    }
                                    val maxR = hypot(union.width, union.height) * 1.25f
                                    reveals[idx] = SpoilerRevealAnim(
                                        center = tapOffset,
                                        maxRadius = maxR.coerceAtLeast(32f),
                                        startMs = android.os.SystemClock.uptimeMillis()
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        val nowMs = frameClockMs.takeIf { it > 0L } ?: (android.os.SystemClock.uptimeMillis())
        val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        activeRegionsByIdx.forEach { (idx, rects) ->
            rects.forEach { r ->
                drawRoundRect(
                    color = baseColor,
                    topLeft = Offset(r.left, r.top),
                    size = Size(r.width, r.height),
                    cornerRadius = corner
                )
            }
            particlesByIdx[idx]?.forEachIndexed { pIdx, p ->
                val twinkle = 0.65f + 0.35f * kotlin.math.sin(nowMs * 0.005f + pIdx * 0.7f).toFloat()
                drawCircle(
                    color = particleColor.copy(alpha = (p.alpha * twinkle).coerceIn(0.18f, 0.98f)),
                    radius = p.radiusPx * density,
                    center = Offset(p.x, p.y)
                )
            }
            reveals[idx]?.let { anim ->
                val p = ((nowMs - anim.startMs).toFloat() / REVEAL_DURATION_MS).coerceIn(0f, 1f)
                val eased = 1f - (1f - p) * (1f - p) * (1f - p)
                drawCircle(
                    color = Color.Black,
                    radius = anim.maxRadius * eased,
                    center = anim.center,
                    blendMode = BlendMode.Clear
                )
            }
        }
    }
}

@Composable
fun TelegramBlurredMediaSpoiler(
    hasSpoiler: Boolean,
    isRevealed: Boolean,
    motionEnabled: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var revealAnim by remember { mutableStateOf<SpoilerRevealAnim?>(null) }
    val showSpoiler = hasSpoiler && !isRevealed

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(if (showSpoiler) Modifier.blur(26.dp) else Modifier),
            content = content
        )

        if (showSpoiler) {
            var boxSize by remember { mutableStateOf(Size.Zero) }
            val particles = remember(boxSize) {
                if (boxSize.width <= 0f || boxSize.height <= 0f) emptyList()
                else createParticlesForRects(listOf(Rect(0f, 0f, boxSize.width, boxSize.height)), maxCount = 180)
            }
            var frameClockMs by remember { mutableLongStateOf(0L) }

            LaunchedEffect(motionEnabled, revealAnim, particles.size) {
                if ((!motionEnabled && revealAnim == null) || particles.isEmpty()) return@LaunchedEffect
                var lastMs = 0L
                while (true) {
                    withFrameMillis { nowMs ->
                        val dt = if (lastMs == 0L) 0.016f else ((nowMs - lastMs) / 1000f).coerceIn(0.001f, 0.05f)
                        lastMs = nowMs
                        if (motionEnabled && boxSize.width > 0f) {
                            advanceParticles(particles, listOf(Rect(0f, 0f, boxSize.width, boxSize.height)), dt)
                        }
                        revealAnim?.let { anim ->
                            if (nowMs - anim.startMs >= REVEAL_DURATION_MS) {
                                revealAnim = null
                                onReveal()
                            }
                        }
                        frameClockMs = nowMs
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            if (revealAnim == null) {
                                val maxR = hypot(size.width.toFloat(), size.height.toFloat()) * 1.15f
                                revealAnim = SpoilerRevealAnim(
                                    center = tapOffset,
                                    maxRadius = maxR.coerceAtLeast(64f),
                                    startMs = android.os.SystemClock.uptimeMillis()
                                )
                            }
                        }
                    }
            ) {
                if (boxSize != size) boxSize = size
                drawRect(color = scheme.background.copy(alpha = 0.25f))
                particles.forEach { p ->
                    drawCircle(
                        color = scheme.textPrimary.copy(alpha = p.alpha),
                        radius = p.radiusPx * density,
                        center = Offset(p.x, p.y)
                    )
                }
                revealAnim?.let { anim ->
                    val nowMs = frameClockMs.takeIf { it > 0L } ?: (android.os.SystemClock.uptimeMillis())
                    val p = ((nowMs - anim.startMs).toFloat() / REVEAL_DURATION_MS).coerceIn(0f, 1f)
                    val eased = 1f - (1f - p) * (1f - p) * (1f - p)
                    drawCircle(
                        color = Color.Black,
                        radius = anim.maxRadius * eased,
                        center = anim.center,
                        blendMode = BlendMode.Clear
                    )
                }
            }
        }
    }
}

private fun computeSpoilerLineRects(layout: TextLayoutResult, range: IntRange): List<Rect> {
    val len = layout.layoutInput.text.length
    if (len == 0) return emptyList()
    val start = range.first.coerceIn(0, len)
    val end = (range.last + 1).coerceIn(start, len)
    if (start >= end) return emptyList()

    val startLine = layout.getLineForOffset(start)
    val endLine = layout.getLineForOffset((end - 1).coerceAtLeast(start))
    val rects = ArrayList<Rect>(endLine - startLine + 1)

    for (line in startLine..endLine) {
        val rawLeft = if (line == startLine) layout.getHorizontalPosition(start, true) else layout.getLineLeft(line)
        val rawRight = if (line == endLine) layout.getHorizontalPosition(end, false) else layout.getLineRight(line)
        val left = minOf(rawLeft, rawRight)
        val right = maxOf(rawLeft, rawRight)
        val top = layout.getLineTop(line) + 2f
        val bottom = layout.getLineBottom(line) - 2f
        if (right > left + 1f && bottom > top + 1f) {
            rects.add(Rect(left, top, right, bottom))
        }
    }
    return rects
}

private fun createParticlesForRects(rects: List<Rect>, maxCount: Int = 150): List<SpoilerParticle> {
    val totalArea = rects.sumOf { (it.width * it.height).toDouble() }.toFloat()
    if (totalArea <= 0f) return emptyList()
    val count = (totalArea / 180f).roundToIntCompat().coerceIn(18, maxCount)
    val rand = Random(rects.hashCode())
    return List(count) {
        val rIdx = rand.nextInt(rects.size)
        val r = rects[rIdx]
        SpoilerParticle(
            x = r.left + rand.nextFloat() * r.width,
            y = r.top + rand.nextFloat() * r.height,
            vx = (rand.nextFloat() - 0.5f) * 90f,
            vy = (rand.nextFloat() - 0.5f) * 90f,
            radiusPx = 0.75f + rand.nextFloat() * 1.25f,
            alpha = 0.35f + rand.nextFloat() * 0.63f,
            regionIdx = rIdx
        )
    }
}

private fun advanceParticles(particles: List<SpoilerParticle>, rects: List<Rect>, dt: Float) {
    if (rects.isEmpty()) return
    for (p in particles) {
        val r = rects[p.regionIdx % rects.size]
        p.x += p.vx * dt
        p.y += p.vy * dt
        if (p.x < r.left || p.x > r.right || p.y < r.top || p.y > r.bottom) {
            p.regionIdx = Random.nextInt(rects.size)
            val nextR = rects[p.regionIdx]
            p.x = nextR.left + Random.nextFloat() * nextR.width
            p.y = nextR.top + Random.nextFloat() * nextR.height
            p.vx = (Random.nextFloat() - 0.5f) * 90f
            p.vy = (Random.nextFloat() - 0.5f) * 90f
        }
    }
}

private fun Float.roundToIntCompat(): Int = kotlin.math.round(this).toInt()
