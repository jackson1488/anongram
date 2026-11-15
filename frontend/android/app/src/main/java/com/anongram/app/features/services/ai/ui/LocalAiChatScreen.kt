package com.anongram.app.features.services.ai.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.services.ai.data.AiAttachment
import com.anongram.app.features.services.ai.data.AiCatalog
import com.anongram.app.features.services.ai.data.AiModelCategory
import com.anongram.app.features.services.ai.data.AiReasoningLevel
import com.anongram.app.features.services.ai.data.LocalAiStore
import com.anongram.app.features.services.ai.i18n.LocalAiI18n
import com.anongram.app.features.services.ai.i18n.LocalAiI18nStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LocalAiChatScreen(
    onClose: () -> Unit,
    onOpenModules: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAiI18n.current
    val state by LocalAiStore.state.collectAsState()

    LaunchedEffect(Unit) {
        LocalAiStore.ensureInitialized(context)
        LocalAiStore.refreshInstalledModels(context)
    }

    var inputText by remember { mutableStateOf("") }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isModelMenuOpen by remember { mutableStateOf(false) }
    var isAiSettingsOpen by remember { mutableStateOf(false) }
    var isPlusMenuOpen by remember { mutableStateOf(false) }
    val pendingAttachments = remember { mutableStateListOf<AiAttachment>() }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        uris.forEach { uri ->
            pendingAttachments.add(LocalAiStore.readAttachmentFromUri(context, uri, isImageHint = true))
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            pendingAttachments.add(LocalAiStore.readAttachmentFromUri(context, uri, isImageHint = false))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            val file = File(context.cacheDir, "ai_cam_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            val uri = Uri.fromFile(file)
            pendingAttachments.add(LocalAiStore.readAttachmentFromUri(context, uri, isImageHint = true))
        }
    }

    val ggufImportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            LocalAiStore.importLocalGguf(context, uri)
        }
    }

    val activeMessages = state.activeSession?.messages ?: emptyList()
    val listState = rememberLazyListState()
    LaunchedEffect(activeMessages.size, activeMessages.lastOrNull()?.content?.length) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.lastIndex)
        }
    }

    AnonGramScreenSurface(modifier = modifier, applySystemBarsPadding = false) {
        if (state.installedModels.isEmpty()) {
            LocalAiFirstRunGate(
                strings = strings,
                downloadingModelId = state.downloadingModelId,
                downloadProgress = state.downloadProgress,
                downloadError = state.downloadError,
                onDownloadPreset = { LocalAiStore.downloadPreset(context, it) },
                onCancelDownload = { LocalAiStore.cancelDownload() },
                onImportGguf = { ggufImportPicker.launch(arrayOf("*/*")) },
                onOpenModules = onOpenModules,
                onClose = onClose
            )
        } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Центр: лента сообщений / стартовый экран + наложенная полупрозрачная шапка сверху
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeMessages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 56.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(scheme.accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = strings.emptyHeroTitle,
                                color = scheme.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.emptyHeroSubtitle,
                                color = scheme.textSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(22.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    strings.quickPrompt1,
                                    strings.quickPrompt2,
                                    strings.quickPrompt3
                                ).forEach { q ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(scheme.card)
                                            .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                                            .clickable {
                                                LocalAiStore.sendMessage(
                                                    context = context,
                                                    prompt = q,
                                                    attachments = emptyList(),
                                                    isRussian = strings.isRussian
                                                )
                                            }
                                            .padding(horizontal = 14.dp, vertical = 11.dp)
                                    ) {
                                        Text(
                                            text = q,
                                            color = scheme.textPrimary,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 56.dp, bottom = 12.dp, start = 14.dp, end = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(activeMessages, key = { it.id }) { msg ->
                                LocalAiMessageBubble(
                                    message = msg,
                                    isGeneratingThis = state.isGenerating && msg.id == activeMessages.lastOrNull()?.id && !msg.isUser,
                                    strings = strings,
                                    onRegenerate = { msgId ->
                                        LocalAiStore.regenerateMessage(context, msgId, strings.isRussian)
                                    },
                                    onEditThoughtsAndRegenerate = { msgId, newThoughts ->
                                        LocalAiStore.editThinkingAndRegenerate(
                                            context = context,
                                            assistantMsgId = msgId,
                                            newThinking = newThoughts,
                                            isRussian = strings.isRussian
                                        )
                                    },
                                    onEditUserMessage = { msgId, newText ->
                                        LocalAiStore.editUserMessageAndRegenerate(
                                            context = context,
                                            userMsgId = msgId,
                                            newText = newText,
                                            isRussian = strings.isRussian
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Наложенная полупрозрачная шапка сверху (как в чате)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        scheme.background.copy(alpha = 0.94f),
                                        scheme.background.copy(alpha = 0.78f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .height(48.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isDrawerOpen = true }, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = null,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { isModelMenuOpen = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.activeModelDisplayName,
                                color = scheme.textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.reasoningLabel(state.reasoningLevel),
                                color = scheme.textSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = scheme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { LocalAiStore.newChat(context) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = strings.newChat,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(scheme.card)
                                .clickable(onClick = onClose),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Превью прикреплённых фото и файлов прямо над полем ввода
                if (pendingAttachments.isNotEmpty() || state.webSearchEnabled) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.webSearchEnabled) {
                            item(key = "web_badge") {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(scheme.accent.copy(alpha = 0.16f))
                                        .clickable { LocalAiStore.setWebSearchEnabled(context, false) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = null,
                                        tint = scheme.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = strings.webSearchEnabledBadge,
                                        color = scheme.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        items(pendingAttachments, key = { it.id }) { att ->
                            AttachmentPreviewChip(
                                attachment = att,
                                onRemove = { pendingAttachments.remove(att) }
                            )
                        }
                    }
                }

                // Нижняя капсула ввода + всплывающее меню '+' поверх клавиатуры
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(26.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isPlusMenuOpen = !isPlusMenuOpen },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = strings.inputPlaceholder,
                                    color = scheme.textMuted,
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = TextStyle(color = scheme.textPrimary, fontSize = 15.sp),
                                cursorBrush = SolidColor(scheme.accent),
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        IconButton(
                            onClick = { },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = scheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val canSend = inputText.isNotBlank() || pendingAttachments.isNotEmpty()
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isGenerating || canSend) scheme.accent
                                    else scheme.surface
                                )
                                .clickable {
                                    if (state.isGenerating) {
                                        LocalAiStore.stopGeneration(context)
                                    } else if (canSend) {
                                        val toSend = pendingAttachments.toList()
                                        val msgText = inputText
                                        inputText = ""
                                        pendingAttachments.clear()
                                        LocalAiStore.sendMessage(
                                            context = context,
                                            prompt = msgText,
                                            attachments = toSend,
                                            isRussian = strings.isRussian
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isGenerating) Icons.Outlined.Stop else Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = if (state.isGenerating || canSend) scheme.onAccent else scheme.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isPlusMenuOpen) {
                        val density = LocalDensity.current
                        val yOffsetPx = with(density) { (-56).dp.roundToPx() }
                        Popup(
                            alignment = Alignment.BottomStart,
                            offset = IntOffset(0, yOffsetPx),
                            onDismissRequest = { isPlusMenuOpen = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(236.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(scheme.card)
                                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                                    .padding(vertical = 8.dp, horizontal = 6.dp)
                            ) {
                                PlusMenuItem(
                                    icon = Icons.Outlined.CameraAlt,
                                    label = strings.attachCamera,
                                    onClick = {
                                        isPlusMenuOpen = false
                                        cameraLauncher.launch(null)
                                    }
                                )
                                PlusMenuItem(
                                    icon = Icons.Outlined.PhotoLibrary,
                                    label = strings.attachPhoto,
                                    onClick = {
                                        isPlusMenuOpen = false
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                PlusMenuItem(
                                    icon = Icons.Outlined.AttachFile,
                                    label = strings.attachFiles,
                                    onClick = {
                                        isPlusMenuOpen = false
                                        filePicker.launch(arrayOf("*/*"))
                                    }
                                )
                                PlusMenuItem(
                                    icon = Icons.Outlined.Public,
                                    label = strings.attachWebSearch,
                                    accentActive = state.webSearchEnabled,
                                    onClick = {
                                        isPlusMenuOpen = false
                                        LocalAiStore.setWebSearchEnabled(context, !state.webSearchEnabled)
                                    }
                                )
                                PlusMenuItem(
                                    icon = Icons.Outlined.Psychology,
                                    label = strings.attachThinkDeeper,
                                    accentActive = state.reasoningLevel == AiReasoningLevel.HIGH,
                                    onClick = {
                                        isPlusMenuOpen = false
                                        LocalAiStore.setReasoningLevel(context, AiReasoningLevel.HIGH)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Компактное меню моделей и уровня рассуждений (в стиле Gemini)
            LocalAiQuickModelMenu(
                visible = isModelMenuOpen,
                state = state,
                strings = strings,
                onDismiss = { isModelMenuOpen = false },
                onSelectModel = { modelId ->
                    LocalAiStore.selectModel(context, modelId)
                },
                onSelectReasoning = { LocalAiStore.setReasoningLevel(context, it) }
            )

            // Левая шторка истории чатов (открывается ТОЛЬКО кнопкой '=')
            LocalAiDrawer(
                visible = isDrawerOpen,
                sessions = state.sessions,
                activeSessionId = state.activeSessionId,
                strings = strings,
                onDismiss = { isDrawerOpen = false },
                onNewChat = { LocalAiStore.newChat(context) },
                onSelectChat = { LocalAiStore.selectChat(it) },
                onTogglePin = { LocalAiStore.togglePinChat(context, it) },
                onDeleteChat = { LocalAiStore.deleteChat(context, it) },
                onOpenAiSettings = { isAiSettingsOpen = true },
                onOpenModules = onOpenModules
            )

            // Отдельный экран настроек ИИ (системный промпт, температура, потоки, контекст)
            LocalAiSettingsScreen(
                visible = isAiSettingsOpen,
                state = state,
                strings = strings,
                onDismiss = { isAiSettingsOpen = false },
                onChangeTemperature = { LocalAiStore.setTemperature(context, it) },
                onSelectPromptPreset = { LocalAiStore.setSystemPromptPreset(context, it) },
                onChangeCustomPrompt = { LocalAiStore.setCustomSystemPrompt(context, it) },
                onSelectThreads = { LocalAiStore.setCpuThreads(context, it) },
                onSelectContext = { LocalAiStore.setContextLength(context, it) },
                onOpenModules = onOpenModules
            )
        }
        }
    }
}

@Composable
private fun PlusMenuItem(
    icon: ImageVector,
    label: String,
    accentActive: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (accentActive) scheme.accent.copy(alpha = 0.18f) else scheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (accentActive) scheme.accent else scheme.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = if (accentActive) scheme.accent else scheme.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AttachmentPreviewChip(
    attachment: AiAttachment,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current

    if (attachment.isImage) {
        val bitmap by produceState<ImageBitmap?>(initialValue = null, attachment.uriString) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(attachment.uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                        BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(12.dp))
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = attachment.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = scheme.textMuted,
                    modifier = Modifier.align(Alignment.Center).size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(scheme.background.copy(alpha = 0.85f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = attachment.name,
                color = scheme.textPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(110.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = scheme.textMuted,
                modifier = Modifier
                    .size(15.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun LocalAiFirstRunGate(
    strings: LocalAiI18nStrings,
    downloadingModelId: String?,
    downloadProgress: Float,
    downloadError: String?,
    onDownloadPreset: (com.anongram.app.features.services.ai.data.AiModelPreset) -> Unit,
    onCancelDownload: () -> Unit,
    onImportGguf: () -> Unit,
    onOpenModules: () -> Unit,
    onClose: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.title,
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = scheme.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.gateTitle,
                color = scheme.textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.gateSubtitle,
                color = scheme.textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            if (downloadError != null) {
                Text(
                    text = downloadError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            var selectedCategory by remember { mutableStateOf(AiModelCategory.ALL) }
            val filteredPresets = remember(selectedCategory) {
                if (selectedCategory == AiModelCategory.ALL) AiCatalog.presets
                else AiCatalog.presets.filter { it.category == selectedCategory }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AiModelCategory.entries) { cat ->
                    val active = selectedCategory == cat
                    val label = when (cat) {
                        AiModelCategory.ALL -> if (strings.isRussian) "Все" else "All"
                        AiModelCategory.GEMMA -> "Gemma"
                        AiModelCategory.DEEPSEEK -> "DeepSeek"
                        AiModelCategory.GPT_OSS -> "GPT-OSS"
                        AiModelCategory.UNCENSORED -> if (strings.isRussian) "Без цензуры" else "Uncensored"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) scheme.accent else scheme.card)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (active) scheme.onAccent else scheme.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
            ) {
                filteredPresets.forEachIndexed { idx, preset ->
                    if (idx > 0) {
                        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                    }
                    val isDownloading = downloadingModelId == preset.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName,
                                    color = scheme.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${preset.sizeMb} MB · ${preset.ramHint}",
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (isDownloading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        color = scheme.accent,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(onClick = onCancelDownload, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = null,
                                            tint = scheme.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(scheme.accent)
                                        .clickable(enabled = downloadingModelId == null) { onDownloadPreset(preset) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = scheme.onAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = strings.gateDownloadBtn,
                                        color = scheme.onAccent,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = scheme.accent,
                                trackColor = scheme.divider
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.card)
                        .clickable(onClick = onImportGguf)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.gateImportBtn,
                        color = scheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.card)
                        .clickable(onClick = onOpenModules)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.gateOpenModulesBtn,
                        color = scheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
