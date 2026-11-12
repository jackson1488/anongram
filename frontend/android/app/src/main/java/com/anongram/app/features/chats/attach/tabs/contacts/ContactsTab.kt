package com.anongram.app.features.chats.attach.tabs.contacts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.NeonEmerald
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.components.AttachHeaderScrim
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.components.ContactsListSkeleton
import com.anongram.app.features.contacts.common.Contact
import com.anongram.app.features.contacts.data.InMemoryContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsTab(
    onSelectContact: (AttachmentDraft.Contact) -> Unit,
    onExpandToFullscreen: () -> Unit = {},
    selectedContacts: List<Contact> = emptyList(),
    onToggleSelectContact: (Contact) -> Unit = {},
    isFullscreen: Boolean = false,
    onBack: () -> Unit = {},
    onQuickCardVisibilityChange: (Boolean) -> Unit = {},
    onOpenQuickCard: ((Contact) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current

    val repo = remember { InMemoryContactRepository() }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var internalQuickCardContact by remember { mutableStateOf<Contact?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        contacts = withContext(Dispatchers.IO) {
            repo.loadContacts()
        }
        isLoading = false
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            val q = searchQuery.trim().lowercase()
            contacts.filter { contact ->
                contact.name.lowercase().contains(q) ||
                    contact.fingerprint.lowercase().contains(q)
            }
        }
    }

    val sortedContacts = remember(filteredContacts) {
        filteredContacts.sortedBy { it.name.lowercase() }
    }

    val openQuickCardAction: (Contact) -> Unit = { contact ->
        if (onOpenQuickCard != null) {
            onOpenQuickCard(contact)
        } else {
            internalQuickCardContact = contact
            onQuickCardVisibilityChange(true)
        }
    }

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topListPadding = if (isFullscreen) statusBarTopDp + 64.dp else 64.dp

    Box(modifier = modifier.fillMaxSize()) {
        // Фоновый градиент (scrim) СЗАДИ поиска, переходящий в прозрачность (zIndex = 5f)
        AttachHeaderScrim(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(5f),
            includeStatusBar = isFullscreen
        )

        // Парящая строка поиска сверху (zIndex = 10f)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(if (isFullscreen) Modifier.safeStatusBarPadding() else Modifier)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .zIndex(10f)
        ) {
            var isFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(scheme.inputBg)
                    .border(1.dp, if (isFocused) scheme.accent else scheme.inputBorder, RoundedCornerShape(25.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.search,
                    tint = scheme.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = strings.contactsSearchPlaceholder,
                            color = scheme.textMuted,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = scheme.textPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(scheme.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                isFocused = it.isFocused
                                if (it.isFocused && !isFullscreen) onExpandToFullscreen()
                            }
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Список контактов (zIndex = 1f) — прокручивается ПОЗАДИ строки поиска
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topListPadding)
                    .zIndex(1f)
            ) {
                ContactsListSkeleton()
            }
        } else if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (contacts.isEmpty()) strings.contactsEmpty else strings.nothingFound,
                    color = scheme.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .zIndex(1f),
                contentPadding = PaddingValues(
                    top = topListPadding,
                    bottom = 84.dp
                )
            ) {
                items(sortedContacts, key = { it.id }) { contact ->
                    val isSelected = selectedContacts.any { it.id == contact.id }
                    ContactListItemRow(
                        contact = contact,
                        isSelected = isSelected,
                        onClick = {
                            if (selectedContacts.isNotEmpty()) {
                                onToggleSelectContact(contact)
                            } else {
                                openQuickCardAction(contact)
                            }
                        },
                        onLongClick = { onToggleSelectContact(contact) },
                        onToggleSelect = { onToggleSelectContact(contact) }
                    )
                    HorizontalDivider(
                        color = scheme.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                }
            }
        }

        // Отображение быстрой карточки контакта, если она открыта внутри ContactsTab
        if (internalQuickCardContact != null) {
            ContactQuickCardSheet(
                contact = internalQuickCardContact!!,
                onSelectContact = { contactDraft ->
                    internalQuickCardContact = null
                    onQuickCardVisibilityChange(false)
                    onSelectContact(contactDraft)
                },
                onDismiss = {
                    internalQuickCardContact = null
                    onQuickCardVisibilityChange(false)
                }
            )
        }
    }
}

