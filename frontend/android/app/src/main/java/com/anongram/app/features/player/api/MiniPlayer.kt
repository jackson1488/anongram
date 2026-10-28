package com.anongram.app.features.player.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anongram.app.features.player.ui.MiniPlayerContent

/**
 * Плавающая таблетка мини-плеера (ТЗ, player: @Composable MiniPlayer(onExpand)).
 * Показывается и в окне вложений, и в чате под шапкой.
 * Слева пауза/play, по центру "Исполнитель - Название", справа крестик, снизу фиолетовая линия.
 */
@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    MiniPlayerContent(
        onExpand = onExpand,
        modifier = modifier
    )
}
