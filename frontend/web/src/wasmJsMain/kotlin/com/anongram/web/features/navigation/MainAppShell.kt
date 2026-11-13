package com.anongram.web.features.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.navigation.NavigationTransitionConfig
import com.anongram.web.core.navigation.AnonGramTransitionScreen
import com.anongram.web.core.navigation.PhysicsSprings
import com.anongram.web.core.navigation.TransitionType
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.theme.ThemeController
import com.anongram.web.core.ui.AnonGramScreenSurface
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.chats.archive.ArchiveScreen
import com.anongram.web.features.chats.common.IChatSession
import com.anongram.web.features.chats.data.InMemoryChatListRepository
import com.anongram.web.features.chats.mainmenu.ChatsMainMenuScreen
import com.anongram.web.features.chats.messages.data.InMemoryChatMessagesRepository
import com.anongram.web.features.chats.messages.state.ChatMessagesViewModel
import com.anongram.web.features.chats.search.ChatSearchScreen
import com.anongram.web.features.chats.search.ChatSearchViewModel
import com.anongram.web.features.chats.state.ChatListViewModel
import com.anongram.web.features.chats.ui.ChatScreen
import com.anongram.web.features.contacts.common.Contact
import com.anongram.web.features.contacts.data.InMemoryContactRepository
import com.anongram.web.features.contacts.state.ContactsViewModel
import com.anongram.web.features.contacts.ui.ContactsScreen
import com.anongram.web.features.navigation.components.AppHeader
import com.anongram.web.features.profile.ui.ProfileScreen
import com.anongram.web.features.qr.ui.QrHubOverlay
import com.anongram.web.features.services.ui.ServicesScreen
import com.anongram.web.features.settings.language.ui.LanguageSettingsScreen
import com.anongram.web.features.settings.ui.SettingsScreen
import com.anongram.web.platform.PlatformBackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private fun CoroutineScope.openOverlay(
    anim: Animatable<Float, *>,
    show: () -> Unit
) {
    val cfg = NavigationTransitionConfig.current()
    show()
    launch {
        if (cfg.transitionType == TransitionType.INSTANT) {
            anim.snapTo(0f)
        } else {
            anim.snapTo(1f)
            anim.animateTo(0f, PhysicsSprings.screenSpringSpec())
        }
    }
}

private fun CoroutineScope.closeOverlay(
    anim: Animatable<Float, *>,
    hide: () -> Unit
) {
    val cfg = NavigationTransitionConfig.current()
    launch {
        if (cfg.transitionType != TransitionType.INSTANT) {
            anim.animateTo(1f, PhysicsSprings.screenSpringSpec())
        }
        hide()
    }
}