/**
 * Строка контакта в списке со скруглением, монограммой, индикатором онлайн и криптографическим отпечатком
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactListItemRow(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) scheme.accentSoft else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар-монограмма с онлайн-индикатором
        Box(modifier = Modifier.size(44.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(scheme.accent.copy(alpha = 0.16f))
                    .border(1.dp, scheme.accent.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.monogram,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (contact.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(NeonEmerald)
                        .border(1.5.dp, scheme.surface, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Имя и криптографический отпечаток
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.name,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.isSasVerified) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = strings.sasVerified,
                        tint = scheme.accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.fingerprint,
                color = scheme.textSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Кружок выбора справа
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) scheme.accent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) scheme.accent else scheme.textMuted.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable { onToggleSelect() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = strings.selected,
                    tint = scheme.onAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Компактное окно быстрого просмотра контакта (Quick Card Sheet):
 * монограмма, имя, онлайн-статус, Ed25519 отпечаток, SAS-статус и кнопка "Отправить контакт".
 */
@Composable
fun ContactQuickCardSheet(
    contact: Contact,
    onSelectContact: (AttachmentDraft.Contact) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.overlayScrim)
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.cardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (delta > 30f) onDismiss()
                    }
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ручка перетягивания
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(scheme.textMuted.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Большой аватар с монограммой и онлайн-индикатором
            Box(modifier = Modifier.size(72.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(scheme.accent.copy(alpha = 0.16f))
                        .border(1.5.dp, scheme.accent.copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.monogram,
                        color = scheme.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (contact.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(NeonEmerald)
                            .border(2.dp, scheme.surface, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Имя контакта
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = contact.name,
                    color = scheme.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.isSasVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = strings.sasVerified,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Онлайн статус
            Text(
                text = if (contact.isOnline) strings.online else strings.offline,
                color = if (contact.isOnline) NeonEmerald else scheme.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Криптографический отпечаток (с возможностью копирования)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.inputBg)
                    .border(1.dp, scheme.inputBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(contact.fingerprint))
                        isCopied = true
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.fingerprintLabel,
                        color = scheme.textMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (isCopied) strings.copiedFingerprint else strings.copyFingerprint,
                        color = scheme.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.fingerprint,
                    color = scheme.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Статус SAS-верификации
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (contact.isSasVerified) scheme.accentSoft else scheme.inputBg)
                    .border(
                        1.dp,
                        if (contact.isSasVerified) scheme.accent.copy(alpha = 0.35f) else scheme.inputBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = if (contact.isSasVerified) scheme.accent else scheme.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (contact.isSasVerified) strings.sasVerified else strings.sasNotVerified,
                    color = if (contact.isSasVerified) scheme.textPrimary else scheme.textMuted,
                    fontSize = 13.sp,
                    fontWeight = if (contact.isSasVerified) FontWeight.Medium else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка "Отправить контакт"
            Button(
                onClick = {
                    onSelectContact(
                        AttachmentDraft.Contact(
                            name = contact.name,
                            fingerprint = contact.fingerprint,
                            isSasVerified = contact.isSasVerified
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = scheme.accent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = strings.sendContact,
                    color = scheme.onAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Перегрузка для совместимости с вызовами через onSend(Contact)
 */
@Composable
fun ContactQuickCardSheet(
    contact: Contact,
    onDismiss: () -> Unit,
    onSend: (Contact) -> Unit
) {
    ContactQuickCardSheet(
        contact = contact,
        onSelectContact = { onSend(contact) },
        onDismiss = onDismiss
    )
}
