package com.anongram.app.core.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ScreenJigsawPiece(
    val col: Int,
    val row: Int,
    val hEdgesTop: Int,
    val hEdgesBottom: Int,
    val vEdgesLeft: Int,
    val vEdgesRight: Int,
    val vx: Float,
    val vy: Float,
    val rotDeg: Float,
    val delay: Float,
    val popScale: Float
)

private fun buildScreenJigsawPath(
    x0: Float,
    y0: Float,
    w: Float,
    h: Float,
    topEdge: Int,
    rightEdge: Int,
    bottomEdge: Int,
    leftEdge: Int
): Path {
    val path = Path()
    val tab = minOf(w, h) * 0.22f
    path.moveTo(x0, y0)

    if (topEdge == 0) {
        path.lineTo(x0 + w, y0)
    } else {
        val dir = topEdge.toFloat()
        path.lineTo(x0 + w * 0.34f, y0)
        path.cubicTo(
            x0 + w * 0.40f, y0,
            x0 + w * 0.36f, y0 + dir * tab,
            x0 + w * 0.50f, y0 + dir * tab
        )
        path.cubicTo(
            x0 + w * 0.64f, y0 + dir * tab,
            x0 + w * 0.60f, y0,
            x0 + w * 0.66f, y0
        )
        path.lineTo(x0 + w, y0)
    }

    if (rightEdge == 0) {
        path.lineTo(x0 + w, y0 + h)
    } else {
        val dir = rightEdge.toFloat()
        path.lineTo(x0 + w, y0 + h * 0.34f)
        path.cubicTo(
            x0 + w, y0 + h * 0.40f,
            x0 + w + dir * tab, y0 + h * 0.36f,
            x0 + w + dir * tab, y0 + h * 0.50f
        )
        path.cubicTo(
            x0 + w + dir * tab, y0 + h * 0.64f,
            x0 + w, y0 + h * 0.60f,
            x0 + w, y0 + h * 0.66f
        )
        path.lineTo(x0 + w, y0 + h)
    }

    if (bottomEdge == 0) {
        path.lineTo(x0, y0 + h)
    } else {
        val dir = bottomEdge.toFloat()
        path.lineTo(x0 + w * 0.66f, y0 + h)
        path.cubicTo(
            x0 + w * 0.60f, y0 + h,
            x0 + w * 0.64f, y0 + h + dir * tab,
            x0 + w * 0.50f, y0 + h + dir * tab
        )
        path.cubicTo(
            x0 + w * 0.36f, y0 + h + dir * tab,
            x0 + w * 0.40f, y0 + h,
            x0 + w * 0.34f, y0 + h
        )
        path.lineTo(x0, y0 + h)
    }

    if (leftEdge == 0) {
        path.lineTo(x0, y0)
    } else {
        val dir = leftEdge.toFloat()
        path.lineTo(x0, y0 + h * 0.66f)
        path.cubicTo(
            x0, y0 + h * 0.60f,
            x0 + dir * tab, y0 + h * 0.64f,
            x0 + dir * tab, y0 + h * 0.50f
        )
        path.cubicTo(
            x0 + dir * tab, y0 + h * 0.36f,
            x0, y0 + h * 0.40f,
            x0, y0 + h * 0.34f
        )
        path.lineTo(x0, y0)
    }
    path.close()
    return path
}

/**
 * Рисует двунаправленный распад/сборку экрана:
 * - Для PUZZLE_PIECES при наличии [screenBitmap] режет реальный экран на 40 пазлов,
 *   которые разлетаются при выходе (0 -> 1) и собираются обратно при входе (1 -> 0).
 * - Для остальных стилей рисует волну испарения экрана + рой частиц.
 */
