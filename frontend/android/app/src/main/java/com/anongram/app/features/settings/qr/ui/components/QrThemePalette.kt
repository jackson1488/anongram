package com.anongram.app.features.settings.qr.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Элемент цветовой темы для экрана QR-кода.
 * @param id Уникальный порядковый номер цвета (0..83).
 * @param color Основной цвет плашки и акцента QR-кода.
 * @param gradient Цвета градиента для фона экрана и экспортируемого изображения.
 */
data class QrThemeItem(
    val id: Int,
    val color: Color,
    val gradient: List<Color>
)

object QrThemePalette {

    val items: List<QrThemeItem> = listOf(
        // Ряд 1: Тёмные / Графитовые / Сланец
        QrThemeItem(0, Color(0xFF18181B), listOf(Color(0xFF24242A), Color(0xFF141416), Color(0xFF09090B))),
        QrThemeItem(1, Color(0xFF2D3139), listOf(Color(0xFF383D47), Color(0xFF24272D), Color(0xFF14161A))),
        QrThemeItem(2, Color(0xFF28353D), listOf(Color(0xFF33434C), Color(0xFF202A30), Color(0xFF11171B))),
        QrThemeItem(3, Color(0xFF334155), listOf(Color(0xFF40516A), Color(0xFF283445), Color(0xFF171E28))),
        QrThemeItem(4, Color(0xFF64748B), listOf(Color(0xFF74869F), Color(0xFF4C586B), Color(0xFF2A313C))),
        QrThemeItem(5, Color(0xFF8C96A8), listOf(Color(0xFF9EA9BD), Color(0xFF6B7587), Color(0xFF3B414D))),

        // Ряд 2: Красные / Коралловые / Терракота
        QrThemeItem(6, Color(0xFFD95B72), listOf(Color(0xFFE46E84), Color(0xFFB53F54), Color(0xFF5A1924))),
        QrThemeItem(7, Color(0xFFDC2626), listOf(Color(0xFFEF4444), Color(0xFFB91C1C), Color(0xFF5B0C0C))),
        QrThemeItem(8, Color(0xFFE11D48), listOf(Color(0xFFF43F5E), Color(0xFFBE123C), Color(0xFF5C071B))),
        QrThemeItem(9, Color(0xFFEA580C), listOf(Color(0xFFF97316), Color(0xFFC2410C), Color(0xFF5E1E04))),
        QrThemeItem(10, Color(0xFFF97316), listOf(Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF7C2D12))),
        QrThemeItem(11, Color(0xFFC2783D), listOf(Color(0xFFD48B52), Color(0xFFA15E2B), Color(0xFF543014))),

