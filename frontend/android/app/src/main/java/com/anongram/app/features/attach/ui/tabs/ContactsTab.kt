package com.anongram.app.features.attach.ui.tabs

import android.content.Context
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.anongram.app.core.ui.safeStatusBarPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachCardDark
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.permissions.PermissionUtils
import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.attach.ui.components.PermissionDeniedView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

/**
 * Флаг временного демо-режима контактов (ТЗ, шаг 14).
 * Пока он включён, генерируются 100 реалистичных контактов.
 */
const val DEMO_CONTACTS = true

data class DeviceContact(
    val name: String,
    val phones: List<String>,
    val username: String = ""
)

private val PurpleAccent = Color(0xFF8B5CF6)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsTab(
    onSelectContact: (AttachmentDraft.Contact) -> Unit,
    onExpandToFullscreen: () -> Unit = {},
    selectedContacts: List<DeviceContact> = emptyList(),
    onToggleSelectContact: (DeviceContact) -> Unit = {},
    isFullscreen: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(DEMO_CONTACTS || PermissionUtils.hasContactsPermission(context))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    if (!hasPermission && !DEMO_CONTACTS) {
        PermissionDeniedView(
            message = "Для выбора и отправки контактов предоставьте доступ к списку контактов.",
            onRequestPermission = {
                launcher.launch(android.Manifest.permission.READ_CONTACTS)
            },
            modifier = modifier
        )
        return
    }

    var contacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var quickCardContact by remember { mutableStateOf<DeviceContact?>(null) }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) {
            if (DEMO_CONTACTS) {
                generateDemoContacts()
            } else {
                loadDeviceContacts(context)
            }
        }
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

    // Группировка по буквам алфавита для stickyHeader (ТЗ, шаг 14)
    val groupedContacts = remember(filteredContacts) {
        filteredContacts
            .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
            .toSortedMap()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        val headerModifier = if (isFullscreen) {
            Modifier
                .fillMaxWidth()
                .safeStatusBarPadding()
                .padding(top = 10.dp, start = 14.dp, end = 14.dp, bottom = 6.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(top = 34.dp, start = 14.dp, end = 14.dp, bottom = 6.dp)
        }

        Box(
            modifier = headerModifier,
            contentAlignment = Alignment.Center
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск контактов", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Поиск",
                        tint = Color(0xFF94A3B8)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Очистить",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AttachCardDark,
                    unfocusedContainerColor = AttachCardDark,
                    focusedBorderColor = PurpleAccent,
                    unfocusedBorderColor = AttachBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .onFocusChanged { if (it.isFocused && !isFullscreen) onExpandToFullscreen() }
            )
        }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (contacts.isEmpty()) "Загрузка контактов..." else "Контакты не найдены",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 84.dp)
                    ) {
                    groupedContacts.forEach { (letter, contactList) ->
                        stickyHeader(key = "header_$letter") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A))
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = letter.toString(),
                                    color = PurpleAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        items(contactList, key = { it.name + it.phones.firstOrNull() }) { contact ->
                            val isSelected = selectedContacts.any { it.name == contact.name }
                            ContactListItemRow(
                                contact = contact,
                                isSelected = isSelected,
                                onClick = { quickCardContact = contact },
                                onLongClick = { onToggleSelectContact(contact) }
                            )
                            HorizontalDivider(
                                color = Color(0x15FFFFFF),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 58.dp)
                            )
                        }
                    }
                }
            }
        }
    }

        // Быстрое нижнее окно контакта (ТЗ, шаг 15: компактное окно по высоте контента)
        if (quickCardContact != null) {
            ContactQuickCardSheet(
                contact = quickCardContact!!,
                onSend = { phoneToSend ->
                    onSelectContact(
                        AttachmentDraft.Contact(
                            name = quickCardContact!!.name,
                            phones = listOf(phoneToSend)
                        )
                    )
                    quickCardContact = null
                },
                onDismiss = { quickCardContact = null }
            )
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
            .background(if (isSelected) PurpleAccent.copy(alpha = 0.15f) else Color.Transparent)
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
                color = Color.White,
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
                color = Color(0xFF94A3B8),
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
                    .background(PurpleAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Выбран",
                    tint = Color.White,
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
private fun ContactQuickCardSheet(
    contact: DeviceContact,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
            .background(Color(0x99000000))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color(0xFF131D31))
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
                    .background(Color(0x44FFFFFF))
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
                color = Color.White,
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
                color = PurpleAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // Если у контакта несколько номеров — выбор номера (ТЗ, шаг 15)
            if (contact.phones.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Выберите номер для отправки:",
                    color = Color(0xFF94A3B8),
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
                            colors = RadioButtonDefaults.colors(selectedColor = PurpleAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = phone, color = Color.White, fontSize = 14.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = selectedPhone,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка "Отправить контакт"
            Button(
                onClick = { onSend(selectedPhone) },
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Отправить контакт",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Генерация 100 реалистичных контактов (русские и киргизские имена, телефоны +996 ..., ТЗ шаг 14)
 */
private fun generateDemoContacts(): List<DeviceContact> {
    val firstNamesRu = listOf(
        "Александр", "Алексей", "Андрей", "Борис", "Валерий", "Виктор", "Владимир",
        "Денис", "Дмитрий", "Евгений", "Иван", "Игорь", "Константин", "Максим",
        "Михаил", "Николай", "Олег", "Павел", "Роман", "Сергей", "Юрий",
        "Анна", "Елена", "Ирина", "Мария", "Ольга", "Татьяна", "Светлана"
    )
    val lastNamesRu = listOf(
        "Иванов", "Смирнов", "Васильев", "Кузнецов", "Соколов", "Попов", "Лебедев",
        "Козлов", "Новиков", "Морозов", "Петров", "Волков", "Соловьев", "Павлов"
    )

    val firstNamesKg = listOf(
        "Азамат", "Адилет", "Айбек", "Айдар", "Алмаз", "Аман", "Аскар", "Бакыт",
        "Данияр", "Жаныбек", "Ильяс", "Канат", "Кубат", "Марат", "Мирлан", "Нурлан",
        "Руслан", "Талант", "Улан", "Эркин", "Айгерим", "Айжамал", "Айдана",
        "Айсулуу", "Бермет", "Гульзат", "Динара", "Жибек", "Мээрим", "Назира"
    )
    val lastNamesKg = listOf(
        "Бакиров", "Токтогулов", "Жумабеков", "Касымов", "Нурбеков", "Султанов",
        "Садыков", "Осмонов", "Беков", "Асанов", "Маратов", "Темиров", "Алиев"
    )

    val list = mutableListOf<DeviceContact>()
    var seed = 1001

    // Генерация 50 кыргызских контактов
    for (i in 0 until 50) {
        val fn = firstNamesKg[i % firstNamesKg.size]
        val ln = lastNamesKg[(i * 3) % lastNamesKg.size]
        val prefix = listOf("+996 550", "+996 700", "+996 770", "+996 500")[i % 4]
        val num = String.format("%s %02d%02d%02d", prefix, (seed % 90 + 10), ((seed * 7) % 90 + 10), ((seed * 13) % 90 + 10))
        val hasMulti = (i % 7 == 0)
        val phones = if (hasMulti) listOf(num, "+996 312 908070") else listOf(num)
        val username = "@${fn.lowercase()}_kg"
        list.add(DeviceContact(name = "$fn $ln", phones = phones, username = username))
        seed += 37
    }

    // Генерация 50 русских контактов
    for (i in 0 until 50) {
        val fn = firstNamesRu[i % firstNamesRu.size]
        val ln = lastNamesRu[(i * 2) % lastNamesRu.size]
        val prefix = listOf("+996 555", "+996 705", "+996 775", "+996 999")[i % 4]
        val num = String.format("%s %02d%02d%02d", prefix, (seed % 90 + 10), ((seed * 5) % 90 + 10), ((seed * 11) % 90 + 10))
        val hasMulti = (i % 6 == 0)
        val phones = if (hasMulti) listOf(num, "+996 312 664422") else listOf(num)
        val username = "@${fn.lowercase()}_${ln.lowercase().take(4)}"
        list.add(DeviceContact(name = "$fn $ln", phones = phones, username = username))
        seed += 43
    }

    return list.sortedBy { it.name }
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
