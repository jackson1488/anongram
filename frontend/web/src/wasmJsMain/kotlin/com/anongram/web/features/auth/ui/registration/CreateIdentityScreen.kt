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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.AnonGramScreenSurface
import com.anongram.web.features.auth.data.crypto.Bip39WordList
import com.anongram.web.features.auth.data.crypto.KeyGenerator
import com.anongram.web.features.auth.domain.model.SeedPhrase
import com.anongram.web.features.auth.domain.model.UserIdentity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateIdentityScreen(
    onNextToBackup: (nickname: String, seedPhrase: SeedPhrase, identity: UserIdentity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    var nickname by remember { mutableStateOf("GhostWalker") }
    var selectedAvatarIndex by remember { mutableIntStateOf(0) }
    var isGeneratingKeys by remember { mutableStateOf(false) }
    var generationStep by remember { mutableIntStateOf(0) }
    var generatedIdentity by remember { mutableStateOf<UserIdentity?>(null) }
    var generatedSeedPhrase by remember { mutableStateOf<SeedPhrase?>(null) }

    val scope = rememberCoroutineScope()

    val avatarPresets = listOf(
        listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)),
        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
        listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(Color(0xFF6366F1), Color(0xFFA855F7))
    )

    fun startKeyGeneration() {
        if (isGeneratingKeys) return
        isGeneratingKeys = true
        generationStep = 1

        scope.launch {
            delay(350)
            val seed = Bip39WordList.generate12Words()
            generatedSeedPhrase = seed
            generationStep = 2
            delay(380)
            generationStep = 3
            delay(380)
            val identity = KeyGenerator.deriveIdentity(nickname.trim().ifEmpty { "GhostWalker" }, seed)
            generatedIdentity = identity
            generationStep = 4
            isGeneratingKeys = false
        }
    }

    LaunchedEffect(Unit) {
        startKeyGeneration()
    }

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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = scheme.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Создание личности",
                            color = scheme.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    val currentColors = avatarPresets[selectedAvatarIndex % avatarPresets.size]
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(20.dp, CircleShape, ambientColor = currentColors[0], spotColor = currentColors[1])
                            .clip(CircleShape)
                            .background(Brush.linearGradient(currentColors))
                            .border(2.dp, scheme.cardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = nickname.trim().take(1).uppercase().ifEmpty { "A" },
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatarPresets.forEachIndexed { index, colors ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(colors))
                                    .border(
                                        width = if (selectedAvatarIndex == index) 2.5.dp else 1.dp,
                                        color = if (selectedAvatarIndex == index) scheme.textPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatarIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAvatarIndex == index) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ПСЕВДОНИМ (NICKNAME)",
                            color = scheme.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(scheme.inputBg)
                                .border(1.dp, scheme.inputBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = nickname,
                                onValueChange = { if (it.length <= 24) nickname = it },
                                textStyle = TextStyle(
                                    color = scheme.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(scheme.accent),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val randomNames = listOf("GhostWalker", "NeonSpectre", "ShadowCipher", "CyberViper", "VoidDrifter", "QuantumEcho")
                                    nickname = randomNames.random()
                                    startKeyGeneration()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Случайный ник",
                                    tint = scheme.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Псевдоним виден только вашим контактам и может быть изменён позже",
                            color = scheme.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Криптографический конвейер",
                            color = scheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CryptoStepRow(1, "Мнемоника BIP-39 (12 слов)", generationStep >= 1, generationStep > 1)
                        Spacer(modifier = Modifier.height(8.dp))
                        CryptoStepRow(2, "Мастер-ключ Ed25519 (Curve25519)", generationStep >= 2, generationStep > 2)
                        Spacer(modifier = Modifier.height(8.dp))
                        CryptoStepRow(3, "Квантовый бандл ML-DSA-87", generationStep >= 3, generationStep > 3)
                        Spacer(modifier = Modifier.height(8.dp))
                        CryptoStepRow(
                            4,
                            "Сетевой Public ID: ${generatedIdentity?.publicId?.take(16) ?: "генерация..."}…",
                            generationStep >= 4,
                            generationStep >= 4
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    val isReady = generatedIdentity != null && generatedSeedPhrase != null && !isGeneratingKeys
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(if (isReady) 16.dp else 0.dp, RoundedCornerShape(28.dp), ambientColor = scheme.accent, spotColor = scheme.accent)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (isReady) Brush.horizontalGradient(listOf(scheme.accent, scheme.accentDim))
                                else SolidColor(scheme.card)
                            )
                            .then(if (!isReady) Modifier.border(1.dp, scheme.cardBorder, RoundedCornerShape(28.dp)) else Modifier)
                            .clickable(enabled = isReady) {
                                val id = generatedIdentity ?: return@clickable
                                val seed = generatedSeedPhrase ?: return@clickable
                                onNextToBackup(nickname, seed, id.copy(nickname = nickname.trim().ifEmpty { "GhostWalker" }))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGeneratingKeys) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = scheme.accent, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Деривация ключей...", color = scheme.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text(
                                text = "Продолжить к сохранению Seed-фразы",
                                color = if (isReady) scheme.onAccent else scheme.textMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CryptoStepRow(
    step: Int,
    label: String,
    isActive: Boolean,
    isDone: Boolean
) {
    val scheme = LocalAnonGramScheme.current
    val emeraldAccent = Color(0xFF10B981)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> emeraldAccent
                        isActive -> scheme.accentSoft
                        else -> scheme.surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    text = "$step",
                    color = if (isActive) scheme.accent else scheme.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (isActive) scheme.textPrimary else scheme.textMuted,
            fontSize = 12.sp,
            fontFamily = if (step == 4) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}
