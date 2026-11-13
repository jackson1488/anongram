package com.anongram.app.features.chats.tools.translate

import android.content.Context
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import io.github.marcosholgado.translatekit.ModelSpec
import io.github.marcosholgado.translatekit.TranslateKit
import io.github.marcosholgado.translatekit.TranslationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.zip.GZIPInputStream

data class TranslationLanguagePack(
    val tag: String,
    val nameRu: String,
    val nameEn: String
)

data class TranslatedMessageState(
    val originalText: String,
    val translatedText: String? = null,
    val sourceLangTag: String? = null,
    val targetLangTag: String = "ru",
    val isShowingTranslation: Boolean = false,
    val isTranslating: Boolean = false,
    val error: String? = null
)

private data class InstalledPairFiles(
    val modelPath: String,
    val vocabPaths: List<String>,
    val shortlistPath: String
)

object LocalMessageTranslator {
    private const val MODELS_JSON_URL =
        "https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data/db/models.json"
    private const val MODELS_DIR_NAME = "bergamot_models"
    private const val INDEX_CACHE_NAME = "models_index.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engineMutex = Mutex()
    private val loadedModels = mutableMapOf<String, TranslationModel>()

    @Volatile
    private var cachedModelsIndex: JSONObject? = null

    private val _translations = MutableStateFlow<Map<String, TranslatedMessageState>>(emptyMap())
    val translations: StateFlow<Map<String, TranslatedMessageState>> = _translations.asStateFlow()

    private val _downloadedLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadedLanguages: StateFlow<Set<String>> = _downloadedLanguages.asStateFlow()

    private val _downloadingLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadingLanguages: StateFlow<Set<String>> = _downloadingLanguages.asStateFlow()

    private val _downloadingProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingProgress: StateFlow<Map<String, Float>> = _downloadingProgress.asStateFlow()

    private val _packSizesBytes = MutableStateFlow<Map<String, Long>>(emptyMap())
    val packSizesBytes: StateFlow<Map<String, Long>> = _packSizesBytes.asStateFlow()

    private val _packErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val packErrors: StateFlow<Map<String, String>> = _packErrors.asStateFlow()

