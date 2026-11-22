package com.anongram.app.features.contacts.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface

/**
 * Единый экран создания / редактирования контакта (наследуется в Главном меню/Контактах и Профиле).
 */
@Composable
fun NewContactScreen(
    initialFirstName: String = "",
    initialLastName: String = "",
    initialNote: String = "",
    avatarResId: Int? = null,
    statusText: String? = null,
    isEditMode: Boolean = false,
    onBack: () -> Unit,
    onCreate: (firstName: String, lastName: String, sharePhone: Boolean, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var firstName by remember(initialFirstName) { mutableStateOf(initialFirstName) }
    var lastName by remember(initialLastName) { mutableStateOf(initialLastName) }
    var sharePhone by remember { mutableStateOf(true) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    BackHandler(onBack = onBack)

    val titleText = when {
        isEditMode && isRu -> "Изменить контакт"
        isEditMode -> "Edit Contact"
        isRu -> "Новый контакт"
        else -> "New Contact"
    }
    val actionText = when {
        isEditMode && isRu -> "Сохранить"
        isEditMode -> "Save"
        isRu -> "Создать"
        else -> "Create"
    }
    val numberHiddenText = if (isRu) "Номер скрыт" else "Number hidden"
    val defaultStatus = statusText ?: if (isRu) "был(а) недавно" else "last seen recently"
    val firstNamePlaceholder = if (isRu) "Имя (обязательно)" else "First name (required)"
    val lastNamePlaceholder = if (isRu) "Фамилия (необязательно)" else "Last name (optional)"
    val phoneHint = if (isRu) {
        "Номер телефона будет виден, когда собеседник добавит Вас в контакты."
    } else {
        "Phone number will be visible when the contact adds you back."
    }
    val sharePhoneLabel = if (isRu) "Показать мой номер телефона" else "Share my phone number"
    val sharePhoneHint = if (isRu) {
        "Отключите, если не хотите, чтобы собеседник видел(а) Ваш номер телефона."
    } else {
        "Disable if you don't want them to see your phone number."
    }
    val notePlaceholder = if (isRu) "Заметка" else "Notes"
    val noteHint = if (isRu) "Заметку видите только Вы." else "Only you can see this note."

    val canSubmit = firstName.trim().isNotEmpty()
    val monogram = remember(firstName, lastName) {
        val f = firstName.trim().firstOrNull()?.uppercaseChar() ?: 'A'
        val l = lastName.trim().firstOrNull()?.uppercaseChar() ?: 'G'
        "$f$l"
    }

    AnonGramScreenSurface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Верхняя панель: стрелка назад, заголовок, капсула «Создать»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isRu) "Назад" else "Back",
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = titleText,
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (canSubmit) scheme.accent else scheme.accent.copy(alpha = 0.42f))
                        .clickable(enabled = canSubmit) {
                            onCreate(firstName.trim(), lastName.trim(), sharePhone, note.trim())
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionText,
                        color = scheme.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Карточка профиля + Имя / Фамилия
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(scheme.accentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarResId != null) {
                                Image(
                                    painter = painterResource(id = avatarResId),
                                    contentDescription = titleText,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = monogram,
                                    color = scheme.accent,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = numberHiddenText,
                                color = scheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = defaultStatus,
                                color = scheme.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                    ContactFieldRow(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = firstNamePlaceholder
                    )
                    HorizontalDivider(
                        color = scheme.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    ContactFieldRow(
                        value = lastName,
                        onValueChange = { lastName = it },
                        placeholder = lastNamePlaceholder
                    )
                }

                Text(
                    text = phoneHint,
                    color = scheme.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Переключатель «Показать мой номер телефона»
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                        .clickable { sharePhone = !sharePhone }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sharePhoneLabel,
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = sharePhone,
                        onCheckedChange = { sharePhone = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent,
                            uncheckedThumbColor = scheme.textMuted,
                            uncheckedTrackColor = scheme.surface
                        )
                    )
                }

                Text(
                    text = sharePhoneHint,
                    color = scheme.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Поле «Заметка»
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                ) {
                    ContactFieldRow(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = notePlaceholder,
                        singleLine = false
                    )
                }

                Text(
                    text = noteHint,
                    color = scheme.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ContactFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = scheme.textMuted,
                fontSize = 15.5.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = scheme.textPrimary,
                fontSize = 15.5.sp
            ),
            cursorBrush = SolidColor(scheme.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
