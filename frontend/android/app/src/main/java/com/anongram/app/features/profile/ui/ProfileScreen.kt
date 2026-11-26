package com.anongram.app.features.profile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramGlassHeaderBackdrop
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
import com.anongram.app.features.settings.account.ui.AccountSettingsScreen
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import com.anongram.app.features.settings.qr.ui.MyQrCodeScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

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
 * - 3-ступенчатое раскрытие: круг -> полноширинная hero-карточка -> полноэкранный просмотр;
 * - Мульти-аватары с перелистыванием, счётчиком и мини-карточками при `avatars.size >= 2`;
 * - Матовый блюр кнопок поверх фото/видео ([settingsGlassPill]);
 * - Выдвижная шторка выбора фото/видео/камеры поверх профиля.
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

    val avatars by ProfileAvatarStore.avatars.collectAsState()
    val primaryAvatar = avatars.firstOrNull() ?: ProfileAvatar.None
    val appearance by ProfileAppearanceStore.myAppearance.collectAsState()
    val animState by AnimationsSettingsStore.state.collectAsState()

    var selectedAvatarIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(avatars.size) {
        if (avatars.isEmpty()) {
            selectedAvatarIndex = 0
        } else if (selectedAvatarIndex >= avatars.size) {
            selectedAvatarIndex = avatars.lastIndex
        }
    }

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
    var deleteConfirmIndex by remember { mutableStateOf<Int?>(null) }
    var customBannerText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(customBannerText) {
        if (customBannerText != null) {
            delay(2200)
            customBannerText = null
        }
    }

    // 3-ступенчатое состояние: круг -> раскрытая hero-карточка -> полноэкранный просмотр
    val hasMediaAvatar = avatars.isNotEmpty()
    var isExpandedHero by remember { mutableStateOf(false) }
    var isFullscreenViewer by remember { mutableStateOf(false) }

    LaunchedEffect(subOverlay, isFullscreenViewer) {
        onSubScreenVisibilityChange(subOverlay != ProfileSubOverlay.None || isFullscreenViewer)
    }

    val pullProgress = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    val heroHaze = remember { HazeState() }
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

    val pullNestedScroll = remember(hasMediaAvatar, isExpandedHero, isFullscreenViewer) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || isFullscreenViewer) return Offset.Zero
                val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                if (isExpandedHero && available.y < -12f) {
                    isExpandedHero = false
                    return Offset.Zero
                }
                if (hasMediaAvatar && atTop && available.y > 0f) {
                    val next = (pullProgress.value + available.y / pullThresholdPx).coerceIn(0f, 1.2f)
                    scope.launch { pullProgress.snapTo(next) }
                    if (!isExpandedHero && next >= 0.75f) {
                        selectedAvatarIndex = 0
                        isExpandedHero = true
                        scope.launch { pullProgress.animateTo(0f, spring(dampingRatio = 0.88f, stiffness = 260f)) }
                    } else if (isExpandedHero && next >= 0.65f) {
                        isFullscreenViewer = true
                        scope.launch { pullProgress.animateTo(0f, spring(dampingRatio = 0.88f, stiffness = 260f)) }
                    }
                    return Offset(0f, available.y * 0.65f)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (pullProgress.value > 0f) {
                    scope.launch { pullProgress.animateTo(0f, spring(dampingRatio = 0.88f, stiffness = 260f)) }
                }
                return Velocity.Zero
            }
        }
    }

    BackHandler(enabled = subOverlay != ProfileSubOverlay.None || isFullscreenViewer || isExpandedHero) {
        when {
            subOverlay is ProfileSubOverlay.AvatarEditor -> subOverlay = ProfileSubOverlay.AvatarPicker
            subOverlay != ProfileSubOverlay.None -> subOverlay = ProfileSubOverlay.None
            isFullscreenViewer -> isFullscreenViewer = false
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
                val target60Dp = LocalConfiguration.current.screenHeightDp.dp * 0.60f
                val extraStage2Dp = (target60Dp - 410.dp).coerceAtLeast(72.dp)
                val stretchDp = (pullProgress.value.coerceIn(0f, 1f) * if (isExpandedHero) extraStage2Dp.value else 36f).dp
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pullNestedScroll),
                    contentPadding = PaddingValues(bottom = 128.dp)
                ) {
                    // 1. Шапка профиля (обычный круг -> плавно раскрывается в полноширинную hero-карточку)
                    item(key = "profile_hero") {
                        AnimatedContent(
                            targetState = isExpandedHero && hasMediaAvatar,
                            transitionSpec = {
                                (fadeIn(tween(340, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(tween(240, easing = FastOutSlowInEasing)))
                                    .using(
                                        SizeTransform(clip = false) { _, _ ->
                                            tween(340, easing = FastOutSlowInEasing)
                                        }
                                    )
                            },
                            label = "profileHeroExpand"
                        ) { expanded ->
                            if (expanded) {
                                val heroPagerState = rememberPagerState(
                                    initialPage = selectedAvatarIndex.coerceIn(0, (avatars.size - 1).coerceAtLeast(0)),
                                    pageCount = { avatars.size.coerceAtLeast(1) }
                                )
                                LaunchedEffect(heroPagerState.currentPage) {
                                    selectedAvatarIndex = heroPagerState.currentPage
                                }
                                LaunchedEffect(selectedAvatarIndex) {
                                    if (selectedAvatarIndex in avatars.indices && heroPagerState.currentPage != selectedAvatarIndex) {
                                        heroPagerState.animateScrollToPage(selectedAvatarIndex)
                                    }
                                }

                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(410.dp + stretchDp)
                                ) {
                                    val boxWidthPx = with(density) { maxWidth.toPx() }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .hazeSource(state = heroHaze)
                                    ) {
                                        HorizontalPager(
                                            state = heroPagerState,
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            val itemAvatar = avatars.getOrNull(page) ?: primaryAvatar
                                            // Видео-аватар воспроизводится ТОЛЬКО когда раскрыт (без паузы, по кругу)
                                            ProfileAvatarView(
                                                avatar = itemAvatar,
                                                displayName = displayName,
                                                monogramFontSize = 54.sp,
                                                emojiFontSize = 72.sp,
                                                playVideo = !isFullscreenViewer && page == heroPagerState.currentPage,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Невидимые зоны тапа: лево/право листают фото при >=2, центр открывает полноэкранный просмотр
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            if (avatars.size >= 2) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(0.28f)
                                                        .fillMaxSize()
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            if (selectedAvatarIndex > 0) {
                                                                selectedAvatarIndex -= 1
                                                            }
                                                        }
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(if (avatars.size >= 2) 0.44f else 1f)
                                                    .fillMaxSize()
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        isFullscreenViewer = true
                                                    }
                                            )
                                            if (avatars.size >= 2) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(0.28f)
                                                        .fillMaxSize()
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            if (selectedAvatarIndex < avatars.lastIndex) {
                                                                selectedAvatarIndex += 1
                                                            }
                                                        }
                                                )
                                            }
                                        }

                                        // Верхние полоски-индикаторы только когда 2 и более фото
                                        if (avatars.size >= 2) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .fillMaxWidth()
                                                    .safeStatusBarPadding()
                                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                avatars.forEachIndexed { idx, _ ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(2.5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (idx == selectedAvatarIndex) Color.White
                                                                else Color.White.copy(alpha = 0.35f)
                                                            )
                                                    )
                                                }
                                            }
                                        }

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
                                                            Color.Black.copy(alpha = 0.72f)
                                                        )
                                                    )
                                                )
                                        )
                                    }

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
                                                heroHaze = heroHaze,
                                                glassBlurEnabled = animState.glassBlurEnabled,
                                                modifier = Modifier.weight(1f)
                                            )
                                            ProfileActionPill(
                                                icon = Icons.Outlined.Edit,
                                                label = strings.edit,
                                                onClick = { subOverlay = ProfileSubOverlay.AccountEdit },
                                                overlayStyle = true,
                                                heroHaze = heroHaze,
                                                glassBlurEnabled = animState.glassBlurEnabled,
                                                modifier = Modifier.weight(1f)
                                            )
                                            ProfileActionPill(
                                                icon = Icons.Outlined.Settings,
                                                label = strings.settings,
                                                onClick = onOpenSettings,
                                                overlayStyle = true,
                                                heroHaze = heroHaze,
                                                glassBlurEnabled = animState.glassBlurEnabled,
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
                                val pullScale = 1f + (pullProgress.value.coerceIn(0f, 1f) * 0.45f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(344.dp)
                                ) {
                                    ProfileCoverBackdrop(
                                        appearance = appearance,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp),
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
                                                        selectedAvatarIndex = 0
                                                        isExpandedHero = true
                                                    } else {
                                                        subOverlay = ProfileSubOverlay.AvatarPicker
                                                    }
                                                }
                                        ) {
                                            // В свернутом круге видео не крутится (показывает миниатюру)
                                            ProfileAvatarView(
                                                avatar = primaryAvatar,
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

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp),
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
                                }
                            }
                        }
                    }

                    item(key = "hero_spacer") {
                        Spacer(modifier = Modifier.height(10.dp))
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

                val isHeroActive = isExpandedHero && hasMediaAvatar
                val topPillModifier = if (isHeroActive) {
                    Modifier
                        .size(40.dp)
                        .settingsGlassPill(
                            hazeState = heroHaze,
                            shape = CircleShape,
                            glassBlurEnabled = animState.glassBlurEnabled
                        )
                } else {
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                }
                val topIconTint = if (isHeroActive) scheme.textPrimary else Color.White

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
                            modifier = topPillModifier.clickable {
                                if (isHeroActive) {
                                    isExpandedHero = false
                                } else {
                                    subOverlay = ProfileSubOverlay.MyQr
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isHeroActive) Icons.AutoMirrored.Filled.ArrowBack else Icons.Outlined.QrCode2,
                                contentDescription = if (isHeroActive) strings.back else "QR",
                                tint = topIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            Box(
                                modifier = topPillModifier.clickable { menuOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    tint = topIconTint,
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

                                // Дополнительные пункты меню на 2-й ступени (раскрытая карточка)
                                if (isHeroActive) {
                                    val activeAvatar = avatars.getOrNull(selectedAvatarIndex)
                                    if (avatars.size >= 2 && selectedAvatarIndex != 0) {
                                        DropdownMenuItem(
                                            text = { Text(strings.menuSetPrimaryPhoto, color = scheme.textPrimary, fontSize = 14.sp) },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.CheckCircleOutline, null, tint = scheme.accent, modifier = Modifier.size(19.dp))
                                            },
                                            onClick = {
                                                menuOpen = false
                                                ProfileAvatarStore.setPrimaryAvatar(context, selectedAvatarIndex)
                                                selectedAvatarIndex = 0
                                            }
                                        )
                                    }
                                    if (activeAvatar != null) {
                                        DropdownMenuItem(
                                            text = { Text(strings.menuSaveToGallery, color = scheme.textPrimary, fontSize = 14.sp) },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Download, null, tint = scheme.textSecondary, modifier = Modifier.size(19.dp))
                                            },
                                            onClick = {
                                                menuOpen = false
                                                scope.launch {
                                                    val ok = withContext(Dispatchers.IO) {
                                                        saveAvatarToGallery(context, activeAvatar)
                                                    }
                                                    if (ok) {
                                                        customBannerText = strings.savedToGalleryToast
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.menuShare, color = scheme.textPrimary, fontSize = 14.sp) },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Share, null, tint = scheme.textSecondary, modifier = Modifier.size(19.dp))
                                            },
                                            onClick = {
                                                menuOpen = false
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        shareAvatarSystem(context, activeAvatar, displayName)
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.menuDeletePhoto, color = CrimsonKillSwitch, fontSize = 14.sp) },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.DeleteOutline, null, tint = CrimsonKillSwitch, modifier = Modifier.size(19.dp))
                                            },
                                            onClick = {
                                                menuOpen = false
                                                deleteConfirmIndex = selectedAvatarIndex
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 3. Вложенные экраны (Шторка фото/видео, Редактор, Цвет профиля, Аккаунт, QR)
            AnimatedContent(
                targetState = subOverlay,
                transitionSpec = {
                    val isSheet = targetState is ProfileSubOverlay.AvatarPicker ||
                        initialState is ProfileSubOverlay.AvatarPicker
                    if (isSheet) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        (slideInVertically(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            initialOffsetY = { it / 4 }
                        ) + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                targetOffsetY = { it / 6 }
                            ) + fadeOut(tween(160)))
                    }
                },
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scheme.background)
                        ) {
                            ProfileAvatarEditorScreen(
                                uri = overlay.uri,
                                isVideo = overlay.isVideo,
                                initialDurationMs = overlay.durationMs,
                                onSaved = { subOverlay = ProfileSubOverlay.None },
                                onBack = { subOverlay = ProfileSubOverlay.AvatarPicker }
                            )
                        }
                    }

                    is ProfileSubOverlay.ColorEditor -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scheme.background)
                        ) {
                            ProfileColorEditorScreen(
                                displayName = displayName,
                                onBack = { subOverlay = ProfileSubOverlay.None }
                            )
                        }
                    }

                    is ProfileSubOverlay.AccountEdit -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scheme.background)
                        ) {
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
                    }

                    is ProfileSubOverlay.MyQr -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scheme.background)
                        ) {
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

            // 4. Полноэкранный просмотр аватара (Этап 3)
            AnimatedVisibility(
                visible = isFullscreenViewer && hasMediaAvatar,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180))
            ) {
                val viewerHaze = remember { HazeState() }
                val viewerPagerState = rememberPagerState(
                    initialPage = selectedAvatarIndex.coerceIn(0, (avatars.size - 1).coerceAtLeast(0)),
                    pageCount = { avatars.size.coerceAtLeast(1) }
                )
                var viewerMenuOpen by remember { mutableStateOf(false) }
                val viewerDragY = remember { Animatable(0f) }
                val dismissThresholdPx = with(density) { 140.dp.toPx() }
                val bgAlpha = (1f - abs(viewerDragY.value) / (dismissThresholdPx * 3.5f)).coerceIn(0.15f, 1f)
                val contentScale = (1f - abs(viewerDragY.value) / (dismissThresholdPx * 7f)).coerceIn(0.84f, 1f)

                LaunchedEffect(isFullscreenViewer) {
                    if (isFullscreenViewer) viewerDragY.snapTo(0f)
                }
                LaunchedEffect(viewerPagerState.currentPage) {
                    selectedAvatarIndex = viewerPagerState.currentPage
                }
                LaunchedEffect(selectedAvatarIndex, avatars.size) {
                    if (selectedAvatarIndex in avatars.indices && viewerPagerState.currentPage != selectedAvatarIndex) {
                        viewerPagerState.scrollToPage(selectedAvatarIndex)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.background.copy(alpha = bgAlpha))
                ) {
                    HorizontalPager(
                        state = viewerPagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = viewerDragY.value
                                scaleX = contentScale
                                scaleY = contentScale
                            }
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    scope.launch {
                                        viewerDragY.snapTo(viewerDragY.value + delta)
                                    }
                                },
                                onDragStopped = { velocity ->
                                    scope.launch {
                                        if (abs(viewerDragY.value) > dismissThresholdPx || abs(velocity) > 1200f) {
                                            val targetY = if (viewerDragY.value >= 0f) dismissThresholdPx * 3.5f else -dismissThresholdPx * 3.5f
                                            viewerDragY.animateTo(targetY, tween(180, easing = FastOutSlowInEasing))
                                            isFullscreenViewer = false
                                            viewerDragY.snapTo(0f)
                                        } else {
                                            viewerDragY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 340f))
                                        }
                                    }
                                }
                            )
                            .hazeSource(state = viewerHaze)
                    ) { page ->
                        val itemAvatar = avatars.getOrNull(page) ?: primaryAvatar
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileAvatarView(
                                avatar = itemAvatar,
                                displayName = displayName,
                                monogramFontSize = 68.sp,
                                emojiFontSize = 96.sp,
                                playVideo = page == viewerPagerState.currentPage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Верхняя панель: чистая ← (без кружка) + Имя слева (+ 1 из N при >= 2 фото) | ⋮
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .safeStatusBarPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .graphicsLayer { alpha = bgAlpha },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { isFullscreenViewer = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = displayName,
                                color = scheme.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (avatars.size >= 2) {
                                Text(
                                    text = String.format(
                                        strings.photoCounterOf,
                                        viewerPagerState.currentPage + 1,
                                        avatars.size
                                    ),
                                    color = scheme.textSecondary,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        Box {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .settingsGlassPill(
                                        hazeState = viewerHaze,
                                        shape = CircleShape,
                                        glassBlurEnabled = animState.glassBlurEnabled
                                    )
                                    .clickable { viewerMenuOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    tint = scheme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = viewerMenuOpen,
                                onDismissRequest = { viewerMenuOpen = false },
                                containerColor = scheme.card,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.border(0.8.dp, scheme.cardBorder, RoundedCornerShape(18.dp))
                            ) {
                                val currentIdx = viewerPagerState.currentPage
                                val currentItem = avatars.getOrNull(currentIdx)
                                if (avatars.size >= 2 && currentIdx != 0) {
                                    DropdownMenuItem(
                                        text = { Text(strings.menuSetPrimaryPhoto, color = scheme.textPrimary, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.CheckCircleOutline, null, tint = scheme.accent, modifier = Modifier.size(19.dp))
                                        },
                                        onClick = {
                                            viewerMenuOpen = false
                                            ProfileAvatarStore.setPrimaryAvatar(context, currentIdx)
                                            selectedAvatarIndex = 0
                                        }
                                    )
                                }
                                if (currentItem != null) {
                                    DropdownMenuItem(
                                        text = { Text(strings.menuSaveToGallery, color = scheme.textPrimary, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Download, null, tint = scheme.textSecondary, modifier = Modifier.size(19.dp))
                                        },
                                        onClick = {
                                            viewerMenuOpen = false
                                            scope.launch {
                                                val ok = withContext(Dispatchers.IO) {
                                                    saveAvatarToGallery(context, currentItem)
                                                }
                                                if (ok) {
                                                    customBannerText = strings.savedToGalleryToast
                                                }
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.menuShare, color = scheme.textPrimary, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Share, null, tint = scheme.textSecondary, modifier = Modifier.size(19.dp))
                                        },
                                        onClick = {
                                            viewerMenuOpen = false
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    shareAvatarSystem(context, currentItem, displayName)
                                                }
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.menuDeletePhoto, color = CrimsonKillSwitch, fontSize = 14.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.DeleteOutline, null, tint = CrimsonKillSwitch, modifier = Modifier.size(19.dp))
                                        },
                                        onClick = {
                                            viewerMenuOpen = false
                                            deleteConfirmIndex = currentIdx
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Нижняя лента мини-карточек (показывается ТОЛЬКО если >= 2 фото/видео)
                    if (avatars.size >= 2) {
                        LazyRow(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .safeNavigationBarPadding()
                                .padding(bottom = 16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            itemsIndexed(avatars) { idx, item ->
                                val isSelected = idx == viewerPagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 54.dp else 46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) scheme.accent else scheme.cardBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            scope.launch { viewerPagerState.animateScrollToPage(idx) }
                                        }
                                ) {
                                    ProfileAvatarView(
                                        avatar = item,
                                        displayName = displayName,
                                        monogramFontSize = 16.sp,
                                        emojiFontSize = 22.sp,
                                        playVideo = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Диалог подтверждения удаления фото/видео
            val pendingDeleteIdx = deleteConfirmIndex
            if (pendingDeleteIdx != null) {
                AlertDialog(
                    onDismissRequest = { deleteConfirmIndex = null },
                    containerColor = scheme.card,
                    title = {
                        Text(
                            text = strings.deletePhotoTitle,
                            color = scheme.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = strings.deletePhotoMessage,
                            color = scheme.textSecondary,
                            fontSize = 14.5.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                deleteConfirmIndex = null
                                ProfileAvatarStore.removeAvatarAt(context, pendingDeleteIdx)
                                if (avatars.size <= 1) {
                                    isFullscreenViewer = false
                                    isExpandedHero = false
                                    selectedAvatarIndex = 0
                                } else if (selectedAvatarIndex >= avatars.size - 1) {
                                    selectedAvatarIndex = (avatars.size - 2).coerceAtLeast(0)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonKillSwitch,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = strings.deleteConfirm, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { deleteConfirmIndex = null },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = strings.cancel, color = scheme.textPrimary)
                        }
                    }
                )
            }

            // 6. Кастомное тематическое уведомление снизу
            AnimatedVisibility(
                visible = customBannerText != null,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeNavigationBarPadding()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.card)
                        .border(0.9.dp, scheme.cardBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = customBannerText.orEmpty(),
                        color = scheme.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
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
    overlayStyle: Boolean = false,
    heroHaze: HazeState? = null,
    glassBlurEnabled: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    val useGlass = overlayStyle && heroHaze != null
    val fgColor = if (useGlass) scheme.textPrimary else if (overlayStyle) Color.White else scheme.textPrimary

    val containerModifier = if (useGlass) {
        modifier.settingsGlassPill(
            hazeState = heroHaze!!,
            shape = RoundedCornerShape(18.dp),
            glassBlurEnabled = glassBlurEnabled
        )
    } else {
        val bgColor = if (overlayStyle) Color.Black.copy(alpha = 0.36f) else scheme.card
        val borderColor = if (overlayStyle) Color.White.copy(alpha = 0.18f) else scheme.cardBorder
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(0.8.dp, borderColor, RoundedCornerShape(18.dp))
    }

    Column(
        modifier = containerModifier
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

private fun saveAvatarToGallery(context: Context, avatar: ProfileAvatar): Boolean {
    return runCatching {
        when (avatar) {
            is ProfileAvatar.None -> false
            is ProfileAvatar.Photo -> {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "AnonGram_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AnonGram")
                    }
                }
                val dest = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                openAvatarStream(context, avatar.uri)?.use { input ->
                    context.contentResolver.openOutputStream(dest)?.use { output ->
                        input.copyTo(output)
                    }
                } ?: return false
                true
            }
            is ProfileAvatar.Video -> {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "AnonGram_${System.currentTimeMillis()}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AnonGram")
                    }
                }
                val dest = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                openAvatarStream(context, avatar.uri)?.use { input ->
                    context.contentResolver.openOutputStream(dest)?.use { output ->
                        input.copyTo(output)
                    }
                } ?: return false
                true
            }
            is ProfileAvatar.Emoji -> {
                val bmp = renderEmojiBitmap(avatar)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "AnonGram_emoji_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AnonGram")
                    }
                }
                val dest = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                context.contentResolver.openOutputStream(dest)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                true
            }
        }
    }.getOrDefault(false)
}

private fun shareAvatarSystem(context: Context, avatar: ProfileAvatar, title: String) {
    runCatching {
        val (file, mime) = when (avatar) {
            is ProfileAvatar.None -> return
            is ProfileAvatar.Photo -> {
                val out = File(context.cacheDir, "share_profile_avatar.jpg")
                openAvatarStream(context, avatar.uri)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                } ?: return
                out to "image/jpeg"
            }
            is ProfileAvatar.Video -> {
                val out = File(context.cacheDir, "share_profile_avatar.mp4")
                openAvatarStream(context, avatar.uri)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                } ?: return
                out to "video/mp4"
            }
            is ProfileAvatar.Emoji -> {
                val out = File(context.cacheDir, "share_profile_avatar.jpg")
                val bmp = renderEmojiBitmap(avatar)
                out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                out to "image/jpeg"
            }
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openAvatarStream(context: Context, uri: Uri) =
    if (uri.scheme == "file" && uri.path != null) {
        File(uri.path!!).takeIf { it.exists() }?.inputStream()
    } else {
        context.contentResolver.openInputStream(uri)
    }

private fun renderEmojiBitmap(avatar: ProfileAvatar.Emoji): Bitmap {
    val size = 512
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bgPaint = Paint().apply {
        color = avatar.bgStart.toArgb()
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 240f
        textAlign = Paint.Align.CENTER
    }
    val y = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(avatar.emoji, size / 2f, y, textPaint)
    return bmp
}
