package com.anongram.app.features.chats.links.browser

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface

@Composable
fun InAppBrowserSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = InAppBrowserI18n.current
    val state by InAppBrowserStore.state.collectAsState()
    val useInAppBrowser by ChatTextSettingsStore.inAppBrowserEnabled.collectAsState()

    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var newSiteInput by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    AnonGramScreenSurface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = strings.browserSettingsTitle,
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Использовать встроенный браузер
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.accent.copy(alpha = 0.22f))
                        .clickable {
                            ChatTextSettingsStore.setInAppBrowserEnabled(context, !useInAppBrowser)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.useInAppBrowser,
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = useInAppBrowser,
                        onCheckedChange = { ChatTextSettingsStore.setInAppBrowserEnabled(context, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = scheme.onAccent,
                            checkedTrackColor = scheme.accent
                        )
                    )
                }
                Text(
                    text = strings.useInAppBrowserDesc,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Cookie и кэш
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                ) {
                    BrowserSettingsRow(
                        icon = Icons.Outlined.CleaningServices,
                        title = strings.deleteCookies,
                        trailing = formatBytes(state.cookieSizeBytes),
                        onClick = {
                            InAppBrowserStore.clearCookies(context)
                            Toast.makeText(context, strings.cookiesClearedToast, Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))
                    BrowserSettingsRow(
                        icon = Icons.Outlined.DeleteOutline,
                        title = strings.clearCache,
                        trailing = formatBytes(state.cacheSizeBytes),
                        onClick = {
                            InAppBrowserStore.clearCache(context)
                            Toast.makeText(context, strings.cacheClearedToast, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                Text(
                    text = strings.cookiesCacheDesc,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // История
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                ) {
                    BrowserSettingsRow(
                        icon = Icons.Outlined.History,
                        title = strings.viewHistory,
                        trailing = null,
                        onClick = { InAppBrowserStore.openHistory(context) }
                    )
                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 52.dp))
                    BrowserSettingsRow(
                        icon = Icons.Outlined.DeleteOutline,
                        title = strings.clearHistory,
                        trailing = strings.pagesCount(state.history.size),
                        onClick = { InAppBrowserStore.clearHistory(context) }
                    )
                }

                // Не открывать во встроенном браузере
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = strings.neverOpenInBrowser,
                        color = scheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    state.excludedDomains.forEach { domain ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = domain,
                                color = scheme.textPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = scheme.textMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { InAppBrowserStore.removeExcludedDomain(context, domain) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                newSiteInput = ""
                                showAddSiteDialog = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(scheme.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = strings.addSite,
                            color = scheme.accent,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = strings.neverOpenDesc,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Поисковая система
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                ) {
                    BrowserSettingsRow(
                        icon = Icons.Outlined.Search,
                        title = strings.searchEngine,
                        trailing = state.searchEngine.label,
                        onClick = { showSearchEngineDialog = true }
                    )
                }
                Text(
                    text = strings.searchEngineDesc,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showSearchEngineDialog) {
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            containerColor = scheme.card,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = strings.searchEngine,
                    color = scheme.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    BrowserSearchEngine.entries.forEach { engine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    InAppBrowserStore.setSearchEngine(context, engine)
                                    showSearchEngineDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.searchEngine == engine,
                                onClick = {
                                    InAppBrowserStore.setSearchEngine(context, engine)
                                    showSearchEngineDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = scheme.accent,
                                    unselectedColor = scheme.textMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = engine.label,
                                color = scheme.textPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) {
                    Text(strings.cancel, color = scheme.accent)
                }
            }
        )
    }

    if (showAddSiteDialog) {
        AlertDialog(
            onDismissRequest = { showAddSiteDialog = false },
            containerColor = scheme.card,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = strings.addSite,
                    color = scheme.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = newSiteInput,
                    onValueChange = { newSiteInput = it },
                    placeholder = { Text(strings.addSiteHint, color = scheme.textMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.accent,
                        unfocusedBorderColor = scheme.islandBorder,
                        focusedTextColor = scheme.textPrimary,
                        unfocusedTextColor = scheme.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newSiteInput.isNotBlank()) {
                            InAppBrowserStore.addExcludedDomain(context, newSiteInput)
                        }
                        showAddSiteDialog = false
                    }
                ) {
                    Text(strings.add, color = scheme.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSiteDialog = false }) {
                    Text(strings.cancel, color = scheme.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun BrowserSettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    trailing: String?,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = scheme.textPrimary,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                color = scheme.accent,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "${"%.1f".format(bytes / (1024f * 1024f))} MB"
    bytes >= 1024L -> "${"%.1f".format(bytes / 1024f)} KB"
    else -> "$bytes B"
}
