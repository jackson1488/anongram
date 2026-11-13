package com.anongram.web.features.auth.ui.registration

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.AnonGramScreenSurface
import com.anongram.web.features.auth.data.crypto.Bip39WordList
import com.anongram.web.features.auth.domain.model.SeedPhrase
import com.anongram.web.features.auth.domain.model.UserIdentity

@Composable
fun BackupSeedScreen(
    seedPhrase: SeedPhrase,
    identity: UserIdentity,
    onConfirmed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val scheme = LocalAnonGramScheme.current
    val warningRed = Color(0xFFEF4444)

    var showQuizDialog by remember { mutableStateOf(false) }
    var copiedBanner by remember { mutableStateOf(false) }

    AnonGramScreenSurface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                .background(scheme.islandBg)
                                .border(0.8.dp, scheme.islandBorder, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = scheme.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Резервная копия ключей", color = scheme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(warningRed.copy(alpha = 0.12f))
                            .border(1.dp, warningRed.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = "Внимание", tint = warningRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Критически важно!", color = warningRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Запишите эти 12 слов на бумагу и сохраните в надёжном месте. Если вы их потеряете, доступ к аккаунту и переписке восстановить невозможно!",
                                color = scheme.textPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    if (index < seedPhrase.words.size) {
                                        SeedWordChip(
                                            number = index + 1,
                                            word = seedPhrase.words[index],
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.accentSoft)
                            .border(1.dp, scheme.accent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(seedPhrase.text))
                                copiedBanner = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (copiedBanner) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Копировать",
                            tint = scheme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (copiedBanner) "12 слов скопированы!" else "Скопировать 12 слов",
                            color = scheme.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = scheme.accent, spotColor = scheme.accent)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.horizontalGradient(listOf(scheme.accent, scheme.accentDim)))
                            .clickable { showQuizDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Я сохранил слова (пройти тест)",
                            color = scheme.onAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showQuizDialog) {
        SeedMemoryQuizDialog(
            seedPhrase = seedPhrase,
            onDismiss = { showQuizDialog = false },
            onSuccess = {
                showQuizDialog = false
                onConfirmed()
            }
        )
    }
}

@Composable
private fun SeedWordChip(number: Int, word: String, modifier: Modifier = Modifier) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$number.", color = scheme.textMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.width(6.dp))
        Text(word, color = scheme.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SeedMemoryQuizDialog(
    seedPhrase: SeedPhrase,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val word3 = seedPhrase.wordAt(3)
    val word7 = seedPhrase.wordAt(7)

    val options3 = remember {
        val d1 = Bip39WordList.words.filter { it != word3 }.random()
        val d2 = Bip39WordList.words.filter { it != word3 && it != d1 }.random()
        listOf(word3, d1, d2).shuffled()
    }
    val options7 = remember {
        val d1 = Bip39WordList.words.filter { it != word7 }.random()
        val d2 = Bip39WordList.words.filter { it != word7 && it != d1 }.random()
        listOf(word7, d1, d2).shuffled()
    }

    var selected3 by remember { mutableStateOf<String?>(null) }
    var selected7 by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.card)
                .border(1.dp, scheme.cardBorder, RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Проверка памяти", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Выберите 3-е и 7-е слово вашей мнемоники для завершения регистрации:",
                fontSize = 13.sp,
                color = scheme.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Какое слово стоит под номером 3?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scheme.accent)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options3.forEach { opt ->
                    val isSelected = selected3 == opt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) scheme.accent else scheme.surface)
                            .border(1.dp, if (isSelected) scheme.accent else scheme.cardBorder, RoundedCornerShape(12.dp))
                            .clickable { selected3 = opt; errorMessage = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(opt, color = if (isSelected) scheme.onAccent else scheme.textPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Какое слово стоит под номером 7?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scheme.accent)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options7.forEach { opt ->
                    val isSelected = selected7 == opt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) scheme.accent else scheme.surface)
                            .border(1.dp, if (isSelected) scheme.accent else scheme.cardBorder, RoundedCornerShape(12.dp))
                            .clickable { selected7 = opt; errorMessage = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(opt, color = if (isSelected) scheme.onAccent else scheme.textPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(20.dp))
            val canConfirm = selected3 != null && selected7 != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (canConfirm) scheme.accent else scheme.surface)
                    .border(1.dp, if (canConfirm) scheme.accent else scheme.cardBorder, RoundedCornerShape(25.dp))
                    .clickable(enabled = canConfirm) {
                        if (selected3 == word3 && selected7 == word7) onSuccess()
                        else errorMessage = "Слова выбраны неверно! Проверьте свои записи."
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Подтвердить и войти", color = if (canConfirm) scheme.onAccent else scheme.textMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
