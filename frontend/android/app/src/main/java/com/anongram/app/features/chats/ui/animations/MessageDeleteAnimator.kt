package com.anongram.app.features.chats.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.anongram.app.core.ui.AppMotionDurationScale
import com.anongram.app.features.settings.animations.data.MessageDeleteStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
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

private data class JigsawPiece(
    val path: Path,
    val centerX: Float,
    val centerY: Float,
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
    val speedMultiplier: Float,
    val bulkStepBoost: Int = 1
)

/**
 * Реестр активных баблов и их вложенных ячеек (`#photo_X`, `#file_X`, `#music_X`)
 * для точного захвата `ImageBitmap` и координат перед удалением.
 * Запись в `GraphicsLayer` активна только когда `isCaptureArmed == true` (меню, выделение, предпросмотр),
 * чтобы не нагружать GPU при обычном открытии чата и скролле.
 */
class MessageSnapshotRegistry {
    private val entries = HashMap<String, CapturedBubbleEntry>()
    val activeDeletes = mutableStateListOf<ActiveDeleteAnimation>()
    var isCaptureArmed by mutableStateOf(false)

    fun register(messageId: String, layer: GraphicsLayer, boundsInWindow: Rect, isIncoming: Boolean) {
        if (messageId.isBlank()) return
        entries[messageId] = CapturedBubbleEntry(layer, boundsInWindow, isIncoming)
    }

    fun unregister(messageId: String) {
        if (messageId.isBlank()) return
        entries.remove(messageId)
    }

