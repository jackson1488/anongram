package com.anongram.app.features.chats.input.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.input.i18n.LocalInputStrings

/**
 * Панель ввода формулы LaTeX строго по макету media_1794253317328.png.
 * Без дублирующего заголовка: тёмное поле ввода с плейсхолдером «Формула LaTeX»
 * и широкая фиолетовая кнопка «Готово» прямо над клавиатурой.
 */
@Composable
fun LatexFormulaInputPanel(
    onInsertFormula: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    var formulaText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Округлое тёмное поле ввода формулы
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.editorSurface)
                    .border(1.dp, scheme.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (formulaText.isEmpty()) {
                    Text(
                        text = strings.latexFormula,
                        color = scheme.textMuted,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                BasicTextField(
                    value = formulaText,
                    onValueChange = { formulaText = it },
                    textStyle = TextStyle(
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Широкая фиолетовая кнопка «Готово» под полем ввода
            Button(
                onClick = {
                    val trimmed = formulaText.trim()
                    if (trimmed.isNotEmpty()) {
                        val formattedFormula = if (trimmed.startsWith("\\(") || trimmed.startsWith("$$")) {
                            trimmed
                        } else {
                            "\\($trimmed\\)"
                        }
                        onInsertFormula(formattedFormula)
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.accent,
                    contentColor = scheme.onAccent
                )
            ) {
                Text(
                    text = strings.done,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
