package com.anongram.app.features.chats.messages.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.messages.ui.components.ChatInputBar
import com.anongram.app.features.chats.messages.ui.components.ChatTopBar
import com.anongram.app.features.chats.messages.ui.components.MessagesList
import com.anongram.app.features.chats.messages.ui.components.ReplyBar
import com.anongram.app.features.chats.messages.ui.components.ScrollDownButton
import kotlinx.coroutines.launch

/**
 * Экран переписки (интерфейс Telegram): тулбар с онлайн-статусом и «печатает…»,
 * обратный список сообщений с группировкой и дат-чипами, кнопка «вниз»,
 * полоса ответа и поле ввода с самолётиком/микрофоном.
 */
@Composable
fun ChatScreen(
    session: IChatSession,
    viewModel: ChatMessagesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val showScrollDown by remember(listState) {
        derivedStateOf { listState.canScrollForward }
    }

    BackHandler(enabled = state.replyTo != null) {
        viewModel.cancelReply()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        ChatTopBar(
            session = session,
            isPeerTyping = state.isPeerTyping,
            onBack = onBack,
            onClearHistory = viewModel::clearHistory
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            MessagesList(
                messages = state.messages,
                listState = listState,
                onReply = viewModel::startReply,
                onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
                onDelete = { message -> viewModel.deleteMessage(message.id) }
            )

            ScrollDownButton(
                visible = showScrollDown,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(index = 0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp)
            )
        }

        state.replyTo?.let { reply ->
            ReplyBar(snippet = reply.text, onClose = viewModel::cancelReply)
        }

        ChatInputBar(
            input = state.input,
            canSend = state.canSend,
            onInputChange = viewModel::onInputChange,
            onSend = viewModel::send
        )
    }
}
