package com.anongram.app.features.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anongram.app.core.navigation.AnonGramTransitionScreen
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.PhysicsSprings
import com.anongram.app.core.navigation.TransitionType
import com.anongram.app.core.navigation.swipeBackGesture
import com.anongram.app.core.theme.ThemeController
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import kotlin.coroutines.cancellation.CancellationException
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.archive.ArchiveScreen
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.data.InMemoryChatListRepository
import com.anongram.app.features.chats.mainmenu.ChatsMainMenuScreen
import com.anongram.app.features.chats.search.ChatSearchScreen
import com.anongram.app.features.chats.search.ChatSearchViewModel
import com.anongram.app.features.chats.state.ChatListViewModel
import com.anongram.app.features.chats.messages.data.InMemoryChatMessagesRepository
import com.anongram.app.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.app.features.chats.ui.ChatScreen
import com.anongram.app.features.contacts.common.Contact
import com.anongram.app.features.contacts.data.ContactRepository
import com.anongram.app.features.contacts.data.InMemoryContactRepository
import com.anongram.app.features.contacts.state.ContactsViewModel
import com.anongram.app.features.contacts.ui.ContactsScreen
import com.anongram.app.features.navigation.components.AppHeader
import com.anongram.app.features.profile.ui.ProfileScreen
import com.anongram.app.features.qr.ui.QrHubOverlay
import com.anongram.app.features.services.ai.ui.LocalAiChatScreen
import com.anongram.app.features.services.ui.ServicesScreen
import com.anongram.app.features.settings.modules.ui.ai.LocalAiModuleSettingsScreen
import com.anongram.app.features.settings.ui.SettingsScreen
import com.anongram.app.features.settings.language.ui.LanguageSettingsScreen
import androidx.compose.ui.platform.LocalContext
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.media.player.ChatMusicPlayerSheet
import com.anongram.app.features.chats.media.player.MusicFullScreenPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Каркас приложения: 4 таба + плавающий остров с центральной QR-кнопкой,
 * полноэкранные оверлеи (поиск, контакты, архив, чат) на движке переходов.
 * Адаптивность: на Expanded-ширинах чат открывается в правой панели (two-pane).
 */
