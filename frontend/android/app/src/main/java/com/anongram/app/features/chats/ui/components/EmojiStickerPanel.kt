package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.attachmentColor

private enum class EmojiSection(val label: String) {
    EMOJI("Эмодзи"),
    GIF("GIF"),
    STICKERS("Стикеры")
}

private val EMOJI_LIST = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥲", "☺️",
    "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗",
    "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓",
    "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕",
    "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤",
    "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰",
    "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✌️", "🤞",
    "🤟", "🤘", "👌", "🤌", "🤏", "👈", "👉", "👆", "👇", "☝️",
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
    "🔥", "✨", "🎉", "🚀", "⚡️", "💥", "💯", "🦾", "👾", "🛡️"
)

private val STICKER_PACKS = listOf("⭐", "🔥", "🐱", "🐶", "🚀", "👾", "🤖")
private val STICKER_SYMBOLS = listOf("🛡️", "⚡️", "🧅", "🔑", "💻", "🤖", "🚀", "✨", "🛰️", "🛸", "🕶️", "🦾")

/**
 * Панель эмодзи, GIF и стикеров (шаг 6).
 * Открывается под полем ввода.
 * Внизу переключатель: Эмодзи, GIF, Стикеры.
 */
@Composable
internal fun EmojiStickerPanel(
    onEmojiClick: (String) -> Unit,
    onStickerClick: (symbol: String, colorSeed: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSection by remember { mutableStateOf(EmojiSection.EMOJI) }
    val scheme = LocalAnonGramScheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(SurfaceDark)
    ) {
        // Контент секции
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeSection) {
                EmojiSection.EMOJI -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 40.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(EMOJI_LIST) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { onEmojiClick(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
                EmojiSection.STICKERS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Лента наборов
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            STICKER_PACKS.forEach { pack ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CardDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = pack, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Создать", tint = scheme.accent, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Настройки", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Сетка стикеров
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(STICKER_SYMBOLS) { symbol ->
                                val seed = symbol.hashCode()
                                Box(
                                    modifier = Modifier
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(attachmentColor(seed))
                                        .clickable { onStickerClick(symbol, seed) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = symbol, fontSize = 32.sp)
                                }
                            }
                        }
                    }
                }
                EmojiSection.GIF -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "GIF галерея (скоро)", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        // Внизу переключатель разделов: Эмодзи | GIF | Стикеры
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IslandBackground)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiSection.values().forEach { section ->
                val isSelected = section == activeSection
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) scheme.accent.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { activeSection = section }
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = section.label,
                        color = if (isSelected) scheme.accent else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
