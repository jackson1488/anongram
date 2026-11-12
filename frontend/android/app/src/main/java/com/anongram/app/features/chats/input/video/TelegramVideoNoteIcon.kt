package com.anongram.app.features.chats.input.video

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Фирменная иконка видео-сообщения (кружочка) в стиле Telegram:
 * Закруглённый корпус камеры с центральным круглым объективом и индикатором видоискателя.
 */
@Composable
fun TelegramVideoNoteIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        // Закруглённый корпус видеокамеры
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.16f),
            size = Size(w * 0.76f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.22f, h * 0.22f),
            style = Stroke(width = stroke)
        )
        // Круглый объектив камеры по центру
        drawCircle(
            color = tint,
            radius = w * 0.22f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = stroke)
        )
        // Точка-индикатор видоискателя в правом верхнем углу
        drawCircle(
            color = tint,
            radius = 1.4.dp.toPx(),
            center = Offset(w * 0.76f, h * 0.28f)
        )
    }
}
