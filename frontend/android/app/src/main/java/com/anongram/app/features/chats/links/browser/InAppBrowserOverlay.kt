package com.anongram.app.features.chats.links.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppBrowserOverlay(
    state: InAppBrowserState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = InAppBrowserI18n.current

    var menuExpanded by remember { mutableStateOf(false) }
    var isFindOpen by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var isAddressEditing by remember { mutableStateOf(false) }
    var addressInput by remember { mutableStateOf("") }
    val addressFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isAddressEditing) {
        if (isAddressEditing) {
            addressInput = state.activeUrl.orEmpty()
            runCatching { addressFocusRequester.requestFocus() }
        }
    }

    val activeUrl = state.activeUrl ?: return

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val target = request?.url?.toString() ?: return false
                    if (!target.startsWith("http://") && !target.startsWith("https://")) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        }
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    InAppBrowserStore.updatePageProgress(url, view?.title, 15)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    InAppBrowserStore.onPageFinished(context, url, view?.title)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    InAppBrowserStore.updatePageProgress(view?.url, view?.title, newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    InAppBrowserStore.updatePageProgress(view?.url, title, state.progress)
                }
            }
        }
    }

    var lastLoadedUrl by remember { mutableStateOf("") }
    LaunchedEffect(activeUrl) {
        if (activeUrl != lastLoadedUrl) {
            lastLoadedUrl = activeUrl
            webView.loadUrl(activeUrl)
        }
    }

    LaunchedEffect(state.reloadNonce) {
        if (state.reloadNonce > 0L) {
            webView.reload()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    val isBookmarked = state.bookmarks.any { it.url == activeUrl }

    BackHandler(enabled = !state.isMinimized && !state.isSettingsOpen && !state.isHistoryOpen && !state.isBookmarksOpen) {
        if (isAddressEditing) {
            isAddressEditing = false
        } else if (isFindOpen) {
            isFindOpen = false
            findQuery = ""
            webView.clearMatches()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            InAppBrowserStore.minimize()
        }
    }

    // Держим WebView живым даже когда свёрнут, просто скрываем его визуально
    if (!state.isMinimized) {
        AnonGramScreenSurface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Верхняя панель: ✕ (или ← при вводе) | Заголовок + URL / поиск | ⌄ | ⋮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(scheme.background)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (isAddressEditing) {
                                    isAddressEditing = false
                                } else {
                                    InAppBrowserStore.close()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAddressEditing) Icons.AutoMirrored.Filled.ArrowBack else Icons.Outlined.Close,
                            contentDescription = strings.closeBrowser,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (isAddressEditing) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                singleLine = true,
                                textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(scheme.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        InAppBrowserStore.navigateFromBar(context, addressInput)
                                        isAddressEditing = false
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(addressFocusRequester),
                                decorationBox = { innerTextField ->
                                    if (addressInput.isEmpty()) {
                                        Text(
                                            text = strings.addressSearchPlaceholder,
                                            color = scheme.textMuted,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (addressInput.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { addressInput = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = strings.closeBrowser,
                                        tint = scheme.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isAddressEditing = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = state.pageTitle.ifBlank { InAppBrowserStore.extractHost(activeUrl) },
                                color = scheme.textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeUrl.removePrefix("https://").removePrefix("http://"),
                                color = scheme.textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Стрелка вниз — свернуть страницу в нижний бар, не закрывая
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { InAppBrowserStore.minimize() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = strings.minimizeBrowser,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Меню ⋮
                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { menuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = null,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .background(scheme.card)
                                .width(220.dp)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                                },
                                text = { Text(strings.openInDefaultBrowser, color = scheme.textPrimary, fontSize = 14.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    InAppBrowserStore.openInDefaultBrowser(context, activeUrl)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Outlined.Refresh, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                                },
                                text = { Text(strings.refresh, color = scheme.textPrimary, fontSize = 14.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    InAppBrowserStore.triggerReload()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Outlined.Search, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                                },
                                text = { Text(strings.findInPage, color = scheme.textPrimary, fontSize = 14.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    isFindOpen = true
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                        null,
                                        tint = if (isBookmarked) scheme.accent else scheme.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        if (isBookmarked) strings.removeBookmark else strings.addBookmark,
                                        color = scheme.textPrimary,
                                        fontSize = 14.5.sp
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    InAppBrowserStore.toggleCurrentBookmark(context)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                                },
                                text = { Text(strings.share, color = scheme.textPrimary, fontSize = 14.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    runCatching {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, activeUrl)
                                        }
                                        context.startActivity(Intent.createChooser(share, strings.share))
                                    }
                                }
                            )
                            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Outlined.History, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                                },
                                text = { Text(strings.history, color = scheme.textPrimary, fontSize = 14.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    InAppBrowserStore.openHistory(context)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Outlined.Bookmarks, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                                },
                                text = { Text(strings.bookmarks, color = scheme.textPrimary, fontSize = 14.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    InAppBrowserStore.openBookmarks(context)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                                },
                                text = { Text(strings.settings, color = scheme.textPrimary, fontSize = 14.5.sp) },
                                onClick = {
                                    menuExpanded = false
                                    InAppBrowserStore.openSettings(context)
                                }
                            )
                        }
                    }
                }

                if (state.isLoading && state.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        color = scheme.accent,
                        trackColor = scheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }

                if (isFindOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(scheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = findQuery,
                            onValueChange = { q ->
                                findQuery = q
                                if (q.isNotBlank()) webView.findAllAsync(q) else webView.clearMatches()
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            decorationBox = { inner ->
                                if (findQuery.isEmpty()) {
                                    Text(strings.findPlaceholder, color = scheme.textMuted, fontSize = 14.sp)
                                }
                                inner()
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = null,
                            tint = scheme.textPrimary,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { webView.findNext(false) }
                                .padding(4.dp)
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = scheme.textPrimary,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { webView.findNext(true) }
                                .padding(4.dp)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = scheme.textSecondary,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isFindOpen = false
                                    findQuery = ""
                                    webView.clearMatches()
                                }
                                .padding(4.dp)
                        )
                    }
                }

                AndroidView(
                    factory = { webView },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun InAppBrowserMinimizedBar(
    state: InAppBrowserState,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = InAppBrowserI18n.current
    val activeUrl = state.activeUrl ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.background)
            .safeNavigationBarPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.islandBg)
                .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .clickable(onClick = onExpand)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = strings.closeBrowser,
                    tint = scheme.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = state.pageTitle.ifBlank { InAppBrowserStore.extractHost(activeUrl) },
                color = scheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = strings.expandBrowser,
                tint = scheme.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun BrowserEntriesSheet(
    title: String,
    emptyText: String,
    entries: List<BrowserPageEntry>,
    onSelect: (String) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onClearAll: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    BackHandler(onBack = onBack)

    AnonGramScreenSurface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, null, tint = scheme.textPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onClearAll != null && entries.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClearAll),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emptyText, color = scheme.textSecondary, fontSize = 14.5.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { "${it.url}_${it.timestampMs}" }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(scheme.card)
                                .clickable { onSelect(item.url) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title.ifBlank { item.url },
                                    color = scheme.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.url,
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (onDelete != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    tint = scheme.textMuted,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onDelete(item.url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
