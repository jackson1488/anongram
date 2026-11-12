package com.anongram.app.features.chats.ui.attach.ui.tabs

import android.content.Context
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.anongram.app.core.ui.safeStatusBarPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.ui.attach.api.AttachmentDraft
import com.anongram.app.features.chats.ui.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.ui.attach.ui.components.PermissionDeniedView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

@androidx.compose.runtime.Immutable
data class DeviceContact(
    val name: String,
    val phones: List<String>,
    val username: String = ""
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsTab(
    onSelectContact: (AttachmentDraft.Contact) -> Unit,
    onExpandToFullscreen: () -> Unit = {},
    selectedContacts: List<DeviceContact> = emptyList(),
    onToggleSelectContact: (DeviceContact) -> Unit = {},
    isFullscreen: Boolean = false,
    onBack: () -> Unit = {},
    onQuickCardVisibilityChange: (Boolean) -> Unit = {},
    onOpenQuickCard: (DeviceContact) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = com.anongram.app.features.chats.ui.attach.i18n.LocalAttachStrings.current
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(PermissionUtils.hasContactsPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    if (!hasPermission) {
        PermissionDeniedView(
            message = strings.contactsPermissionDesc,
            onRequestPermission = {
                launcher.launch(android.Manifest.permission.READ_CONTACTS)
            },
            modifier = modifier
        )
        return
    }

    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        isLoading = true
        contacts = withContext(Dispatchers.IO) {
            loadDeviceContacts(context)
        }
        isLoading = false
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else {
            val q = searchQuery.trim().lowercase()
            contacts.filter { contact ->
                contact.name.lowercase().contains(q) ||
                    contact.phones.any { it.replace(" ", "").contains(q) } ||
                    contact.username.lowercase().contains(q)
            }
        }
    }

    val sortedContacts = remember(filteredContacts) {
        filteredContacts.sortedBy { it.name.lowercase() }
    }

    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxSize()) {
        // Фоновый градиент (scrim) СЗАДИ поиска, переходящий в прозрачность (zIndex = 5f)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(68.dp)
                .zIndex(5f)
                .background(
                    Brush.verticalGradient(
                        0.0f to scheme.surface,
                        0.75f to scheme.surface.copy(alpha = 0.95f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // Парящая строка поиска сверху (zIndex = 10f)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = if (isFullscreen) 6.dp else 4.dp
                )
                .zIndex(10f)
        ) {
            var isFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.inputBg)
                    .border(1.dp, if (isFocused) scheme.accent else scheme.inputBorder, RoundedCornerShape(24.dp))
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
                    .padding(top = 58.dp)
                    .zIndex(1f)
            ) {
                com.anongram.app.features.chats.ui.attach.ui.components.ContactsListSkeleton()
            }
        } else if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (contacts.isEmpty()) strings.contactsEmpty else strings.contactsSearchPlaceholder,
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
                    top = 58.dp,
                    bottom = 84.dp
                )
            ) {
                items(sortedContacts, key = { it.name + (it.phones.firstOrNull() ?: "") }) { contact ->
                    val isSelected = selectedContacts.any { it.name == contact.name }
                    ContactListItemRow(
                        contact = contact,
                        isSelected = isSelected,
                        onClick = {
                            if (selectedContacts.isNotEmpty()) {
                                onToggleSelectContact(contact)
                            } else {
                                onOpenQuickCard(contact)
                            }
                        },
                        onLongClick = { onToggleSelectContact(contact) }
                    )
                    HorizontalDivider(
                        color = scheme.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                }
            }
        }
    }
}

/**
 * Строка контакта в списке со скруглением и аватаром от имени
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactListItemRow(
    contact: DeviceContact,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val avatarColor = remember(contact.name) {
        val colors = listOf(
            Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF10B981),
            Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF06B6D4)
        )
        colors[(contact.name.hashCode().absoluteValue) % colors.size]
    }
    val initials = remember(contact.name) {
        val parts = contact.name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
        else contact.name.take(2).uppercase()
    }

    Row(
        modifier = Modifier
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
        // Аватар с инициалами
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = if (contact.username.isNotBlank()) "${contact.username} • ${contact.phones.firstOrNull() ?: ""}"
                           else contact.phones.firstOrNull() ?: ""
            Text(
                text = subtitle,
                color = scheme.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(scheme.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = LocalAttachStrings.current.selected,
                    tint = scheme.onAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Небольшое компактное окно контакта по быстрому тапу (ТЗ, шаг 15):
 * фиксированная высота по контенту, большой аватар, имя, ID/номер, кнопка "Отправить контакт".
 */
@Composable
internal fun ContactQuickCardSheet(
    contact: DeviceContact,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current
    var selectedPhone by remember(contact) { mutableStateOf(contact.phones.firstOrNull() ?: "") }

    val avatarColor = remember(contact.name) {
        val colors = listOf(
            Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF10B981),
            Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF06B6D4)
        )
        colors[(contact.name.hashCode().absoluteValue) % colors.size]
    }
    val initials = remember(contact.name) {
        val parts = contact.name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
        else contact.name.take(2).uppercase()
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
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
                .clickable(enabled = false) {}
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

            // Большой аватар
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Имя
            Text(
                text = contact.name,
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ID / Юзернейм или телефон
            val idLabel = if (contact.username.isNotBlank()) contact.username else selectedPhone
            Text(
                text = idLabel,
                color = scheme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // Если у контакта несколько номеров — выбор номера (ТЗ, шаг 15)
            if (contact.phones.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = strings.choosePhonePrompt,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                contact.phones.forEach { phone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPhone = phone }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedPhone == phone),
                            onClick = { selectedPhone = phone },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = scheme.accent,
                                unselectedColor = scheme.textMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = phone, color = scheme.textPrimary, fontSize = 14.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = selectedPhone,
                    color = scheme.textSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка "Отправить контакт"
            Button(
                onClick = { onSend(selectedPhone) },
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

private fun loadDeviceContacts(context: Context): List<DeviceContact> {
    val list = mutableListOf<DeviceContact>()
    val contentResolver = context.contentResolver
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

    try {
        contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            val map = mutableMapOf<String, MutableList<String>>()

            while (cursor.moveToNext()) {
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                val number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
                if (name.isNotBlank() && number.isNotBlank()) {
                    map.getOrPut(name) { mutableListOf() }.add(number)
                }
            }

            map.forEach { (name, phones) ->
                list.add(DeviceContact(name = name, phones = phones.distinct()))
            }
        }
    } catch (_: Exception) {}

    return list
}