    // Все 60 языков из Mozilla Firefox Translations (Bergamot), переводимые напрямую с EN и между собой через EN
    val supportedPacks: List<TranslationLanguagePack> = listOf(
        TranslationLanguagePack("ru", "Русский", "Russian"),
        TranslationLanguagePack("uk", "Украинский", "Ukrainian"),
        TranslationLanguagePack("be", "Белорусский", "Belarusian"),
        TranslationLanguagePack("de", "Немецкий", "German"),
        TranslationLanguagePack("fr", "Французский", "French"),
        TranslationLanguagePack("es", "Испанский", "Spanish"),
        TranslationLanguagePack("it", "Итальянский", "Italian"),
        TranslationLanguagePack("pt", "Португальский", "Portuguese"),
        TranslationLanguagePack("pl", "Польский", "Polish"),
        TranslationLanguagePack("tr", "Турецкий", "Turkish"),
        TranslationLanguagePack("az", "Азербайджанский", "Azerbaijani"),
        TranslationLanguagePack("zh", "Китайский (упр.)", "Chinese (Simp.)"),
        TranslationLanguagePack("zh_hant", "Китайский (трад.)", "Chinese (Trad.)"),
        TranslationLanguagePack("ja", "Японский", "Japanese"),
        TranslationLanguagePack("ko", "Корейский", "Korean"),
        TranslationLanguagePack("ar", "Арабский", "Arabic"),
        TranslationLanguagePack("fa", "Персидский", "Persian"),
        TranslationLanguagePack("he", "Иврит", "Hebrew"),
        TranslationLanguagePack("hi", "Хинди", "Hindi"),
        TranslationLanguagePack("ur", "Урду", "Urdu"),
        TranslationLanguagePack("bn", "Бенгальский", "Bengali"),
        TranslationLanguagePack("vi", "Вьетнамский", "Vietnamese"),
        TranslationLanguagePack("th", "Тайский", "Thai"),
        TranslationLanguagePack("id", "Индонезийский", "Indonesian"),
        TranslationLanguagePack("ms", "Малайский", "Malay"),
        TranslationLanguagePack("tl", "Тагальский", "Tagalog"),
        TranslationLanguagePack("nl", "Нидерландский", "Dutch"),
        TranslationLanguagePack("sv", "Шведский", "Swedish"),
        TranslationLanguagePack("da", "Датский", "Danish"),
        TranslationLanguagePack("nb", "Норвежский (букмол)", "Norwegian Bokmal"),
        TranslationLanguagePack("nn", "Норвежский (нюнорск)", "Norwegian Nynorsk"),
        TranslationLanguagePack("no", "Норвежский", "Norwegian"),
        TranslationLanguagePack("fi", "Финский", "Finnish"),
        TranslationLanguagePack("is", "Исландский", "Icelandic"),
        TranslationLanguagePack("et", "Эстонский", "Estonian"),
        TranslationLanguagePack("lv", "Латышский", "Latvian"),
        TranslationLanguagePack("lt", "Литовский", "Lithuanian"),
        TranslationLanguagePack("cs", "Чешский", "Czech"),
        TranslationLanguagePack("sk", "Словацкий", "Slovak"),
        TranslationLanguagePack("sl", "Словенский", "Slovenian"),
        TranslationLanguagePack("hr", "Хорватский", "Croatian"),
        TranslationLanguagePack("sr", "Сербский", "Serbian"),
        TranslationLanguagePack("bs", "Боснийский", "Bosnian"),
        TranslationLanguagePack("hbs", "Сербохорватский", "Serbo-Croatian"),
        TranslationLanguagePack("bg", "Болгарский", "Bulgarian"),
        TranslationLanguagePack("ro", "Румынский", "Romanian"),
        TranslationLanguagePack("hu", "Венгерский", "Hungarian"),
        TranslationLanguagePack("el", "Греческий", "Greek"),
        TranslationLanguagePack("sq", "Албанский", "Albanian"),
        TranslationLanguagePack("ca", "Каталанский", "Catalan"),
        TranslationLanguagePack("gl", "Галисийский", "Galician"),
        TranslationLanguagePack("eu", "Баскский", "Basque"),
        TranslationLanguagePack("af", "Африкаанс", "Afrikaans"),
        TranslationLanguagePack("ug", "Уйгурский", "Uyghur"),
        TranslationLanguagePack("ta", "Тамильский", "Tamil"),
        TranslationLanguagePack("te", "Телугу", "Telugu"),
        TranslationLanguagePack("kn", "Каннада", "Kannada"),
        TranslationLanguagePack("ml", "Малаялам", "Malayalam"),
        TranslationLanguagePack("mr", "Маратхи", "Marathi"),
        TranslationLanguagePack("gu", "Гуджарати", "Gujarati")
    )

    fun refreshDownloadedModels(context: Context? = null) {
        val appContext = context?.applicationContext ?: return
        val root = File(appContext.filesDir, MODELS_DIR_NAME)
        if (!root.exists()) {
            _downloadedLanguages.value = emptySet()
            _packSizesBytes.value = emptyMap()
            return
        }

        val installed = mutableSetOf<String>()
        val sizes = mutableMapOf<String, Long>()
        for (pack in supportedPacks) {
            val pair1 = File(root, "${pack.tag}-en")
            val pair2 = File(root, "en-${pack.tag}")
            val has1 = isPairValid(pair1)
            val has2 = isPairValid(pair2)
            if (has1 || has2) {
                installed.add(pack.tag)
                sizes[pack.tag] = dirSize(pair1) + dirSize(pair2)
            }
        }
        _downloadedLanguages.value = installed
        _packSizesBytes.value = sizes
    }

    fun downloadLanguagePack(context: Context, langTag: String) {
        val appContext = context.applicationContext
        if (_downloadingLanguages.value.contains(langTag)) return

        _downloadingLanguages.update { it + langTag }
        _downloadingProgress.update { it + (langTag to 0f) }
        _packErrors.update { it - langTag }

        scope.launch {
            try {
                ensureLanguageDownloadedInternal(appContext, langTag) { pct ->
                    _downloadingProgress.update { it + (langTag to pct) }
                }
                refreshDownloadedModels(appContext)
            } catch (e: Exception) {
                _packErrors.update { it + (langTag to (e.localizedMessage ?: "Download failed")) }
            } finally {
                _downloadingLanguages.update { it - langTag }
                _downloadingProgress.update { it - langTag }
            }
        }
    }

