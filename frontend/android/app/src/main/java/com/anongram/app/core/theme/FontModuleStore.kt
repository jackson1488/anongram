package com.anongram.app.core.theme

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

/** Шрифт, применяемый к тексту сообщений (если область ALL или MESSAGES_ONLY). */
val LocalChatMessageFontFamily = staticCompositionLocalOf<FontFamily?> { null }

object FontModuleStore {
    private const val PREFS_NAME = "anongram_font_module_prefs"
    private const val KEY_ACTIVE_FONT_ID = "active_font_id"
    private const val KEY_APPLY_SCOPE = "apply_scope"
    private const val CATALOG_CACHE_FILE = "catalog_cache.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(FontModuleState())
    val state: StateFlow<FontModuleState> = _state.asStateFlow()

    private val fontFamilyCache = ConcurrentHashMap<String, FontFamily>()
    @Volatile
    private var initialized = false
    private var downloadJob: Job? = null

    private fun fontsDir(context: Context): File =
        File(context.applicationContext.filesDir, "fonts").apply { mkdirs() }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val p = prefs(context)
            val activeId = p.getString(KEY_ACTIVE_FONT_ID, null)
            val applyScope = runCatching {
                FontApplyScope.valueOf(p.getString(KEY_APPLY_SCOPE, FontApplyScope.ALL.name)!!)
            }.getOrDefault(FontApplyScope.ALL)

            val cacheFile = File(fontsDir(context), CATALOG_CACHE_FILE)
            val cachedCatalog = if (cacheFile.exists()) {
                runCatching { FontsourceClient.parseCatalogJson(cacheFile.readText(Charsets.UTF_8)) }
                    .getOrDefault(emptyList())
            } else emptyList()

