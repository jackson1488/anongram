package com.anongram.app.core.theme

import androidx.compose.runtime.Immutable

/** Базовая полярность темы: Тёмная (включая Ч/Б и AMOLED) или Светлая (включая Б/Ч). */
enum class ThemePolarity {
    DARK,
    LIGHT
}

/** Глубина тонирования фонов и поверхностей. */
enum class ThemeTintMode {
    /** Строгий чёрно-белый (для DARK) или бело-чёрный (для LIGHT) без цветного шума. */
    MONOCHROME,
    /** Глубокий AMOLED/белый фон + цветной акцент и пузыри. */
    AMOLED_ACCENT,
    /** Мягкая тонировка поверхностей и карточек в оттенок темы. */
    SOFT_OS,
    /** Полная палитра как в кастомных ОС: весь экран, панели и инпут в гармонии с выбранным оттенком. */
    FULL_OS
}

/** Визуальный стиль исходящих пузырей сообщений. */
enum class BubbleColorStyle {
    GRADIENT,
    TINTED,
    OUTLINE
}

/**
 * Компактная спецификация темы для генератора палитр и цветового круга.
 * Сериализуется в компактную строку `AG1:...` для сохранения и обмена темами.
 */
@Immutable
data class ThemeSpec(
    val polarity: ThemePolarity = ThemePolarity.DARK,
    val tintMode: ThemeTintMode = ThemeTintMode.AMOLED_ACCENT,
    val bubbleStyle: BubbleColorStyle = BubbleColorStyle.TINTED,
    val hue: Float = 186f,
    val saturation: Float = 0.92f
) {
    fun encode(): String {
        val p = if (polarity == ThemePolarity.DARK) "D" else "L"
        val t = tintMode.ordinal
        val b = bubbleStyle.ordinal
        val h = ((hue % 360f) + 360f).mod(360f).toInt()
        val s = (saturation.coerceIn(0f, 1f) * 100f).toInt()
        return "AG1:$p:$t:$b:$h:$s"
    }

    companion object {
        fun decodeOrNull(raw: String): ThemeSpec? {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("AG1:")) return null
            val parts = trimmed.removePrefix("AG1:").split(":")
            if (parts.size != 5) return null
            val polarity = when (parts[0].uppercase()) {
                "D", "DARK" -> ThemePolarity.DARK
                "L", "LIGHT" -> ThemePolarity.LIGHT
                else -> return null
            }
            val tint = ThemeTintMode.entries.getOrNull(parts[1].toIntOrNull() ?: return null) ?: return null
            val bubble = BubbleColorStyle.entries.getOrNull(parts[2].toIntOrNull() ?: return null) ?: return null
            val hue = (parts[3].toIntOrNull() ?: return null).coerceIn(0, 359).toFloat()
            val sat = (parts[4].toIntOrNull() ?: return null).coerceIn(0, 100) / 100f
            return ThemeSpec(
                polarity = polarity,
                tintMode = tint,
                bubbleStyle = bubble,
                hue = hue,
                saturation = sat
            )
        }
    }
}

/** Пользовательская сохранённая тема из конструктора. */
@Immutable
data class SavedCustomTheme(
    val id: String,
    val name: String,
    val spec: ThemeSpec
)