    fun deleteLanguagePack(context: Context, langTag: String) {
        val appContext = context.applicationContext
        scope.launch {
            engineMutex.withLock {
                listOf("$langTag-en", "en-$langTag").forEach { pair ->
                    loadedModels.remove(pair)?.close()
                    File(File(appContext.filesDir, MODELS_DIR_NAME), pair).deleteRecursively()
                }
            }
            refreshDownloadedModels(appContext)
        }
    }

    fun toggleShowOriginal(messageId: String) {
        _translations.update { map ->
            val current = map[messageId] ?: return@update map
            if (current.translatedText == null) return@update map
            map + (messageId to current.copy(isShowingTranslation = !current.isShowingTranslation))
        }
    }

    fun translateOrToggle(
        context: Context,
        messageId: String,
        text: String,
        explicitTargetLang: String? = null
    ) {
        if (text.isBlank()) return
        val existing = _translations.value[messageId]
        if (existing != null && existing.originalText == text && existing.translatedText != null) {
            toggleShowOriginal(messageId)
            return
        }
        if (existing?.isTranslating == true) return

        val targetLang = resolveTargetLanguage(context, explicitTargetLang)
        launchTranslation(
            appContext = context.applicationContext,
            messageId = messageId,
            text = text,
            targetLang = targetLang,
            isAuto = false
        )
    }

    fun autoTranslateIfNeeded(
        context: Context,
        messageId: String,
        text: String
    ) {
        if (text.isBlank()) return
        ModulesSettingsStore.ensureInitialized(context)
        if (!ModulesSettingsStore.state.value.autoTranslateIncoming) return

        val existing = _translations.value[messageId]
        if (existing != null && existing.originalText == text) return

        val targetLang = resolveTargetLanguage(context, null)
        launchTranslation(
            appContext = context.applicationContext,
            messageId = messageId,
            text = text,
            targetLang = targetLang,
            isAuto = true
        )
    }

    private fun resolveTargetLanguage(context: Context, explicitTargetLang: String?): String {
        ModulesSettingsStore.ensureInitialized(context)
        val configured = explicitTargetLang ?: ModulesSettingsStore.state.value.targetTranslateLanguage
        if (configured != "auto") return configured
        val sysLang = Locale.getDefault().language.lowercase(Locale.ROOT)
        if (sysLang == "en" || supportedPacks.any { it.tag == sysLang }) {
            return sysLang
        }
        return if (sysLang.startsWith("ru")) "ru" else "en"
    }