    suspend fun triggerDeleteAnimations(
        targetKeys: List<String>,
        style: MessageDeleteStyle,
        speedMultiplier: Float,
        reduceMotion: Boolean
    ) {
        if (reduceMotion || style == MessageDeleteStyle.NONE) return
        val distinctTargets = targetKeys.distinct()
        val fullIds = distinctTargets.filter { "#" !in it }.toMutableSet()
        val subKeysByParent = distinctTargets.filter { "#" in it }.groupBy { it.substringBefore("#") }

        val resolvedKeys = LinkedHashSet<String>()
        resolvedKeys.addAll(fullIds)

        for ((parentId, subKeys) in subKeysByParent) {
            if (parentId in fullIds) continue
            val registeredChildren = entries.keys.filter { it.startsWith("$parentId#") }
            val allChildrenDeleted = registeredChildren.isNotEmpty() &&
                registeredChildren.all { it in subKeys } &&
                entries.containsKey(parentId)
            if (allChildrenDeleted) {
                resolvedKeys.add(parentId)
            } else {
                resolvedKeys.addAll(subKeys)
            }
        }

        val bulkStepBoost = when {
            resolvedKeys.size > 6 -> 3
            resolvedKeys.size > 3 -> 2
            else -> 1
        }

        for (key in resolvedKeys) {
            val entry = entries[key] ?: continue
            if (entry.boundsInWindow.width <= 1f || entry.boundsInWindow.height <= 1f) continue
            val bitmap = runCatching { entry.layer.toImageBitmap() }.getOrNull() ?: continue
            if (bitmap.width <= 1 || bitmap.height <= 1) continue
            activeDeletes.add(
                ActiveDeleteAnimation(
                    id = "${key}_${System.nanoTime()}",
                    bitmap = bitmap,
                    boundsInWindow = entry.boundsInWindow,
                    isIncoming = entry.isIncoming,
                    style = style,
                    speedMultiplier = speedMultiplier,
                    bulkStepBoost = bulkStepBoost
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
 * Оборачивает конкретный бабл или подэлемент (`#photo_X`, `#file_X`, `#music_X`)
 * для точного захвата его реальных границ и `GraphicsLayer` без лишних пустых отступов.
 */
@Composable
fun DeleteSnapshotBox(
    snapshotKey: String,
    isIncoming: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val registry = LocalMessageSnapshotRegistry.current
    if (registry == null || snapshotKey.isBlank() || !registry.isCaptureArmed) {
        Box(modifier = modifier) { content() }
        return
    }
    val layer = rememberGraphicsLayer()
    DisposableEffect(snapshotKey, registry) {
        onDispose { registry.unregister(snapshotKey) }
    }
    Box(
        modifier = modifier
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }
            .onGloballyPositioned { coordinates ->
                registry.register(
                    messageId = snapshotKey,
                    layer = layer,
                    boundsInWindow = coordinates.boundsInWindow(),
                    isIncoming = isIncoming
                )
            }
    ) {
        content()
    }
}

/**
 * Оверлей поверх чата (или предпросмотра), отрисовывающий распад на частицы (Щелчок Таноса),
 * адаптивный настоящий пазл с ушками/выемками или уезд строго в координатах удалённого бабла.
 */
@Composable
fun MessageDeleteOverlay(
    registry: MessageSnapshotRegistry,
    modifier: Modifier = Modifier
) {
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInWindow() }
    ) {
        registry.activeDeletes.forEach { anim ->
            key(anim.id) {
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
    val density = LocalDensity.current.density
    val progress = remember(anim.id) { Animatable(0f) }

    val width = anim.boundsInWindow.width
    val height = anim.boundsInWindow.height

    val particles by produceState<List<DisintegrateParticle>?>(
        initialValue = if (anim.style == MessageDeleteStyle.PARTICLES) null else emptyList(),
        anim.id,
        density
    ) {
        if (anim.style == MessageDeleteStyle.PARTICLES) {
            value = withContext(Dispatchers.Default) {
                sampleParticles(anim.bitmap, anim.isIncoming, anim.bulkStepBoost, density)
            }
        }
    }

    val jigsawPieces = remember(anim.id, width, height, density) {
        if (anim.style == MessageDeleteStyle.PUZZLE) {
            buildAdaptiveJigsawPieces(
                width = width,
                height = height,
                density = density,
                seed = anim.id.hashCode()
            )
        } else {
            emptyList()
        }
    }

    val isReady = particles != null
    LaunchedEffect(anim.id, isReady) {
        if (!isReady) return@LaunchedEffect
        val mult = anim.speedMultiplier.coerceIn(0.4f, 2.0f)
        val durationMs = when (anim.style) {
            MessageDeleteStyle.PARTICLES -> (840 * mult).toInt().coerceAtLeast(260)
            MessageDeleteStyle.PUZZLE -> (680 * mult).toInt().coerceAtLeast(240)
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

    Canvas(modifier = modifier.fillMaxSize()) {
        when (anim.style) {
            MessageDeleteStyle.PARTICLES -> {
                val activeParticles = particles
                if (activeParticles == null) {
                    drawImage(
                        image = anim.bitmap,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
                    )
                    return@Canvas
                }
                val waveFront = (p / 0.46f).coerceIn(0f, 1f)
                if (waveFront < 1f) {
                    val clipLeft = if (anim.isIncoming) left else left + width * waveFront
                    val clipRight = if (anim.isIncoming) left + width * (1f - waveFront) else left + width
                    if (clipRight > clipLeft + 0.5f) {
                        clipRect(
                            left = clipLeft,
                            top = top,
                            right = clipRight,
                            bottom = top + height
                        ) {
                            drawImage(
                                image = anim.bitmap,
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
                            )
                        }
                    }
                }
                val scaleX = width / anim.bitmap.width.coerceAtLeast(1)
                val scaleY = height / anim.bitmap.height.coerceAtLeast(1)
                for (pt in activeParticles) {
                    val localT = ((p - pt.startAt) / pt.lifeSpan).coerceIn(0f, 1f)
                    if (localT <= 0f || localT >= 1f) continue
                    val px = pt.x * scaleX
                    val py = pt.y * scaleY
                    val ease = localT * localT
                    val curX = left + px + pt.vx * ease
                    val curY = top + py + pt.vy * ease
                    val alpha = (1f - localT).coerceIn(0f, 1f)
                    val r = (pt.size * (1f - localT * 0.45f)).coerceAtLeast(0.65f)
                    drawCircle(
                        color = pt.color.copy(alpha = pt.color.alpha * alpha),
                        radius = r,
                        center = Offset(curX, curY)
                    )
                }
            }
            MessageDeleteStyle.PUZZLE -> {
                val dstSize = IntSize(
                    width.roundToInt().coerceAtLeast(1),
                    height.roundToInt().coerceAtLeast(1)
                )
                for (piece in jigsawPieces) {
                    val localT = ((p - piece.startAt) / (1f - piece.startAt).coerceAtLeast(0.28f)).coerceIn(0f, 1f)
                    if (localT >= 1f) continue
                    val ease = 1f - (1f - localT) * (1f - localT)
                    val alpha = (1f - localT).coerceIn(0f, 1f)
                    val curDx = piece.dx * ease
                    val curDy = piece.dy * ease

                    translate(left = left + curDx, top = top + curDy) {
                        rotate(
                            degrees = piece.rotationDeg * ease,
                            pivot = Offset(piece.centerX, piece.centerY)
                        ) {
                            clipPath(piece.path) {
                                drawImage(
                                    image = anim.bitmap,
                                    dstOffset = IntOffset.Zero,
                                    dstSize = dstSize,
                                    alpha = alpha
                                )
                            }
                            if (ease > 0.01f) {
                                drawPath(
                                    path = piece.path,
                                    color = Color.White.copy(alpha = (0.28f * alpha).coerceIn(0f, 0.28f)),
                                    style = Stroke(width = 1.4f)
                                )
                            }
                        }
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

private fun sampleParticles(
    bitmap: ImageBitmap,
    isIncoming: Boolean,
    bulkStepBoost: Int,
    density: Float
): List<DisintegrateParticle> {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 1 || h <= 1) return emptyList()

    val pixelMap = runCatching { bitmap.toPixelMap() }.getOrNull() ?: return emptyList()
    val maxParticles = when {
        bulkStepBoost >= 3 -> 380
        bulkStepBoost == 2 -> 560
        else -> 950
    }
    val specksPerCell = if (bulkStepBoost > 1) 1 else 2
    val targetCells = (maxParticles / specksPerCell).coerceAtLeast(120)
    val dynamicStep = sqrt((w.toFloat() * h.toFloat()) / targetCells).roundToInt().coerceAtLeast(6)
    val step = max(6, dynamicStep)
    val safeDensity = density.coerceIn(1f, 4f)
    val rnd = Random(w * 31 + h)
    val result = ArrayList<DisintegrateParticle>(maxParticles.coerceAtMost(((w / step) + 1) * ((h / step) + 1) * specksPerCell))

    var y = step / 2
    while (y < h) {
        var x = step / 2
        while (x < w) {
            val c = pixelMap[x, y]
            if (c.alpha > 0.12f) {
                val normX = x.toFloat() / w
                val wave = if (isIncoming) (1f - normX) else normX
                for (k in 0 until specksPerCell) {
                    val jx = (rnd.nextFloat() - 0.5f) * step * 0.85f
                    val jy = (rnd.nextFloat() - 0.5f) * step * 0.85f
                    val startAt = (wave * 0.44f + (rnd.nextFloat() - 0.5f) * 0.06f).coerceIn(0f, 0.48f)
                    val angle = (rnd.nextFloat() * 140f - 110f) * (PI / 180.0)
                    val speed = (48f + rnd.nextFloat() * 125f) * safeDensity * 0.55f
                    val dirBias = (if (isIncoming) -28f else 28f) * safeDensity * 0.55f
                    val dustRadius = safeDensity * (0.75f + rnd.nextFloat() * 0.95f)
                    result.add(
                        DisintegrateParticle(
                            x = (x + jx).coerceIn(0f, w.toFloat()),
                            y = (y + jy).coerceIn(0f, h.toFloat()),
                            size = dustRadius,
                            color = c,
                            vx = (cos(angle).toFloat() * speed) + dirBias,
                            vy = sin(angle).toFloat() * speed - 30f * safeDensity * 0.55f,
                            startAt = startAt,
                            lifeSpan = (0.40f + rnd.nextFloat() * 0.24f).coerceAtMost(1f - startAt)
                        )
                    )
                }
            }
            x += step
        }
        y += step
    }
    return result
}

/**
 * Вычисляет адаптивную сетку пазла (`cols x rows`) по реальным габаритам сообщения в dp.
 * Целевой размер кусочка ~48 dp (`44–56 dp`): маленькие баблы дают `3×2`, крупные/альбомы — до `7×8`.
 */
internal fun computeAdaptivePuzzleGrid(widthPx: Float, heightPx: Float, density: Float): Pair<Int, Int> {
    val safeDensity = density.coerceAtLeast(1f)
    val widthDp = widthPx / safeDensity
    val heightDp = heightPx / safeDensity
    val cols = (widthDp / 48f).roundToInt().coerceIn(3, 7)
    val rows = (heightDp / 48f).roundToInt().coerceIn(2, 8)
    return cols to rows
}

private fun buildAdaptiveJigsawPieces(
    width: Float,
    height: Float,
    density: Float,
    seed: Int
): List<JigsawPiece> {
    if (width <= 2f || height <= 2f) return emptyList()
    val (cols, rows) = computeAdaptivePuzzleGrid(width, height, density)
    val cellW = width / cols
    val cellH = height / rows
    val tabSize = min(cellW, cellH) * 0.22f
    val rnd = Random(seed)

    // Случайные (детерминированные) знаки замков на внутренних гранях: +1 = ушко, -1 = выемка
    val hEdges = Array(max(0, rows - 1)) { IntArray(cols) { if (rnd.nextBoolean()) 1 else -1 } }
    val vEdges = Array(rows) { IntArray(max(0, cols - 1)) { if (rnd.nextBoolean()) 1 else -1 } }

    val pieces = ArrayList<JigsawPiece>(cols * rows)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val x0 = c * cellW
            val y0 = r * cellH
            val x1 = if (c == cols - 1) width else (c + 1) * cellW
            val y1 = if (r == rows - 1) height else (r + 1) * cellH

            val topSign = if (r == 0) 0 else -hEdges[r - 1][c]
            val rightSign = if (c == cols - 1) 0 else vEdges[r][c]
            val bottomSign = if (r == rows - 1) 0 else hEdges[r][c]
            val leftSign = if (c == 0) 0 else -vEdges[r][c - 1]

            val path = buildJigsawPiecePath(
                x0 = x0,
                y0 = y0,
                x1 = x1,
                y1 = y1,
                topSign = topSign,
                rightSign = rightSign,
                bottomSign = bottomSign,
                leftSign = leftSign,
                tabSize = tabSize
            )

            val normX = (c + 0.5f) / cols - 0.5f
            val normY = (r + 0.5f) / rows - 0.5f
            val spreadX = normX * 220f + (rnd.nextFloat() * 90f - 45f)
            val spreadY = normY * 140f + (rnd.nextFloat() * 90f - 45f) - 18f
            val rot = (if (normX >= 0f) 1f else -1f) * (10f + rnd.nextFloat() * 22f)
            val startAt = (((c + r).toFloat() / (cols + rows).coerceAtLeast(1)) * 0.34f).coerceIn(0f, 0.34f)

            pieces.add(
                JigsawPiece(
                    path = path,
                    centerX = (x0 + x1) * 0.5f,
                    centerY = (y0 + y1) * 0.5f,
                    dx = spreadX,
                    dy = spreadY,
                    rotationDeg = rot,
                    startAt = startAt
                )
            )
        }
    }
    return pieces
}

private fun buildJigsawPiecePath(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    topSign: Int,
    rightSign: Int,
    bottomSign: Int,
    leftSign: Int,
    tabSize: Float
): Path {
    val path = Path()
    path.moveTo(x0, y0)
    // Верхняя грань: слева направо, внешняя нормаль (0, -1)
    addJigsawEdge(path, x0, y0, x1, y0, nx = 0f, ny = -1f, sign = topSign, tabSize = tabSize)
    // Правая грань: сверху вниз, внешняя нормаль (+1, 0)
    addJigsawEdge(path, x1, y0, x1, y1, nx = 1f, ny = 0f, sign = rightSign, tabSize = tabSize)
    // Нижняя грань: справа налево, внешняя нормаль (0, +1)
    addJigsawEdge(path, x1, y1, x0, y1, nx = 0f, ny = 1f, sign = bottomSign, tabSize = tabSize)
    // Левая грань: снизу вверх, внешняя нормаль (-1, 0)
    addJigsawEdge(path, x0, y1, x0, y0, nx = -1f, ny = 0f, sign = leftSign, tabSize = tabSize)
    path.close()
    return path
}

/**
 * Добавляет симметричный замок пазла (ушко при `sign = +1`, выемку при `sign = -1`, прямую при `sign = 0`).
 * Симметрия относительно середины ребра гарантирует стык соседних кусочков без зазоров.
 */
private fun addJigsawEdge(
    path: Path,
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
    nx: Float,
    ny: Float,
    sign: Int,
    tabSize: Float
) {
    if (sign == 0) {
        path.lineTo(bx, by)
        return
    }
    val dx = bx - ax
    val dy = by - ay
    val s = sign.toFloat() * tabSize

    fun pt(t: Float, n: Float): Offset = Offset(ax + dx * t + nx * n, ay + dy * t + ny * n)

    val p1 = pt(0.35f, 0f)
    path.lineTo(p1.x, p1.y)

    // Вход в шейку -> вершина ушка
    val c1a = pt(0.41f, 0f)
    val c1b = pt(0.37f, s * 0.92f)
    val peak = pt(0.50f, s)
    path.cubicTo(c1a.x, c1a.y, c1b.x, c1b.y, peak.x, peak.y)

    // Вершина ушка -> выход к линии ребра
    val c2a = pt(0.63f, s * 0.92f)
    val c2b = pt(0.59f, 0f)
    val p2 = pt(0.65f, 0f)
    path.cubicTo(c2a.x, c2a.y, c2b.x, c2b.y, p2.x, p2.y)

    path.lineTo(bx, by)
}
