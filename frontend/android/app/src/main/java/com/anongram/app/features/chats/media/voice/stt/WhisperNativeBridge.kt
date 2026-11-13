package com.anongram.app.features.chats.media.voice.stt

/**
 * JNI-мост к локальному C++ движку `whisper.cpp` (`libanongram_whisper.so`).
 */
object WhisperNativeBridge {

    interface SegmentListener {
        fun onNewSegment(segmentText: String)
    }

    val isAvailable: Boolean by lazy {
        try {
            System.loadLibrary("anongram_whisper")
            true
        } catch (_: Throwable) {
            false
        }
    }

    external fun transcribePcm16k(
        modelPath: String,
        language: String,
        pcmFloats: FloatArray,
        listener: SegmentListener?
    ): String
}