    private fun launchTranslation(
        appContext: Context,
        messageId: String,
        text: String,
        targetLang: String,
        isAuto: Boolean
    ) {
        if (!isAuto) {
            _translations.update { map ->
                map + (
                    messageId to TranslatedMessageState(
                        originalText = text,
                        targetLangTag = targetLang,
                        isTranslating = true
                    )
                )
            }
        }

        scope.launch {
            try {
                val translated = engineMutex.withLock {
                    TranslateKit.init(appContext)
                    check(TranslateKit.isInitialized()) {
                        "Bergamot engine unavailable on this CPU architecture"
                    }

                    val detectedRaw = TranslateKit.detectLanguage(text).language
                    val detectedTag = normalizeDetectedLang(detectedRaw)

                    var effectiveSource = detectedTag ?: if (targetLang == "ru") "en" else "ru"
                    var effectiveTarget = targetLang

                    if (effectiveSource == effectiveTarget) {
                        if (isAuto) return@withLock null
                        effectiveTarget = if (effectiveTarget == "ru") "en" else "ru"
                    }

                    // Строим маршрут: прямой (если одна из сторон en) или через pivot en
                    val hops = if (effectiveSource == "en" || effectiveTarget == "en") {
                        listOf(effectiveSource to effectiveTarget)
                    } else {
                        listOf(effectiveSource to "en", "en" to effectiveTarget)
                    }

                    // При автопереводе переводим только если нужные модели уже скачаны;
                    // при ручном нажатии — до-скачиваем недостающую модель автоматически
                    for ((src, dst) in hops) {
                        val pairKey = "$src-$dst"
                        val pairDir = File(File(appContext.filesDir, MODELS_DIR_NAME), pairKey)
                        if (!isPairValid(pairDir)) {
                            if (isAuto) return@withLock null
                            downloadPairInternal(appContext, pairKey) {}
                            refreshDownloadedModels(appContext)
                        }
                    }

                    var currentText = text
                    for ((src, dst) in hops) {
                        val pairKey = "$src-$dst"
                        val model = getOrLoadModel(appContext, src, dst)
                        currentText = model.translate(currentText, isHtml = false).text
                    }
                    effectiveSource to currentText
                }

                if (translated == null) {
                    if (!isAuto) {
                        _translations.update { it - messageId }
                    }
                    return@launch
                }

                val (effectiveSource, translatedText) = translated
                _translations.update { map ->
                    map + (
                        messageId to TranslatedMessageState(
                            originalText = text,
                            translatedText = translatedText,
                            sourceLangTag = effectiveSource,
                            targetLangTag = targetLang,
                            isShowingTranslation = true,
                            isTranslating = false
                        )
                    )
                }
            } catch (e: Exception) {
                if (!isAuto) {
                    _translations.update { map ->
                        map + (
                            messageId to TranslatedMessageState(
                                originalText = text,
                                targetLangTag = targetLang,
                                isShowingTranslation = false,
                                isTranslating = false,
                                error = e.localizedMessage ?: "Translation failed"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun normalizeDetectedLang(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "un" || raw == "und") return null
        val lower = raw.lowercase(Locale.ROOT)
        if (lower == "zh-tw" || lower == "zh-hant") return "zh_hant"
        val base = lower.substringBefore('-')
        if (base == "en") return "en"
        if (supportedPacks.any { it.tag == base }) return base
        return null
    }

    private fun getOrLoadModel(appContext: Context, src: String, dst: String): TranslationModel {
        val pairKey = "$src-$dst"
        loadedModels[pairKey]?.let { return it }

        val files = readInstalledPairFiles(File(File(appContext.filesDir, MODELS_DIR_NAME), pairKey))
            ?: error("Model $pairKey not installed")

        // Держим в памяти до 3 последних моделей для экономии RAM
        if (loadedModels.size >= 3) {
            val oldestKey = loadedModels.keys.firstOrNull()
            if (oldestKey != null) {
                loadedModels.remove(oldestKey)?.close()
            }
        }

        val model = TranslateKit.loadModel(
            ModelSpec(
                sourceLang = src,
                targetLang = dst,
                modelPath = files.modelPath,
                vocabPaths = files.vocabPaths,
                shortlistPath = files.shortlistPath,
                configYaml = null
            )
        )
        loadedModels[pairKey] = model
        return model
    }

    private fun ensureLanguageDownloadedInternal(
        appContext: Context,
        langTag: String,
        onProgress: (Float) -> Unit
    ) {
        val index = loadModelsIndex(appContext)
        val modelsObj = index.getJSONObject("models")
        val pairsToDownload = listOf("$langTag-en", "en-$langTag").filter { modelsObj.has(it) }
        if (pairsToDownload.isEmpty()) {
            error("No Bergamot models found for $langTag")
        }

        pairsToDownload.forEachIndexed { idx, pairKey ->
            val baseFraction = idx.toFloat() / pairsToDownload.size
            val span = 1f / pairsToDownload.size
            downloadPairInternal(appContext, pairKey) { p ->
                onProgress((baseFraction + p * span).coerceIn(0f, 1f))
            }
        }
        onProgress(1f)
    }

    private fun downloadPairInternal(
        appContext: Context,
        pairKey: String,
        onProgress: (Float) -> Unit
    ) {
        val index = loadModelsIndex(appContext)
        val baseUrl = index.getString("baseUrl").trimEnd('/')
        val modelsObj = index.getJSONObject("models")
        if (!modelsObj.has(pairKey)) {
            error("Pair $pairKey not available")
        }
        val entries = modelsObj.getJSONArray(pairKey)
        if (entries.length() == 0) error("Empty model list for $pairKey")

        // Предпочитаем Release, иначе первый доступный
        var chosen = entries.getJSONObject(0)
        for (i in 0 until entries.length()) {
            val item = entries.getJSONObject(i)
            if (item.optString("releaseStatus") == "Release") {
                chosen = item
                break
            }
        }

        val filesObj = chosen.getJSONObject("files")
        val modelRel = filesObj.getJSONObject("model").getString("path")
        val shortlistRel = filesObj.getJSONObject("lexicalShortlist").getString("path")
        val vocabRels = if (filesObj.has("vocab")) {
            listOf(filesObj.getJSONObject("vocab").getString("path"))
        } else {
            listOf(
                filesObj.getJSONObject("srcVocab").getString("path"),
                filesObj.getJSONObject("trgVocab").getString("path")
            )
        }

        val pairDir = File(File(appContext.filesDir, MODELS_DIR_NAME), pairKey)
        val tmpDir = File(File(appContext.filesDir, MODELS_DIR_NAME), "${pairKey}.tmp")
        tmpDir.deleteRecursively()
        tmpDir.mkdirs()

        try {
            val totalSteps = 2 + vocabRels.size
            var done = 0

            val modelFile = File(tmpDir, modelRel.substringAfterLast('/').removeSuffix(".gz"))
            downloadAndGunzip("$baseUrl/$modelRel", modelFile) { p ->
                onProgress(((done + p) / totalSteps).coerceIn(0f, 1f))
            }
            done++

            val shortlistFile = File(tmpDir, shortlistRel.substringAfterLast('/').removeSuffix(".gz"))
            downloadAndGunzip("$baseUrl/$shortlistRel", shortlistFile) { p ->
                onProgress(((done + p) / totalSteps).coerceIn(0f, 1f))
            }
            done++

            val vocabFiles = vocabRels.mapIndexed { vIdx, rel ->
                val target = File(tmpDir, "vocab_${vIdx}_${rel.substringAfterLast('/').removeSuffix(".gz")}")
                downloadAndGunzip("$baseUrl/$rel", target) { p ->
                    onProgress(((done + p) / totalSteps).coerceIn(0f, 1f))
                }
                done++
                target
            }

            val meta = JSONObject().apply {
                put("model", modelFile.name)
                put("shortlist", shortlistFile.name)
                put("vocabs", org.json.JSONArray(vocabFiles.map { it.name }))
            }
            File(tmpDir, "meta.json").writeText(meta.toString())

            pairDir.deleteRecursively()
            if (!tmpDir.renameTo(pairDir)) {
                tmpDir.copyRecursively(pairDir, overwrite = true)
                tmpDir.deleteRecursively()
            }
        } catch (e: Exception) {
            tmpDir.deleteRecursively()
            throw e
        }
    }

    private fun loadModelsIndex(appContext: Context): JSONObject {
        cachedModelsIndex?.let { return it }
        val cacheFile = File(appContext.filesDir, INDEX_CACHE_NAME)
        return try {
            val conn = (URL(MODELS_JSON_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 20_000
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            cacheFile.writeText(text)
            JSONObject(text).also { cachedModelsIndex = it }
        } catch (e: Exception) {
            if (cacheFile.exists()) {
                JSONObject(cacheFile.readText()).also { cachedModelsIndex = it }
            } else {
                throw e
            }
        }
    }

    private fun downloadAndGunzip(
        urlStr: String,
        destFile: File,
        onProgress: (Float) -> Unit
    ) {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
        }
        val total = conn.contentLengthLong.coerceAtLeast(1L)
        val gzTmp = File(destFile.parentFile, destFile.name + ".gz")

        conn.inputStream.use { input ->
            FileOutputStream(gzTmp).use { output ->
                val buf = ByteArray(16 * 1024)
                var copied = 0L
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    output.write(buf, 0, read)
                    copied += read
                    onProgress((copied.toFloat() / total).coerceIn(0f, 0.9f))
                }
            }
        }

        GZIPInputStream(gzTmp.inputStream().buffered()).use { gz ->
            FileOutputStream(destFile).use { out ->
                gz.copyTo(out, 16 * 1024)
            }
        }
        gzTmp.delete()
        onProgress(1f)
    }

    private fun isPairValid(pairDir: File): Boolean = readInstalledPairFiles(pairDir) != null

    private fun readInstalledPairFiles(pairDir: File): InstalledPairFiles? {
        val metaFile = File(pairDir, "meta.json")
        if (!metaFile.exists()) return null
        return try {
            val json = JSONObject(metaFile.readText())
            val modelFile = File(pairDir, json.getString("model"))
            val shortlistFile = File(pairDir, json.getString("shortlist"))
            val vocabsArr = json.getJSONArray("vocabs")
            val vocabs = (0 until vocabsArr.length()).map { idx ->
                File(pairDir, vocabsArr.getString(idx))
            }
            if (!modelFile.exists() || !shortlistFile.exists() || vocabs.isEmpty() || vocabs.any { !it.exists() }) {
                null
            } else {
                InstalledPairFiles(
                    modelPath = modelFile.absolutePath,
                    vocabPaths = vocabs.map { it.absolutePath },
                    shortlistPath = shortlistFile.absolutePath
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
