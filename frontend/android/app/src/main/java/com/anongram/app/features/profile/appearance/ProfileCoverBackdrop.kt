package com.anongram.app.features.profile.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Общая обложка шапки профиля (цвет/градиент + узор из фонового эмодзи).
 * Используется в собственном [ProfileScreen], в редакторе цвета и в [UserProfileScreen] чата.
 */
@Composable
fun ProfileCoverBackdrop(
    appearance: ProfileAppearanceState,
    modifier: Modifier = Modifier,
    previewDarkOverride: Boolean? = null
) {
    val scheme = LocalAnonGramScheme.current
    val isDark = previewDarkOverride ?: scheme.isDark
    val preset = appearance.preset

    val topColor: Color
    val bottomColor: Color
    if (preset != null) {
        topColor = if (isDark) {
            lerp(preset.primary, Color.Black, 0.28f)
        } else {
            lerp(preset.primary, Color.White, 0.14f)
        }
        bottomColor = if (isDark) {
            lerp(preset.secondary, Color.Black, 0.42f)
        } else {
            preset.secondary
        }
    } else {
        topColor = scheme.surface
        bottomColor = scheme.background
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(colors = listOf(topColor, bottomColor))
        )
    ) {
        val emoji = appearance.backgroundEmoji
        if (!emoji.isNullOrBlank()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val positions = listOf(
                    Triple(0.08f, 0.18f, -18f),
                    Triple(0.26f, 0.34f, 14f),
                    Triple(0.76f, 0.20f, 18f),
                    Triple(0.90f, 0.42f, -12f),
                    Triple(0.14f, 0.68f, 12f),
                    Triple(0.82f, 0.72f, -16f),
                    Triple(0.32f, 0.80f, -8f),
                    Triple(0.66f, 0.84f, 10f)
                )
                positions.forEach { (xFrac, yFrac, rot) ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .offset(
                                x = maxWidth * xFrac,
                                y = maxHeight * yFrac
                            )
                            .rotate(rot)
                            .alpha(0.22f)
                    )
                }
            }
        }
        // Бесшовное растворение нижней кромки шапки в фон экрана
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1.0f to scheme.background
                    )
                )
        )
    }
}
