package com.anongram.app.features.chats.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.anongram.app.core.ui.AppMotionDurationScale
import com.anongram.app.features.settings.animations.data.MessageDeleteStyle
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Immutable
data class CapturedBubbleEntry(
    val layer: GraphicsLayer,
    val boundsInWindow: Rect,
    val isIncoming: Boolean
)

private data class DisintegrateParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color,
    val vx: Float,
    val vy: Float,
    val startAt: Float,
    val lifeSpan: Float
)

private data class PuzzleTile(
    val srcX: Int,
    val srcY: Int,
    val srcW: Int,
    val srcH: Int,
    val dx: Float,
    val dy: Float,
    val rotationDeg: Float,
    val startAt: Float
)

data class ActiveDeleteAnimation(
    val id: String,
    val bitmap: ImageBitmap,
    val boundsInWindow: Rect,
    val isIncoming: Boolean,
    val style: MessageDeleteStyle,
    val speedMultiplier: Float
)

/**
 * Реестр активных баблов на экране для точного захвата их `ImageBitmap` и координат перед удалением.
 */
class MessageSnapshotRegistry {
    private val entries = HashMap<String, CapturedBubbleEntry>()
    val activeDeletes = mutableStateListOf<ActiveDeleteAnimation>()

    fun register(messageId: String, layer: GraphicsLayer, boundsInWindow: Rect, isIncoming: Boolean) {
        entries[messageId] = CapturedBubbleEntry(layer, boundsInWindow, isIncoming)
    }

    fun unregister(messageId: String) {
        entries.remove(messageId)
    }

    suspend fun triggerDeleteAnimations(
        targetKeys: List<String>,
        style: MessageDeleteStyle,
        speedMultiplier: Float,
        reduceMotion: Boolean
    ) {
        if (reduceMotion || style == MessageDeleteStyle.NONE) return
        val fullIds = targetKeys.filter { "#" !in it }.distinct()
        for (msgId in fullIds) {
            val entry = entries[msgId] ?: continue
            if (entry.boundsInWindow.width <= 1f || entry.boundsInWindow.height <= 1f) continue
            val bitmap = runCatching { entry.layer.toImageBitmap() }.getOrNull() ?: continue
            if (bitmap.width <= 1 || bitmap.height <= 1) continue
            activeDeletes.add(
                ActiveDeleteAnimation(
                    id = "${msgId}_${System.nanoTime()}",
                    bitmap = bitmap,
                    boundsInWindow = entry.boundsInWindow,
                    isIncoming = entry.isIncoming,
                    style = style,
                    speedMultiplier = speedMultiplier
                )
            )
        }
    }

    fun finish(animId: String) {
        activeDeletes.removeAll { it.id == animId }
    }
}

val LocalMessageSnapshotRegistry = staticCompositionLocalOf<MessageSnapshotRegistry?> { null }

@Composable
fun rememberMessageSnapshotRegistry(): MessageSnapshotRegistry {
    return remember { MessageSnapshotRegistry() }
}

/**
 * Оверлей поверх чата, отрисовывающий распад на частицы (Щелчок Таноса), Пазл или Уезд
 * строго в координатах и размерах удалённого бабла.
 */
@Composable
fun MessageDeleteOverlay(
    registry: MessageSnapshotRegistry,
    modifier: Modifier = Modifier
) {
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInWindow() }
    ) {
        registry.activeDeletes.forEach { anim ->
            androidx.compose.runtime.key(anim.id) {
                SingleMessageDeleteEffect(
                    anim = anim,
                    overlayOrigin = overlayOrigin,
                    onFinished = { registry.finish(anim.id) }
                )
            }
        }
    }
}

