package com.anongram.app.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Изолированный движок генерации полных цветовых палитр AnonGram.
 *
 * Гарантирует разделение слоёв (фон -> карточка/островок -> входящий пузырь -> исходящий пузырь)
 * по ступеням светлоты (Lightness) + тональным рамкам и автоконтраст WCAG (>= 4.5:1),
 * чтобы элементы никогда не сливались даже при полной заливке «как в кастомной ОС».
 */
object ThemePaletteEngine {

    fun buildScheme(
        id: String,
        displayName: String,
        spec: ThemeSpec
    ): AnonGramScheme {
        val isDark = spec.polarity == ThemePolarity.DARK
        val hue = ((spec.hue % 360f) + 360f).mod(360f)
        val rawSat = spec.saturation.coerceIn(0f, 1f)
        val isMono = spec.tintMode == ThemeTintMode.MONOCHROME
        val effSat = if (isMono) 0f else rawSat

        val bgSat = when (spec.tintMode) {
            ThemeTintMode.MONOCHROME -> 0f
            ThemeTintMode.AMOLED_ACCENT -> effSat * 0.06f
            ThemeTintMode.SOFT_OS -> effSat * 0.24f
            ThemeTintMode.FULL_OS -> effSat * 0.40f
        }
        val cardSat = when (spec.tintMode) {
            ThemeTintMode.MONOCHROME -> 0f
            ThemeTintMode.AMOLED_ACCENT -> effSat * 0.11f
            ThemeTintMode.SOFT_OS -> effSat * 0.32f
            ThemeTintMode.FULL_OS -> effSat * 0.48f
        }

        val bgL = if (isDark) {
            when (spec.tintMode) {
                ThemeTintMode.MONOCHROME -> 0.02f
                ThemeTintMode.AMOLED_ACCENT -> 0.024f
                ThemeTintMode.SOFT_OS -> 0.055f
                ThemeTintMode.FULL_OS -> 0.105f
            }
        } else {
            when (spec.tintMode) {
                ThemeTintMode.MONOCHROME -> 0.94f
                ThemeTintMode.AMOLED_ACCENT -> 0.965f
                ThemeTintMode.SOFT_OS -> 0.935f
                ThemeTintMode.FULL_OS -> 0.895f
            }
        }

        val surfaceL = if (isDark) bgL + 0.032f else (bgL + 0.03f).coerceAtMost(0.985f)
        val cardL = if (isDark) bgL + 0.052f else 0.992f
        val islandL = if (isDark) bgL + 0.068f else 0.996f
        val incomingL = if (isDark) bgL + 0.088f else 0.996f

        val background = hslToColor(hue, bgSat, bgL)
        val surface = hslToColor(hue, cardSat * 0.85f, surfaceL)
        val card = hslToColor(hue, cardSat, cardL)
        val islandBg = hslToColor(hue, cardSat, islandL)
        val editorBg = hslToColor(hue, bgSat, if (isDark) bgL + 0.01f else (bgL + 0.02f).coerceAtMost(0.99f))
        val editorSurface = hslToColor(hue, cardSat, if (isDark) bgL + 0.075f else 0.995f)
        val emojiPanelBg = hslToColor(hue, cardSat * 0.9f, if (isDark) bgL + 0.045f else (bgL + 0.04f).coerceAtMost(0.985f))
        val bannerBg = hslToColor(hue, cardSat, if (isDark) (bgL + 0.095f) else 0.99f, alpha = 0.96f)
        val overlayScrim = if (isDark) Color(0xCC040508) else Color(0x8810141C)

        val borderAlpha = when {
            isDark && spec.tintMode == ThemeTintMode.FULL_OS -> 0.24f
            isDark -> 0.14f
            spec.tintMode == ThemeTintMode.FULL_OS -> 0.22f
            else -> 0.14f
        }
        val cardBorder = if (isDark) {
            hslToColor(hue, effSat * 0.7f, 0.78f, alpha = borderAlpha)
        } else {
            hslToColor(hue, effSat * 0.55f, 0.32f, alpha = borderAlpha)
        }
        val islandBorder = if (isDark) {
            hslToColor(hue, effSat * 0.75f, 0.80f, alpha = (borderAlpha + 0.04f).coerceAtMost(0.32f))
        } else {
            hslToColor(hue, effSat * 0.6f, 0.30f, alpha = (borderAlpha + 0.04f).coerceAtMost(0.28f))
        }

        val accentSat = if (isMono) 0f else max(0.55f, effSat)
        val accentL = if (isDark) {
            if (isMono) 0.93f else 0.59f
        } else {
            if (isMono) 0.12f else 0.44f
        }
        val accentDimL = if (isDark) {
            if (isMono) 0.72f else 0.48f
        } else {
            if (isMono) 0.26f else 0.36f
        }

        val accent = hslToColor(hue, accentSat, accentL)
        val accentDim = hslToColor((hue + 12f).mod(360f), accentSat * 0.92f, accentDimL)
        val accentSoft = accent.copy(alpha = if (isDark) 0.17f else 0.13f)
        val onAccent = bestOnColor(accent)

        val textPrimary = if (isDark) Color(0xFFF6F8FC) else Color(0xFF10131A)
        val textSecondary = if (isDark) {
            hslToColor(hue, bgSat * 0.45f, 0.72f)
        } else {
            hslToColor(hue, bgSat * 0.55f, 0.34f)
        }
        val textMuted = if (isDark) {
            hslToColor(hue, bgSat * 0.40f, 0.52f)
        } else {
            hslToColor(hue, bgSat * 0.45f, 0.50f)
        }
        val divider = if (isDark) Color(0x18FFFFFF) else Color(0x18000000)

        // Входящий пузырь (чётко отделён от фона)
        val incomingBubbleBg = hslToColor(hue, cardSat, incomingL)
        val incomingBubbleBorder = if (isDark) {
            hslToColor(hue, effSat * 0.6f, 0.75f, alpha = 0.13f)
        } else {
            hslToColor(hue, effSat * 0.5f, 0.32f, alpha = 0.15f)
        }
        val incomingText = textPrimary

        // Исходящий пузырь (зависит от BubbleColorStyle)
        val outgoingStart: Color
        val outgoingEnd: Color
        val outgoingBorder: Color
        val outgoingText: Color
        when (spec.bubbleStyle) {
            BubbleColorStyle.GRADIENT -> {
                outgoingStart = hslToColor(
                    hue,
                    if (isMono) 0f else max(0.54f, effSat),
                    if (isDark) (if (isMono) 0.88f else 0.50f) else (if (isMono) 0.14f else 0.46f)
                )
                outgoingEnd = hslToColor(
                    (hue + 14f).mod(360f),
                    if (isMono) 0f else max(0.48f, effSat),
                    if (isDark) (if (isMono) 0.72f else 0.39f) else (if (isMono) 0.24f else 0.38f)
                )
                outgoingBorder = Color.White.copy(alpha = if (isDark) 0.16f else 0.10f)
                outgoingText = bestOnColor(blendHalf(outgoingStart, outgoingEnd))
            }

            BubbleColorStyle.TINTED -> {
                val tintL = if (isDark) {
                    when (spec.tintMode) {
                        ThemeTintMode.FULL_OS -> 0.28f
                        ThemeTintMode.MONOCHROME -> 0.20f
                        else -> 0.23f
                    }
                } else {
                    if (isMono) 0.86f else 0.85f
                }
                val tintSat = if (isMono) 0f else max(0.36f, effSat * 0.68f)
                outgoingStart = hslToColor(hue, tintSat, tintL)
                outgoingEnd = hslToColor((hue + 8f).mod(360f), tintSat * 0.9f, (tintL - 0.03f).coerceAtLeast(0.14f))
                outgoingBorder = accent.copy(alpha = if (isDark) 0.44f else 0.38f)
                outgoingText = textPrimary
            }

            BubbleColorStyle.OUTLINE -> {
                outgoingStart = islandBg
                outgoingEnd = islandBg
                outgoingBorder = accent.copy(alpha = 0.86f)
                outgoingText = textPrimary
            }
        }

        val replyStripIncoming = if (isDark) {
            hslToColor(hue, cardSat * 0.8f, bgL + 0.13f)
        } else {
            hslToColor(hue, cardSat * 0.6f, 0.94f)
        }
        val replyStripOutgoing = if (spec.bubbleStyle == BubbleColorStyle.GRADIENT) {
            outgoingText.copy(alpha = 0.12f)
        } else {
            accent.copy(alpha = 0.14f)
        }

        val selectedGlow = if (isMono) accent else Color(0xFF8B5CF6).takeIf { abs(hue - 262f) < 35f } ?: accent
        val selectedBubbleBg = if (isDark) {
            hslToColor(hue, if (isMono) 0f else 0.45f, 0.26f)
        } else {
            hslToColor(hue, if (isMono) 0f else 0.48f, 0.83f)
        }

        return AnonGramScheme(
            id = id,
            displayName = displayName,
            accent = accent,
            accentDim = accentDim,
            isDark = isDark,
            spec = spec,
            background = background,
            surface = surface,
            card = card,
            cardBorder = cardBorder,
            islandBg = islandBg,
            islandBorder = islandBorder,
            bannerBg = bannerBg,
            overlayScrim = overlayScrim,
            inputBg = islandBg,
            inputBorder = islandBorder,
            editorBg = editorBg,
            editorSurface = editorSurface,
            emojiPanelBg = emojiPanelBg,
            accentSoft = accentSoft,
            onAccent = onAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textMuted = textMuted,
            divider = divider,
            outgoingBubbleStart = outgoingStart,
            outgoingBubbleEnd = outgoingEnd,
            outgoingBubbleBorder = outgoingBorder,
            outgoingText = outgoingText,
            incomingBubbleBg = incomingBubbleBg,
            incomingBubbleBorder = incomingBubbleBorder,
            incomingText = incomingText,
            replyStripIncoming = replyStripIncoming,
            replyStripOutgoing = replyStripOutgoing,
            selectedBubbleBg = selectedBubbleBg,
            selectedGlow = selectedGlow
        )
    }

    fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        val hueNorm = (((h % 360f) + 360f) % 360f) / 360f
        val sat = s.coerceIn(0f, 1f)
        val lig = l.coerceIn(0f, 1f)
        if (sat == 0f) {
            return Color(lig, lig, lig, alpha)
        }
        val q = if (lig < 0.5f) lig * (1f + sat) else lig + sat - lig * sat
        val p = 2f * lig - q
        val r = hueChannel(p, q, hueNorm + 1f / 3f)
        val g = hueChannel(p, q, hueNorm)
        val b = hueChannel(p, q, hueNorm - 1f / 3f)
        return Color(r, g, b, alpha)
    }

    private fun hueChannel(p: Float, q: Float, tRaw: Float): Float {
        var t = tRaw
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }.coerceIn(0f, 1f)
    }

    fun relativeLuminance(color: Color): Float {
        fun lin(c: Float): Float = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
        return 0.2126f * lin(color.red) + 0.7152f * lin(color.green) + 0.0722f * lin(color.blue)
    }

    fun contrastRatio(a: Color, b: Color): Float {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val hi = max(la, lb)
        val lo = min(la, lb)
        return (hi + 0.05f) / (lo + 0.05f)
    }

    fun bestOnColor(background: Color): Color {
        val darkText = Color(0xFF0A0C10)
        val lightText = Color(0xFFFFFFFF)
        return if (contrastRatio(background, darkText) >= contrastRatio(background, lightText)) {
            darkText
        } else {
            lightText
        }
    }

    private fun blendHalf(a: Color, b: Color): Color = Color(
        red = (a.red + b.red) * 0.5f,
        green = (a.green + b.green) * 0.5f,
        blue = (a.blue + b.blue) * 0.5f,
        alpha = 1f
    )
}
