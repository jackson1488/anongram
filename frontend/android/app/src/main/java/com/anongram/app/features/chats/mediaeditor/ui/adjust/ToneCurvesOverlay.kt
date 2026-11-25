package com.anongram.app.features.chats.mediaeditor.ui.adjust

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.anongram.app.features.chats.mediaeditor.engine.PhotoFilterEngine
import com.anongram.app.features.chats.mediaeditor.model.CurveChannel
import com.anongram.app.features.chats.mediaeditor.model.CurvePoint
import com.anongram.app.features.chats.mediaeditor.model.ToneCurvesState
import kotlin.math.hypot

@Composable
fun ToneCurvesOverlay(
    curves: ToneCurvesState,
    onCurvesChange: (ToneCurvesState) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel = curves.activeChannel
    val points = curves.pointsFor(activeChannel)
    var activePointIndex by remember { mutableIntStateOf(-1) }

    val channelColor = when (activeChannel) {
        CurveChannel.ALL -> Color.White
        CurveChannel.RED -> Color(0xFFFF453A)
        CurveChannel.GREEN -> Color(0xFF32D74B)
        CurveChannel.BLUE -> Color(0xFF0A84FF)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeChannel, points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val hitRadius = 28.dp.toPx()

                        var nearestIdx = -1
                        var nearestDist = Float.MAX_VALUE
                        points.forEachIndexed { idx, pt ->
                            val px = pt.x * w
                            val py = (1f - pt.y) * h
                            val d = hypot(offset.x - px, offset.y - py)
                            if (d < hitRadius && d < nearestDist) {
                                nearestDist = d
                                nearestIdx = idx
                            }
                        }

                        if (nearestIdx >= 0) {
                            activePointIndex = nearestIdx
                        } else {
                            // Add a new control point on tap/drag
                            val nx = (offset.x / w).coerceIn(0.03f, 0.97f)
                            val ny = (1f - offset.y / h).coerceIn(0f, 1f)
                            val updated = (points + CurvePoint(nx, ny)).sortedBy { it.x }
                            activePointIndex = updated.indexOfFirst { it.x == nx && it.y == ny }
                            onCurvesChange(curves.withPoints(activeChannel, updated))
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (activePointIndex !in points.indices) return@detectDragGestures
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val isFirst = activePointIndex == 0
                        val isLast = activePointIndex == points.lastIndex
                        val minX = if (isFirst) 0f else points[activePointIndex - 1].x + 0.02f
                        val maxX = if (isLast) 1f else points[activePointIndex + 1].x - 0.02f

                        val nx = if (isFirst) 0f else if (isLast) 1f else (change.position.x / w).coerceIn(minX, maxX)
                        val ny = (1f - change.position.y / h).coerceIn(0f, 1f)
                        val mutable = points.toMutableList()
                        mutable[activePointIndex] = CurvePoint(nx, ny)
                        onCurvesChange(curves.withPoints(activeChannel, mutable))
                    },
                    onDragEnd = { activePointIndex = -1 },
                    onDragCancel = { activePointIndex = -1 }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        // 4x4 grid (0.00..1.00)
        val gridColor = Color.White.copy(alpha = 0.22f)
        for (i in 1..3) {
            val x = w * (i / 4f)
            val y = h * (i / 4f)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
        }
        // Diagonal baseline
        drawLine(
            color = Color.White.copy(alpha = 0.28f),
            start = Offset(0f, h),
            end = Offset(w, 0f),
            strokeWidth = 1.dp.toPx()
        )

        // Draw smooth LUT curve
        val lut = PhotoFilterEngine.buildCurveLut(points)
        val curvePath = Path()
        for (i in 0..255 step 4) {
            val x = (i / 255f) * w
            val y = (1f - lut[i] / 255f) * h
            if (i == 0) curvePath.moveTo(x, y) else curvePath.lineTo(x, y)
        }
        curvePath.lineTo(w, (1f - lut[255] / 255f) * h)
        drawPath(
            path = curvePath,
            color = channelColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Control points
        points.forEach { pt ->
            val center = Offset(pt.x * w, (1f - pt.y) * h)
            drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = 8.dp.toPx(), center = center)
            drawCircle(color = channelColor, radius = 6.dp.toPx(), center = center)
        }
    }
}
