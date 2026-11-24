package com.anongram.app.features.chats.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.anonGramProgressiveHeaderBlur
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.ui.accentColor
import com.anongram.app.features.contacts.common.Contact
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Экран поиска (Telegram-style): круглая стеклянная кнопка «Назад»,
 * закруглённая капсула ввода с автофокусом и кнопкой очистки,
 * бесшовный прогрессивный блюр над результатами «ЧАТЫ» и «ЛЮДИ».
 */
@Composable
fun ChatSearchScreen(
    viewModel: ChatSearchViewModel,
    onBack: () -> Unit,
    onSessionOpen: (IChatSession) -> Unit,
    onContactOpen: (Contact) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val scheme = LocalAnonGramScheme.current
    val animState by AnimationsSettingsStore.state.collectAsState()
    val hazeState = remember { HazeState() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) { }
    }

    val topBarBackgroundModifier = Modifier.anonGramProgressiveHeaderBlur(
        hazeState = hazeState,
        glassBlurEnabled = animState.glassBlurEnabled,
        scrollFraction = 0.5f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // 1. Основной контент результатов (прокручивается под стеклянную строку поиска)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(scheme.background)
        ) {
            when {
                !results.hasQuery -> SearchHint(text = "Введите запрос: имя, отпечаток ключа или текст")
                results.chats.isEmpty() && results.people.isEmpty() -> SearchHint(text = "Ничего не найдено")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 66.dp,
                        bottom = 112.dp
                    )
                ) {
                    if (results.chats.isNotEmpty()) {
                        item(key = "chats_header") { SearchSectionHeader(label = "ЧАТЫ") }
                        items(results.chats.size, key = { "chat_${results.chats[it].id}" }) { index ->
                            val session = results.chats[index]
                            SearchSessionRow(session = session, onClick = { onSessionOpen(session) })
                        }
                    }
                    if (results.people.isNotEmpty()) {
                        item(key = "people_header") { SearchSectionHeader(label = "ЛЮДИ") }
                        items(results.people.size, key = { "person_${results.people[it].id}" }) { index ->
                            val contact = results.people[index]
                            SearchContactRow(contact = contact, onClick = { onContactOpen(contact) })
                        }
                    }
                }
            }
        }

        // 2. Парящая панель поиска в стиле Telegram с бесшовным блюром
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .then(topBarBackgroundModifier)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Круглая стеклянная кнопка «Назад»
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .settingsGlassPill(
                            hazeState = hazeState,
                            shape = CircleShape,
                            glassBlurEnabled = animState.glassBlurEnabled
                        )
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Закруглённая капсула поиска Telegram
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(scheme.inputBg)
                        .border(0.8.dp, scheme.inputBorder, RoundedCornerShape(21.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = if (query.isNotEmpty()) 28.dp else 0.dp)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(scheme.accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                        }),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Поиск",
                                        color = scheme.textMuted,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(scheme.textMuted.copy(alpha = 0.20f))
                                .clickable { viewModel.onQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Очистить",
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SearchSectionHeader(label: String) {
    val scheme = LocalAnonGramScheme.current

    Text(
        text = label,
        color = scheme.textMuted,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchSessionRow(session: IChatSession, onClick: () -> Unit) {
    val scheme = LocalAnonGramScheme.current
    val accent = session.accentColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = session.avatarMonogram ?: "··",
                fontWeight = FontWeight.Bold,
                color = scheme.textPrimary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                color = scheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${session.securityLabel} · ${session.fingerprint}",
                color = scheme.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = accent.copy(alpha = 0.7f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun SearchContactRow(contact: Contact, onClick: () -> Unit) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(scheme.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.monogram,
                fontWeight = FontWeight.Bold,
                color = scheme.textPrimary,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = scheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.fingerprint,
                color = scheme.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SearchHint(text: String) {
    val scheme = LocalAnonGramScheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = scheme.textMuted, fontSize = 13.sp)
    }
}