@Composable
private fun SingleMessageDeleteEffect(
    anim: ActiveDeleteAnimation,
    overlayOrigin: Offset,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(anim.id) { Animatable(0f) }

    val particles = remember(anim.id) {
        if (anim.style == MessageDeleteStyle.PARTICLES) {
            sampleParticles(anim.bitmap, anim.isIncoming)
        } else {
            emptyList()
        }
    }

    val puzzleTiles = remember(anim.id) {
        if (anim.style == MessageDeleteStyle.PUZZLE) {
            buildPuzzleTiles(anim.bitmap.width, anim.bitmap.height, anim.id.hashCode())
        } else {
            emptyList()
        }
    }

    LaunchedEffect(anim.id) {
        val mult = anim.speedMultiplier.coerceIn(0.4f, 2.0f)
        val durationMs = when (anim.style) {
            MessageDeleteStyle.PARTICLES -> (880 * mult).toInt().coerceAtLeast(280)
            MessageDeleteStyle.PUZZLE -> (620 * mult).toInt().coerceAtLeast(220)
            MessageDeleteStyle.SLIDE_OUT -> (320 * mult).toInt().coerceAtLeast(120)
            MessageDeleteStyle.NONE -> 0
        }
        if (durationMs > 0) {
            withContext(AppMotionDurationScale) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = durationMs,
                        easing = if (anim.style == MessageDeleteStyle.SLIDE_OUT) FastOutSlowInEasing else LinearEasing
                    )
                )
            }
        }
        onFinished()
    }

    val p = progress.value
    val left = anim.boundsInWindow.left - overlayOrigin.x
    val top = anim.boundsInWindow.top - overlayOrigin.y
    val width = anim.boundsInWindow.width
    val height = anim.boundsInWindow.height

    Canvas(modifier = modifier.fillMaxSize()) {
        when (anim.style) {
            MessageDeleteStyle.PARTICLES -> {
                for (pt in particles) {
                    val localT = ((p - pt.startAt) / pt.lifeSpan).coerceIn(0f, 1f)
                    if (localT >= 1f) continue
                    if (localT <= 0f) {
                        drawCircle(
                            color = pt.color,
                            radius = pt.size * 0.55f,
                            center = Offset(left + pt.x, top + pt.y)
                        )
                    } else {
                        val ease = localT * localT
                        val curX = left + pt.x + pt.vx * ease
                        val curY = top + pt.y + pt.vy * ease
                        val alpha = (1f - localT).coerceIn(0f, 1f)
                        val r = (pt.size * 0.55f * (1f - localT * 0.5f)).coerceAtLeast(0.8f)
                        drawCircle(
                            color = pt.color.copy(alpha = pt.color.alpha * alpha),
                            radius = r,
                            center = Offset(curX, curY)
                        )
                    }
                }
            }
            MessageDeleteStyle.PUZZLE -> {
                val scaleX = width / anim.bitmap.width.coerceAtLeast(1)
                val scaleY = height / anim.bitmap.height.coerceAtLeast(1)
                for (tile in puzzleTiles) {
                    val localT = ((p - tile.startAt) / (1f - tile.startAt).coerceAtLeast(0.25f)).coerceIn(0f, 1f)
                    if (localT >= 1f) continue
                    val ease = 1f - (1f - localT) * (1f - localT)
                    val dstX = left + tile.srcX * scaleX + tile.dx * ease
                    val dstY = top + tile.srcY * scaleY + tile.dy * ease
                    val dstW = (tile.srcW * scaleX).roundToInt().coerceAtLeast(1)
                    val dstH = (tile.srcH * scaleY).roundToInt().coerceAtLeast(1)
                    val cx = dstX + dstW / 2f
                    val cy = dstY + dstH / 2f
                    rotate(degrees = tile.rotationDeg * ease, pivot = Offset(cx, cy)) {
                        drawImage(
                            image = anim.bitmap,
                            srcOffset = IntOffset(tile.srcX, tile.srcY),
                            srcSize = IntSize(tile.srcW, tile.srcH),
                            dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
                            dstSize = IntSize(dstW, dstH),
                            alpha = (1f - localT).coerceIn(0f, 1f)
                        )
                    }
                }
            }
            MessageDeleteStyle.SLIDE_OUT -> {
                val dir = if (anim.isIncoming) -1f else 1f
                val dx = dir * width * 0.95f * p
                val alpha = (1f - p).coerceIn(0f, 1f)
                drawImage(
                    image = anim.bitmap,
                    dstOffset = IntOffset((left + dx).roundToInt(), top.roundToInt()),
                    dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
                    alpha = alpha
                )
            }
            MessageDeleteStyle.NONE -> Unit
        }
    }
}

private fun sampleParticles(bitmap: ImageBitmap, isIncoming: Boolean): List<DisintegrateParticle> {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 1 || h <= 1) return emptyList()

    val pixelMap = runCatching { bitmap.toPixelMap() }.getOrNull() ?: return emptyList()
    val step = max(4, min(w, h) / 42).coerceAtMost(14)
    val rnd = Random(w * 31 + h)
    val result = ArrayList<DisintegrateParticle>((w / step) * (h / step))

    var y = step / 2
    while (y < h) {
        var x = step / 2
        while (x < w) {
            val c = pixelMap[x, y]
            if (c.alpha > 0.12f) {
                val normX = x.toFloat() / w
                val wave = if (isIncoming) (1f - normX) else normX
                val startAt = (wave * 0.42f + rnd.nextFloat() * 0.14f).coerceIn(0f, 0.56f)
                val angle = (rnd.nextFloat() * 140f - 110f) * (PI / 180.0)
                val speed = 60f + rnd.nextFloat() * 160f
                val dirBias = if (isIncoming) -35f else 35f
                result.add(
                    DisintegrateParticle(
                        x = x.toFloat(),
                        y = y.toFloat(),
                        size = step * (0.95f + rnd.nextFloat() * 0.45f),
                        color = c,
                        vx = (cos(angle).toFloat() * speed) + dirBias,
                        vy = sin(angle).toFloat() * speed - 45f,
                        startAt = startAt,
                        lifeSpan = (0.42f + rnd.nextFloat() * 0.25f).coerceAtMost(1f - startAt)
                    )
                )
            }
            x += step
        }
        y += step
    }
    return result
}

private fun buildPuzzleTiles(width: Int, height: Int, seed: Int): List<PuzzleTile> {
    val cols = 5
    val rows = 3
    val tileW = (width / cols).coerceAtLeast(1)
    val tileH = (height / rows).coerceAtLeast(1)
    val rnd = Random(seed)
    val list = ArrayList<PuzzleTile>(cols * rows)

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val sx = c * tileW
            val sy = r * tileH
            val sw = if (c == cols - 1) (width - sx).coerceAtLeast(1) else tileW
            val sh = if (r == rows - 1) (height - sy).coerceAtLeast(1) else tileH
            val goRight = ((c + r) % 2 == 0)
            val dx = (if (goRight) 1f else -1f) * (90f + rnd.nextFloat() * 140f)
            val dy = (r - 1) * 45f + (rnd.nextFloat() * 70f - 35f)
            val rot = (if (goRight) 1f else -1f) * (8f + rnd.nextFloat() * 18f)
            val startAt = ((c + r) * 0.045f).coerceIn(0f, 0.35f)
            list.add(PuzzleTile(sx, sy, sw, sh, dx, dy, rot, startAt))
        }
    }
    return list
}
