package com.anongram.app.features.chats.tools.translate

import android.content.Context
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

object LocalMessageTranslator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _translations = MutableStateFlow<Map<String, TranslatedMessageState>>(emptyMap())
    val translations: StateFlow<Map<String, TranslatedMessageState>> = _translations.asStateFlow()

    private val _downloadedLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadedLanguages: StateFlow<Set<String>> = _downloadedLanguages.asStateFlow()

    private val _downloadingLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadingLanguages: StateFlow<Set<String>> = _downloadingLanguages.asStateFlow()

    private val _packErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val packErrors: StateFlow<Map<String, String>> = _packErrors.asStateFlow()

    val supportedPacks: List<TranslationLanguagePack> = listOf(
        TranslationLanguagePack("ru", "Русский", "Russian"),
        TranslationLanguagePack("en", "Английский", "English"),
        TranslationLanguagePack("uk", "Украинский", "Ukrainian"),
        TranslationLanguagePack("de", "Немецкий", "German"),
        TranslationLanguagePack("fr", "Французский", "French"),
        TranslationLanguagePack("es", "Испанский", "Spanish"),
        TranslationLanguagePack("tr", "Турецкий", "Turkish"),
        TranslationLanguagePack("zh", "Китайский", "Chinese"),
        TranslationLanguagePack("ar", "Арабский", "Arabic"),
        TranslationLanguagePack("ja", "Японский", "Japanese"),
        TranslationLanguagePack("ko", "Корейский", "Korean"),
        TranslationLanguagePack("it", "Итальянский", "Italian"),
        TranslationLanguagePack("pt", "Португальский", "Portuguese"),
        TranslationLanguagePack("pl", "Польский", "Polish")
    ).filter { TranslateLanguage.fromLanguageTag(it.tag) != null }

    fun refreshDownloadedModels() {
        val manager = RemoteModelManager.getInstance()
        manager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                _downloadedLanguages.value = models.mapNotNull { model ->
                    TranslateLanguage.fromLanguageTag(model.language) ?: model.language
                }.toSet()
            }
    }

    fun downloadLanguagePack(langTag: String) {
        val mlkitLang = TranslateLanguage.fromLanguageTag(langTag) ?: return
        if (_downloadingLanguages.value.contains(langTag)) return

        _downloadingLanguages.update { it + langTag }
        _packErrors.update { it - langTag }

        val model = TranslateRemoteModel.Builder(mlkitLang).build()
        val conditions = DownloadConditions.Builder().build()
        RemoteModelManager.getInstance()
            .download(model, conditions)
            .addOnSuccessListener {
                _downloadingLanguages.update { it - langTag }
                refreshDownloadedModels()
            }
            .addOnFailureListener { e ->
                _downloadingLanguages.update { it - langTag }
                _packErrors.update { it + (langTag to (e.localizedMessage ?: "Download failed")) }
            }
    }

    fun deleteLanguagePack(langTag: String) {
        val mlkitLang = TranslateLanguage.fromLanguageTag(langTag) ?: return
        val model = TranslateRemoteModel.Builder(mlkitLang).build()
        RemoteModelManager.getInstance()
            .deleteDownloadedModel(model)
            .addOnSuccessListener {
                refreshDownloadedModels()
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
            messageId = messageId,
            text = text,
            targetLang = targetLang,
            isAuto = true
        )
    }

    private fun resolveTargetLanguage(context: Context, explicitTargetLang: String?): String {
        ModulesSettingsStore.ensureInitialized(context)
        val configured = explicitTargetLang ?: ModulesSettingsStore.state.value.targetTranslateLanguage
        if (configured != "auto" && TranslateLanguage.fromLanguageTag(configured) != null) {
            return configured
        }
        val sysLang = Locale.getDefault().language
        if (TranslateLanguage.fromLanguageTag(sysLang) != null) {
            return sysLang
        }
        return if (ModulesI18n.current.isRussian) "ru" else "en"
    }

    private fun launchTranslation(
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
                val identifier = LanguageIdentification.getClient()
                val detectedRaw = try {
                    identifier.identifyLanguage(text).awaitResult()
                } finally {
                    identifier.close()
                }

                val detectedTag = detectedRaw.takeIf { it != "und" }
                    ?.substringBefore('-')
                    ?.lowercase(Locale.ROOT)
                    ?.takeIf { TranslateLanguage.fromLanguageTag(it) != null }

                var effectiveSource = detectedTag ?: if (targetLang == "ru") "en" else "ru"
                var effectiveTarget = targetLang

                if (effectiveSource == effectiveTarget) {
                    if (isAuto) {
                        return@launch
                    }
                    // When manually translating text that is already in targetLang, translate to EN/RU fallback
                    effectiveTarget = if (effectiveTarget == "ru") "en" else "ru"
                }

                val mlkitSource = TranslateLanguage.fromLanguageTag(effectiveSource)
                    ?: TranslateLanguage.ENGLISH
                val mlkitTarget = TranslateLanguage.fromLanguageTag(effectiveTarget)
                    ?: TranslateLanguage.RUSSIAN

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(mlkitSource)
                    .setTargetLanguage(mlkitTarget)
                    .build()
                val translator = Translation.getClient(options)
                val translated = try {
                    translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitResult()
                    translator.translate(text).awaitResult()
                } finally {
                    translator.close()
                }

                refreshDownloadedModels()
                _translations.update { map ->
                    map + (
                        messageId to TranslatedMessageState(
                            originalText = text,
                            translatedText = translated,
                            sourceLangTag = effectiveSource,
                            targetLangTag = effectiveTarget,
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

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { e ->
            if (cont.isActive) cont.resumeWithException(e)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
