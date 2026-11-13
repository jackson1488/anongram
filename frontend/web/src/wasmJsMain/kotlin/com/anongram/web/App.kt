package com.anongram.web

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App() {
    var counter by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("AnonGram Web (Compose Wasm) готов к работе!") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF0F172A),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(420.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚡ AnonGram Web",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Тестовая версия на Compose Multiplatform (Wasm / WebAssembly)",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )

                    HorizontalDivider(color = Color(0xFF334155))

                    Text(
                        text = "Кликов: $counter",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { counter++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Нажми меня")
                        }

                        OutlinedButton(
                            onClick = { counter = 0 }
                        ) {
                            Text("Сбросить")
                        }
                    }

                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}
