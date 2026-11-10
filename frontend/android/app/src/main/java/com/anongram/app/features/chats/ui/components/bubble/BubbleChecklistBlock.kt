package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.ChecklistItem

/**
 * Модульный компонент интерактивного чек-листа (☐ / ☑ / [ ] / [x]).
 * Позволяет отмечать пункты галочкой прямо в бабле чата с зачёркиванием выполненного текста.
 */
@Composable
fun BubbleChecklistBlock(
    items: List<ChecklistItem>,
    textSizeSp: Int = 15,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    // Локальное состояние отмеченных пунктов для мгновенной обратной связи при тапе
    val checkedState = remember(items) {
        mutableStateMapOf<Int, Boolean>().apply {
            items.forEach { put(it.id, it.isChecked) }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            val isChecked = checkedState[item.id] ?: item.isChecked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checkedState[item.id] = !isChecked }
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Крупный акцентный чек-бокс
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isChecked) scheme.accent else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (isChecked) scheme.accent else Color(0xFF94A3B8),
                            shape = RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Выполнено",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = item.text,
                    color = if (isChecked) Color(0xFF94A3B8) else Color.White,
                    fontSize = textSizeSp.sp,
                    lineHeight = (textSizeSp * 1.25f).sp,
                    fontWeight = FontWeight.Normal,
                    textDecoration = TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
