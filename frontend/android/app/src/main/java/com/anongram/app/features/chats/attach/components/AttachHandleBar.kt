package com.anongram.app.features.chats.attach.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Эргономичная ручка шторки для раздела «Геопозиция»:
 * Легко нащупываемая видимая полоска 36x4 dp со скруглёнными краями
 * в удобной тач-зоне 28 dp для уверенного перетаскивания шторки над картой.
 */
@Composable
fun AttachHandleBar(
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(scheme.textMuted.copy(alpha = 0.38f))
        )
    }
}
