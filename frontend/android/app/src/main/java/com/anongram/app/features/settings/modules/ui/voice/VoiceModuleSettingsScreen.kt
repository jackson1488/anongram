package com.anongram.app.features.settings.modules.ui.voice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.assets.ExternalAssetManager
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.i18n.ModulesI18n

/**
 * Отдельный экран настройки модуля расшифровки ГС (чистый плоский стиль без баблов и кружков).
 */
@Composable
fun VoiceModuleSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current

    val assetManager = remember { ExternalAssetManager.getInstance(context) }
    val assetStates by assetManager.states.collectAsState()
    val settings by ModulesSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        ModulesSettingsStore.ensureInitialized(context)
        assetManager.refreshAllStates()
    }

    BackHandler(onBack = onBack)

    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Чистая плоская шапка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .height(56.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.voiceSectionTitle,
                    color = scheme.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                VoiceModuleSection(
                    settings = settings,
                    assetStates = assetStates,
                    assetManager = assetManager
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
