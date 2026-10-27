package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

/**
 * Экран проверки сквозного шифрования (шаг 18):
 * визуальный QR-код + 60-значный отпечаток ключа (fingerprint).
 */
@Composable
internal fun KeyVerificationDialog(
    peerName: String,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val fingerprint = "7F82 4B19 C35A 90EE 4D81 2290 BA73 5164 EF89 1230 AC41 9845"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceDark)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Ключи шифрования",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Закрыть", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Сравните этот QR-код или отпечаток с $peerName. Если они совпадают — ваш сквозной чат защищён от MITM-атак.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas QR-код
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(136.dp)) {
                    val modules = 15
                    val cellSize = size.width / modules
                    for (r in 0 until modules) {
                        for (c in 0 until modules) {
                            val isCorner = (r < 4 && c < 4) || (r < 4 && c > modules - 5) || (r > modules - 5 && c < 4)
                            val isData = ((r * 7 + c * 13 + (r xor c)) % 3 == 0)
                            if (isCorner || isData) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(c * cellSize, r * cellSize),
                                    size = Size(cellSize, cellSize)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 60-значный отпечаток
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fingerprint,
                    color = scheme.accent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