@Composable
fun MainAppShell(
    themeController: ThemeController,
    widthSizeClass: WindowWidthSizeClass,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableIntStateOf(0) }
    var isQrOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isContactsOpen by remember { mutableStateOf(false) }
    var isArchiveOpen by remember { mutableStateOf(false) }
    var isLanguageOpen by remember { mutableStateOf(false) }
    var isLocalAiOpen by remember { mutableStateOf(false) }
    var isLocalAiModulesOpen by remember { mutableStateOf(false) }
    var isSettingsSubScreenOpen by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<IChatSession?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val audioController = remember { ChatAudioController.getInstance(context) }
    val audioPlayerState by audioController.state.collectAsState()

    val scheme by themeController.scheme.collectAsState()
    val animState by AnimationsSettingsStore.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized(context)
    }

    var predictiveSwipeEdge by remember { mutableStateOf(0) }
    var predictiveTouchYFraction by remember { mutableStateOf(0.5f) }
    var isPredictiveHolding by remember { mutableStateOf(false) }
    var isSystemGestureActive by remember { mutableStateOf(false) }
    val navConfig = remember(animState, predictiveSwipeEdge, predictiveTouchYFraction, isPredictiveHolding, isSystemGestureActive) {
        animState.toNavigationTransitionConfig().copy(
            swipeEdge = predictiveSwipeEdge,
            touchYFraction = predictiveTouchYFraction,
            isPredictiveHolding = isPredictiveHolding,
            isSystemGestureActive = isSystemGestureActive
        )
    }
    val messagesRepository = remember { InMemoryChatMessagesRepository() }
    val chatsRepository = remember { InMemoryChatListRepository(messagesRepository) }
    val contactsRepository = remember { InMemoryContactRepository() }

    val chatsViewModel: ChatListViewModel = viewModel(
        factory = viewModelFactory { initializer { ChatListViewModel(chatsRepository) } }
    )
    val searchViewModel: ChatSearchViewModel = viewModel(
        key = "chat_search",
        factory = viewModelFactory {
            initializer { ChatSearchViewModel(chatsRepository, contactsRepository) }
        }
    )
    val contactsViewModel: ContactsViewModel = viewModel(
        factory = viewModelFactory { initializer { ContactsViewModel(contactsRepository) } }
    )

    val detailProgress = remember { Animatable(0f) }
    val searchProgress = remember { Animatable(0f) }
    val contactsProgress = remember { Animatable(0f) }
    val archiveProgress = remember { Animatable(0f) }
    val languageProgress = remember { Animatable(0f) }
    val localAiProgress = remember { Animatable(0f) }
    val localAiModulesProgress = remember { Animatable(0f) }

    var isDetailExiting by remember { mutableStateOf(false) }
    var isSearchExiting by remember { mutableStateOf(false) }
    var isContactsExiting by remember { mutableStateOf(false) }
    var isArchiveExiting by remember { mutableStateOf(false) }
    var isLanguageExiting by remember { mutableStateOf(false) }
    var isLocalAiExiting by remember { mutableStateOf(false) }
    var isLocalAiModulesExiting by remember { mutableStateOf(false) }

    fun openSession(session: IChatSession) {
        chatsViewModel.clearSelection()
        chatsViewModel.markRead(session.id)
        isDetailExiting = false
        scope.openOverlay(detailProgress, navConfig) { selectedSession = session }
    }

    fun closeSession() {
        chatsViewModel.refresh()
        isDetailExiting = true
        scope.closeOverlay(detailProgress, navConfig) {
            selectedSession = null
            isDetailExiting = false
        }
    }

    fun openContact(contact: Contact) {
        scope.launch {
            val session = chatsViewModel.findOrCreateDirectChat(contact.name, contact.fingerprint)
            isSearchOpen = false
            isContactsOpen = false
            openSession(session)
        }
    }

    fun openSearch() {
        chatsViewModel.clearSelection()
        isSearchExiting = false
        scope.openOverlay(searchProgress, navConfig) { isSearchOpen = true }
    }

    fun closeSearch() {
        isSearchExiting = true
        scope.closeOverlay(searchProgress, navConfig) {
            isSearchOpen = false
            isSearchExiting = false
        }
    }

    fun openContacts() {
        chatsViewModel.clearSelection()
        isContactsExiting = false
        scope.openOverlay(contactsProgress, navConfig) { isContactsOpen = true }
    }

    fun closeContacts() {
        isContactsExiting = true
        scope.closeOverlay(contactsProgress, navConfig) {
            isContactsOpen = false
            isContactsExiting = false
        }
    }

    fun openArchive() {
        chatsViewModel.clearSelection()
        isArchiveExiting = false
        scope.openOverlay(archiveProgress, navConfig) { isArchiveOpen = true }
    }

    fun closeArchive() {
        isArchiveExiting = true
        scope.closeOverlay(archiveProgress, navConfig) {
            isArchiveOpen = false
            isArchiveExiting = false
        }
    }

    fun openLanguage() {
        isLanguageExiting = false
        scope.openOverlay(languageProgress, navConfig) { isLanguageOpen = true }
    }

    fun closeLanguage() {
        isLanguageExiting = true
        scope.closeOverlay(languageProgress, navConfig) {
            isLanguageOpen = false
            isLanguageExiting = false
        }
    }

    fun openLocalAi() {
        isLocalAiExiting = false
        scope.openOverlay(localAiProgress, navConfig) { isLocalAiOpen = true }
    }

    fun closeLocalAi() {
        isLocalAiExiting = true
        scope.closeOverlay(localAiProgress, navConfig) {
            isLocalAiOpen = false
            isLocalAiExiting = false
        }
    }

    fun openLocalAiModules() {
        isLocalAiModulesExiting = false
        scope.openOverlay(localAiModulesProgress, navConfig) { isLocalAiModulesOpen = true }
    }

    fun closeLocalAiModules() {
        isLocalAiModulesExiting = true
        scope.closeOverlay(localAiModulesProgress, navConfig) {
            isLocalAiModulesOpen = false
            isLocalAiModulesExiting = false
        }
    }

    val showDetailPane = false
    val showDetailOverlay = selectedSession != null

    BackHandler(enabled = isQrOpen) {
        isQrOpen = false
    }

    val hasTransitionOverlay = !isQrOpen && (
        isLocalAiModulesOpen || isLocalAiOpen || isLanguageOpen || isSearchOpen || isContactsOpen || isArchiveOpen || selectedSession != null
    )
    androidx.compose.runtime.LaunchedEffect(hasTransitionOverlay) {
        if (!hasTransitionOverlay) {
            isSystemGestureActive = false
        }
    }
    val isPureSwipeOrParallax = (navConfig.transitionType == TransitionType.SPRING_SWIPE ||
        navConfig.transitionType == TransitionType.CUPERTINO_PARALLAX) &&
        !navConfig.enableMiniCardCombo &&
        !navConfig.enablePredictiveBackCombo
    val usePredictiveBack = navConfig.transitionType != TransitionType.INSTANT && !isPureSwipeOrParallax
    BackHandler(enabled = hasTransitionOverlay && !usePredictiveBack) {
        when {
            isLocalAiModulesOpen -> closeLocalAiModules()
            isLocalAiOpen -> closeLocalAi()
            isLanguageOpen -> closeLanguage()
            isSearchOpen -> closeSearch()
            isContactsOpen -> closeContacts()
            isArchiveOpen -> closeArchive()
            else -> closeSession()
        }
    }
    PredictiveBackHandler(enabled = hasTransitionOverlay && usePredictiveBack) { backEvents ->
        val (activeProgress, setExiting, onFinish) = when {
            isLocalAiModulesOpen -> Triple(localAiModulesProgress, { v: Boolean -> isLocalAiModulesExiting = v }, ::closeLocalAiModules)
            isLocalAiOpen -> Triple(localAiProgress, { v: Boolean -> isLocalAiExiting = v }, ::closeLocalAi)
            isLanguageOpen -> Triple(languageProgress, { v: Boolean -> isLanguageExiting = v }, ::closeLanguage)
            isSearchOpen -> Triple(searchProgress, { v: Boolean -> isSearchExiting = v }, ::closeSearch)
            isContactsOpen -> Triple(contactsProgress, { v: Boolean -> isContactsExiting = v }, ::closeContacts)
            isArchiveOpen -> Triple(archiveProgress, { v: Boolean -> isArchiveExiting = v }, ::closeArchive)
            else -> Triple(detailProgress, { v: Boolean -> isDetailExiting = v }, ::closeSession)
        }
        val screenH = context.resources.displayMetrics.heightPixels.toFloat().coerceAtLeast(1f)
        setExiting(true)
        isPredictiveHolding = true
        isSystemGestureActive = true
        try {
            backEvents.collect { event ->
                predictiveSwipeEdge = if (event.swipeEdge == 1) 1 else 0
                predictiveTouchYFraction = (event.touchY / screenH).coerceIn(0f, 1f)
                activeProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            isPredictiveHolding = false
            onFinish()
            predictiveSwipeEdge = 0
            predictiveTouchYFraction = 0.5f
        } catch (_: CancellationException) {
            isPredictiveHolding = false
            scope.snapBackOverlay(activeProgress, navConfig) {
                setExiting(false)
                isSystemGestureActive = false
                predictiveSwipeEdge = 0
                predictiveTouchYFraction = 0.5f
            }
        }
    }

    com.anongram.app.features.chats.links.browser.InAppBrowserHost(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background)
        ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .then(
                        if (showDetailPane) Modifier.weight(0.42f).fillMaxHeight()
                        else Modifier.fillMaxSize()
                    )
            ) {
                when (currentTab) {
                    0 -> ChatsMainMenuScreen(
                        viewModel = chatsViewModel,
                        onChatOpen = { session -> openSession(session) },
                        onOpenSearch = { openSearch() },
                        onOpenArchive = { openArchive() },
                        onOpenContacts = { openContacts() }
                    )

                    1 -> {
                        AppHeader()
                        ServicesScreen(onOpenLocalAi = { openLocalAi() })
                    }

                    2 -> {
                        SettingsScreen(
                            currentThemeId = scheme.id,
                            onThemeSelect = themeController::selectTheme,
                            onOpenLanguage = { openLanguage() },
                            onLogout = onLogout,
                            onSubScreenVisibilityChange = { isSettingsSubScreenOpen = it }
                        )
                    }

                    3 -> {
                        AppHeader()
                        ProfileScreen()
                    }
                }
            }

            // Two-pane на широких экранах: чат живёт в правой панели.
            val chatsState by chatsViewModel.uiState.collectAsState()
            if (showDetailPane) {
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .background(scheme.background)
                        .safeStatusBarPadding()
                ) {
                    selectedSession?.let { session ->
                        val chatMessagesViewModel: ChatMessagesViewModel = viewModel(
                            key = "chat_msgs_${session.id}",
                            factory = viewModelFactory {
                                initializer { ChatMessagesViewModel(session.id, messagesRepository, chatsRepository) }
                            }
                        )
                        ChatScreen(
                            session = session,
                            viewModel = chatMessagesViewModel,
                            onBack = { closeSession() },
                            activeSessions = chatsState.sessions,
                            onOpenTargetSession = { target -> openSession(target) },
                            onOpenDirectPeer = { name, handleOrPhone, onReady ->
                                scope.launch {
                                    val target = chatsViewModel.findOrCreateDirectChat(name, handleOrPhone)
                                    onReady(target)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Полноэкранный чат на компактных экранах.
        val chatsState by chatsViewModel.uiState.collectAsState()
        if (showDetailOverlay) {
            selectedSession?.let { session ->
                var isChatChildOverlayOpen by remember(session.id) { mutableStateOf(false) }
                val chatMessagesViewModel: ChatMessagesViewModel = viewModel(
                    key = "chat_msgs_${session.id}",
                    factory = viewModelFactory {
                        initializer { ChatMessagesViewModel(session.id, messagesRepository, chatsRepository) }
                    }
                )
                AnonGramTransitionScreen(
                    progress = detailProgress.value,
                    isExit = isDetailExiting,
                    config = navConfig,
                    modifier = Modifier.swipeBackGesture(
                        enabled = navConfig.allowFullWidthSwipe && !isChatChildOverlayOpen,
                        onDragStart = { isDetailExiting = true },
                        onDragProgress = { p -> scope.launch { detailProgress.snapTo(p) } },
                        onDragEnd = { shouldClose ->
                            if (shouldClose) closeSession()
                            else scope.snapBackOverlay(detailProgress, navConfig) { isDetailExiting = false }
                        }
                    )
                ) {
                    ChatScreen(
                        session = session,
                        viewModel = chatMessagesViewModel,
                        onBack = { closeSession() },
                        activeSessions = chatsState.sessions,
                        onOpenTargetSession = { target -> openSession(target) },
                        onOpenDirectPeer = { name, handleOrPhone, onReady ->
                            scope.launch {
                                val target = chatsViewModel.findOrCreateDirectChat(name, handleOrPhone)
                                onReady(target)
                            }
                        },
                        onChildOverlayOpenChange = { isChatChildOverlayOpen = it }
                    )
                }
            }
        }

        if (isSearchOpen) {
            AnonGramTransitionScreen(
                progress = searchProgress.value,
                isExit = isSearchExiting,
                config = navConfig,
                modifier = Modifier.swipeBackGesture(
                    enabled = navConfig.allowFullWidthSwipe,
                    onDragStart = { isSearchExiting = true },
                    onDragProgress = { p -> scope.launch { searchProgress.snapTo(p) } },
                    onDragEnd = { shouldClose ->
                        if (shouldClose) closeSearch()
                        else scope.snapBackOverlay(searchProgress, navConfig) { isSearchExiting = false }
                    }
                )
            ) {
                ChatSearchScreen(
                    viewModel = searchViewModel,
                    onBack = { closeSearch() },
                    onSessionOpen = { session ->
                        closeSearch()
                        openSession(session)
                    },
                    onContactOpen = { contact -> openContact(contact) }
                )
            }
        }

        if (isContactsOpen) {
            AnonGramTransitionScreen(
                progress = contactsProgress.value,
                isExit = isContactsExiting,
                config = navConfig,
                modifier = Modifier.swipeBackGesture(
                    enabled = navConfig.allowFullWidthSwipe,
                    onDragStart = { isContactsExiting = true },
                    onDragProgress = { p -> scope.launch { contactsProgress.snapTo(p) } },
                    onDragEnd = { shouldClose ->
                        if (shouldClose) closeContacts()
                        else scope.snapBackOverlay(contactsProgress, navConfig) { isContactsExiting = false }
                    }
                )
            ) {
                ContactsScreen(
                    viewModel = contactsViewModel,
                    onBack = { closeContacts() },
                    onContactClick = { contact -> openContact(contact) },
                    onOpenQr = {
                        closeContacts()
                        isQrOpen = true
                    },
                    onOpenSearch = {
                        closeContacts()
                        openSearch()
                    }
                )
            }
        }

        if (isArchiveOpen) {
            AnonGramTransitionScreen(
                progress = archiveProgress.value,
                isExit = isArchiveExiting,
                config = navConfig,
                modifier = Modifier.swipeBackGesture(
                    enabled = navConfig.allowFullWidthSwipe,
                    onDragStart = { isArchiveExiting = true },
                    onDragProgress = { p -> scope.launch { archiveProgress.snapTo(p) } },
                    onDragEnd = { shouldClose ->
                        if (shouldClose) closeArchive()
                        else scope.snapBackOverlay(archiveProgress, navConfig) { isArchiveExiting = false }
                    }
                )
            ) {
                ArchiveScreen(
                    viewModel = chatsViewModel,
                    onBack = { closeArchive() },
                    onChatOpen = { session ->
                        closeArchive()
                        openSession(session)
                    }
                )
            }
        }

        if (isLanguageOpen) {
            AnonGramTransitionScreen(
                progress = languageProgress.value,
                isExit = isLanguageExiting,
                config = navConfig,
                modifier = Modifier.swipeBackGesture(
                    enabled = navConfig.allowFullWidthSwipe,
                    onDragStart = { isLanguageExiting = true },
                    onDragProgress = { p -> scope.launch { languageProgress.snapTo(p) } },
                    onDragEnd = { shouldClose ->
                        if (shouldClose) closeLanguage()
                        else scope.snapBackOverlay(languageProgress, navConfig) { isLanguageExiting = false }
                    }
                )
            ) {
                LanguageSettingsScreen(
                    onBack = { closeLanguage() }
                )
            }
        }

        if (isLocalAiOpen) {
            AnonGramTransitionScreen(
                progress = localAiProgress.value,
                isExit = isLocalAiExiting,
                config = navConfig,
                modifier = Modifier.swipeBackGesture(
                    enabled = navConfig.allowFullWidthSwipe,
                    onDragStart = { isLocalAiExiting = true },
                    onDragProgress = { p -> scope.launch { localAiProgress.snapTo(p) } },
                    onDragEnd = { shouldClose ->
                        if (shouldClose) closeLocalAi()
                        else scope.snapBackOverlay(localAiProgress, navConfig) { isLocalAiExiting = false }
                    }
                )
            ) {
                LocalAiChatScreen(
                    onClose = { closeLocalAi() },
                    onOpenModules = { openLocalAiModules() }
                )
            }
        }

        if (isLocalAiModulesOpen) {
            AnonGramTransitionScreen(
                progress = localAiModulesProgress.value,
                isExit = isLocalAiModulesExiting,
                config = navConfig,
                modifier = Modifier.swipeBackGesture(
                    enabled = navConfig.allowFullWidthSwipe,
                    onDragStart = { isLocalAiModulesExiting = true },
                    onDragProgress = { p -> scope.launch { localAiModulesProgress.snapTo(p) } },
                    onDragEnd = { shouldClose ->
                        if (shouldClose) closeLocalAiModules()
                        else scope.snapBackOverlay(localAiModulesProgress, navConfig) { isLocalAiModulesExiting = false }
                    }
                )
            ) {
                LocalAiModuleSettingsScreen(
                    onBack = { closeLocalAiModules() },
                    modifier = Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                )
            }
        }

        if (isQrOpen) {
            QrHubOverlay(
                onClose = { isQrOpen = false },
                onOpenContacts = {
                    isQrOpen = false
                    openContacts()
                }
            )
        }

        val incomingShare by com.anongram.app.features.chats.tools.IncomingShareBus.payload.collectAsState()
        incomingShare?.let { sharePayload ->
            val toolsStrings = com.anongram.app.features.chats.tools.i18n.ToolsI18n.current
            com.anongram.app.features.chats.tools.ForwardTargetPickerScreen(
                activeSessions = chatsState.sessions,
                titleOverride = toolsStrings.selectChatTitle,
                multiSelectMode = true,
                onClose = { com.anongram.app.features.chats.tools.IncomingShareBus.clear() },
                onConfirmMultiSelect = { targets, comment ->
                    val summary = listOfNotNull(sharePayload.text, comment.takeIf { it.isNotBlank() }).joinToString("\n")
                    android.widget.Toast.makeText(
                        context,
                        if (summary.isNotBlank()) "Отправлено (${targets.size})" else "Отправлено (${targets.size})",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    com.anongram.app.features.chats.tools.IncomingShareBus.clear()
                    targets.firstOrNull()?.let { openSession(it) }
                },
                onSelectSavedMessages = {
                    com.anongram.app.features.chats.tools.IncomingShareBus.clear()
                    chatsState.sessions.firstOrNull { it.id == "0" }?.let { openSession(it) }
                },
                onSelectSession = { target ->
                    com.anongram.app.features.chats.tools.IncomingShareBus.clear()
                    openSession(target)
                },
                onSelectGlobalItem = { name, handle ->
                    com.anongram.app.features.chats.tools.IncomingShareBus.clear()
                    scope.launch {
                        val target = chatsViewModel.findOrCreateDirectChat(name, handle)
                        openSession(target)
                    }
                }
            )
        }

        // Остров виден только на корневых экранах: любой дочерний экран
        // (чат, поиск, контакты, архив, QR, язык, AI) его скрывает.
        val isRootScreen = selectedSession == null && !isSearchOpen && !isContactsOpen &&
            !isArchiveOpen && !isQrOpen && !isLanguageOpen && incomingShare == null &&
            !isLocalAiOpen && !isLocalAiModulesOpen &&
            !(currentTab == 2 && isSettingsSubScreenOpen)

        AnimatedVisibility(
            visible = isRootScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            MainFloatingIsland(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onQrClick = { isQrOpen = true },
                modifier = Modifier.safeNavigationBarPadding()
            )
        }

        // Глобальные оверлеи карточки плеера: открываются из любого таба поверх всего интерфейса
        if (!showDetailOverlay && audioPlayerState.isSheetOpen) {
            ChatMusicPlayerSheet(
                controller = audioController,
                onDismiss = { audioController.closeSheet() }
            )
        }

        if (!showDetailOverlay && audioPlayerState.isFullScreenOpen) {
            MusicFullScreenPlayer(
                controller = audioController,
                onDismiss = { audioController.closeFullScreen() }
            )
        }
    }
    }
}

/** Открытие оверлея: подложить экран под анимацию входа. */
private fun CoroutineScope.openOverlay(
    progress: Animatable<Float, *>,
    config: NavigationTransitionConfig,
    show: () -> Unit
) {
    launch {
        val skipEnter = config.transitionType == TransitionType.INSTANT ||
            (config.transitionType.isBidirectional && !config.enableEnterAnimation && !config.enableBaseSmoothEnter) ||
            (!config.transitionType.isBidirectional && !config.enableBaseSmoothEnter)
        if (skipEnter) {
            progress.snapTo(0f)
            show()
        } else {
            progress.snapTo(1f)
            show()
            androidx.compose.runtime.withFrameNanos { }
            if (config.transitionType == TransitionType.PARTICLE_DISSOLVE) {
                androidx.compose.runtime.withFrameNanos { }
            }
            progress.animateTo(
                targetValue = 0f,
                animationSpec = if (config.transitionType == TransitionType.SPRING_SWIPE) {
                    PhysicsSprings.screenSpringSpec(
                        damping = config.springDamping,
                        stiffness = config.springStiffness * config.speedMultiplier
                    )
                } else {
                    tween(durationMillis = config.effectiveDurationMs)
                }
            )
        }
    }
}

/** Закрытие оверлея: анимация выхода, затем скрытие. */
private fun CoroutineScope.closeOverlay(
    progress: Animatable<Float, *>,
    config: NavigationTransitionConfig,
    hide: () -> Unit
) {
    launch {
        if (config.transitionType != TransitionType.INSTANT) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = if (config.transitionType == TransitionType.SPRING_SWIPE) {
                    PhysicsSprings.screenSpringSpec(
                        damping = config.springDamping,
                        stiffness = config.springStiffness * config.speedMultiplier
                    )
                } else {
                    tween(durationMillis = config.effectiveDurationMs)
                }
            )
        }
        hide()
        progress.snapTo(0f)
    }
}

/** Возврат оверлея на место при отмене свайпа или системного Predictive Back. */
private fun CoroutineScope.snapBackOverlay(
    progress: Animatable<Float, *>,
    config: NavigationTransitionConfig,
    onDone: () -> Unit = {}
) {
    launch {
        if (config.transitionType == TransitionType.INSTANT) {
            progress.snapTo(0f)
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = if (config.transitionType == TransitionType.SPRING_SWIPE) {
                    PhysicsSprings.screenSpringSpec(
                        damping = config.springDamping,
                        stiffness = config.springStiffness * config.speedMultiplier
                    )
                } else {
                    tween(durationMillis = config.effectiveDurationMs)
                }
            )
        }
        onDone()
    }
}