@Composable
fun ParticleOverlay(
    progress: Float,
    isExit: Boolean,
    style: ParticleStyle,
    screenBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    val factor = progress.coerceIn(0f, 1f)
    if (factor <= 0.005f || factor >= 0.995f) return

    val cols = 5
    val rows = 8
    val jigsawPieces = remember {
        val rnd = Random(77)
        val hEdges = Array(rows + 1) { r ->
            IntArray(cols) {
                if (r == 0 || r == rows) 0 else if (rnd.nextBoolean()) 1 else -1
            }
        }
        val vEdges = Array(rows) {
            IntArray(cols + 1) { c ->
                if (c == 0 || c == cols) 0 else if (rnd.nextBoolean()) 1 else -1
            }
        }
        buildList(cols * rows) {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val normX = (c + 0.5f) / cols - 0.5f
                    val normY = (r + 0.5f) / rows - 0.5f
                    val dirSign = if (normX >= 0f) 1f else -1f
                    add(
                        ScreenJigsawPiece(
                            col = c,
                            row = r,
                            hEdgesTop = hEdges[r][c],
                            hEdgesBottom = hEdges[r + 1][c],
                            vEdgesLeft = vEdges[r][c],
                            vEdgesRight = vEdges[r][c + 1],
                            vx = normX * 2.2f + dirSign * (0.25f + rnd.nextFloat() * 0.45f),
                            vy = normY * 1.6f + (rnd.nextFloat() - 0.5f) * 0.7f,
                            rotDeg = dirSign * (14f + rnd.nextFloat() * 34f),
                            delay = ((cols - 1 - c) * 0.03f) + rnd.nextFloat() * 0.14f,
                            popScale = 1.06f + rnd.nextFloat() * 0.10f
                        )
                    )
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val dstSize = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))

        if (style == ParticleStyle.PUZZLE_PIECES && screenBitmap != null) {
            val cellW = size.width / cols
            val cellH = size.height / rows
            for (piece in jigsawPieces) {
                val localT = ((factor - piece.delay) / (1f - piece.delay).coerceAtLeast(0.25f))
                    .coerceIn(0f, 1f)
                if (localT >= 0.99f) continue

                val eased = localT * localT * (3f - 2f * localT)
                val x0 = piece.col * cellW
                val y0 = piece.row * cellH
                val cx = x0 + cellW * 0.5f
                val cy = y0 + cellH * 0.5f

                val dx = piece.vx * size.width * 0.42f * eased
                val dy = piece.vy * size.height * 0.36f * eased
                val scale = if (eased < 0.35f) {
                    1f + (piece.popScale - 1f) * (eased / 0.35f)
                } else {
                    piece.popScale - (piece.popScale - 0.55f) * ((eased - 0.35f) / 0.65f)
                }
                val alpha = (1f - eased * eased).coerceIn(0f, 1f)
                val tilePath = buildScreenJigsawPath(
                    x0 = x0,
                    y0 = y0,
                    w = cellW,
                    h = cellH,
                    topEdge = piece.hEdgesTop,
                    rightEdge = piece.vEdgesRight,
                    bottomEdge = piece.hEdgesBottom,
                    leftEdge = piece.vEdgesLeft
                )

                withTransform({
                    translate(dx, dy)
                    rotate(piece.rotDeg * eased, pivot = Offset(cx, cy))
                    scale(scale, scale, pivot = Offset(cx, cy))
                }) {
                    clipPath(tilePath) {
                        drawImage(
                            image = screenBitmap,
                            dstSize = dstSize,
                            alpha = alpha
                        )
                    }
                    if (eased > 0.02f) {
                        drawPath(
                            path = tilePath,
                            color = Color.White.copy(alpha = 0.22f * alpha),
                            style = Stroke(width = 1.4f)
                        )
                    }
                }
            }
            return@Canvas
        }

        if (screenBitmap != null) {
            val waveRight = size.width * (1f - factor * 1.05f).coerceIn(0f, 1f)
            if (waveRight > 1f) {
                clipRect(left = 0f, top = 0f, right = waveRight, bottom = size.height) {
                    drawImage(
                        image = screenBitmap,
                        dstSize = dstSize,
                        alpha = (1f - factor * 0.35f).coerceIn(0f, 1f)
                    )
                }
            }
        }

        val rand = Random(42)
        val count = if (style == ParticleStyle.PUZZLE_PIECES) 48 else 96
        val waveX = size.width * (1f - factor)

        for (i in 0 until count) {
            val originX = if (screenBitmap != null) {
                (waveX + (rand.nextFloat() - 0.5f) * size.width * 0.25f).coerceIn(0f, size.width)
            } else {
                (i % 8) * (size.width / 8f) + (size.width / 16f)
            }
            val originY = (i / 12f) * (size.height / (count / 12f).coerceAtLeast(1f)) + rand.nextFloat() * 48f

            val angle = rand.nextFloat() * 2f * PI.toFloat()
            val speed = (rand.nextFloat() * 190f + 50f) * factor
            val dx = originX + cos(angle) * speed + (if (isExit) factor * 90f else -factor * 90f)
            val dy = originY + sin(angle) * speed - (factor * 110f)

            val alpha = ((1.0f - factor) * 0.92f).coerceIn(0f, 1f)
            if (alpha <= 0.01f) continue

            when (style) {
                ParticleStyle.PUZZLE_PIECES -> {
                    val rectSize = 22f * (1.0f - factor * 0.35f)
                    val rot = factor * (rand.nextFloat() - 0.5f) * 180f
                    rotate(rot, pivot = Offset(dx, dy)) {
                        drawRoundRect(
                            color = Color(0xFF00F2FE).copy(alpha = alpha * 0.8f),
                            topLeft = Offset(dx - rectSize / 2, dy - rectSize / 2),
                            size = Size(rectSize, rectSize),
                            cornerRadius = CornerRadius(6f, 6f),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
                ParticleStyle.DIGITAL_VOXELS -> {
                    val voxelSize = (rand.nextFloat() * 10f + 4f) * (1.0f - factor * 0.35f)
                    drawRect(
                        color = Color(0xFF10B981).copy(alpha = alpha),
                        topLeft = Offset(dx - voxelSize / 2, dy - voxelSize / 2),
                        size = Size(voxelSize, voxelSize)
                    )
                }
                ParticleStyle.ASH_EMBERS -> {
                    val radius = (rand.nextFloat() * 4.5f + 2f) * (1.0f - factor * 0.25f)
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = alpha),
                        radius = radius,
                        center = Offset(dx, dy)
                    )
                }
                ParticleStyle.VAPOR_DUST -> {
                    val radius = (rand.nextFloat() * 3.5f + 1.8f) * (1.0f - factor * 0.3f)
                    drawCircle(
                        color = Color(0xFF00F2FE).copy(alpha = alpha * 0.85f),
                        radius = radius,
                        center = Offset(dx, dy)
                    )
                }
            }
        }
    }
}