        // Ряд 3: Персиковые / Охра / Золото
        QrThemeItem(12, Color(0xFFE08A68), listOf(Color(0xFFEB9C7C), Color(0xFFBD6D4E), Color(0xFF5E3120))),
        QrThemeItem(13, Color(0xFFD98850), listOf(Color(0xFFE69A66), Color(0xFFB86B35), Color(0xFF5C3316))),
        QrThemeItem(14, Color(0xFFD99426), listOf(Color(0xFFE6A73D), Color(0xFFB57816), Color(0xFF593906))),
        QrThemeItem(15, Color(0xFFD97706), listOf(Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFF542502))),
        QrThemeItem(16, Color(0xFFA17A24), listOf(Color(0xFFB88E33), Color(0xFF826117), Color(0xFF453308))),
        QrThemeItem(17, Color(0xFF9C8E3E), listOf(Color(0xFFB0A24F), Color(0xFF7A6E2B), Color(0xFF3F3812))),

        // Ряд 4: Жёлто-зелёные / Оливковые / Зелёные
        QrThemeItem(18, Color(0xFFCCA028), listOf(Color(0xFFDEB53D), Color(0xFFA67F18), Color(0xFF574107))),
        QrThemeItem(19, Color(0xFFB5A838), listOf(Color(0xFFC7BC4E), Color(0xFF918625), Color(0xFF4A440E))),
        QrThemeItem(20, Color(0xFF7D9E43), listOf(Color(0xFF90B354), Color(0xFF627F2F), Color(0xFF324314))),
        QrThemeItem(21, Color(0xFF52A549), listOf(Color(0xFF68BD5E), Color(0xFF3C8534), Color(0xFF1B4516))),
        QrThemeItem(22, Color(0xFF4D8844), listOf(Color(0xFF5F9E56), Color(0xFF396B32), Color(0xFF1A3816))),
        QrThemeItem(23, Color(0xFF56A96E), listOf(Color(0xFF6CBD84), Color(0xFF3F8A55), Color(0xFF1B472A))),

        // Ряд 5: Мята / Бирюза / Небесный / Джинс
        QrThemeItem(24, Color(0xFF4BB97E), listOf(Color(0xFF63CCA4), Color(0xFF369B64), Color(0xFF154E30))),
        QrThemeItem(25, Color(0xFF38B2AC), listOf(Color(0xFF4FD1C5), Color(0xFF288C87), Color(0xFF104744))),
        QrThemeItem(26, Color(0xFF2DD4BF), listOf(Color(0xFF5EEAD4), Color(0xFF14B8A6), Color(0xFF0F5B52))),
        QrThemeItem(27, Color(0xFF0D9488), listOf(Color(0xFF14B8A6), Color(0xFF0F766E), Color(0xFF0A3B37))),
        QrThemeItem(28, Color(0xFF38BDF8), listOf(Color(0xFF7DD3FC), Color(0xFF0284C7), Color(0xFF034A70))),
        QrThemeItem(29, Color(0xFF4F75C2), listOf(Color(0xFF688EDB), Color(0xFF3B5B9E), Color(0xFF1C2E52))),

        // Ряд 6: Королевский синий / Индиго / Фиолетовый
        QrThemeItem(30, Color(0xFF2563EB), listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8), Color(0xFF0F2B75))),
        QrThemeItem(31, Color(0xFF6366F1), listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF252175))),
        QrThemeItem(32, Color(0xFF7C3AED), listOf(Color(0xFF9333EA), Color(0xFF6D28D9), Color(0xFF3B1278))),
        QrThemeItem(33, Color(0xFF9333EA), listOf(Color(0xFFA855F7), Color(0xFF7E22CE), Color(0xFF451073))),
        QrThemeItem(34, Color(0xFFA855F7), listOf(Color(0xFFC084FC), Color(0xFF9333EA), Color(0xFF521585))),
        QrThemeItem(35, Color(0xFFB026FF), listOf(Color(0xFFC457FF), Color(0xFF9105EB), Color(0xFF4C027E))),

        // Ряд 7: Маджента / Фуксия / Розовый / Пыльная роза
        QrThemeItem(36, Color(0xFF9D25B0), listOf(Color(0xFFBA3ECF), Color(0xFF7E168F), Color(0xFF430A4D))),
        QrThemeItem(37, Color(0xFFC026D3), listOf(Color(0xFFD946EF), Color(0xFFA21CAF), Color(0xFF560B5E))),
        QrThemeItem(38, Color(0xFFDB2777), listOf(Color(0xFFF472B6), Color(0xFFBE185D), Color(0xFF60082D))),
        QrThemeItem(39, Color(0xFFEC4899), listOf(Color(0xFFF472B6), Color(0xFFDB2777), Color(0xFF700D39))),
        QrThemeItem(40, Color(0xFF9F4A63), listOf(Color(0xFFB55D77), Color(0xFF7E3348), Color(0xFF431622))),
        QrThemeItem(41, Color(0xFFA8636E), listOf(Color(0xFFBC7782), Color(0xFF854752), Color(0xFF472228))),

        // Ряд 8: Cyberpunk & Neon
        QrThemeItem(42, Color(0xFF00F0FF), listOf(Color(0xFF38F9D7), Color(0xFF00B4D8), Color(0xFF032B43))),
        QrThemeItem(43, Color(0xFF00FF66), listOf(Color(0xFF69FF97), Color(0xFF00B042), Color(0xFF043414))),
        QrThemeItem(44, Color(0xFFFFE600), listOf(Color(0xFFFFF720), Color(0xFFD4AC0D), Color(0xFF423403))),
        QrThemeItem(45, Color(0xFFFF007F), listOf(Color(0xFFFF529A), Color(0xFFB8005A), Color(0xFF450021))),
        QrThemeItem(46, Color(0xFF9D00FF), listOf(Color(0xFFB54DFF), Color(0xFF6A00B0), Color(0xFF280044))),
        QrThemeItem(47, Color(0xFFFF6600), listOf(Color(0xFFFF8C3A), Color(0xFFCC4400), Color(0xFF4D1700))),

        // Ряд 9: Deep Space & Obsidian
        QrThemeItem(48, Color(0xFF0A0A0C), listOf(Color(0xFF1B1B22), Color(0xFF0E0E12), Color(0xFF050507))),
        QrThemeItem(49, Color(0xFF0F172A), listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF050811))),
        QrThemeItem(50, Color(0xFF064E3B), listOf(Color(0xFF047857), Color(0xFF064E3B), Color(0xFF021B14))),
        QrThemeItem(51, Color(0xFF311042), listOf(Color(0xFF581C87), Color(0xFF311042), Color(0xFF15041D))),
        QrThemeItem(52, Color(0xFF1E293B), listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF080C12))),
        QrThemeItem(53, Color(0xFF172554), listOf(Color(0xFF1E40AF), Color(0xFF172554), Color(0xFF070F26))),

        // Ряд 10: Luxury Gems & Jewelry
        QrThemeItem(54, Color(0xFFD4AF37), listOf(Color(0xFFF1D779), Color(0xFFA67C1E), Color(0xFF4A3408))),
        QrThemeItem(55, Color(0xFF9B111E), listOf(Color(0xFFD72638), Color(0xFF800C19), Color(0xFF3D030A))),
        QrThemeItem(56, Color(0xFF097969), listOf(Color(0xFF2E8B57), Color(0xFF097969), Color(0xFF04342C))),
        QrThemeItem(57, Color(0xFF0F52BA), listOf(Color(0xFF2A75D3), Color(0xFF0A3B85), Color(0xFF031633))),
        QrThemeItem(58, Color(0xFF6C3082), listOf(Color(0xFF9947B8), Color(0xFF512063), Color(0xFF230B2C))),
        QrThemeItem(59, Color(0xFFFFC87C), listOf(Color(0xFFFFDAA8), Color(0xFFC78B3E), Color(0xFF54340E))),

        // Ряд 11: Nature & Earth
        QrThemeItem(60, Color(0xFF8A9A5B), listOf(Color(0xFFA4B674), Color(0xFF6B7B42), Color(0xFF343C1D))),
        QrThemeItem(61, Color(0xFF2E5A44), listOf(Color(0xFF447D60), Color(0xFF244836), Color(0xFF11251B))),
        QrThemeItem(62, Color(0xFFB35434), listOf(Color(0xFFD47352), Color(0xFF8B3A1F), Color(0xFF45190B))),
        QrThemeItem(63, Color(0xFFC2B280), listOf(Color(0xFFD8CBA2), Color(0xFF998A5B), Color(0xFF4D4428))),
        QrThemeItem(64, Color(0xFF4E3629), listOf(Color(0xFF70503D), Color(0xFF3B281E), Color(0xFF1B110B))),
        QrThemeItem(65, Color(0xFFA0522D), listOf(Color(0xFFC46E44), Color(0xFF7E3D1F), Color(0xFF3C1A0B))),

        // Ряд 12: Nordic Pastels
        QrThemeItem(66, Color(0xFFB39DDB), listOf(Color(0xFFD1C4E9), Color(0xFF9575CD), Color(0xFF4527A0))),
        QrThemeItem(67, Color(0xFFA5D6A7), listOf(Color(0xFFC8E6C9), Color(0xFF81C784), Color(0xFF2E7D32))),
        QrThemeItem(68, Color(0xFFFFCCBC), listOf(Color(0xFFFFE0B2), Color(0xFFFFAB91), Color(0xFFBF360C))),
        QrThemeItem(69, Color(0xFF90CAF9), listOf(Color(0xFFBBDEFB), Color(0xFF64B5F6), Color(0xFF1565C0))),
        QrThemeItem(70, Color(0xFFF48FB1), listOf(Color(0xFFF8BBD0), Color(0xFFF06292), Color(0xFFAD1457))),
        QrThemeItem(71, Color(0xFFFFF59D), listOf(Color(0xFFFFF9C4), Color(0xFFFFF176), Color(0xFFF57F17))),

        // Ряд 13: Ocean & Marine
        QrThemeItem(72, Color(0xFF003B46), listOf(Color(0xFF07575B), Color(0xFF003B46), Color(0xFF001F24))),
        QrThemeItem(73, Color(0xFFFF6F61), listOf(Color(0xFFFF8A7A), Color(0xFFD94D40), Color(0xFF6E1F18))),
        QrThemeItem(74, Color(0xFF66CDAA), listOf(Color(0xFF8CE3C3), Color(0xFF45A583), Color(0xFF194C3B))),
        QrThemeItem(75, Color(0xFF4682B4), listOf(Color(0xFF6495ED), Color(0xFF31628C), Color(0xFF122C42))),
        QrThemeItem(76, Color(0xFF1F3A52), listOf(Color(0xFF325475), Color(0xFF162D42), Color(0xFF091624))),
        QrThemeItem(77, Color(0xFF008080), listOf(Color(0xFF20B2AA), Color(0xFF005A5A), Color(0xFF002929))),

        // Ряд 14: Sunset & Twilight
        QrThemeItem(78, Color(0xFFE63946), listOf(Color(0xFFFF5964), Color(0xFFBA202C), Color(0xFF59070E))),
        QrThemeItem(79, Color(0xFF6A0572), listOf(Color(0xFF8E0E99), Color(0xFF4F0355), Color(0xFF26012A))),
        QrThemeItem(80, Color(0xFFC70039), listOf(Color(0xFFE31B54), Color(0xFF930028), Color(0xFF420011))),
        QrThemeItem(81, Color(0xFF2A0845), listOf(Color(0xFF4E1678), Color(0xFF2A0845), Color(0xFF10021C))),
        QrThemeItem(82, Color(0xFFFFB703), listOf(Color(0xFFFFC72C), Color(0xFFD49200), Color(0xFF5E3F00))),
        QrThemeItem(83, Color(0xFF900C3F), listOf(Color(0xFFB81B57), Color(0xFF6B072D), Color(0xFF330114)))
    )

    val defaultTheme: QrThemeItem = items[0]

    private const val PREFS_NAME = "anongram_qr_theme_prefs"
    private const val KEY_THEME_ID = "selected_qr_theme_id"

    /**
     * Получить сохранённую пользователем тему QR-кода из постоянного хранилища.
     */
    fun getSavedTheme(context: Context): QrThemeItem {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getInt(KEY_THEME_ID, defaultTheme.id)
        return items.firstOrNull { it.id == savedId } ?: defaultTheme
    }

    /**
     * Сохранить выбранную тему QR-кода в постоянное хранилище (не сбрасывается при перезаходе).
     */
    fun saveTheme(context: Context, theme: QrThemeItem) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_ID, theme.id).apply()
    }
}

/**
 * Панель палитры расцветок (84 оттенка в 14 рядах по 6 колонок) с плавной прокруткой («листать можно»):
 * - Скруглённая полупрозрачная карточка с мягким матовым бордером iPhone Glass;
 * - Внутренний скролл позволяет легко и комфортно листать все 84 темы;
 * - Выделенный кружок имеет внешнее белое кольцо с отступом;
 * - Клик по любому кружку мгновенно активирует и сохраняет цветовую тему.
 */
@Composable
fun QrColorPaletteSheet(
    selectedTheme: QrThemeItem,
    onSelectTheme: (QrThemeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1E1E24).copy(alpha = 0.94f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Поглощает клики внутри палитры, предотвращая случайное закрытие */ }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val chunked = QrThemePalette.items.chunked(6)
            for (row in chunked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (item in row) {
                        val isSelected = item.id == selectedTheme.id
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, Color.White, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectTheme(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 32.dp else 38.dp)
                                    .clip(CircleShape)
                                    .background(item.color)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
