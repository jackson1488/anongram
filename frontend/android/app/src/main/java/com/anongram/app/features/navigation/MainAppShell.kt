package com.anongram.app.features.navigation

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anongram.app.core.navigation.AnonGramTransitionScreen
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.PhysicsSprings
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.ThemeController
import com.anongram.app.features.chats.archive.ArchiveScreen
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.data.InMemoryChatListRepository
import com.anongram.app.features.chats.mainmenu.ChatsMainMenuScreen
import com.anongram.app.features.chats.search.ChatSearchScreen
import com.anongram.app.features.chats.search.ChatSearchViewModel
import com.anongram.app.features.chats.state.ChatListViewModel
import com.anongram.app.features.chats.ui.ChatDetailScreen
import com.anongram.app.features.contacts.common.Contact
import com.anongram.app.features.contacts.data.ContactRepository
import com.anongram.app.features.contacts.data.InMemoryContactRepository
import com.anongram.app.features.contacts.state.ContactsViewModel
import com.anongram.app.features.contacts.ui.ContactsScreen
import com.anongram.app.features.navigation.components.AppHeader
import com.anongram.app.features.profile.ui.ProfileScreen
import com.anongram.app.features.qr.ui.QrHubOverlay
import com.anongram.app.features.services.ui.ServicesScreen
import com.anongram.app.features.settings.ui.SettingsScreen
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
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableIntStateOf(0) }
    var isQrOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isContactsOpen by remember { mutableStateOf(false) }
    var isArchiveOpen by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<IChatSession?>(null) }
    val scope = rememberCoroutineScope()

    val scheme by themeController.scheme.collectAsState()

    val navConfig = remember { NavigationTransitionConfig() }
    val chatsRepository = remember { InMemoryChatListRepository() }
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

    fun openSession(session: IChatSession) {
        chatsViewModel.clearSelection()
        scope.openOverlay(detailProgress) { selectedSession = session }
    }

    fun closeSession() {
        scope.closeOverlay(detailProgress) { selectedSession = null }
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
        scope.openOverlay(searchProgress) { isSearchOpen = true }
    }

    fun closeSearch() {
        scope.closeOverlay(searchProgress) { isSearchOpen = false }
    }

    fun openContacts() {
        chatsViewModel.clearSelection()
        scope.openOverlay(contactsProgress) { isContactsOpen = true }
    }

    fun closeContacts() {
        scope.closeOverlay(contactsProgress) { isContactsOpen = false }
    }

    fun openArchive() {
        chatsViewModel.clearSelection()
        scope.openOverlay(archiveProgress) { isArchiveOpen = true }
    }

    fun closeArchive() {
        scope.closeOverlay(archiveProgress) { isArchiveOpen = false }
    }

    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val showDetailPane = selectedSession != null && isExpanded
    val showDetailOverlay = selectedSession != null && !isExpanded

    BackHandler(
        enabled = selectedSession != null || isSearchOpen || isContactsOpen ||
            isArchiveOpen || isQrOpen
    ) {
        when {
            isQrOpen -> isQrOpen = false
            isSearchOpen -> closeSearch()
            isContactsOpen -> closeContacts()
            isArchiveOpen -> closeArchive()
            selectedSession != null -> closeSession()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .then(
                        if (showDetailPane) Modifier.weight(0.42f).fillMaxHeight()
                        else Modifier.fillMaxSize()
                    )
                    .padding(bottom = 80.dp)
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
                        ServicesScreen()
                    }

                    2 -> {
                        AppHeader()
                        SettingsScreen(
                            currentThemeId = scheme.id,
                            onThemeSelect = themeController::selectTheme
                        )
                    }

                    3 -> {
                        AppHeader()
                        ProfileScreen()
                    }
                }
            }

            // Two-pane на широких экранах: чат живёт в правой панели.
            if (showDetailPane) {
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .background(ObsidianBlack)
                        .statusBarsPadding()
                        .padding(bottom = 80.dp)
                ) {
                    selectedSession?.let { session ->
                        ChatDetailScreen(
                            session = session,
                            onBack = { closeSession() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Полноэкранный чат на компактных экранах.
        if (showDetailOverlay) {
            selectedSession?.let { session ->
                AnonGramTransitionScreen(
                    progress = detailProgress.value,
                    isExit = false,
                    config = navConfig
                ) {
                    ChatDetailScreen(
                        session = session,
                        onBack = { closeSession() }
                    )
                }
            }
        }

        if (isSearchOpen) {
            AnonGramTransitionScreen(
                progress = searchProgress.value,
                isExit = false,
                config = navConfig
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
                isExit = false,
                config = navConfig
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
                isExit = false,
                config = navConfig
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

        if (isQrOpen) {
            QrHubOverlay(
                onClose = { isQrOpen = false },
                onOpenContacts = {
                    isQrOpen = false
                    openContacts()
                }
            )
        }

        // Остров виден только на корневых экранах: любой дочерний экран
        // (чат, поиск, контакты, архив, QR) его скрывает.
        val isRootScreen = !showDetailOverlay && !isSearchOpen && !isContactsOpen &&
            !isArchiveOpen && !isQrOpen

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
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

/** Открытие оверлея: подложить экран под анимацию входа. */
private fun CoroutineScope.openOverlay(
    progress: Animatable<Float, *>,
    show: () -> Unit
) {
    launch {
        progress.snapTo(1f)
        show()
        progress.animateTo(
            targetValue = 0f,
            animationSpec = PhysicsSprings.screenSpringSpec()
        )
    }
}

/** Закрытие оверлея: анимация выхода, затем скрытие. */
private fun CoroutineScope.closeOverlay(
    progress: Animatable<Float, *>,
    hide: () -> Unit
) {
    launch {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = PhysicsSprings.screenSpringSpec()
        )
        hide()
        progress.snapTo(0f)
    }
}
