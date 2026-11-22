package com.anongram.app.features.chats.user_profile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

@Composable
internal fun UserProfileTopOverlayBar(
    session: IChatSession,
    hazeState: HazeState,
    isCollapsed: Boolean,
    collapsedSubtitle: String,
    selectionCount: Int,
    strings: UserProfileStrings,
    onBack: () -> Unit,
    onClearSelection: () -> Unit,
    onShowInChat: () -> Unit,
    onForwardSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    overflowMenu: @Composable () -> Unit,
    onOpenOverflow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val collapseAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isCollapsed || selectionCount > 0) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "profileHeaderCollapse"
    )

    val bgModifier = if (collapseAlpha > 0.05f) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = scheme.background.copy(alpha = 0.35f * collapseAlpha),
                tint = HazeTint(color = scheme.surface.copy(alpha = 0.55f * collapseAlpha)),
                blurRadius = 22.dp,
                noiseFactor = 0.02f
            )
        ) {
            progressive = HazeProgressive.verticalGradient(
                startIntensity = 1f,
                endIntensity = 0f
            )
        }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(bgModifier)
            .safeStatusBarPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (selectionCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircleHeaderButton(
                    hazeState = hazeState,
                    onClick = onClearSelection,
                    content = {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = strings.back,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                )
                Text(
                    text = selectionCount.toString(),
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = scheme.card.copy(alpha = 0.52f),
                            tint = HazeTint(color = scheme.surface.copy(alpha = 0.48f)),
                            blurRadius = 20.dp,
                            noiseFactor = 0.02f
                        )
                    )
                    .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectionCount == 1) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onShowInChat),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = strings.showInChat,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onForwardSelected),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Redo,
                        contentDescription = strings.forward,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDeleteSelected),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = strings.delete,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            CircleHeaderButton(
                hazeState = hazeState,
                onClick = onBack,
                content = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (collapseAlpha > 0.02f) {
                    Text(
                        text = session.title,
                        color = scheme.textPrimary.copy(alpha = collapseAlpha),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (collapsedSubtitle.isNotBlank()) {
                        Text(
                            text = collapsedSubtitle,
                            color = scheme.textSecondary.copy(alpha = collapseAlpha),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box {
                CircleHeaderButton(
                    hazeState = hazeState,
                    onClick = onOpenOverflow,
                    content = {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = strings.moreOptions,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                )
                overflowMenu()
            }
        }
    }
}

@Composable
internal fun UserProfileHeroHeader(
    session: IChatSession,
    isExpandedHero: Boolean,
    strings: UserProfileStrings,
    onToggleExpand: () -> Unit,
    overlayActions: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val initials = (session.avatarMonogram ?: session.title.take(2)).uppercase()
    val statusText = if (session.isOnline) strings.online else strings.lastSeenRecently
    val avatarResIds = session.avatarResIds
    val primaryAvatar = avatarResIds.firstOrNull() ?: session.avatarResId
    val canExpandHero = primaryAvatar != null
    val effectiveExpanded = canExpandHero && isExpandedHero

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (effectiveExpanded && primaryAvatar != null) {
            val heroHeight = if (overlayActions != null) 420.dp else 340.dp
            val heroHaze = remember { HazeState() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clickable(onClick = onToggleExpand)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = primaryAvatar),
                    contentDescription = session.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = heroHaze)
                )

                if (avatarResIds.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .safeStatusBarPadding()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        avatarResIds.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (idx == 0) Color.White else Color.White.copy(alpha = 0.35f)
                                    )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .hazeEffect(
                            state = heroHaze,
                            style = HazeStyle(
                                backgroundColor = scheme.background.copy(alpha = 0.25f),
                                tint = HazeTint(color = scheme.background.copy(alpha = 0.55f)),
                                blurRadius = 24.dp,
                                noiseFactor = 0.02f
                            )
                        ) {
                            progressive = HazeProgressive.verticalGradient(
                                startIntensity = 0f,
                                endIntensity = 1f
                            )
                        }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    scheme.background.copy(alpha = 0.65f),
                                    scheme.background
                                )
                            )
                        )
                        .padding(top = 36.dp, bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                        Text(
                            text = session.title,
                            color = scheme.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = if (session.isOnline) scheme.accent else scheme.textSecondary,
                                fontSize = 13.5.sp
                            )
                            if (!session.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scheme.card.copy(alpha = 0.72f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = strings.whenQuestion,
                                        color = scheme.textSecondary,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                    if (overlayActions != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        overlayActions()
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.safeStatusBarPadding())
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(scheme.accent, scheme.accent.copy(alpha = 0.72f))
                        )
                    )
                    .then(
                        if (canExpandHero) Modifier.clickable(onClick = onToggleExpand)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (primaryAvatar != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = primaryAvatar),
                        contentDescription = session.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(92.dp)
                    )
                } else {
                    Text(
                        text = initials,
                        color = scheme.onAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = session.title,
                color = scheme.textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = statusText,
                    color = if (session.isOnline) scheme.accent else scheme.textSecondary,
                    fontSize = 13.sp
                )
                if (!session.isOnline) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.card)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.whenQuestion,
                            color = scheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CircleHeaderButton(
    hazeState: HazeState,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = scheme.card.copy(alpha = 0.45f),
                    tint = HazeTint(color = scheme.surface.copy(alpha = 0.45f)),
                    blurRadius = 18.dp,
                    noiseFactor = 0.02f
                )
            )
            .border(0.6.dp, scheme.islandBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun UserProfileAvatarFullscreenOverlay(
    avatarResIds: List<Int>,
    title: String,
    strings: UserProfileStrings,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val safeList = if (avatarResIds.isNotEmpty()) avatarResIds else return
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { safeList.size })
    var isMenuOpen by remember { mutableStateOf(false) }
    var isHudVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIdx ->
            var scale by remember(pageIdx) { mutableFloatStateOf(1f) }
            var offsetX by remember(pageIdx) { mutableFloatStateOf(0f) }
            var offsetY by remember(pageIdx) { mutableFloatStateOf(0f) }
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                val next = (scale * zoomChange).coerceIn(1f, 4f)
                scale = next
                if (next > 1.02f) {
                    offsetX = (offsetX + panChange.x).coerceIn(-600f * next, 600f * next)
                    offsetY = (offsetY + panChange.y).coerceIn(-600f * next, 600f * next)
                } else {
                    offsetX = 0f
                    offsetY = 0f
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { isHudVisible = !isHudVisible }
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = safeList[pageIdx]),
                    contentDescription = title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformable(
                            state = transformState,
                            canPan = { scale > 1.02f }
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                )
            }
        }

        // Верхняя панель: простая стрелка назад (без кружка), имя, 3 точки (Сохранить / Поделиться с иконками)
        androidx.compose.animation.AnimatedVisibility(
            visible = isHudVisible,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
                    .safeStatusBarPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (safeList.size > 1) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${safeList.size}",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )
                    }
                }
                Box {
                    androidx.compose.material3.IconButton(onClick = { isMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = strings.moreOptions,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    val scheme = LocalAnonGramScheme.current
                    androidx.compose.material3.DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        modifier = Modifier
                            .background(scheme.card, RoundedCornerShape(16.dp))
                            .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = scheme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            text = { Text(strings.saveToGallery, color = scheme.textPrimary, fontSize = 14.sp) },
                            onClick = {
                                isMenuOpen = false
                                val resId = safeList[pagerState.currentPage]
                                val ok = saveDrawableToGallery(context, resId, "${title}_${pagerState.currentPage + 1}")
                                android.widget.Toast.makeText(
                                    context,
                                    if (ok) strings.savedToGalleryToast else "Error",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Share,
                                    contentDescription = null,
                                    tint = scheme.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            text = { Text(strings.sharePhoto, color = scheme.textPrimary, fontSize = 14.sp) },
                            onClick = {
                                isMenuOpen = false
                                val resId = safeList[pagerState.currentPage]
                                shareDrawableSystem(context, resId, "${title}_${pagerState.currentPage + 1}")
                            }
                        )
                    }
                }
            }
        }

        // Нижняя лента миниатюр альбома аватаров
        androidx.compose.animation.AnimatedVisibility(
            visible = isHudVisible && safeList.size > 1,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(vertical = 14.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                items(safeList.size) { idx ->
                    val isCurrent = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isCurrent) 52.dp else 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isCurrent) 2.dp else 0.7.dp,
                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(idx) }
                            }
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = safeList[idx]),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private fun saveDrawableToGallery(context: android.content.Context, resId: Int, name: String): Boolean {
    return runCatching {
        val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId) ?: return false
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "AnonGram_${name}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AnonGram")
            }
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
        }
        true
    }.getOrDefault(false)
}

private fun shareDrawableSystem(context: android.content.Context, resId: Int, name: String) {
    runCatching {
        val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId) ?: return
        val file = java.io.File(context.cacheDir, "share_avatar_${name}.jpg")
        java.io.FileOutputStream(file).use { out ->
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, name)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
