package com.anongram.app.features.settings.account.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.features.settings.account.ui.components.AccountInfoCard

/**
 * Экран раздела «Аккаунт» (`features/settings/account/`).
 * Позволяет редактировать никнейм, публичный @handle и описание профиля без привязки к SIM-карте.
 */
@Composable
fun AccountSettingsScreen(
    initialDisplayName: String,
    initialHandle: String,
    onSaveAccount: (displayName: String, handle: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(initialDisplayName) }
    var handle by remember { mutableStateOf(initialHandle.removePrefix("@")) }
    var bio by remember { mutableStateOf("Децентрализованный анонимный профиль AnonGram (Ed25519)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131924))
    ) {
        // Верхняя панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B2331))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Аккаунт",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    onSaveAccount(displayName.trim().ifBlank { "Джексон" }, handle.trim().ifBlank { "jackson" })
                    Toast.makeText(context, "Профиль обновлён", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Сохранить",
                    tint = Color(0xFF8B5CF6)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AccountInfoCard(
                displayName = displayName,
                onDisplayNameChange = { displayName = it },
                handle = handle,
                onHandleChange = { handle = it.replace("@", "") },
                bio = bio,
                onBioChange = { bio = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onSaveAccount(displayName.trim().ifBlank { "Джексон" }, handle.trim().ifBlank { "jackson" })
                    Toast.makeText(context, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Сохранить изменения",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
