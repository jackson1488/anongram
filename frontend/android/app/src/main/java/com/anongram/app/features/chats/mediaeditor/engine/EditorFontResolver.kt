package com.anongram.app.features.chats.mediaeditor.engine

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.anongram.app.core.theme.FontModuleStore
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorStrings
import com.anongram.app.features.chats.mediaeditor.model.EditorFontSpec
import java.io.File

data class ResolvedComposeFont(
    val fontFamily: FontFamily,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontStyle: FontStyle = FontStyle.Normal
)

object EditorFontResolver {

    fun labelFor(spec: EditorFontSpec, strings: MediaEditorStrings): String = when (spec) {
        is EditorFontSpec.Basic -> when (spec.familyKey) {
            "roboto" -> strings.fontRoboto
            "italic" -> strings.fontItalic
            "serif" -> strings.fontSerif
            "condensed" -> strings.fontCondensed
            "mono" -> strings.fontMono
            "bold" -> strings.fontBold
            else -> strings.fontRoboto
        }
        is EditorFontSpec.ModuleFont -> spec.displayName
    }

    fun resolveCompose(context: Context, spec: EditorFontSpec): ResolvedComposeFont = when (spec) {
        is EditorFontSpec.Basic -> when (spec.familyKey) {
            "roboto" -> ResolvedComposeFont(FontFamily.Default, FontWeight.Medium)
            "italic" -> ResolvedComposeFont(FontFamily.Default, FontWeight.Medium, FontStyle.Italic)
            "serif" -> ResolvedComposeFont(FontFamily.Serif, FontWeight.Medium)
            "condensed" -> ResolvedComposeFont(
                FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD)),
                FontWeight.Bold
            )
            "mono" -> ResolvedComposeFont(FontFamily.Monospace, FontWeight.Medium)
            "bold" -> ResolvedComposeFont(
                FontFamily(Typeface.create("sans-serif-black", Typeface.NORMAL)),
                FontWeight.Black
            )
            else -> ResolvedComposeFont(FontFamily.Default)
        }
        is EditorFontSpec.ModuleFont -> {
            val moduleFamily = FontModuleStore.resolveFontFamily(context, spec.fontId)
            ResolvedComposeFont(moduleFamily ?: FontFamily.Default, FontWeight.Normal)
        }
    }

    fun resolveTypeface(context: Context, spec: EditorFontSpec): Typeface = when (spec) {
        is EditorFontSpec.Basic -> when (spec.familyKey) {
            "roboto" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            "italic" -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            "serif" -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            "condensed" -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
            "mono" -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            "bold" -> Typeface.create("sans-serif-black", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
        is EditorFontSpec.ModuleFont -> {
            val file = File(File(context.applicationContext.filesDir, "fonts"), spec.fileName)
            if (file.exists() && file.isFile) {
                runCatching { Typeface.createFromFile(file) }.getOrDefault(Typeface.DEFAULT)
            } else {
                Typeface.DEFAULT
            }
        }
    }
}
