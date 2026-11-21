package com.anongram.app.features.chats.user_profile.api

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
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
    onAddContact: (() -> Unit)? = null,
    onDeleteChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val strings = remember(lang) { UserProfileI18n.forLanguage(lang) }

    var isExpandedHero by remember { mutableStateOf(false) }
    var isOverflowOpen by remember { mutableStateOf(false) }
    var isCustomAutoDeleteOpen by remember { mutableStateOf(false) }
    var isQrOverlayOpen by remember { mutableStateOf(false) }
    var isQrScannerOpen by remember { mutableStateOf(false) }
    var addedToContactsLocal by remember(session.id) { mutableStateOf(isContact) }

    val buckets = remember(messages) { extractProfileBuckets(messages) }
    val availableTabs = remember(buckets) { buckets.availableTabs() }
    var selectedTab by remember(availableTabs) {
        mutableStateOf(availableTabs.firstOrNull() ?: UserProfileTab.MEDIA)
    }
    if (availableTabs.isNotEmpty() && selectedTab !in availableTabs) {
        selectedTab = availableTabs.first()
    }

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
            isQrScannerOpen -> isQrScannerOpen = false
            isQrOverlayOpen -> isQrOverlayOpen = false
            isCustomAutoDeleteOpen -> isCustomAutoDeleteOpen = false
            else -> onBack()
        }
    }

    AnonGramScreenSurface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                item(key = "hero_header") {
                    UserProfileHeroHeader(
                        session = session,
                        isExpandedHero = isExpandedHero,
                        strings = strings,
                        onToggleExpand = { isExpandedHero = !isExpandedHero }
                    )
                }

                item(key = "actions_row") {
                    UserProfileActionsRow(
                        isMuted = session.isMuted,
                        strings = strings,
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
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    stickyHeader(key = "sticky_tabs") {
                        UserProfileTabBar(
                            tabs = availableTabs,
                            selectedTab = selectedTab,
                            strings = strings,
                            onSelectTab = { selectedTab = it }
                        )
                    }

                    when (selectedTab) {
                        UserProfileTab.MEDIA -> {
                            val chunked = buckets.media.chunked(3)
                            items(chunked) { rowItems ->
                                UserProfileMediaGridRow(
                                    rowItems = rowItems,
                                    onMediaClick = onOpenMediaMessage
                                )
                            }
                        }

                        UserProfileTab.GIF -> {
                            val chunked = buckets.gifs.chunked(3)
                            items(chunked) { rowItems ->
                                UserProfileMediaGridRow(
                                    rowItems = rowItems,
                                    onMediaClick = onOpenMediaMessage
                                )
                            }
                        }

                        UserProfileTab.FILES -> {
                            items(buckets.files) { fileItem ->
                                UserProfileFileRow(item = fileItem)
                            }
                        }

                        UserProfileTab.LINKS -> {
                            items(buckets.links) { linkItem ->
                                UserProfileLinkRow(item = linkItem)
                            }
                        }

                        UserProfileTab.AUDIO -> {
                            items(buckets.audio) { audioItem ->
                                UserProfileAudioRow(
                                    item = audioItem,
                                    onAudioClick = { onBack() }
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            UserProfileTopOverlayBar(
                session = session,
                isCollapsed = isCollapsed,
                collapsedSubtitle = if (availableTabs.isNotEmpty()) {
                    tabSummaryText(selectedTab, buckets, strings)
                } else {
                    ""
                },
                strings = strings,
                onBack = onBack,
                onOpenOverflow = { isOverflowOpen = true },
                overflowMenu = {
                    UserProfileOverflowMenu(
                        expanded = isOverflowOpen,
                        selfDestruct = selfDestruct,
                        isContact = addedToContactsLocal,
                        strings = strings,
                        onDismiss = { isOverflowOpen = false },
                        onSetSelfDestruct = onSetSelfDestruct,
                        onOpenCustomAutoDelete = { isCustomAutoDeleteOpen = true },
                        onShareContact = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(session.title, "${session.title} (${session.fingerprint})"))
                            Toast.makeText(context, strings.shareContactToast, Toast.LENGTH_SHORT).show()
                        },
                        onBlockUser = {
                            Toast.makeText(context, strings.blockedToast, Toast.LENGTH_SHORT).show()
                        },
                        onEditContact = {
                            Toast.makeText(context, session.title, Toast.LENGTH_SHORT).show()
                        },
                        onDeleteContact = {
                            addedToContactsLocal = false
                            onDeleteChat?.invoke()
                        },
                        onOpenKeyVerification = onOpenKeyVerification,
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
                        gifs.add(UserProfileMediaItem(msg, uri, isVideo = false))
                    } else {
                        media.add(UserProfileMediaItem(msg, uri, isVideo = att.isVideo))
                    }
                }
            }

            is MessageAttachment.MediaAlbum -> {
                att.items.forEach { item ->
                    if (item.uri.isNotBlank()) {
                        media.add(UserProfileMediaItem(msg, item.uri, isVideo = item.isVideo))
                    }
                }
            }

            is MessageAttachment.File -> {
                val isGif = att.name.endsWith(".gif", ignoreCase = true) && !att.uri.isNullOrBlank()
                if (isGif) {
                    gifs.add(UserProfileMediaItem(msg, att.uri!!, isVideo = false))
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
