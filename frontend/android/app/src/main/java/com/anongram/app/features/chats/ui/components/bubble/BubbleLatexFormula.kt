package com.anongram.app.features.chats.ui.components.bubble

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.anongram.app.features.chats.input.banner.ChatActionBannerBus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Карточка отображения математической формулы LaTeX в сообщении.
 * Содержит бейдж «∑ LaTeX», математические глифы и кнопку копирования.
 */
@Composable
fun BubbleLatexFormula(
    formula: String,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current

    val rawMath = formula
        .removePrefix("\\(")
        .removeSuffix("\\)")
        .removePrefix("$$")
        .removeSuffix("$$")
        .removePrefix("$")
        .removeSuffix("$")
        .trim()

    val renderedMath = formatLatexToReadableMath(rawMath)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Верхняя плашка: бейдж «∑ LaTeX» и кнопка копирования
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(scheme.accentSoft)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Functions,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LaTeX",
                        color = scheme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("LaTeX Formula", formula))
                            ChatActionBannerBus.show(Icons.Outlined.ContentCopy) { it.bannerFormulaCopied }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Скопировать формулу",
                        tint = scheme.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Копировать",
                        color = scheme.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Основной математический текст крупным шрифтом
            Text(
                text = renderedMath,
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )
        }
    }
}

/**
 * Преобразует типовые математические конструкции LaTeX в читаемые Unicode-символы.
 */
private fun formatLatexToReadableMath(raw: String): String {
    var s = raw
    // Дроби: \frac{a}{b} -> (a / b)
    s = s.replace(Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}"), "($1 / $2)")
    // Квадратный корень: \sqrt{x} -> √(x)
    s = s.replace(Regex("\\\\sqrt\\{([^}]+)\\}"), "√($1)")
    // Степени: ^2 -> ², ^3 -> ³, ^n -> ⁿ
    s = s.replace("^2", "²")
        .replace("^3", "³")
        .replace("^0", "⁰")
        .replace("^1", "¹")
        .replace("^4", "⁴")
        .replace("^n", "ⁿ")
        .replace("^x", "ˣ")
    // Индексы
    s = s.replace("_0", "₀")
        .replace("_1", "₁")
        .replace("_2", "₂")
        .replace("_i", "ᵢ")
        .replace("_n", "ₙ")

    // Греческие буквы
    val greek = mapOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η", "\\theta" to "θ",
        "\\iota" to "ι", "\\kappa" to "κ", "\\lambda" to "λ", "\\mu" to "μ",
        "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π", "\\rho" to "ρ",
        "\\sigma" to "σ", "\\tau" to "τ", "\\upsilon" to "υ", "\\phi" to "φ",
        "\\chi" to "χ", "\\psi" to "ψ", "\\omega" to "ω",
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Phi" to "Φ",
        "\\Psi" to "Ψ", "\\Omega" to "Ω"
    )
    greek.forEach { (tex, sym) -> s = s.replace(tex, sym) }

    // Математические операторы
    val operators = mapOf(
        "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫", "\\iint" to "∬",
        "\\infty" to "∞", "\\approx" to "≈", "\\neq" to "≠", "\\leq" to "≤",
        "\\geq" to "≥", "\\times" to "×", "\\cdot" to "·", "\\pm" to "±",
        "\\mp" to "∓", "\\div" to "÷", "\\in" to "∈", "\\subset" to "⊂",
        "\\forall" to "∀", "\\exists" to "∃", "\\nabla" to "∇", "\\partial" to "∂"
    )
    operators.forEach { (tex, sym) -> s = s.replace(tex, sym) }

    return s
}
