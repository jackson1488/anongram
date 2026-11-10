package com.anongram.app.features.auth.ui.login

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.auth.data.crypto.Bip39WordList
import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
import kotlinx.coroutines.launch

/**
 * Экран восстановления аккаунта AnonGram по 12 словам мнемоники BIP-39.
 *
 * Возможности:
 * 1. Пошаговый ввод каждого из 12 слов;
 * 2. Автодополнение (Autocomplete) из официального словаря BIP-39 при наборе;
 * 3. Быстрая вставка всей фразы из буфера обмена одной кнопкой;
 * 4. Валидация слов на лету;
 * 5. Деривация мастер-ключа и вход в аккаунт.
 *
 * @param onRestoreAccount Колбэк запуска процедуры восстановления с введённой мнемоникой.
 * @param onBack Колбэк возврата на экран приветствия.
 * @param modifier Модификатор макета.
 */
@Composable
fun RestoreAccountScreen(
    onRestoreAccount: suspend (SeedPhrase) -> Result<UserIdentity>,
    onRestoreSuccess: (UserIdentity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val obsidianBackground = Color(0xFF0D0B14)
    val purpleAccent = Color(0xFF8B5CF6)
    val lilacAccent = Color(0xFFC084FC)
    val cardBackground = Color(0xFF161224)
    val cardBorder = Color(0x338B5CF6)
    val errorRed = Color(0xFFEF4444)

    val words = remember { mutableStateListOf(*Array(12) { "" }) }
    var activeSlotIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Получаем список подсказок для текущего активного слота
    val currentInput = words.getOrElse(activeSlotIndex) { "" }
    val suggestions = remember(currentInput) {
        if (currentInput.length >= 2) {
            Bip39WordList.findSuggestions(currentInput, limit = 6)
        } else {
            emptyList()
        }
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        val pastedWords = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

        if (pastedWords.size >= 12) {
            for (i in 0 until 12) {
                words[i] = pastedWords[i].lowercase()
            }
            errorMessage = null
            Toast.makeText(context, "Вставлено 12 слов мнемоники", Toast.LENGTH_SHORT).show()
        } else if (pastedWords.isNotEmpty()) {
            for (i in 0 until minOf(pastedWords.size, 12)) {
                words[i] = pastedWords[i].lowercase()
            }
            Toast.makeText(context, "Вставлено ${minOf(pastedWords.size, 12)} слов", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
        }
    }

    fun validateAndRestore() {
        val cleanWords = words.map { it.trim().lowercase() }
        val emptyIndex = cleanWords.indexOfFirst { it.isEmpty() }
        if (emptyIndex != -1) {
            errorMessage = "Заполните слово #${emptyIndex + 1}"
            activeSlotIndex = emptyIndex
            return
        }

        for ((index, word) in cleanWords.withIndex()) {
            if (!Bip39WordList.isValidWord(word)) {
                errorMessage = "Слово #${index + 1} «$word» не входит в словарь BIP-39"
                activeSlotIndex = index
                return
            }
        }

        errorMessage = null
        isLoading = true

        scope.launch {
            val seedPhrase = SeedPhrase(cleanWords)
            val result = onRestoreAccount(seedPhrase)
            isLoading = false
            result.onSuccess { identity ->
                onRestoreSuccess(identity)
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Ошибка восстановления идентичности"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(obsidianBackground)
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Тулбар
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F1A30))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Вход по Seed-фразе",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки управления: Вставить из буфера и Очистить
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1D1730))
                            .border(1.dp, Color(0x4D8B5CF6), RoundedCornerShape(14.dp))
                            .clickable { pasteFromClipboard() }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Вставить",
                            tint = lilacAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Вставить из буфера",
                            color = lilacAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1F1524))
                            .border(1.dp, Color(0x33EF4444), RoundedCornerShape(14.dp))
                            .clickable {
                                for (i in 0 until 12) words[i] = ""
                                errorMessage = null
                                activeSlotIndex = 0
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Очистить",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Сетка 3x4 слотов для ввода слов мнемоники
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackground)
                        .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val slotIndex = row * 3 + col
                                val isSelected = activeSlotIndex == slotIndex
                                val wordValue = words[slotIndex]
                                val isValid = wordValue.isEmpty() || Bip39WordList.isValidWord(wordValue)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xFF2C1F4A)
                                                wordValue.isNotEmpty() -> Color(0xFF201834)
                                                else -> Color(0xFF191228)
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                !isValid -> errorRed
                                                isSelected -> lilacAccent
                                                else -> Color(0x338B5CF6)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { activeSlotIndex = slotIndex }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "${slotIndex + 1}.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        BasicTextField(
                                            value = wordValue,
                                            onValueChange = { newVal ->
                                                words[slotIndex] = newVal.trim().lowercase()
                                                errorMessage = null
                                            },
                                            textStyle = TextStyle(
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            cursorBrush = SolidColor(purpleAccent),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Панель подсказок автокомплита BIP-39
                AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = "Подсказки словаря BIP-39:",
                            color = lilacAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions) { sugg ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(purpleAccent)
                                        .clickable {
                                            words[activeSlotIndex] = sugg
                                            if (activeSlotIndex < 11) {
                                                activeSlotIndex++
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = sugg,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = errorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Нижняя кнопка подтверждения восстановления
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = purpleAccent, spotColor = purpleAccent)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF7C3AED), Color(0xFF9333EA), Color(0xFFC084FC))
                            )
                        )
                        .clickable(enabled = !isLoading) { validateAndRestore() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Восстановить аккаунт",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
