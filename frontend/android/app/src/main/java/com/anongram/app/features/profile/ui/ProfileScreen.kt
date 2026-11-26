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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramGlassHeaderBackdrop
import com.anongram.app.core.ui.AnonGramScreenSurface
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
import com.anongram.app.features.settings.account.ui.AccountSettingsScreen
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.qr.ui.MyQrCodeScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

private sealed interface ProfileSubOverlay {
    data object None : ProfileSubOverlay
    data object AvatarPicker : ProfileSubOverlay
    data class AvatarEditor(val uri: Uri, val isVideo: Boolean, val durationMs: Long) : ProfileSubOverlay
    data object ColorEditor : ProfileSubOverlay
    data object AccountEdit : ProfileSubOverlay
    data object MyQr : ProfileSubOverlay
}

/**
 * Полноценный раздел «Профиль» (`features/profile/`):
 * - Бесшовный блюр шапки ([AnonGramGlassHeaderBackdrop]);
 * - Раскрытие круглого аватара в полноширинную hero-карточку при потягивании вниз (как в [UserProfileScreen]);
 * - Зацикленное видео-аватар (до 20с) воспроизводится только при раскрытии hero-карточки, без паузы;
 * - Кнопка «Изменить» открывает экран [AccountSettingsScreen] (с выходом и подтверждением).
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
    val animState by AnimationsSettingsStore.state.collectAsState()

    var displayName by remember {
        mutableStateOf(prefs.getString("display_name", null) ?: identity?.nickname ?: "AnonUser")
    }
    var handle by remember {
        mutableStateOf(
            prefs.getString("handle", null)
                ?: "@${(identity?.nickname ?: "ANONUSER").uppercase().replace(" ", "")}"
        )
    }
    val bio by remember {
        mutableStateOf(prefs.getString("bio", null) ?: strings.defaultBio)
    }
    val birthday by remember {
        mutableStateOf(prefs.getString("birthday", null) ?: strings.defaultBirthday)
    }

    val rawPublicId = identity?.publicId ?: "0x4D9F88E1A7C2"
    val displayPublicId = if (rawPublicId.length >= 14) {
        "0x${rawPublicId.take(6)}…${rawPublicId.takeLast(6)}"
    } else {
        rawPublicId
    }

    var subOverlay by remember { mutableStateOf<ProfileSubOverlay>(ProfileSubOverlay.None) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(subOverlay) {
        onSubScreenVisibilityChange(subOverlay != ProfileSubOverlay.None)
    }

    // Механика «круг -> тянешь вниз -> полноширинная hero-карточка поверх экрана» (как в UserProfileScreen)
    val hasMediaAvatar = avatar !is ProfileAvatar.None
    var isExpandedHero by remember { mutableStateOf(false) }
    val pullProgress = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    val pullThresholdPx = with(density) { 140.dp.toPx() }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 12
        }
    }
    val animatedHeaderAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        label = "profileHeaderAlpha"
    )

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
            // 1. Основной скроллируемый контент (проходит под шапкой с бесшовным блюром)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(scheme.background)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (pullProgress.value > 0f && !isExpandedHero) {
                                translationY = pullProgress.value * 90f
                            }
                        }
                        .nestedScroll(pullNestedScroll),
                    contentPadding = PaddingValues(bottom = 128.dp)
                ) {
                    // 1. Шапка профиля (обычный круг -> раскрывается в полноширинную hero-карточку)
                    item(key = "profile_hero") {
                        if (isExpandedHero && hasMediaAvatar) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(410.dp)
                                    .clickable { isExpandedHero = false }
                            ) {
                                // Видео-аватар воспроизводится ТОЛЬКО когда раскрыт (без паузы, по кругу)
                                ProfileAvatarView(
                                    avatar = avatar,
                                    displayName = displayName,
                                    monogramFontSize = 54.sp,
                                    emojiFontSize = 72.sp,
                                    playVideo = true,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Градиент снизу под имя и кнопки
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(175.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.78f)
                                                )
                                            )
                                        )
                                )

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        color = Color.White,
                                        fontSize = 23.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = strings.onlineStatus,
                                        color = Color.White.copy(alpha = 0.80f),
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ProfileActionPill(
                                            icon = Icons.Outlined.AddAPhoto,
                                            label = strings.selectPhoto,
                                            onClick = { subOverlay = ProfileSubOverlay.AvatarPicker },
                                            overlayStyle = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProfileActionPill(
                                            icon = Icons.Outlined.Edit,
                                            label = strings.edit,
                                            onClick = { subOverlay = ProfileSubOverlay.AccountEdit },
                                            overlayStyle = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProfileActionPill(
                                            icon = Icons.Outlined.Settings,
                                            label = strings.settings,
                                            onClick = onOpenSettings,
                                            overlayStyle = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        } else {
                            val avatarSize by animateDpAsState(
                                targetValue = 96.dp,
                                animationSpec = spring(),
                                label = "profileAvatarSize"
                            )
                            val pullScale = 1f + (pullProgress.value.coerceIn(0f, 1f) * 0.25f)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(252.dp)
                            ) {
                                ProfileCoverBackdrop(
                                    appearance = appearance,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val ringColor = appearance.preset?.primary ?: scheme.accent
                                    Box(
                                        modifier = Modifier
                                            .size(avatarSize)
                                            .graphicsLayer {
                                                scaleX = pullScale
                                                scaleY = pullScale
                                            }
                                            .clip(CircleShape)
                                            .border(2.dp, ringColor.copy(alpha = 0.85f), CircleShape)
                                            .clickable {
                                                if (hasMediaAvatar) {
                                                    isExpandedHero = true
                                                } else {
                                                    subOverlay = ProfileSubOverlay.AvatarPicker
                                                }
                                            }
                                    ) {
                                        // В свернутом круге видео не крутится (показывает миниатюру)
                                        ProfileAvatarView(
                                            avatar = avatar,
                                            displayName = displayName,
                                            monogramFontSize = 34.sp,
                                            emojiFontSize = 42.sp,
                                            playVideo = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = displayName,
                                        color = appearance.namePreset?.primary
                                            ?: if (appearance.preset != null) Color.White else scheme.textPrimary,
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
                    }

                    // 2. Три быстрые кнопки (когда hero не раскрыт)
                    if (!(isExpandedHero && hasMediaAvatar)) {
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
                                    onClick = { subOverlay = ProfileSubOverlay.AccountEdit },
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
                    } else {
                        item(key = "hero_spacer") {
                            Spacer(modifier = Modifier.height(14.dp))
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
                }
            }

            // 2. Парящая прозрачная шапка с бесшовным матовым блюром (как на всех экранах)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                if (animatedHeaderAlpha > 0.05f) {
                    AnonGramGlassHeaderBackdrop(
                        hazeState = hazeState,
                        glassBlurEnabled = animState.glassBlurEnabled,
                        scrollFraction = animatedHeaderAlpha,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
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
                                        subOverlay = ProfileSubOverlay.AccountEdit
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
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 3. Вложенные экраны (Шторка фото/видео, Редактор, Цвет профиля, Аккаунт, QR)
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

                    is ProfileSubOverlay.AccountEdit -> {
                        AccountSettingsScreen(
                            initialDisplayName = displayName,
                            initialHandle = handle,
                            onSaveAccount = { newName, newHandle ->
                                displayName = newName
                                handle = "@${newHandle.removePrefix("@").uppercase()}"
                                prefs.edit()
                                    .putString("display_name", displayName)
                                    .putString("handle", handle)
                                    .apply()
                            },
                            onBack = { subOverlay = ProfileSubOverlay.None },
                            onLogout = {
                                subOverlay = ProfileSubOverlay.None
                                vault.clear()
                                onLogout?.invoke()
                            }
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
    }
}

@Composable
private fun ProfileActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlayStyle: Boolean = false
) {
    val scheme = LocalAnonGramScheme.current
    val bgColor = if (overlayStyle) Color.Black.copy(alpha = 0.36f) else scheme.card
    val borderColor = if (overlayStyle) Color.White.copy(alpha = 0.18f) else scheme.cardBorder
    val fgColor = if (overlayStyle) Color.White else scheme.textPrimary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(0.8.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = fgColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = fgColor,
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
