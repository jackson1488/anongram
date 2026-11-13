package com.anongram.app.features.settings.modules.ui.translation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.chats.tools.translate.LocalMessageTranslator
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.anongram.app.features.settings.modules.ui.voice.formatMb

/**
 * Экран настройки модуля локального офлайн-перевода сообщений на движке Bergamot (чистый плоский стиль).
 */
@Composable
fun TranslationModuleSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current

    val settings by ModulesSettingsStore.state.collectAsState()
    val downloaded by LocalMessageTranslator.downloadedLanguages.collectAsState()
    val downloading by LocalMessageTranslator.downloadingLanguages.collectAsState()
    val downloadingProgress by LocalMessageTranslator.downloadingProgress.collectAsState()
    val packSizesBytes by LocalMessageTranslator.packSizesBytes.collectAsState()
    val packErrors by LocalMessageTranslator.packErrors.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        ModulesSettingsStore.ensureInitialized(context)
        LocalMessageTranslator.refreshDownloadedModels(context)
    }

    BackHandler(onBack = onBack)

    val filteredPacks = remember(searchQuery, downloaded) {
        val q = searchQuery.trim().lowercase()
        val list = if (q.isEmpty()) {
            LocalMessageTranslator.supportedPacks
        } else {
            LocalMessageTranslator.supportedPacks.filter {
                it.nameRu.lowercase().contains(q) ||
                    it.nameEn.lowercase().contains(q) ||
                    it.tag.lowercase().contains(q)
            }
        }
        // Скачанные языки всегда поднимаем наверх списка
        list.sortedByDescending { downloaded.contains(it.tag) }
    }

    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Плоская шапка
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
                    text = strings.translationSectionTitle,
                    color = scheme.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Переключатель кнопки «Перевести»
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ModulesSettingsStore.setShowTranslateButton(
                                context,
                                !settings.showTranslateButton
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
                        Text(
                            text = strings.showTranslateButtonTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.showTranslateButtonDesc,
                            color = scheme.textSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = settings.showTranslateButton,
                        onCheckedChange = {
                            ModulesSettingsStore.setShowTranslateButton(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent,
                            uncheckedThumbColor = scheme.textMuted,
                            uncheckedTrackColor = scheme.surface
                        )
                    )
                }

                HorizontalDivider(
                    color = scheme.divider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 18.dp)
                )

                // Переключатель автоперевода входящих
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ModulesSettingsStore.setAutoTranslateIncoming(
                                context,
                                !settings.autoTranslateIncoming
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
                        Text(
                            text = strings.autoTranslateIncomingTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.autoTranslateIncomingDesc,
                            color = scheme.textSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = settings.autoTranslateIncoming,
                        onCheckedChange = {
                            ModulesSettingsStore.setAutoTranslateIncoming(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent,
                            uncheckedThumbColor = scheme.textMuted,
                            uncheckedTrackColor = scheme.surface
                        )
                    )
                }

                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                // Заголовок выбора целевого языка
                Text(
                    text = strings.targetLanguageHeader,
                    color = scheme.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp)
                )

                val targetOptions = listOf(
                    "auto" to strings.langAuto,
                    "ru" to strings.langRu,
                    "en" to strings.langEn
                )
                targetOptions.forEachIndexed { idx, (code, label) ->
                    val isSelected = settings.targetTranslateLanguage == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ModulesSettingsStore.setTargetTranslateLanguage(context, code)
                            }
                            .padding(horizontal = 18.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) scheme.accent else scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (idx < targetOptions.lastIndex) {
                        HorizontalDivider(
                            color = scheme.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }
                }

                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                // Заголовок офлайн-языков (Bergamot)
                Text(
                    text = "${strings.offlineLanguagePacksHeader} (${LocalMessageTranslator.supportedPacks.size})",
                    color = scheme.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp)
                )

                // Плоский поиск языка
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = scheme.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 14.5.sp
                        ),
                        cursorBrush = SolidColor(scheme.accent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (strings.isRussian) "Поиск языка…" else "Search language…",
                                    color = scheme.textMuted,
                                    fontSize = 14.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                HorizontalDivider(
                    color = scheme.divider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 18.dp)
                )

                filteredPacks.forEachIndexed { idx, pack ->
                    val isDownloaded = downloaded.contains(pack.tag)
                    val isDownloading = downloading.contains(pack.tag)
                    val progress = downloadingProgress[pack.tag] ?: 0f
                    val sizeBytes = packSizesBytes[pack.tag] ?: 0L
                    val errorText = packErrors[pack.tag]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = if (strings.isRussian) pack.nameRu else pack.nameEn,
                                color = scheme.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val sub = when {
                                errorText != null -> errorText
                                isDownloading -> strings.downloadingProgress((progress * 100).toInt())
                                isDownloaded -> strings.installedLabel(formatMb(sizeBytes))
                                else -> pack.tag.uppercase()
                            }
                            Text(
                                text = sub,
                                color = when {
                                    errorText != null -> CrimsonKillSwitch
                                    isDownloaded -> scheme.accent
                                    else -> scheme.textSecondary
                                },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        when {
                            isDownloading -> {
                                CircularProgressIndicator(
                                    color = scheme.accent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            isDownloaded -> {
                                IconButton(
                                    onClick = { LocalMessageTranslator.deleteLanguagePack(context, pack.tag) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = strings.deleteBtn,
                                        tint = CrimsonKillSwitch,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            else -> {
                                IconButton(
                                    onClick = { LocalMessageTranslator.downloadLanguagePack(context, pack.tag) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = strings.downloadBtn,
                                        tint = scheme.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (idx < filteredPacks.lastIndex) {
                        HorizontalDivider(
                            color = scheme.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    }
                }

                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                Text(
                    text = strings.translationPrivacyNote,
                    color = scheme.textSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
