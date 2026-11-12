package com.anongram.app.features.chats.attach.tabs.location

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.attach.components.AttachHeaderScrim
import com.anongram.app.features.chats.attach.components.AttachTopBar
import com.anongram.app.features.chats.attach.i18n.AttachI18nStrings

@Composable
internal fun BoxScope.LocationFloatingTopBar(
    attachStrings: AttachI18nStrings,
    searchQuery: String,
    isSearchActive: Boolean,
    topBarAlpha: Float,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit
) {
    AttachHeaderScrim(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .zIndex(9f)
            .graphicsLayer { alpha = topBarAlpha }
    )

    AttachTopBar(
        title = AttachTab.LOCATION.label(attachStrings),
        searchQuery = searchQuery,
        isSearchActive = isSearchActive,
        searchPlaceholder = attachStrings.locationSearchPlaceholder,
        onBack = onBack,
        onSearchQueryChange = onSearchQueryChange,
        onToggleSearch = onToggleSearch,
        hasActionMenu = false,
        hasSearchToggle = true,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .safeStatusBarPadding()
            .zIndex(10f)
            .graphicsLayer {
                alpha = topBarAlpha
                translationY = -8.dp.toPx() * (1f - topBarAlpha)
            }
    )
}
