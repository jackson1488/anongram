package com.anongram.web.features.chats.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.common.timeLabel
import com.anongram.web.features.chats.mainmenu.components.ChatListItemCard
import com.anongram.web.features.chats.mainmenu.components.SearchRow
import com.anongram.web.features.contacts.common.Contact

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeStatusBarPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = scheme.textPrimary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .clickable(onClick = onBack)
            )
            Box(modifier = Modifier.weight(1f)) {
                SearchRow(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = "Поиск по чатам и отпечаткам"
                )
            }
        }

        if (!results.hasQuery) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Введите имя, фразу или 0x-отпечаток", color = scheme.textMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (results.chats.isNotEmpty()) {
                    item(key = "hdr_chats") {
                        Text(
                            text = "ЧАТЫ",
                            color = scheme.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(results.chats, key = { "c_${it.id}" }) { session ->
                        ChatListItemCard(
                            session = session,
                            timeLabel = session.timeLabel(),
                            selectionMode = false,
                            isSelected = false,
                            onClick = { onSessionOpen(session) },
                            onLongClick = {}
                        )
                    }
                }

                if (results.people.isNotEmpty()) {
                    item(key = "hdr_people") {
                        Text(
                            text = "ЛЮДИ",
                            color = scheme.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }
                    items(results.people, key = { "p_${it.id}" }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactOpen(contact) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(scheme.accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(contact.monogram, color = scheme.textPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(contact.name, color = scheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(contact.fingerprint, color = scheme.textMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
