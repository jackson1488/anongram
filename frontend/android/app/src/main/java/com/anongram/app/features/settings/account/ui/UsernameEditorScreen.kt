package com.anongram.app.features.settings.account.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.profile.i18n.ProfileI18n

/**
 * Отдельный экран редактирования публичного @username (как в Telegram, адаптирован под AnonGram).
 */
@Composable
fun UsernameEditorScreen(
    initialUsername: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    var rawInput by remember {
        mutableStateOf(initialUsername.removePrefix("@"))
    }
    val sanitized = rawInput.filter { it.isLetterOrDigit() || it == '_' }
    val isValid = sanitized.length >= 5

    BackHandler(onBack = onBack)

    AnonGramScreenSurface(
        applySystemBarsPadding = false,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
                .padding(horizontal = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.card)
                        .border(0.8.dp, scheme.cardBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = strings.usernameScreenTitle,
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isValid) scheme.accent else scheme.accent.copy(alpha = 0.35f))
                        .clickable(enabled = isValid) {
                            onSave(sanitized.uppercase())
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.save,
                        color = scheme.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = strings.usernameFieldTitle,
                    color = scheme.accent,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "anongram.me/",
                        color = scheme.textSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    BasicTextField(
                        value = rawInput,
                        onValueChange = { next ->
                            rawInput = next.filter { it.isLetterOrDigit() || it == '_' }
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(scheme.accent),
                        textStyle = TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.usernameHelper,
                color = scheme.textSecondary,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
