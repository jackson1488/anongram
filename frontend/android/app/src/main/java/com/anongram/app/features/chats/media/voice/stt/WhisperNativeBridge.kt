package com.anongram.app.features.chats.media.voice.stt

/**
 * JNI-мост к локальному C++ движку `whisper.cpp` (`libanongram_whisper.so`).
 */
object WhisperNativeBridge {

    interface SegmentListener {
        fun onNewSegment(segmentText: String)
    }

    @Volatile
    private var loaded: Boolean = false

    val isAvailable: Boolean
        get() {
            if (loaded) return true
            return try {
                System.loadLibrary("anongram_whisper")
                loaded = true
                true
            } catch (_: Throwable) {
                false
            }
        }

    fun ensureLoaded(context: android.content.Context): Boolean {
        if (loaded) return true
        val ok = com.anongram.app.core.assets.NativeModuleManager.loadLibrary(
            context,
            com.anongram.app.core.assets.NativeModuleManager.WHISPER_SO,
            "anongram_whisper"
        )
        if (ok) loaded = true
        return ok
    }

    external fun transcribePcm16k(
        modelPath: String,
        language: String,
        pcmFloats: FloatArray,
        listener: SegmentListener?
    ): String
}
