package com.anongram.app.features.player.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anongram.app.features.player.ui.FullPlayerSheetContent

/**
 * Нижнее окно полного плеера (ТЗ, player: @Composable FullPlayerSheet(onDismiss)).
 * Очередь треков, текущий трек, ползунок перемотки, повтор, пауза/play, вперёд/назад.
 */
@Composable
fun FullPlayerSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    FullPlayerSheetContent(
        onDismiss = onDismiss,
        modifier = modifier
    )
}
