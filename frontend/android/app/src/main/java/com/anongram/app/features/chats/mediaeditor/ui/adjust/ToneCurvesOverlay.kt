package com.anongram.app.features.chats.mediaeditor.ui.adjust

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val currentCurves by rememberUpdatedState(curves)
    val currentPoints by rememberUpdatedState(points)
    val currentOnChange by rememberUpdatedState(onCurvesChange)
    var activePointIndex by remember { mutableIntStateOf(-1) }

    val channelColor = when (activeChannel) {
        CurveChannel.ALL -> Color.White
        CurveChannel.RED -> Color(0xFFFF453A)
        CurveChannel.GREEN -> Color(0xFF32D74B)
        CurveChannel.BLUE -> Color(0xFF0A84FF)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeChannel) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val hitRadius = 32.dp.toPx()
                            val pts = currentPoints

                            var nearestIdx = -1
                            var nearestDist = Float.MAX_VALUE
                            pts.forEachIndexed { idx, pt ->
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
                                val nx = (offset.x / w).coerceIn(0.03f, 0.97f)
                                val ny = (1f - offset.y / h).coerceIn(0f, 1f)
                                val updated = (pts + CurvePoint(nx, ny)).sortedBy { it.x }
                                activePointIndex = updated.indexOfFirst { it.x == nx && it.y == ny }
                                currentOnChange(currentCurves.withPoints(activeChannel, updated))
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pts = currentPoints
                            if (activePointIndex !in pts.indices) return@detectDragGestures
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            val isFirst = activePointIndex == 0
                            val isLast = activePointIndex == pts.lastIndex
                            val minX = if (isFirst) 0f else pts[activePointIndex - 1].x + 0.02f
                            val maxX = if (isLast) 1f else pts[activePointIndex + 1].x - 0.02f

                            val nx = if (isFirst) 0f else if (isLast) 1f else (change.position.x / w).coerceIn(minX, maxX)
                            val ny = (1f - change.position.y / h).coerceIn(0f, 1f)
                            val mutable = pts.toMutableList()
                            mutable[activePointIndex] = CurvePoint(nx, ny)
                            currentOnChange(currentCurves.withPoints(activeChannel, mutable))
                        },
                        onDragEnd = { activePointIndex = -1 },
                        onDragCancel = { activePointIndex = -1 }
                    )
                }
        ) {
            val w = size.width
            val h = size.height

            // Vertical 4-column grid lines (0.00, 0.25, 0.50, 0.75, 1.00)
            val gridColor = Color.White.copy(alpha = 0.28f)
            for (i in 1..3) {
                val x = w * (i / 4f)
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
            }

            // Smooth LUT curve line
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
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Control points
            points.forEach { pt ->
                val center = Offset(pt.x * w, (1f - pt.y) * h)
                drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 7.dp.toPx(), center = center)
                drawCircle(color = channelColor, radius = 5.dp.toPx(), center = center)
            }
        }

        // Bottom labels: 0.00  0.25  0.50  0.75  1.00 (matching Telegram screenshot 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0.00", "0.25", "0.50", "0.75", "1.00").forEach { lbl ->
                Text(
                    text = lbl,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