@Composable
fun MainAppShell(
    themeController: ThemeController,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme by themeController.scheme.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isQrOpen by remember { mutableStateOf(false) }
    var selectedSession by remember { mutableStateOf<IChatSession?>(null) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isContactsOpen by remember { mutableStateOf(false) }
    var isArchiveOpen by remember { mutableStateOf(false) }
    var isLanguageOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val messagesRepository = remember { InMemoryChatMessagesRepository() }
    val chatsRepository = remember(messagesRepository) { InMemoryChatListRepository(messagesRepository) }
    val contactsRepository = remember { InMemoryContactRepository() }

    val chatsViewModel = remember(chatsRepository) { ChatListViewModel(chatsRepository) }
    val searchViewModel = remember(chatsRepository, contactsRepository) {
        ChatSearchViewModel(chatsRepository, contactsRepository)
    }
    val contactsViewModel = remember(contactsRepository) { ContactsViewModel(contactsRepository) }

    val detailProgress = remember { Animatable(0f) }
    val searchProgress = remember { Animatable(0f) }
    val contactsProgress = remember { Animatable(0f) }
    val archiveProgress = remember { Animatable(0f) }
    val languageProgress = remember { Animatable(0f) }

    fun openSession(session: IChatSession) {
        chatsViewModel.clearSelection()
        chatsViewModel.markRead(session.id)
        scope.openOverlay(detailProgress) { selectedSession = session }
    }

    fun closeSession() {
        chatsViewModel.refresh()
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

    fun openLanguage() {
        scope.openOverlay(languageProgress) { isLanguageOpen = true }
    }

    fun closeLanguage() {
        scope.closeOverlay(languageProgress) { isLanguageOpen = false }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        val showDetailOverlay = !isWide && selectedSession != null
        val isSubScreenOpen = showDetailOverlay || isSearchOpen || isContactsOpen || isArchiveOpen || isLanguageOpen

        PlatformBackHandler(enabled = selectedSession != null && !isWide) { closeSession() }
        PlatformBackHandler(enabled = isSearchOpen) { closeSearch() }
        PlatformBackHandler(enabled = isContactsOpen) { closeContacts() }
        PlatformBackHandler(enabled = isArchiveOpen) { closeArchive() }
        PlatformBackHandler(enabled = isLanguageOpen) { closeLanguage() }
        PlatformBackHandler(enabled = isQrOpen) { isQrOpen = false }

        AnonGramScreenSurface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (selectedTab != 0) {
                                    AppHeader(modifier = Modifier.safeStatusBarPadding())
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    when (selectedTab) {
                                        0 -> ChatsMainMenuScreen(
                                            viewModel = chatsViewModel,
                                            onChatOpen = { openSession(it) },
                                            onOpenSearch = { openSearch() },
                                            onOpenArchive = { openArchive() },
                                            onOpenContacts = { openContacts() }
                                        )
                                        1 -> ServicesScreen()
                                        2 -> SettingsScreen(
                                            currentThemeId = scheme.id,
                                            onThemeSelect = { themeController.selectTheme(it) },
                                            onOpenLanguage = { openLanguage() },
                                            onLogout = onLogout
                                        )
                                        3 -> ProfileScreen(onLogout = onLogout)
                                    }
                                }
                            }
                            if (!isSubScreenOpen) {
                                MainFloatingIsland(
                                    selectedTab = selectedTab,
                                    onSelectTab = {
                                        if (it != 0) chatsViewModel.clearSelection()
                                        selectedTab = it
                                    },
                                    onOpenStealthQr = { isQrOpen = true },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(LocalAnonGramScheme.current.divider)
                        )

                        Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                            val active = selectedSession
                            if (active != null) {
                                val chatVm = remember(active.id) {
                                    ChatMessagesViewModel(active.id, messagesRepository, chatsRepository)
                                }
                                ChatScreen(
                                    session = active,
                                    viewModel = chatVm,
                                    onBack = { closeSession() },
                                    showBackButton = false
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = LocalAnonGramScheme.current.accent,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "AnonGram End-to-End Encrypted",
                                        color = LocalAnonGramScheme.current.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Выберите чат слева, чтобы начать переписку",
                                        color = LocalAnonGramScheme.current.textSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (selectedTab != 0) {
                            AppHeader(modifier = Modifier.safeStatusBarPadding())
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> ChatsMainMenuScreen(
                                    viewModel = chatsViewModel,
                                    onChatOpen = { openSession(it) },
                                    onOpenSearch = { openSearch() },
                                    onOpenArchive = { openArchive() },
                                    onOpenContacts = { openContacts() }
                                )
                                1 -> ServicesScreen()
                                2 -> SettingsScreen(
                                    currentThemeId = scheme.id,
                                    onThemeSelect = { themeController.selectTheme(it) },
                                    onOpenLanguage = { openLanguage() },
                                    onLogout = onLogout
                                )
                                3 -> ProfileScreen(onLogout = onLogout)
                            }
                        }
                    }

                    if (!isSubScreenOpen) {
                        MainFloatingIsland(
                            selectedTab = selectedTab,
                            onSelectTab = {
                                if (it != 0) chatsViewModel.clearSelection()
                                selectedTab = it
                            },
                            onOpenStealthQr = { isQrOpen = true },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                if (isArchiveOpen) {
                    AnonGramTransitionScreen(
                        progress = archiveProgress.value,
                        isExit = true,
                        config = NavigationTransitionConfig.current()
                    ) {
                        ArchiveScreen(
                            viewModel = chatsViewModel,
                            onBack = { closeArchive() },
                            onChatOpen = { openSession(it) }
                        )
                    }
                }

                if (isContactsOpen) {
                    AnonGramTransitionScreen(
                        progress = contactsProgress.value,
                        isExit = true,
                        config = NavigationTransitionConfig.current()
                    ) {
                        ContactsScreen(
                            viewModel = contactsViewModel,
                            onBack = { closeContacts() },
                            onContactClick = { openContact(it) },
                            onOpenQr = { isQrOpen = true },
                            onOpenSearch = { openSearch() }
                        )
                    }
                }

                if (isSearchOpen) {
                    AnonGramTransitionScreen(
                        progress = searchProgress.value,
                        isExit = true,
                        config = NavigationTransitionConfig.current()
                    ) {
                        ChatSearchScreen(
                            viewModel = searchViewModel,
                            onBack = { closeSearch() },
                            onSessionOpen = { session ->
                                isSearchOpen = false
                                openSession(session)
                            },
                            onContactOpen = { openContact(it) }
                        )
                    }
                }

                if (isLanguageOpen) {
                    AnonGramTransitionScreen(
                        progress = languageProgress.value,
                        isExit = true,
                        config = NavigationTransitionConfig.current()
                    ) {
                        LanguageSettingsScreen(onBack = { closeLanguage() })
                    }
                }

                if (showDetailOverlay) {
                    selectedSession?.let { active ->
                        val chatViewModel = remember(active.id) {
                            ChatMessagesViewModel(active.id, messagesRepository, chatsRepository)
                        }
                        AnonGramTransitionScreen(
                            progress = detailProgress.value,
                            isExit = true,
                            config = NavigationTransitionConfig.current()
                        ) {
                            ChatScreen(
                                session = active,
                                viewModel = chatViewModel,
                                onBack = { closeSession() }
                            )
                        }
                    }
                }

                if (isQrOpen) {
                    QrHubOverlay(
                        onClose = { isQrOpen = false },
                        onOpenContacts = { openContacts() }
                    )
                }
            }
        }
    }
}
