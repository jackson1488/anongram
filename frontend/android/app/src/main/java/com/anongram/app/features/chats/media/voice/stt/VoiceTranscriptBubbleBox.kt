package com.anongram.app.features.chats.media.voice.stt

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.assets.AssetDownloadState
import com.anongram.app.core.assets.ExternalAssetManager
import com.anongram.app.core.assets.ModuleAssetSpec
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import kotlinx.coroutines.delay

/**
 * Блок расшифровки голосового сообщения с AI-анимацией стриминга токен за токеном
 * и мигающим курсором `▍`, а также 1-тап загрузкой недостающей модели.
 */
@Composable
fun VoiceTranscriptBubbleBox(
    cacheKey: String,
    audioUri: String?,
    bubbleTextColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current
    val assetManager = remember { ExternalAssetManager.getInstance(context) }
    val assetStates by assetManager.states.collectAsState()

    var retryToken by remember { mutableIntStateOf(0) }
    var targetText by remember(cacheKey) { mutableStateOf(VoiceTranscriptCache.get(cacheKey).orEmpty()) }
    var shownWordCount by remember(cacheKey) {
        mutableIntStateOf(if (targetText.isNotBlank()) Int.MAX_VALUE else 0)
    }
    var isTranscribing by remember(cacheKey) { mutableStateOf(targetText.isBlank()) }
    var missingSpec by remember(cacheKey) { mutableStateOf<ModuleAssetSpec?>(null) }
    var errorMessage by remember(cacheKey) { mutableStateOf<String?>(null) }

    // Если пользователь скачал недостающую модель прямо из плашки — автоматически запускаем расшифровку
    val missingId = missingSpec?.id
    val missingDownloadState = if (missingId != null) assetStates[missingId] else null
    LaunchedEffect(missingDownloadState) {
        if (missingSpec != null && missingDownloadState is AssetDownloadState.Installed) {
            missingSpec = null
            retryToken += 1
        }
    }

    LaunchedEffect(cacheKey, retryToken) {
        val cached = VoiceTranscriptCache.get(cacheKey)
        if (!cached.isNullOrBlank()) {
            targetText = cached
            shownWordCount = Int.MAX_VALUE
            isTranscribing = false
            missingSpec = null
            errorMessage = null
            return@LaunchedEffect
        }

        if (audioUri.isNullOrBlank()) {
            isTranscribing = false
            errorMessage = "Audio file missing"
            return@LaunchedEffect
        }

        isTranscribing = true
        missingSpec = null
        errorMessage = null
        targetText = ""
        shownWordCount = 0

        try {
            VoiceTranscriber.transcribe(context, audioUri).collect { event ->
                when (event) {
                    is TranscriptionEvent.Streaming -> {
                        if (event.partialText.isNotBlank()) {
                            targetText = event.partialText
                        }
                    }
                    is TranscriptionEvent.Completed -> {
                        targetText = event.finalText
                        isTranscribing = false
                        if (event.finalText.isNotBlank()) {
                            VoiceTranscriptCache.put(cacheKey, event.finalText)
                        }
                    }
                    is TranscriptionEvent.ModelMissing -> {
                        missingSpec = event.spec
                        isTranscribing = false
                    }
                    is TranscriptionEvent.Error -> {
                        errorMessage = event.message
                        isTranscribing = false
                    }
                }
            }
        } catch (t: Throwable) {
            errorMessage = t.message ?: "Error"
            isTranscribing = false
        }
    }

    // AI token-by-token (word-by-word) стриминг-анимация
    val tokens = remember(targetText) {
        if (targetText.isBlank()) emptyList()
        else targetText.trim().split(Regex("\\s+"))
    }
    LaunchedEffect(tokens, isTranscribing) {
        while (shownWordCount < tokens.size) {
            shownWordCount += 1
            delay(42L)
        }
    }

    val isAnimatingTokens = shownWordCount < tokens.size
    val showCursor = isTranscribing || isAnimatingTokens

    val infiniteTransition = rememberInfiniteTransition(label = "TranscriptCursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TranscriptCursorAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bubbleTextColor.copy(alpha = 0.12f))
            .animateContentSize(animationSpec = tween(180))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val spec = missingSpec
        if (spec != null) {
            val dlState = assetStates[spec.id] ?: AssetDownloadState.NotInstalled
            Text(
                text = strings.voiceModelMissingTitle,
                color = bubbleTextColor.copy(alpha = 0.92f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            when (dlState) {
                is AssetDownloadState.Downloading -> {
                    Text(
                        text = strings.downloadingProgress((dlState.progress * 100).toInt()),
                        color = bubbleTextColor.copy(alpha = 0.8f),
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { dlState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = bubbleTextColor.copy(alpha = 0.2f)
                    )
                }
                is AssetDownloadState.Extracting -> {
                    Text(
                        text = strings.extractingLabel,
                        color = bubbleTextColor.copy(alpha = 0.8f),
                        fontSize = 11.5.sp
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor)
                            .clickable { assetManager.startDownload(spec) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = scheme.onAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = strings.voiceDownloadModelAction(spec.approxSizeMb),
                            color = scheme.onAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else if (errorMessage != null && targetText.isBlank()) {
            Text(
                text = errorMessage!!,
                color = CrimsonKillSwitch,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        VoiceTranscriptCache.remove(cacheKey)
                        retryToken += 1
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = strings.voiceRetryTranscript,
                    tint = bubbleTextColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = strings.voiceRetryTranscript,
                    color = bubbleTextColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val visibleWords = if (shownWordCount >= tokens.size) {
                tokens
            } else {
                tokens.take(shownWordCount)
            }
            val displayString = visibleWords.joinToString(" ")
            val annotated = buildAnnotatedString {
                if (displayString.isEmpty() && isTranscribing) {
                    withStyle(SpanStyle(color = bubbleTextColor.copy(alpha = 0.68f))) {
                        append(strings.voiceTranscribing)
                    }
                } else {
                    append(displayString)
                }
                if (showCursor) {
                    withStyle(SpanStyle(color = accentColor.copy(alpha = cursorAlpha))) {
                        append(" ▍")
                    }
                }
            }

            Text(
                text = annotated,
                color = bubbleTextColor.copy(alpha = 0.92f),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            if (!showCursor && targetText.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                VoiceTranscriptCache.remove(cacheKey)
                                retryToken += 1
                            }
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = strings.voiceRetryTranscript,
                            tint = bubbleTextColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.voiceRetryTranscript,
                            color = bubbleTextColor.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(targetText))
                                Toast.makeText(context, strings.voiceCopiedToast, Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = strings.voiceCopyTranscript,
                            tint = bubbleTextColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.voiceCopyTranscript,
                            color = bubbleTextColor.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
