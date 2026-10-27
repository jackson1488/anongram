package com.anongram.app.features.chats.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor

private enum class AttachTab(val label: String, val isSoon: Boolean = false) {
    GALLERY("Галерея"),
    FILE("Файл"),
    GEO("Геопозиция"),
    ARTICLE("Статья", isSoon = true),
    CONTACT("Контакт"),
    MUSIC("Музыка"),
    LIST("Список", isSoon = true)
}

/**
 * Меню вложений (скрепка) — шаги 4 и 5:
 * нижнее модальное окно с ручкой перетаскивания (50%-100%),
 * плавающая панель-таблетка со вкладками внизу:
 * Галерея, Файл, Геопозиция, Статья (скоро), Контакт, Музыка, Список (скоро).
 */
@Composable
internal fun AttachBottomSheet(
    onDismiss: () -> Unit,
    onSendAttachment: (MessageAttachment, String) -> Unit,
    onOpenGeoPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(AttachTab.GALLERY) }
    val scheme = LocalAnonGramScheme.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceDark)
                .clickable(enabled = false) {}
        ) {
            // Ручка для перетаскивания
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.5f))
            )

            // Контент активной вкладки
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    AttachTab.GALLERY -> GalleryTabContent(onSend = { att, cap ->
                        onSendAttachment(att, cap)
                        onDismiss()
                    })
                    AttachTab.FILE -> FileTabContent(onSend = { att ->
                        onSendAttachment(att, "")
                        onDismiss()
                    })
                    AttachTab.GEO -> {
                        // Быстрый переход в полноэкранный пикер
                        onOpenGeoPicker()
                    }
                    AttachTab.CONTACT -> ContactTabContent(onSend = { att ->
                        onSendAttachment(att, "")
                        onDismiss()
                    })
                    AttachTab.MUSIC -> MusicTabContent(onSend = { att ->
                        onSendAttachment(att, "")
                        onDismiss()
                    })
                    AttachTab.ARTICLE, AttachTab.LIST -> SoonTabContent(tabName = activeTab.label)
                }
            }

            // Внизу плавающая панель-таблетка со вкладками
            AttachTabBar(
                activeTab = activeTab,
                onTabSelect = { tab ->
                    if (tab == AttachTab.GEO) {
                        onOpenGeoPicker()
                    } else {
                        activeTab = tab
                    }
                }
            )
        }
    }
}

