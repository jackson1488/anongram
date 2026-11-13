package com.anongram.app.features.chats.media.voice.stt

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.anongram.app.core.assets.ExternalAssetManager
import com.anongram.app.core.assets.ModuleAssetManifest
import com.anongram.app.core.assets.ModuleAssetSpec
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.data.VoiceSttEngine
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * События потоковой расшифровки голосового сообщения.
 */
sealed interface TranscriptionEvent {
    data class Streaming(val partialText: String) : TranscriptionEvent
    data class Completed(val finalText: String) : TranscriptionEvent
    data class ModelMissing(val spec: ModuleAssetSpec) : TranscriptionEvent
    data class Error(val message: String) : TranscriptionEvent
}

/**
 * Единый роутер расшифровки ГС, выбирающий движок из [ModulesSettingsStore]
 * (Whisper / Vosk / Системный-Google) и отдающий поток частичных результатов.
 */
object VoiceTranscriber {

    fun transcribe(
        context: Context,
        audioUri: String
    ): Flow<TranscriptionEvent> {
        ModulesSettingsStore.ensureInitialized(context)
        val settings = ModulesSettingsStore.state.value
        val assetManager = ExternalAssetManager.getInstance(context)

        return when (settings.voiceEngine) {
            VoiceSttEngine.WHISPER -> {
                val spec = ModuleAssetManifest.findById(settings.whisperModelId)
                    ?: ModuleAssetManifest.WHISPER_TINY_Q5
                val modelFile = assetManager.getInstalledModelPath(spec)
                if (modelFile == null) {
                    flow { emit(TranscriptionEvent.ModelMissing(spec)) }
                } else {
                    transcribeWithWhisper(context, audioUri, modelFile, settings.whisperLanguage)
                }
            }

            VoiceSttEngine.VOSK -> {
                val spec = ModuleAssetManifest.findById(settings.voskAssetId)
                    ?: ModuleAssetManifest.VOSK_RU
                val modelDir = assetManager.getInstalledModelPath(spec)
                if (modelDir == null) {
                    flow { emit(TranscriptionEvent.ModelMissing(spec)) }
                } else {
                    transcribeWithVosk(context, audioUri, modelDir)
                }
            }

            VoiceSttEngine.SYSTEM_GOOGLE -> {
                transcribeWithSystemGoogle(context, audioUri, settings.systemLanguage)
            }
        }
    }

