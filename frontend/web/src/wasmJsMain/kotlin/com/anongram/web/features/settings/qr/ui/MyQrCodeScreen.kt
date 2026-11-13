package com.anongram.web.features.settings.qr.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.AnonGramScreenSurface
import com.anongram.web.core.ui.safeNavigationBarPadding
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.platform.WebStorage

private data class QrCardPalette(
    val id: String,
    val start: Color,
    val end: Color
)

@Composable
fun MyQrCodeScreen(
    handle: String,
    publicKeyHex: String,
    onClose: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val clipboard = LocalClipboardManager.current

    val cleanHandle = handle.removePrefix("@").uppercase().ifBlank { "ANON" }
    val qrPayload = "anongram://user?handle=$cleanHandle&vk=${publicKeyHex.take(16)}"

    val palettes = remember(scheme.accent, scheme.accentDim) {
        listOf(
            QrCardPalette("theme", scheme.accent, scheme.accentDim),
            QrCardPalette("emerald", Color(0xFF10B981), Color(0xFF047857)),
            QrCardPalette("ocean", Color(0xFF0EA5E9), Color(0xFF1D4ED8)),
            QrCardPalette("violet", Color(0xFF8B5CF6), Color(0xFF6D28D9)),
            QrCardPalette("amber", Color(0xFFF59E0B), Color(0xFFD97706)),
            QrCardPalette("mono", Color(0xFF27272A), Color(0xFF09090B))
        )
    }

    var selectedPaletteId by remember {
        mutableStateOf(WebStorage.getString("anongram_qr_palette") ?: "theme")
    }
    val activePalette = palettes.firstOrNull { it.id == selectedPaletteId } ?: palettes.first()
    var copied by remember { mutableStateOf(false) }

    AnonGramScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
                .safeNavigationBarPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .border(0.8.dp, scheme.islandBorder, CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (isRu) "Мой QR-код" else "My QR Code",
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(activePalette.start, activePalette.end)))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DeterministicQrMatrix(
                        payload = qrPayload,
                        dotColor = activePalette.end,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, activePalette.start, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = activePalette.start,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "@$cleanHandle",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ML-DSA-87 · ${publicKeyHex.take(12)}...",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                palettes.forEach { pal ->
                    val isSel = pal.id == activePalette.id
                    Box(
                        modifier = Modifier
                            .size(if (isSel) 34.dp else 28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(pal.start, pal.end)))
                            .border(
                                width = if (isSel) 2.5.dp else 1.dp,
                                color = if (isSel) scheme.textPrimary else scheme.cardBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedPaletteId = pal.id
                                WebStorage.putString("anongram_qr_palette", pal.id)
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.accent)
                    .clickable {
                        clipboard.setText(AnnotatedString(qrPayload))
                        copied = true
                    }
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = scheme.onAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (copied) {
                        if (isRu) "Ссылка скопирована" else "Link copied"
                    } else {
                        if (isRu) "Скопировать QR-ссылку" else "Copy QR Link"
                    },
                    color = scheme.onAccent,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DeterministicQrMatrix(
    payload: String,
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    val grid = remember(payload) {
        val n = 21
        val m = Array(n) { BooleanArray(n) }
        fun drawFinder(r0: Int, c0: Int) {
            for (r in 0..6) for (c in 0..6) {
                m[r0 + r][c0 + c] = r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4)
            }
        }
        drawFinder(0, 0)
        drawFinder(0, n - 7)
        drawFinder(n - 7, 0)

        var seed = payload.hashCode() * 2654435761L
        for (r in 0 until n) {
            for (c in 0 until n) {
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= n - 8
                val inBottomLeft = r >= n - 8 && c < 8
                val inCenter = r in 8..12 && c in 8..12
                if (!inTopLeft && !inTopRight && !inBottomLeft && !inCenter) {
                    seed = (seed * 6364136223846793005L + 1442695040888963407L)
                    m[r][c] = ((seed ushr 33) and 1L) == 1L
                }
            }
        }
        m
    }

    Canvas(modifier = modifier) {
        val n = grid.size
        val cell = size.minDimension / n
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (grid[r][c]) {
                    drawRoundRect(
                        color = dotColor,
                        topLeft = Offset(c * cell + cell * 0.08f, r * cell + cell * 0.08f),
                        size = Size(cell * 0.84f, cell * 0.84f),
                        cornerRadius = CornerRadius(cell * 0.22f, cell * 0.22f)
                    )
                }
            }
        }
    }
}
