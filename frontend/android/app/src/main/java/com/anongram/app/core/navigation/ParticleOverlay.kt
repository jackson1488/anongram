package com.anongram.app.core.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders particle disintegration or assembly across the screen
 */
@Composable
fun ParticleOverlay(
    progress: Float,
    isExit: Boolean,
    style: ParticleStyle,
    modifier: Modifier = Modifier
) {
    val factor = if (isExit) progress else (1.0f - progress)
    if (factor <= 0.01f || factor >= 0.99f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val rand = Random(42) // deterministic seed for consistent particles
        val count = if (style == ParticleStyle.PUZZLE_PIECES) 48 else 88

        for (i in 0 until count) {
            val originX = (i % 8) * (size.width / 8f) + (size.width / 16f)
            val originY = (i / 8) * (size.height / 11f) + (size.height / 22f)

            val angle = rand.nextFloat() * 2f * PI.toFloat()
            val speed = (rand.nextFloat() * 180f + 60f) * factor
            val dx = originX + cos(angle) * speed
            val dy = originY + sin(angle) * speed - (factor * 120f) // upward draft

            val alpha = ((1.0f - factor) * 0.9f).coerceIn(0f, 1f)
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
                    val voxelSize = (rand.nextFloat() * 8f + 4f) * (1.0f - factor * 0.4f)
                    drawRect(
                        color = Color(0xFF10B981).copy(alpha = alpha),
                        topLeft = Offset(dx - voxelSize / 2, dy - voxelSize / 2),
                        size = Size(voxelSize, voxelSize)
                    )
                }
                ParticleStyle.ASH_EMBERS -> {
                    val radius = (rand.nextFloat() * 4f + 2f) * (1.0f - factor * 0.25f)
                    drawCircle(
                        color = Color(0xFFFF5722).copy(alpha = alpha * 0.95f),
                        radius = radius,
                        center = Offset(dx, dy)
                    )
                }
                ParticleStyle.VAPOR_DUST -> {
                    val radius = (rand.nextFloat() * 3f + 1.5f) * (1.0f - factor * 0.3f)
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