    private fun transcribeWithWhisper(
        context: Context,
        audioUri: String,
        modelFile: File,
        language: String
    ): Flow<TranscriptionEvent> = callbackFlow {
        if (!WhisperNativeBridge.isAvailable) {
            trySend(TranscriptionEvent.Error("Whisper native engine unavailable"))
            close()
            return@callbackFlow
        }

        val pcmShorts = AudioPcmDecoder.decodeTo16kMonoPcm(context, audioUri)
        val pcmFloats = FloatArray(pcmShorts.size) { i -> pcmShorts[i] / 32768.0f }

        val accumulated = StringBuilder()
        val fullText = WhisperNativeBridge.transcribePcm16k(
            modelPath = modelFile.absolutePath,
            language = language,
            pcmFloats = pcmFloats,
            listener = object : WhisperNativeBridge.SegmentListener {
                override fun onNewSegment(segmentText: String) {
                    accumulated.append(segmentText)
                    trySend(TranscriptionEvent.Streaming(accumulated.toString().trim()))
                }
            }
        ).trim()

        trySend(TranscriptionEvent.Completed(fullText))
        close()
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    private fun transcribeWithVosk(
        context: Context,
        audioUri: String,
        modelDir: File
    ): Flow<TranscriptionEvent> = flow {
        val pcmShorts = AudioPcmDecoder.decodeTo16kMonoPcm(context, audioUri)
        val model = Model(modelDir.absolutePath)
        try {
            val recognizer = Recognizer(model, 16_000.0f)
            try {
                val fullText = StringBuilder()
                val chunkSize = 4096
                val byteBuf = ByteBuffer.allocate(chunkSize * 2).order(ByteOrder.LITTLE_ENDIAN)

                var offset = 0
                while (offset < pcmShorts.size) {
                    val count = minOf(chunkSize, pcmShorts.size - offset)
                    byteBuf.clear()
                    for (i in 0 until count) {
                        byteBuf.putShort(pcmShorts[offset + i])
                    }
                    val bytes = byteBuf.array()
                    val lenBytes = count * 2

                    if (recognizer.acceptWaveForm(bytes, lenBytes)) {
                        val segment = extractJsonField(recognizer.result, "text")
                        if (segment.isNotBlank()) {
                            if (fullText.isNotEmpty()) fullText.append(' ')
                            fullText.append(segment)
                            emit(TranscriptionEvent.Streaming(fullText.toString()))
                        }
                    } else {
                        val partial = extractJsonField(recognizer.partialResult, "partial")
                        if (partial.isNotBlank()) {
                            val combined = if (fullText.isEmpty()) partial else "${fullText} $partial"
                            emit(TranscriptionEvent.Streaming(combined))
                        }
                    }
                    offset += count
                }

                val tail = extractJsonField(recognizer.finalResult, "text")
                if (tail.isNotBlank()) {
                    if (fullText.isNotEmpty()) fullText.append(' ')
                    fullText.append(tail)
                }
                emit(TranscriptionEvent.Completed(fullText.toString().trim()))
            } finally {
                runCatching { recognizer.close() }
            }
        } finally {
            runCatching { model.close() }
        }
    }.flowOn(Dispatchers.IO)

    private fun transcribeWithSystemGoogle(
        context: Context,
        audioUri: String,
        languageTag: String
    ): Flow<TranscriptionEvent> = callbackFlow {
        val isRussian = LanguageSettingsStore.language.value == AppLanguage.RU
        val strings = ModulesI18n.getSync(isRussian)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            trySend(TranscriptionEvent.Error(strings.voiceSystemUnsupportedError))
            close()
            return@callbackFlow
        }

        val tempPcm = File(context.cacheDir, "stt_sys_${System.nanoTime()}.pcm")
        val pfd: ParcelFileDescriptor? = withContext(Dispatchers.IO) {
            runCatching {
                val pcmShorts = AudioPcmDecoder.decodeTo16kMonoPcm(context, audioUri)
                val byteBuf = ByteBuffer.allocate(pcmShorts.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (s in pcmShorts) byteBuf.putShort(s)
                tempPcm.writeBytes(byteBuf.array())
                ParcelFileDescriptor.open(tempPcm, ParcelFileDescriptor.MODE_READ_ONLY)
            }.getOrNull()
        }

        if (pfd == null) {
            tempPcm.delete()
            trySend(TranscriptionEvent.Error("Audio decode error"))
            close()
            return@callbackFlow
        }

        var recognizer: SpeechRecognizer? = null
        var lastPartial = ""
        withContext(Dispatchers.Main) {
            val useOnDevice = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            val useStandard = SpeechRecognizer.isRecognitionAvailable(context)
            if (!useOnDevice && !useStandard) {
                runCatching { pfd.close() }
                tempPcm.delete()
                trySend(TranscriptionEvent.Error(strings.voiceSystemUnsupportedError))
                close()
                return@withContext
            }

            recognizer = if (useOnDevice) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }.apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}

                    override fun onPartialResults(partialResults: Bundle?) {
                        val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = list?.firstOrNull()?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            lastPartial = text
                            trySend(TranscriptionEvent.Streaming(text))
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = list?.firstOrNull()?.trim().orEmpty().ifBlank { lastPartial }
                        trySend(TranscriptionEvent.Completed(text))
                        runCatching { pfd.close() }
                        tempPcm.delete()
                        close()
                    }

                    override fun onError(error: Int) {
                        if (lastPartial.isNotBlank() &&
                            (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                        ) {
                            trySend(TranscriptionEvent.Completed(lastPartial))
                        } else {
                            trySend(TranscriptionEvent.Error("Google Speech error ($error)"))
                        }
                        runCatching { pfd.close() }
                        tempPcm.delete()
                        close()
                    }
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pfd)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16000)
                }
                startListening(intent)
            }
        }

        awaitClose {
            runCatching { recognizer?.destroy() }
            runCatching { pfd.close() }
            tempPcm.delete()
        }
    }

    private fun extractJsonField(json: String?, key: String): String {
        if (json.isNullOrBlank()) return ""
        return runCatching { JSONObject(json).optString(key, "").trim() }.getOrDefault("")
    }
}
