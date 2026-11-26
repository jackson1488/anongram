package com.anongram.app.features.profile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.profile.appearance.ProfileAppearanceStore
import com.anongram.app.features.profile.appearance.ProfileColorEditorScreen
import com.anongram.app.features.profile.appearance.ProfileCoverBackdrop
import com.anongram.app.features.profile.avatar.ProfileAvatar
import com.anongram.app.features.profile.avatar.ProfileAvatarEditorScreen
import com.anongram.app.features.profile.avatar.ProfileAvatarPickerScreen
import com.anongram.app.features.profile.avatar.ProfileAvatarStore
import com.anongram.app.features.profile.avatar.ProfileAvatarView
import com.anongram.app.features.profile.i18n.ProfileI18n
import com.anongram.app.features.settings.qr.ui.MyQrCodeScreen
import kotlinx.coroutines.launch

private sealed interface ProfileSubOverlay {
    data object None : ProfileSubOverlay
    data object AvatarPicker : ProfileSubOverlay
    data class AvatarEditor(val uri: Uri, val isVideo: Boolean, val durationMs: Long) : ProfileSubOverlay
    data object ColorEditor : ProfileSubOverlay
    data object MyQr : ProfileSubOverlay
}

/**
 * Полноценный раздел «Профиль» (`features/profile/`):
 * - Обложка и цвет профиля из общего движка [ProfileAppearanceStore];
 * - Круглый аватар с раскрытием в hero-карточку при потягивании вниз;
 * - 3 быстрые кнопки «Выбрать фото · Изменить · Настройки»;
 * - Карточка данных, вкладки публикаций и переходы в редактор фото/видео и цвета профиля.
 */
