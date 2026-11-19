package com.anongram.app.features.settings.modules.ui.fonts

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.FontApplyScope
import com.anongram.app.core.theme.FontFilterCategory
import com.anongram.app.core.theme.FontModuleStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.anongram.app.features.settings.modules.ui.voice.formatMb
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@Composable
fun FontsModuleSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val modStrings = ModulesI18n.current
    val isRu = modStrings.isRussian
    val state by FontModuleStore.state.collectAsState()

    LaunchedEffect(Unit) {
        FontModuleStore.ensureInitialized(context)
        FontModuleStore.loadCatalog(context, forceRefresh = false)
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchRequested by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(FontFilterCategory.CYRILLIC) }
    var isOverflowOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 36
        }
    }
    val showSearchInHeader = isSearchRequested || isScrolled || searchQuery.isNotEmpty()

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            FontModuleStore.importFromUri(context, uri) { count ->
                val msg = if (isRu) {
                    if (count > 0) "Импортировано шрифтов: $count" else "Не найден .ttf / .otf"
                } else {
                    if (count > 0) "Imported fonts: $count" else "No .ttf / .otf found"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredCatalog = remember(state.catalog, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        state.catalog.filter { item ->
            val catOk = when (selectedCategory) {
                FontFilterCategory.ALL -> true
                FontFilterCategory.CYRILLIC -> item.hasCyrillic
                FontFilterCategory.SANS_SERIF -> item.category == "sans-serif"
                FontFilterCategory.SERIF -> item.category == "serif"
                FontFilterCategory.MONOSPACE -> item.category == "monospace"
                FontFilterCategory.DISPLAY -> item.category == "display"
                FontFilterCategory.HANDWRITING -> item.category == "handwriting"
            }
            val queryOk = q.isEmpty() || item.family.lowercase().contains(q) || item.id.lowercase().contains(q)
            catOk && queryOk
        }
    }

    val installedIds = remember(state.installedFonts) {
        state.installedFonts.map { it.id }.toSet()
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val headerBarHeight = 56.dp
    val headerTotalHeight = statusBarTop + headerBarHeight + 10.dp
    val hazeState = remember { HazeState() }
    val headerTintAlpha = if (scheme.isDark) 0.56f else 0.72f
    val headerBlurRadius = 22.dp

    AnonGramScreenSurface(modifier = modifier, applySystemBarsPadding = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background)
                    .hazeSource(state = hazeState),
                contentPadding = PaddingValues(
                    top = headerTotalHeight + 6.dp,
                    bottom = navBottom + 24.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Где применять шрифт
                item(key = "scope_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRu) "Где применять" else "Apply to",
                                color = scheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (state.activeFontId != null) {
                                Text(
                                    text = if (isRu) "Сбросить" else "Reset",
                                    color = scheme.accent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { FontModuleStore.selectFont(context, null) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val scopes = listOf(
                                FontApplyScope.ALL to (if (isRu) "Всё" else "All"),
                                FontApplyScope.MESSAGES_ONLY to (if (isRu) "Сообщения" else "Messages"),
                                FontApplyScope.UI_ONLY to (if (isRu) "Интерфейс" else "UI only")
                            )
                            scopes.forEach { (sc, label) ->
                                val active = state.applyScope == sc
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (active) scheme.accent else scheme.surface)
                                        .border(1.dp, if (active) scheme.accent else scheme.islandBorder, RoundedCornerShape(20.dp))
                                        .clickable { FontModuleStore.setApplyScope(context, sc) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) scheme.onAccent else scheme.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Установленные / импортированные шрифты
                if (state.installedFonts.isNotEmpty()) {
                    item(key = "installed_header") {
                        Text(
                            text = if (isRu) "Установленные (${state.installedFonts.size})" else "Installed (${state.installedFonts.size})",
                            color = scheme.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(state.installedFonts, key = { "inst_${it.id}" }) { font ->
                        val isActive = state.activeFontId == font.id
                        val previewFamily = remember(font.id, font.fileName) {
                            FontModuleStore.resolveFontFamily(context, font.id) ?: FontFamily.Default
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(scheme.card)
                                .border(
                                    width = if (isActive) 1.3.dp else 0.8.dp,
                                    color = if (isActive) scheme.accent else scheme.islandBorder,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { FontModuleStore.selectFont(context, font.id) }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = font.displayName,
                                    color = scheme.textPrimary,
                                    fontSize = 15.sp,
                                    fontFamily = previewFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isRu) {
                                        "Аа Бб Привет! Hello 123 · ${formatMb(font.sizeBytes)}"
                                    } else {
                                        "Aa Bb Hello World 123 · ${formatMb(font.sizeBytes)}"
                                    },
                                    color = scheme.textSecondary,
                                    fontSize = 12.5.sp,
                                    fontFamily = previewFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = scheme.textMuted,
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable { FontModuleStore.deleteFont(context, font) }
                            )
                        }
                    }
                }

                // 3. Категории каталога
                item(key = "categories") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(FontFilterCategory.entries) { cat ->
                            val active = selectedCategory == cat
                            val label = when (cat) {
                                FontFilterCategory.ALL -> if (isRu) "Все" else "All"
                                FontFilterCategory.CYRILLIC -> if (isRu) "Кириллица" else "Cyrillic"
                                FontFilterCategory.SANS_SERIF -> "Sans"
                                FontFilterCategory.SERIF -> "Serif"
                                FontFilterCategory.MONOSPACE -> "Mono"
                                FontFilterCategory.DISPLAY -> "Display"
                                FontFilterCategory.HANDWRITING -> if (isRu) "Рукописный" else "Handwriting"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (active) scheme.accent else scheme.card)
                                    .border(1.dp, if (active) scheme.accent else scheme.islandBorder, RoundedCornerShape(20.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (active) scheme.onAccent else scheme.textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 4. Заголовок внешнего каталога
                item(key = "catalog_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) {
                                "Каталог Fontsource (${filteredCatalog.size})"
                            } else {
                                "Fontsource Catalog (${filteredCatalog.size})"
                            },
                            color = scheme.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.isCatalogLoading) {
                            Text(
                                text = if (isRu) "Загрузка..." else "Loading...",
                                color = scheme.accent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (!state.catalogError.isNullOrBlank()) {
                    item(key = "catalog_error") {
                        Text(
                            text = state.catalogError.orEmpty(),
                            color = scheme.textMuted,
                            fontSize = 12.5.sp
                        )
                    }
                }

                // 5. Список шрифтов из внешнего каталога
                items(filteredCatalog.take(200), key = { "cat_${it.id}" }) { item ->
                    val isInstalled = item.id in installedIds
                    val isDownloading = state.downloadingFontId == item.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.card)
                            .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                            .clickable(enabled = !isDownloading) {
                                if (isInstalled) {
                                    FontModuleStore.selectFont(context, item.id)
                                } else {
                                    FontModuleStore.downloadFont(context, item)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.family,
                                color = scheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = buildString {
                                    append(item.category)
                                    if (item.hasCyrillic) append(if (isRu) " · Кириллица" else " · Cyrillic")
                                },
                                color = scheme.textSecondary,
                                fontSize = 11.5.sp
                            )
                            if (isDownloading) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { state.downloadProgress },
                                    color = scheme.accent,
                                    trackColor = scheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        if (isInstalled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Бесшовная шапка с HazeProgressive блюром + трансформация заголовка в Поиск + единый бабл (Поиск + ⋮)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(headerTotalHeight)
                    .zIndex(2f)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = scheme.background,
                                tint = HazeTint(scheme.background.copy(alpha = headerTintAlpha)),
                                blurRadius = headerBlurRadius,
                                noiseFactor = 0f
                            )
                        ) {
                            progressive = HazeProgressive.verticalGradient(
                                startIntensity = 1f,
                                endIntensity = 0f
                            )
                        }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarTop + 6.dp, start = 12.dp, end = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .settingsGlassPill(hazeState = hazeState)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = modStrings.back,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        AnimatedContent(
                            targetState = showSearchInHeader,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "fontsHeaderSearch"
                        ) { inSearch ->
                            if (inSearch) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .settingsGlassPill(hazeState = hazeState)
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                                        cursorBrush = SolidColor(scheme.accent),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { inner ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = if (isRu) "Поиск шрифтов..." else "Search fonts...",
                                                    color = scheme.textMuted,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            inner()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty() || isSearchRequested) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = null,
                                            tint = scheme.textMuted,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable {
                                                    searchQuery = ""
                                                    isSearchRequested = false
                                                }
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = if (isRu) "Шрифты" else "Fonts",
                                    color = scheme.textPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Единый бабл-капсула: Поиск + Три точки (импорт / сброс / обновление)
                    Box {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .settingsGlassPill(hazeState = hazeState)
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (isSearchRequested) {
                                            searchQuery = ""
                                            isSearchRequested = false
                                        } else {
                                            isSearchRequested = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = if (isRu) "Поиск" else "Search",
                                    tint = if (showSearchInHeader) scheme.accent else scheme.textPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { isOverflowOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    tint = scheme.textPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isOverflowOpen,
                            onDismissRequest = { isOverflowOpen = false },
                            containerColor = scheme.card
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isRu) "Импорт с телефона (.ttf/.otf/.zip)" else "Import from storage",
                                        color = scheme.textPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = scheme.accent)
                                },
                                onClick = {
                                    isOverflowOpen = false
                                    importPicker.launch(arrayOf("*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isRu) "Обновить каталог" else "Refresh catalog",
                                        color = scheme.textPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = scheme.textSecondary)
                                },
                                onClick = {
                                    isOverflowOpen = false
                                    FontModuleStore.loadCatalog(context, forceRefresh = true)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isRu) "Сбросить на системный" else "Reset to system font",
                                        color = scheme.textPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = scheme.textSecondary)
                                },
                                onClick = {
                                    isOverflowOpen = false
                                    FontModuleStore.selectFont(context, null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
