package com.anongram.app.features.chats.mediaeditor.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.anongram.app.core.theme.FontModuleStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.engine.EditorFontResolver
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.model.EditorFontSpec

@Composable
fun EditorFontPickerMenu(
    expanded: Boolean,
    selectedFont: EditorFontSpec,
    onSelectFont: (EditorFontSpec) -> Unit,
    onOpenDownloadFonts: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!expanded) return

    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val fontState by FontModuleStore.state.collectAsState()

    LaunchedEffect(Unit) {
        FontModuleStore.ensureInitialized(context)
    }

    Popup(
        alignment = Alignment.BottomStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(min = 230.dp, max = 300.dp)
                .heightIn(max = 340.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                item {
                    Text(
                        text = strings.fontsBasicSection,
                        color = scheme.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(EditorFontSpec.basicFonts, key = { it.id }) { spec ->
                    FontMenuRow(
                        spec = spec,
                        selected = selectedFont.id == spec.id,
                        onClick = {
                            onSelectFont(spec)
                            onDismiss()
                        }
                    )
                }

                if (fontState.installedFonts.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            color = scheme.divider,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Text(
                            text = strings.fontsModuleSection,
                            color = scheme.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    items(fontState.installedFonts, key = { it.id }) { item ->
                        val spec = EditorFontSpec.ModuleFont(
                            fontId = item.id,
                            displayName = item.displayName,
                            fileName = item.fileName
                        )
                        FontMenuRow(
                            spec = spec,
                            selected = selectedFont.id == spec.id,
                            onClick = {
                                onSelectFont(spec)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = scheme.divider, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onOpenDownloadFonts()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.fontsDownloadMore,
                    color = scheme.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FontMenuRow(
    spec: EditorFontSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    val resolved = EditorFontResolver.resolveCompose(context, spec)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = EditorFontResolver.labelFor(spec, strings),
            color = if (selected) scheme.accent else scheme.textPrimary,
            fontSize = 15.sp,
            fontFamily = resolved.fontFamily,
            fontWeight = resolved.fontWeight,
            fontStyle = resolved.fontStyle
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