@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit = {},
    onSubScreenVisibilityChange: (Boolean) -> Unit = {},
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val vault = remember { KeystoreVault(context) }
    val identity = remember { vault.loadIdentity() }
    val prefs = remember { context.applicationContext.getSharedPreferences("anongram_profile_meta", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        ProfileAvatarStore.ensureLoaded(context)
        ProfileAppearanceStore.ensureLoaded(context)
    }

    val avatar by ProfileAvatarStore.avatar.collectAsState()
    val appearance by ProfileAppearanceStore.myAppearance.collectAsState()

    var displayName by remember {
        mutableStateOf(prefs.getString("display_name", null) ?: identity?.nickname ?: "AnonUser")
    }
    var bio by remember {
        mutableStateOf(prefs.getString("bio", null) ?: strings.defaultBio)
    }
    val birthday by remember {
        mutableStateOf(prefs.getString("birthday", null) ?: strings.defaultBirthday)
    }

    val rawPublicId = identity?.publicId ?: "0x4D9F88E1A7C2"
    val handle = "@${(identity?.nickname ?: "ANONUSER").uppercase().replace(" ", "")}"
    val displayPublicId = if (rawPublicId.length >= 14) {
        "0x${rawPublicId.take(6)}…${rawPublicId.takeLast(6)}"
    } else {
        rawPublicId
    }

    var subOverlay by remember { mutableStateOf<ProfileSubOverlay>(ProfileSubOverlay.None) }
    var menuOpen by remember { mutableStateOf(false) }
    var editDialogOpen by remember { mutableStateOf(false) }
    var postsTab by remember { mutableIntStateOf(0) } // 0 = Публикации, 1 = Архив

    LaunchedEffect(subOverlay) {
        onSubScreenVisibilityChange(subOverlay != ProfileSubOverlay.None)
    }

    // Механика «круг -> тянешь вниз -> hero-аватар»
    val hasMediaAvatar = avatar !is ProfileAvatar.None
    var isExpandedHero by remember { mutableStateOf(false) }
    val pullProgress = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val pullThresholdPx = with(density) { 140.dp.toPx() }

    val pullNestedScroll = remember(hasMediaAvatar, isExpandedHero) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                if (isExpandedHero && available.y < -12f) {
                    isExpandedHero = false
                    return Offset.Zero
                }
                if (hasMediaAvatar && atTop && available.y > 0f && !isExpandedHero) {
                    val next = (pullProgress.value + available.y / pullThresholdPx).coerceIn(0f, 1.2f)
                    scope.launch { pullProgress.snapTo(next) }
                    if (next >= 0.85f) {
                        isExpandedHero = true
                        scope.launch { pullProgress.animateTo(0f, spring()) }
                    }
                    return Offset(0f, available.y * 0.65f)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pullProgress.value > 0f) {
                    scope.launch { pullProgress.animateTo(0f, spring()) }
                }
                return Velocity.Zero
            }
        }
    }

    BackHandler(enabled = subOverlay != ProfileSubOverlay.None || isExpandedHero) {
        when {
            subOverlay is ProfileSubOverlay.AvatarEditor -> subOverlay = ProfileSubOverlay.AvatarPicker
            subOverlay != ProfileSubOverlay.None -> subOverlay = ProfileSubOverlay.None
            isExpandedHero -> isExpandedHero = false
        }
    }

    AnonGramScreenSurface(
        applySystemBarsPadding = false,
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullNestedScroll),
                contentPadding = PaddingValues(bottom = 128.dp)
            ) {
                // 1. Шапка профиля с обложкой и раскрывающимся hero-аватаром
                item(key = "profile_hero") {
                    val heroHeight by animateDpAsState(
                        targetValue = if (isExpandedHero && hasMediaAvatar) 330.dp else 248.dp,
                        animationSpec = spring(),
                        label = "profileHeroHeight"
                    )
                    val avatarSize by animateDpAsState(
                        targetValue = if (isExpandedHero && hasMediaAvatar) 168.dp else 94.dp,
                        animationSpec = spring(),
                        label = "profileAvatarSize"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(heroHeight)
                    ) {
                        ProfileCoverBackdrop(
                            appearance = appearance,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Верхняя панель: QR слева | Меню 3 точки справа
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .safeStatusBarPadding()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.25f))
                                    .clickable { subOverlay = ProfileSubOverlay.MyQr },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode2,
                                    contentDescription = "QR",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .clickable { menuOpen = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuOpen,
                                    onDismissRequest = { menuOpen = false },
                                    containerColor = scheme.card,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.border(0.8.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.changeProfileColor, color = scheme.textPrimary, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Palette, null, tint = scheme.accent, modifier = Modifier.size(19.dp))
                                        },
                                        onClick = {
                                            menuOpen = false
                                            subOverlay = ProfileSubOverlay.ColorEditor
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.changeName, color = scheme.textPrimary, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.AlternateEmail, null, tint = scheme.textSecondary, modifier = Modifier.size(19.dp))
                                        },
                                        onClick = {
                                            menuOpen = false
                                            editDialogOpen = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.copyLink, color = scheme.textPrimary, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Link, null, tint = scheme.textSecondary, modifier = Modifier.size(19.dp))
                                        },
                                        onClick = {
                                            menuOpen = false
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("AnonGram", "anongram://user?handle=${handle.removePrefix("@")}"))
                                            Toast.makeText(context, strings.linkCopied, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        // Центр: Круглый (или раскрытый) аватар, имя и «в сети»
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val ringColor = appearance.preset?.primary ?: scheme.accent
                            val avatarShape = if (isExpandedHero && hasMediaAvatar) RoundedCornerShape(28.dp) else CircleShape
                            Box(
                                modifier = Modifier
                                    .size(avatarSize)
                                    .clip(avatarShape)
                                    .border(2.dp, ringColor.copy(alpha = 0.85f), avatarShape)
                                    .clickable {
                                        if (hasMediaAvatar && !isExpandedHero) {
                                            isExpandedHero = true
                                        } else {
                                            subOverlay = ProfileSubOverlay.AvatarPicker
                                        }
                                    }
                            ) {
                                ProfileAvatarView(
                                    avatar = avatar,
                                    displayName = displayName,
                                    monogramFontSize = 34.sp,
                                    emojiFontSize = 42.sp,
                                    playVideo = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = displayName,
                                color = appearance.namePreset?.primary ?: if (appearance.preset != null) Color.White else scheme.textPrimary,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.onlineStatus,
                                color = if (appearance.preset != null) Color.White.copy(alpha = 0.78f) else scheme.accent,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 2. Три быстрые кнопки-пилюли: Выбрать фото | Изменить | Настройки
                item(key = "profile_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileActionPill(
                            icon = Icons.Outlined.AddAPhoto,
                            label = strings.selectPhoto,
                            onClick = { subOverlay = ProfileSubOverlay.AvatarPicker },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileActionPill(
                            icon = Icons.Outlined.Edit,
                            label = strings.edit,
                            onClick = { editDialogOpen = true },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileActionPill(
                            icon = Icons.Outlined.Settings,
                            label = strings.settings,
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Карточка информации о профиле
                item(key = "profile_info_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.card)
                            .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileInfoField(value = displayPublicId, label = strings.phoneLabel)
                        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                        ProfileInfoField(value = bio, label = strings.bioLabel)
                        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                        ProfileInfoField(value = handle, label = strings.usernameLabel)
                        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                        ProfileInfoField(value = birthday, label = strings.birthdayLabel)
                    }
                }

                // 4. Вкладки «Публикации | Архив публикаций» + пустое состояние с кнопкой «Добавить»
                item(key = "profile_posts") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, start = 14.dp, end = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(strings.postsTab, strings.postsArchiveTab).forEachIndexed { idx, label ->
                                val selected = postsTab == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(17.dp))
                                        .background(if (selected) scheme.accentSoft else Color.Transparent)
                                        .clickable { postsTab = idx }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) scheme.accent else scheme.textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        Text(
                            text = strings.noPostsTitle,
                            color = scheme.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.noPostsSubtitle,
                            color = scheme.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(scheme.accent)
                                .clickable { subOverlay = ProfileSubOverlay.AvatarPicker }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddAPhoto,
                                contentDescription = null,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.addPost,
                                color = scheme.onAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Вложенные экраны (Выбор фото/видео, Редактор с круглым кропом, Цвет профиля, QR)
            AnimatedContent(
                targetState = subOverlay,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "profileSubOverlay"
            ) { overlay ->
                when (overlay) {
                    is ProfileSubOverlay.None -> Unit

                    is ProfileSubOverlay.AvatarPicker -> {
                        ProfileAvatarPickerScreen(
                            displayName = displayName,
                            subtitleText = "$displayPublicId · $handle",
                            onPickMedia = { uri, isVideo, durationMs ->
                                subOverlay = ProfileSubOverlay.AvatarEditor(uri, isVideo, durationMs)
                            },
                            onBack = { subOverlay = ProfileSubOverlay.None }
                        )
                    }

                    is ProfileSubOverlay.AvatarEditor -> {
                        ProfileAvatarEditorScreen(
                            uri = overlay.uri,
                            isVideo = overlay.isVideo,
                            initialDurationMs = overlay.durationMs,
                            onSaved = { subOverlay = ProfileSubOverlay.None },
                            onBack = { subOverlay = ProfileSubOverlay.AvatarPicker }
                        )
                    }

                    is ProfileSubOverlay.ColorEditor -> {
                        ProfileColorEditorScreen(
                            displayName = displayName,
                            onBack = { subOverlay = ProfileSubOverlay.None }
                        )
                    }

                    is ProfileSubOverlay.MyQr -> {
                        MyQrCodeScreen(
                            handle = handle,
                            publicKeyHex = rawPublicId,
                            onClose = { subOverlay = ProfileSubOverlay.None },
                            onOpenScanner = { subOverlay = ProfileSubOverlay.None }
                        )
                    }
                }
            }
        }

        if (editDialogOpen) {
            EditProfileDialog(
                initialName = displayName,
                initialBio = bio,
                onDismiss = { editDialogOpen = false },
                onSave = { newName, newBio ->
                    displayName = newName.ifBlank { displayName }
                    bio = newBio
                    prefs.edit()
                        .putString("display_name", displayName)
                        .putString("bio", bio)
                        .apply()
                    editDialogOpen = false
                }
            )
        }
    }
}

@Composable
private fun ProfileActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.card)
            .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = scheme.textPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = scheme.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileInfoField(
    value: String,
    label: String
) {
    val scheme = LocalAnonGramScheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = value,
            color = scheme.textPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = scheme.textMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialBio: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    var name by remember { mutableStateOf(initialName) }
    var bio by remember { mutableStateOf(initialBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.card,
        title = {
            Text(strings.editNameTitle, color = scheme.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(color = scheme.textPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.inputBg)
                        .border(0.8.dp, scheme.inputBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                )
                BasicTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.inputBg)
                        .border(0.8.dp, scheme.inputBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), bio.trim()) }) {
                Text(strings.save, color = scheme.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = scheme.textSecondary)
            }
        }
    )
}
