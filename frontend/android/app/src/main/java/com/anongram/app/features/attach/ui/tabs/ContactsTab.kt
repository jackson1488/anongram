package com.anongram.app.features.attach.ui.tabs

import android.content.Context
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class DeviceContact(
    val name: String,
    val phones: List<String>
)

@Composable
fun ContactsTab(
    onSelectContact: (AttachmentDraft.Contact) -> Unit,
    modifier: Modifier = Modifier
) {
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
    var selectedContactForMultiPhone by remember { mutableStateOf<DeviceContact?>(null) }
    var chosenPhone by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) {
            loadDeviceContacts(context)
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else {
            val q = searchQuery.trim().lowercase()
            contacts.filter { contact ->
                contact.name.lowercase().contains(q) ||
                    contact.phones.any { it.contains(q) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Pill
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск контактов", color = Color(0xFF64748B), fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF94A3B8)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF131D31),
                unfocusedContainerColor = Color(0xFF131D31),
                focusedBorderColor = Color(0xFF8B5CF6),
                unfocusedBorderColor = Color(0x22FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (contacts.isEmpty()) "Список контактов пуст" else "Контакты не найдены",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredContacts, key = { it.name + it.phones.joinToString() }) { contact ->
                    ContactRow(
                        contact = contact,
                        onClick = {
                            if (contact.phones.size > 1) {
                                selectedContactForMultiPhone = contact
                                chosenPhone = contact.phones.firstOrNull()
                            } else {
                                onSelectContact(
                                    AttachmentDraft.Contact(
                                        name = contact.name,
                                        phones = contact.phones
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Phone picker dialog if multiple phone numbers
    selectedContactForMultiPhone?.let { contact ->
        AlertDialog(
            onDismissRequest = { selectedContactForMultiPhone = null },
            title = {
                Text(
                    text = "Выберите номер телефона",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = contact.name,
                        color = Color(0xFF8B5CF6),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    contact.phones.forEach { phone ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chosenPhone = phone }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = (chosenPhone == phone),
                                onClick = { chosenPhone = phone },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF8B5CF6),
                                    unselectedColor = Color(0xFF94A3B8)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = phone, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = chosenPhone ?: contact.phones.firstOrNull() ?: ""
                        selectedContactForMultiPhone = null
                        onSelectContact(
                            AttachmentDraft.Contact(
                                name = contact.name,
                                phones = listOf(p)
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Выбрать", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedContactForMultiPhone = null }) {
                    Text("Отмена", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF182238),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ContactRow(
    contact: DeviceContact,
    onClick: () -> Unit
) {
    val avatarBg = remember(contact.name) {
        val colors = listOf(
            Color(0xFF8B5CF6),
            Color(0xFF3B82F6),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFFEC4899),
            Color(0xFF06B6D4)
        )
        val idx = contact.name.hashCode().absoluteValue % colors.size
        colors[idx]
    }

    val initials = remember(contact.name) {
        val parts = contact.name.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
        } else {
            contact.name.take(2).uppercase()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.phones.joinToString(", "),
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun loadDeviceContacts(context: Context): List<DeviceContact> {
    val result = mutableListOf<DeviceContact>()
    val contentResolver = context.contentResolver
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

    try {
        val cursor = contentResolver.query(uri, projection, null, null, sortOrder)
        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val map = linkedMapOf<String, MutableList<String>>()

            while (c.moveToNext()) {
                val name = if (nameIndex >= 0) c.getString(nameIndex) ?: "Без имени" else "Без имени"
                val number = if (numberIndex >= 0) c.getString(numberIndex) ?: "" else ""
                if (number.isNotBlank()) {
                    val list = map.getOrPut(name) { mutableListOf() }
                    if (!list.contains(number)) {
                        list.add(number)
                    }
                }
            }

            for ((name, phones) in map) {
                result.add(DeviceContact(name, phones))
            }
        }
    } catch (_: Exception) {}
    return result
}
