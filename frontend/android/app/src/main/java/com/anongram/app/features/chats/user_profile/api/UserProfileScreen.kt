package com.anongram.app.features.chats.user_profile.api

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import kotlinx.coroutines.launch
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.navigation.swipeBackGesture
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.state.SelfDestruct
import com.anongram.app.features.chats.user_profile.i18n.UserProfileI18n
import com.anongram.app.features.chats.user_profile.model.UserProfileAudioItem
import com.anongram.app.features.chats.user_profile.model.UserProfileBuckets
import com.anongram.app.features.chats.user_profile.model.UserProfileFileItem
import com.anongram.app.features.chats.user_profile.model.UserProfileLinkItem
import com.anongram.app.features.chats.user_profile.model.UserProfileMediaItem
import com.anongram.app.features.chats.user_profile.model.UserProfileTab
import com.anongram.app.features.chats.user_profile.ui.AddToContactsCard
import com.anongram.app.features.chats.user_profile.ui.AutoDeleteCustomSheet
import com.anongram.app.features.chats.user_profile.ui.UserProfileActionsRow
import com.anongram.app.features.chats.user_profile.ui.UserProfileAudioRow
import com.anongram.app.features.chats.user_profile.ui.UserProfileAvatarFullscreenOverlay
import com.anongram.app.features.chats.user_profile.ui.UserProfileFileRow
import com.anongram.app.features.chats.user_profile.ui.UserProfileHeroHeader
import com.anongram.app.features.chats.user_profile.ui.UserProfileInfoCard
import com.anongram.app.features.chats.user_profile.ui.UserProfileLinkRow
import com.anongram.app.features.chats.user_profile.ui.UserProfileMediaGridRow
import com.anongram.app.features.chats.user_profile.ui.UserProfileOverflowMenu
import com.anongram.app.features.chats.user_profile.ui.UserProfileTabBar
import com.anongram.app.features.chats.user_profile.ui.UserProfileTopOverlayBar
import com.anongram.app.features.chats.user_profile.ui.tabSummaryText
import com.anongram.app.features.settings.qr.ui.MyQrCodeScreen
import com.anongram.app.features.settings.qr.ui.QrCameraScannerScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private val URL_REGEX = Regex("""https?://[^\s<>"')]+""", RegexOption.IGNORE_CASE)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserProfileScreen(
    session: IChatSession,
    messages: List<ChatMessage>,
    selfDestruct: SelfDestruct,
    isContact: Boolean = false,
    username: String? = null,
    bio: String? = null,
    birthday: String? = null,
    onBack: () -> Unit,
    onToggleMute: () -> Unit,
    onSetSelfDestruct: (SelfDestruct) -> Unit,
    onOpenKeyVerification: () -> Unit,
    onOpenMediaMessage: (ChatMessage) -> Unit,
    onShowInChat: (ChatMessage) -> Unit = {},
    onForwardMessages: (List<String>) -> Unit = {},
    onDeleteMessages: (List<String>) -> Unit = {},
    onAddContact: (() -> Unit)? = null,
    onDeleteChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val strings = remember(lang) { UserProfileI18n.forLanguage(lang) }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.anongram.app.features.chats.user_profile.data.BlockedUsersStore.ensureInitialized(context)
    }
    val blockedIds by com.anongram.app.features.chats.user_profile.data.BlockedUsersStore.blockedIds.collectAsState()
    val isBlocked = session.id in blockedIds

    val scope = rememberCoroutineScope()
    val heroExpandProgress = remember(session.id) { Animatable(0f) }
    val isExpandedHero by remember { derivedStateOf { heroExpandProgress.value >= 0.45f } }
    var currentAvatarIndex by remember(session.id) { mutableIntStateOf(0) }
    var isAvatarFullscreenOpen by remember(session.id) { mutableStateOf(false) }
    var isOverflowOpen by remember { mutableStateOf(false) }
    var isCustomAutoDeleteOpen by remember { mutableStateOf(false) }
    var isQrOverlayOpen by remember { mutableStateOf(false) }
    var isQrScannerOpen by remember { mutableStateOf(false) }
    var addedToContactsLocal by remember(session.id) { mutableStateOf(isContact) }
    var gridColumns by remember { mutableIntStateOf(3) }
    var selectedKeys by remember(session.id) { mutableStateOf(emptySet<String>()) }
    val hazeState = remember { HazeState() }

    val buckets = remember(messages) { extractProfileBuckets(messages) }
    val availableTabs = remember(buckets) { buckets.availableTabs() }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { availableTabs.size.coerceAtLeast(1) }
    )
    val selectedTab = availableTabs.getOrElse(pagerState.currentPage) { UserProfileTab.MEDIA }

    val listState = rememberLazyListState()
    val isCollapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >= 1 || listState.firstVisibleItemScrollOffset > 220
        }
    }

    val derivedHandle = remember(username, session.title) {
        username?.takeIf { it.isNotBlank() }
            ?: session.title.lowercase().replace(Regex("[^a-z0-9_а-яё]"), "_").take(20)
    }

    BackHandler {
        when {
            isAvatarFullscreenOpen -> isAvatarFullscreenOpen = false
            selectedKeys.isNotEmpty() -> selectedKeys = emptySet()
            isQrScannerOpen -> isQrScannerOpen = false
            isQrOverlayOpen -> isQrOverlayOpen = false
            isCustomAutoDeleteOpen -> isCustomAutoDeleteOpen = false
            else -> onBack()
        }
    }

    val hasRealAvatar = session.avatarResId != null
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }.coerceAtLeast(600f)
    val expandThresholdPx = (screenHeightPx * 0.45f).coerceAtLeast(260f)

    val pullDownNestedScroll = remember(hasRealAvatar, expandThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (hasRealAvatar && heroExpandProgress.value > 0f && available.y < 0f && source == NestedScrollSource.UserInput) {
                    val next = (heroExpandProgress.value + available.y / expandThresholdPx).coerceIn(0f, 1.35f)
                    scope.launch { heroExpandProgress.snapTo(next) }
                    return if (next > 0.01f) Offset(0f, available.y) else Offset.Zero
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (
                    hasRealAvatar &&
                    available.y > 0f &&
                    source == NestedScrollSource.UserInput &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                ) {
                    val next = (heroExpandProgress.value + available.y / expandThresholdPx).coerceIn(0f, 1.35f)
                    scope.launch { heroExpandProgress.snapTo(next) }
                    if (next >= 1.24f) {
                        isAvatarFullscreenOpen = true
                        scope.launch { heroExpandProgress.snapTo(1f) }
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (hasRealAvatar && heroExpandProgress.value > 0f) {
                    if (available.y > 1200f) {
                        if (heroExpandProgress.value >= 0.92f) {
                            isAvatarFullscreenOpen = true
                        }
                        scope.launch { heroExpandProgress.animateTo(1f, spring()) }
                    } else if (available.y < -900f) {
                        scope.launch { heroExpandProgress.animateTo(0f, spring()) }
                    } else {
                        val target = if (heroExpandProgress.value >= 0.45f) 1f else 0f
                        scope.launch { heroExpandProgress.animateTo(target, spring()) }
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (hasRealAvatar && (heroExpandProgress.value in 0.001f..0.999f || heroExpandProgress.value > 1.001f)) {
                    val target = if (heroExpandProgress.value >= 0.45f || available.y > 1200f) 1f else 0f
                    scope.launch { heroExpandProgress.animateTo(target, spring()) }
                }
                return Velocity.Zero
            }
        }
    }

    val swipeBackOffset = remember { Animatable(0f) }

    AnonGramScreenSurface(
        applySystemBarsPadding = false,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationX = swipeBackOffset.value * screenWidthPx }
            .swipeBackGesture(
                enabled = !isAvatarFullscreenOpen && !isQrOverlayOpen && !isQrScannerOpen && !isCustomAutoDeleteOpen && selectedKeys.isEmpty(),
                onDragStart = {},
                onDragProgress = { p -> scope.launch { swipeBackOffset.snapTo(p) } },
                onDragEnd = { shouldClose ->
                    if (shouldClose) {
                        onBack()
                    } else {
                        scope.launch { swipeBackOffset.animateTo(0f, spring()) }
                    }
                }
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .nestedScroll(pullDownNestedScroll)
                    .navigationBarsPadding()
            ) {
                item(key = "hero_header") {
                    UserProfileHeroHeader(
                        session = session,
                        isExpandedHero = isExpandedHero,
                        strings = strings,
                        expandProgress = heroExpandProgress.value,
                        currentAvatarIndex = currentAvatarIndex,
                        onSelectAvatarIndex = { currentAvatarIndex = it },
                        onOpenFullscreen = {
                            if (hasRealAvatar) {
                                isAvatarFullscreenOpen = true
                            }
                        },
                        onToggleExpand = {
                            if (hasRealAvatar) {
                                isAvatarFullscreenOpen = true
                            }
                        },
                        overlayActions = if (hasRealAvatar && isExpandedHero) {
                            {
                                UserProfileActionsRow(
                                    isMuted = session.isMuted,
                                    strings = strings,
                                    hazeState = hazeState,
                                    onChatClick = onBack,
                                    onMuteToggle = onToggleMute,
                                    onCallClick = {
                                        Toast.makeText(context, strings.callSoonToast, Toast.LENGTH_SHORT).show()
                                    },
                                    onVideoClick = {
                                        Toast.makeText(context, strings.callSoonToast, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else null
                    )
                }

                if (!(hasRealAvatar && isExpandedHero)) {
                    item(key = "actions_row") {
                        UserProfileActionsRow(
                            isMuted = session.isMuted,
                            strings = strings,
                            hazeState = hazeState,
                            onChatClick = onBack,
                            onMuteToggle = onToggleMute,
                            onCallClick = {
                                Toast.makeText(context, strings.callSoonToast, Toast.LENGTH_SHORT).show()
                            },
                            onVideoClick = {
                                Toast.makeText(context, strings.callSoonToast, Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                item(key = "info_card") {
                    UserProfileInfoCard(
                        anonIdOrFingerprint = session.fingerprint,
                        username = derivedHandle,
                        bio = bio,
                        birthday = birthday,
                        securityLabel = session.securityLabel,
                        strings = strings,
                        onOpenQr = { isQrOverlayOpen = true }
                    )
                }

                if (!addedToContactsLocal) {
                    item(key = "add_to_contacts") {
                        Spacer(modifier = Modifier.height(10.dp))
                        AddToContactsCard(
                            strings = strings,
                            onClick = {
                                addedToContactsLocal = true
                                onAddContact?.invoke()
                                Toast.makeText(context, strings.addedToContactsToast, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                if (availableTabs.isNotEmpty()) {
                    item(key = "tabs_spacer") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item(key = "pager_content") {
                        val stickyTabsIndex = when {
                            hasRealAvatar && isExpandedHero && addedToContactsLocal -> 3
                            hasRealAvatar && isExpandedHero -> 4
                            addedToContactsLocal -> 4
                            else -> 5
                        }
                        val pagerToOuterNestedScroll = remember(listState, stickyTabsIndex) {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    if (available.y < 0f && listState.firstVisibleItemIndex < stickyTabsIndex) {
                                        val consumed = listState.dispatchRawDelta(-available.y)
                                        if (consumed != 0f) {
                                            return Offset(0f, -consumed)
                                        }
                                    }
                                    return Offset.Zero
                                }

                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    if (available.y > 0f) {
                                        val outerConsumed = listState.dispatchRawDelta(-available.y)
                                        if (outerConsumed != 0f) {
                                            return Offset(0f, -outerConsumed)
                                        }
                                    }
                                    return Offset.Zero
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillParentMaxHeight(0.92f)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(pagerToOuterNestedScroll)
                            ) { pageIdx ->
                                val pageTab = availableTabs[pageIdx]
                                when (pageTab) {
                                    UserProfileTab.MEDIA, UserProfileTab.GIF -> {
                                        val itemsList = if (pageTab == UserProfileTab.MEDIA) buckets.media else buckets.gifs
                                        val chunked = remember(itemsList, gridColumns) { itemsList.chunked(gridColumns) }
                                        var pinchAccum by remember { mutableFloatStateOf(1f) }
                                        val tileBounds = remember { mutableStateMapOf<String, Rect>() }
                                        var gridOriginInRoot by remember { mutableStateOf(Offset.Zero) }
                                        var dragVisitedKeys by remember { mutableStateOf(emptySet<String>()) }

                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .onGloballyPositioned { coords ->
                                                    gridOriginInRoot = coords.positionInRoot()
                                                }
                                                .pointerInput(pageTab) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        pinchAccum = 1f
                                                        do {
                                                            val event = awaitPointerEvent()
                                                            val pressedCount = event.changes.count { it.pressed }
                                                            if (pressedCount >= 2) {
                                                                val zoom = event.calculateZoom()
                                                                pinchAccum *= zoom
                                                                if (pinchAccum >= 1.24f) {
                                                                    gridColumns = (gridColumns - 1).coerceAtLeast(2)
                                                                    pinchAccum = 1f
                                                                } else if (pinchAccum <= 0.80f) {
                                                                    gridColumns = (gridColumns + 1).coerceAtMost(6)
                                                                    pinchAccum = 1f
                                                                }
                                                                event.changes.forEach { it.consume() }
                                                            }
                                                        } while (event.changes.any { it.pressed })
                                                    }
                                                }
                                                .pointerInput(pageTab, gridColumns) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { startOffset ->
                                                            val rootPt = gridOriginInRoot + startOffset
                                                            val hitKey = tileBounds.entries.firstOrNull { it.value.contains(rootPt) }?.key
                                                            if (hitKey != null) {
                                                                dragVisitedKeys = setOf(hitKey)
                                                                selectedKeys = selectedKeys + hitKey
                                                            } else {
                                                                dragVisitedKeys = emptySet()
                                                            }
                                                        },
                                                        onDragEnd = { dragVisitedKeys = emptySet() },
                                                        onDragCancel = { dragVisitedKeys = emptySet() },
                                                        onDrag = { change, _ ->
                                                            change.consume()
                                                            val rootPt = gridOriginInRoot + change.position
                                                            val hitKey = tileBounds.entries.firstOrNull { it.value.contains(rootPt) }?.key
                                                            if (hitKey != null && hitKey !in dragVisitedKeys) {
                                                                dragVisitedKeys = dragVisitedKeys + hitKey
                                                                selectedKeys = selectedKeys + hitKey
                                                            }
                                                        }
                                                    )
                                                },
                                            contentPadding = PaddingValues(top = 56.dp, bottom = 72.dp)
                                        ) {
                                            items(chunked) { rowItems ->
                                                UserProfileMediaGridRow(
                                                    rowItems = rowItems,
                                                    columns = gridColumns,
                                                    selectedKeys = selectedKeys,
                                                    onTilePositioned = { key, rect ->
                                                        tileBounds[key] = rect
                                                    },
                                                    onMediaClick = { item ->
                                                        if (selectedKeys.isNotEmpty()) {
                                                            val k = item.itemKey
                                                            selectedKeys = if (k in selectedKeys) selectedKeys - k else selectedKeys + k
                                                        } else {
                                                            onOpenMediaMessage(item.message)
                                                        }
                                                    },
                                                    onMediaLongClick = { item ->
                                                        val k = item.itemKey
                                                        selectedKeys = if (k in selectedKeys) selectedKeys - k else selectedKeys + k
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    UserProfileTab.FILES -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = 56.dp, bottom = 72.dp)
                                        ) {
                                            items(buckets.files) { fileItem ->
                                                UserProfileFileRow(item = fileItem)
                                            }
                                        }
                                    }

                                    UserProfileTab.LINKS -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = 56.dp, bottom = 72.dp)
                                        ) {
                                            items(buckets.links) { linkItem ->
                                                UserProfileLinkRow(item = linkItem)
                                            }
                                        }
                                    }

                                    UserProfileTab.AUDIO -> {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = 56.dp, bottom = 72.dp)
                                        ) {
                                            items(buckets.audio) { audioItem ->
                                                UserProfileAudioRow(
                                                    item = audioItem,
                                                    onAudioClick = { onBack() }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Плавающие Liquid Glass таблетки вкладок поверх скроллящегося контента
                            UserProfileTabBar(
                                tabs = availableTabs,
                                selectedTab = selectedTab,
                                hazeState = hazeState,
                                strings = strings,
                                onSelectTab = { tab ->
                                    val idx = availableTabs.indexOf(tab)
                                    if (idx >= 0) {
                                        scope.launch { pagerState.animateScrollToPage(idx) }
                                    }
                                },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                } else {
                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }

            UserProfileTopOverlayBar(
                session = session,
                hazeState = hazeState,
                isCollapsed = isCollapsed,
                collapsedSubtitle = if (availableTabs.isNotEmpty()) {
                    tabSummaryText(selectedTab, buckets, strings)
                } else {
                    ""
                },
                selectionCount = selectedKeys.size,
                strings = strings,
                onBack = onBack,
                onClearSelection = { selectedKeys = emptySet() },
                onShowInChat = {
                    val targetMsgId = selectedKeys.firstOrNull()?.substringBefore("#")
                    selectedKeys = emptySet()
                    val targetMsg = targetMsgId?.let { id -> messages.firstOrNull { it.id == id } }
                    if (targetMsg != null) {
                        onShowInChat(targetMsg)
                    }
                },
                onForwardSelected = {
                    val ids = selectedKeys.map { it.substringBefore("#") }.distinct()
                    selectedKeys = emptySet()
                    if (ids.isNotEmpty()) {
                        onForwardMessages(ids)
                    }
                },
                onDeleteSelected = {
                    val ids = selectedKeys.map { it.substringBefore("#") }.distinct()
                    selectedKeys = emptySet()
                    if (ids.isNotEmpty()) {
                        onDeleteMessages(ids)
                    }
                },
                onOpenOverflow = { isOverflowOpen = true },
                overflowMenu = {
                    val isInMediaSection by remember {
                        derivedStateOf {
                            val pagerItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "pager_content" }
                            listState.firstVisibleItemIndex >= 2 || (pagerItem != null && pagerItem.offset <= 500)
                        }
                    }
                    val isMediaGridTab = isInMediaSection && (selectedTab == UserProfileTab.MEDIA || selectedTab == UserProfileTab.GIF)
                    UserProfileOverflowMenu(
                        expanded = isOverflowOpen,
                        selfDestruct = selfDestruct,
                        isContact = addedToContactsLocal,
                        isBlocked = isBlocked,
                        showGridZoomOptions = isMediaGridTab,
                        canZoomIn = gridColumns > 2,
                        canZoomOut = gridColumns < 6,
                        strings = strings,
                        onDismiss = { isOverflowOpen = false },
                        onZoomIn = { gridColumns = (gridColumns - 1).coerceAtLeast(2) },
                        onZoomOut = { gridColumns = (gridColumns + 1).coerceAtMost(6) },
                        onSetSelfDestruct = onSetSelfDestruct,
                        onOpenCustomAutoDelete = { isCustomAutoDeleteOpen = true },
                        onShareContact = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(session.title, "${session.title} (${session.fingerprint})"))
                            Toast.makeText(context, strings.shareContactToast, Toast.LENGTH_SHORT).show()
                        },
                        onBlockUser = {
                            val nowBlocked = com.anongram.app.features.chats.user_profile.data.BlockedUsersStore.toggle(context, session.id)
                            Toast.makeText(
                                context,
                                if (nowBlocked) strings.blockedToast else strings.unblockedToast,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onEditContact = {
                            Toast.makeText(context, session.title, Toast.LENGTH_SHORT).show()
                        },
                        onDeleteContact = {
                            addedToContactsLocal = false
                            onDeleteChat?.invoke()
                        },
                        onRestrictCopying = {
                            Toast.makeText(context, strings.restrictCopyToast, Toast.LENGTH_SHORT).show()
                        },
                        onReport = {
                            Toast.makeText(context, strings.reportToast, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (isAvatarFullscreenOpen && session.avatarResIds.isNotEmpty()) {
                UserProfileAvatarFullscreenOverlay(
                    avatarResIds = session.avatarResIds,
                    title = session.title,
                    strings = strings,
                    initialPage = currentAvatarIndex,
                    onClose = { isAvatarFullscreenOpen = false }
                )
            }

            if (isCustomAutoDeleteOpen) {
                AutoDeleteCustomSheet(
                    current = selfDestruct,
                    isRu = isRu,
                    strings = strings,
                    onDismiss = { isCustomAutoDeleteOpen = false },
                    onApply = onSetSelfDestruct
                )
            }

            if (isQrOverlayOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(40f)
                ) {
                    MyQrCodeScreen(
                        handle = derivedHandle,
                        publicKeyHex = session.fingerprint,
                        onClose = { isQrOverlayOpen = false },
                        onOpenScanner = { isQrScannerOpen = true }
                    )
                }
            }

            if (isQrScannerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(45f)
                ) {
                    QrCameraScannerScreen(
                        onBack = { isQrScannerOpen = false }
                    )
                }
            }
        }
    }
}

private fun extractProfileBuckets(messages: List<ChatMessage>): UserProfileBuckets {
    val media = mutableListOf<UserProfileMediaItem>()
    val files = mutableListOf<UserProfileFileItem>()
    val links = mutableListOf<UserProfileLinkItem>()
    val gifs = mutableListOf<UserProfileMediaItem>()
    val audio = mutableListOf<UserProfileAudioItem>()

    messages.asReversed().forEach { msg ->
        when (val att = msg.attachment) {
            is MessageAttachment.Photo -> {
                val uri = att.uri
                if (!uri.isNullOrBlank()) {
                    val isGif = uri.endsWith(".gif", ignoreCase = true) ||
                        att.label.contains("gif", ignoreCase = true)
                    if (isGif) {
                        gifs.add(UserProfileMediaItem(msg, uri, isVideo = false, itemKey = "${msg.id}#gif"))
                    } else {
                        media.add(UserProfileMediaItem(msg, uri, isVideo = att.isVideo, itemKey = "${msg.id}#photo"))
                    }
                }
            }

            is MessageAttachment.MediaAlbum -> {
                att.items.forEachIndexed { idx, item ->
                    if (item.uri.isNotBlank()) {
                        media.add(UserProfileMediaItem(msg, item.uri, isVideo = item.isVideo, itemKey = "${msg.id}#$idx"))
                    }
                }
            }

            is MessageAttachment.File -> {
                val isGif = att.name.endsWith(".gif", ignoreCase = true) && !att.uri.isNullOrBlank()
                if (isGif) {
                    gifs.add(UserProfileMediaItem(msg, att.uri!!, isVideo = false, itemKey = "${msg.id}#fgif"))
                } else {
                    files.add(UserProfileFileItem(msg, att))
                }
            }

            is MessageAttachment.FileGroup -> {
                att.items.forEach { f ->
                    files.add(UserProfileFileItem(msg, f))
                }
            }

            is MessageAttachment.Music -> {
                audio.add(
                    UserProfileAudioItem(
                        message = msg,
                        title = att.title,
                        subtitle = listOf(att.artist, att.durationLabel).filter { it.isNotBlank() }.joinToString(" · "),
                        uri = att.uri,
                        isVoice = false
                    )
                )
            }

            is MessageAttachment.MusicGroup -> {
                att.items.forEach { track ->
                    audio.add(
                        UserProfileAudioItem(
                            message = msg,
                            title = track.title,
                            subtitle = listOf(track.artist, track.durationLabel).filter { it.isNotBlank() }.joinToString(" · "),
                            uri = track.uri,
                            isVoice = false
                        )
                    )
                }
            }

            is MessageAttachment.Voice -> {
                audio.add(
                    UserProfileAudioItem(
                        message = msg,
                        title = "Голосовое сообщение",
                        subtitle = att.durationLabel,
                        uri = att.uri,
                        isVoice = true
                    )
                )
            }

            else -> Unit
        }

        if (msg.text.isNotBlank()) {
            URL_REGEX.findAll(msg.text).forEach { match ->
                links.add(UserProfileLinkItem(msg, match.value))
            }
        }
    }

    return UserProfileBuckets(
        media = media,
        files = files,
        links = links,
        gifs = gifs,
        audio = audio
    )
}