@Composable
private fun AttachTabBar(
    activeTab: AttachTab,
    onTabSelect: (AttachTab) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(IslandBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttachTab.values().forEach { tab ->
            val isSelected = tab == activeTab
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) scheme.accent else Color.Transparent)
                    .clickable { onTabSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (tab.isSoon) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Скоро",
                        tint = if (isSelected) Color.White else scheme.accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// --- Вкладка: Галерея ---

@Composable
private fun GalleryTabContent(
    onSend: (MessageAttachment, String) -> Unit
) {
    var withoutCompression by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }
    val selectedIndices = remember { mutableStateListOf<Int>() }
    val scheme = LocalAnonGramScheme.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Без сжатия", color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = withoutCompression,
                onCheckedChange = { withoutCompression = it },
                colors = SwitchDefaults.colors(checkedThumbColor = scheme.accent)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selectedIndices.isNotEmpty()) {
                Button(
                    onClick = {
                        val first = selectedIndices.first()
                        onSend(
                            MessageAttachment.Photo(
                                label = "IMG_${1000 + first}.jpg",
                                colorSeed = first
                            ),
                            caption
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.accent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = "Отправить (${selectedIndices.size})", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Сетка фото
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(18) { index ->
                val isSelected = index in selectedIndices
                Box(
                    modifier = Modifier
                        .height(110.dp)
                        .background(attachmentColor(index))
                        .clickable {
                            if (isSelected) selectedIndices.remove(index) else selectedIndices.add(index)
                        },
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(scheme.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Вкладка: Файл ---

private data class DemoFile(val name: String, val size: String, val kind: FileKind)

@Composable
private fun FileTabContent(onSend: (MessageAttachment) -> Unit) {
    val demoFiles = remember {
        listOf(
            DemoFile("contract_e2ee.pdf", "1.4 МБ", FileKind.PDF),
            DemoFile("backup_keys.zip", "3.8 МБ", FileKind.ARCHIVE),
            DemoFile("anon_whitepaper.doc", "540 КБ", FileKind.DOC),
            DemoFile("audit_report.pdf", "8.2 МБ", FileKind.PDF),
            DemoFile("session_dump.bin", "92 КБ", FileKind.UNKNOWN)
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // 3 карточки сверху
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FileSourceCard(
                icon = Icons.Outlined.Folder,
                title = "Внутреннее",
                subtitle = "Обзор файлов",
                modifier = Modifier.weight(1f),
                onClick = { onSend(MessageAttachment.File("storage_file.pdf", "2.1 МБ", FileKind.PDF)) }
            )
            FileSourceCard(
                icon = Icons.Outlined.Folder,
                title = "Папка приложения",
                subtitle = "AnonGram/Files",
                modifier = Modifier.weight(1f),
                onClick = { onSend(MessageAttachment.File("anongram_log.txt", "45 КБ", FileKind.DOC)) }
            )
            FileSourceCard(
                icon = Icons.Outlined.Image,
                title = "Галерея",
                subtitle = "Без сжатия",
                modifier = Modifier.weight(1f),
                onClick = { onSend(MessageAttachment.File("raw_photo.png", "12 МБ", FileKind.IMAGE)) }
            )
        }

        Text(
            text = "Файлы А-Я",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(demoFiles) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSend(MessageAttachment.File(file.name, file.size, file.kind)) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = file.kind.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = file.name, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = file.size, color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSourceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = LocalAnonGramScheme.current.accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(text = subtitle, color = TextMuted, fontSize = 10.sp, maxLines = 1)
    }
}

// --- Вкладка: Контакт ---

private data class DemoContact(val name: String, val phone: String)

@Composable
private fun ContactTabContent(onSend: (MessageAttachment) -> Unit) {
    var query by remember { mutableStateOf("") }
    val contacts = remember {
        listOf(
            DemoContact("Сатоши Накамото", "+1 (555) 019-2834"),
            DemoContact("Алиса Климова", "+7 (999) 123-45-67"),
            DemoContact("Боб Смит", "+1 (202) 555-0143"),
            DemoContact("Дэвид Чаум", "+31 20 525 9111"),
            DemoContact("Хэл Финни", "+1 (415) 555-0199")
        )
    }
    val filtered = contacts.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SearchInputRow(query = query, onQueryChange = { query = it }, placeholder = "Поиск контактов...")

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSend(MessageAttachment.Contact(contact.name, contact.phone)) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LocalAnonGramScheme.current.accent.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = contact.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = contact.phone, color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- Вкладка: Музыка ---

private data class DemoTrack(val title: String, val artist: String, val duration: String)

@Composable
private fun MusicTabContent(onSend: (MessageAttachment) -> Unit) {
    var query by remember { mutableStateOf("") }
    val tracks = remember {
        listOf(
            DemoTrack("Resonance", "HOME", "3:32"),
            DemoTrack("Nightcall", "Kavinsky", "4:19"),
            DemoTrack("Master Boot Record", "C:\\> CHKDSK /F", "4:56"),
            DemoTrack("Digital Love", "Daft Punk", "4:58")
        )
    }
    val filtered = tracks.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SearchInputRow(query = query, onQueryChange = { query = it }, placeholder = "Поиск треков...")

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSend(MessageAttachment.Music(track.title, track.artist, track.duration, track.title.hashCode()))
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LocalAnonGramScheme.current.accent.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = track.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${track.artist} · ${track.duration}", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SoonTabContent(tabName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = LocalAnonGramScheme.current.accent, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Раздел «$tabName»", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "Скоро будет доступен", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SearchInputRow(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    val scheme = LocalAnonGramScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(text = placeholder, color = TextMuted, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(scheme.accent)
                )
            }
        }
    }
}