            _state.update {
                it.copy(
                    activeFontId = activeId,
                    applyScope = applyScope,
                    catalog = cachedCatalog
                )
            }
            refreshInstalledFonts(context)
            initialized = true
        }
    }

    fun refreshInstalledFonts(context: Context) {
        val dir = fontsDir(context)
        val catalogMap = _state.value.catalog.associateBy { it.id }
        val files = dir.listFiles()?.filter {
            it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true))
        }.orEmpty()

        val installed = files.map { f ->
            val baseId = f.nameWithoutExtension
            val isImported = baseId.startsWith("imp_")
            val cleanId = baseId.removePrefix("imp_")
            val catItem = catalogMap[baseId]
            InstalledFontItem(
                id = baseId,
                displayName = catItem?.family ?: cleanId.replace('_', ' ').replace('-', ' '),
                fileName = f.name,
                sizeBytes = f.length(),
                isImported = isImported
            )
        }.sortedBy { it.displayName }

        _state.update { st ->
            val validActive = if (st.activeFontId != null && installed.any { it.id == st.activeFontId }) {
                st.activeFontId
            } else null
            st.copy(installedFonts = installed, activeFontId = validActive)
        }
    }

    fun loadCatalog(context: Context, forceRefresh: Boolean = false) {
        ensureInitialized(context)
        if (_state.value.isCatalogLoading) return
        if (!forceRefresh && _state.value.catalog.isNotEmpty()) return

        _state.update { it.copy(isCatalogLoading = true, catalogError = null) }
        scope.launch {
            FontsourceClient.fetchCatalog()
                .onSuccess { rawJson ->
                    val parsed = withContext(Dispatchers.Default) {
                        FontsourceClient.parseCatalogJson(rawJson)
                    }
                    if (parsed.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                File(fontsDir(context), CATALOG_CACHE_FILE).writeText(rawJson, Charsets.UTF_8)
                            }
                        }
                    }
                    _state.update {
                        it.copy(
                            catalog = parsed,
                            isCatalogLoading = false,
                            catalogError = null
                        )
                    }
                    refreshInstalledFonts(context)
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isCatalogLoading = false,
                            catalogError = err.message ?: "Network error"
                        )
                    }
                }
        }
    }

    fun downloadFont(context: Context, item: FontCatalogItem) {
        ensureInitialized(context)
        downloadJob?.cancel()
        _state.update {
            it.copy(
                downloadingFontId = item.id,
                downloadProgress = 0f,
                downloadError = null
            )
        }
        downloadJob = scope.launch {
            val urlRes = FontsourceClient.resolveTtfUrl(item.id)
            val ttfUrl = urlRes.getOrElse { err ->
                _state.update {
                    it.copy(downloadingFontId = null, downloadError = err.message ?: "Failed")
                }
                return@launch
            }
            val dest = File(fontsDir(context), "${item.id}.ttf")
            val dlRes = FontsourceClient.downloadFile(ttfUrl, dest) { p ->
                _state.update { it.copy(downloadProgress = p) }
            }
            dlRes.onSuccess {
                fontFamilyCache.remove(item.id)
                refreshInstalledFonts(context)
                selectFont(context, item.id)
                _state.update { it.copy(downloadingFontId = null, downloadProgress = 1f) }
            }.onFailure { err ->
                _state.update {
                    it.copy(downloadingFontId = null, downloadError = err.message ?: "Download failed")
                }
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri, onDone: (Int) -> Unit = {}) {
        ensureInitialized(context)
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                val displayName = queryDisplayName(context, uri) ?: "custom_font.ttf"
                val lower = displayName.lowercase()
                var importedCount = 0
                var lastId: String? = null
                val dir = fontsDir(context)

                if (lower.endsWith(".zip")) {
                    context.contentResolver.openInputStream(uri)?.use { rawIn ->
                        ZipInputStream(rawIn).use { zis ->
                            while (true) {
                                val entry = zis.nextEntry ?: break
                                val eName = File(entry.name).name
                                if (!entry.isDirectory && (eName.endsWith(".ttf", true) || eName.endsWith(".otf", true))) {
                                    val safeBase = "imp_" + eName.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                    val ext = eName.substringAfterLast('.', "ttf")
                                    val outFile = File(dir, "$safeBase.$ext")
                                    FileOutputStream(outFile).use { out -> zis.copyTo(out) }
                                    fontFamilyCache.remove(safeBase)
                                    lastId = safeBase
                                    importedCount++
                                }
                                zis.closeEntry()
                            }
                        }
                    }
                } else if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
                    val safeBase = "imp_" + displayName.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9_-]"), "_")
                    val ext = displayName.substringAfterLast('.', "ttf")
                    val outFile = File(dir, "$safeBase.$ext")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(outFile).use { out -> input.copyTo(out) }
                    }
                    fontFamilyCache.remove(safeBase)
                    lastId = safeBase
                    importedCount = 1
                }
                importedCount to lastId
            }
            refreshInstalledFonts(context)
            count.second?.let { selectFont(context, it) }
            onDone(count.first)
        }
    }

    fun selectFont(context: Context, fontId: String?) {
        ensureInitialized(context)
        val editor = prefs(context).edit()
        if (fontId == null) editor.remove(KEY_ACTIVE_FONT_ID) else editor.putString(KEY_ACTIVE_FONT_ID, fontId)
        editor.apply()
        _state.update { it.copy(activeFontId = fontId) }
    }

    fun setApplyScope(context: Context, scopeValue: FontApplyScope) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_APPLY_SCOPE, scopeValue.name).apply()
        _state.update { it.copy(applyScope = scopeValue) }
    }

    fun deleteFont(context: Context, font: InstalledFontItem) {
        ensureInitialized(context)
        File(fontsDir(context), font.fileName).delete()
        fontFamilyCache.remove(font.id)
        if (_state.value.activeFontId == font.id) {
            selectFont(context, null)
        }
        refreshInstalledFonts(context)
    }

    fun resolveFontFamily(context: Context, fontId: String?): FontFamily? {
        if (fontId.isNullOrBlank()) return null
        fontFamilyCache[fontId]?.let { return it }
        val item = _state.value.installedFonts.firstOrNull { it.id == fontId } ?: return null
        val file = File(fontsDir(context), item.fileName)
        if (!file.exists() || file.length() == 0L) return null
        val family = runCatching { FontFamily(Font(file)) }.getOrNull() ?: return null
        fontFamilyCache[fontId] = family
        return family
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }
}
