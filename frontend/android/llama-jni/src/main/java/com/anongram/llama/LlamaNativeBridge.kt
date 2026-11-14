package com.anongram.llama

fun interface LlamaTokenListener {
    fun onToken(token: String)
}

object LlamaNativeBridge {
    val isAvailable: Boolean by lazy {
        runCatching {
            System.loadLibrary("anongram_llama")
            true
        }.getOrDefault(false)
    }

    external fun nativeLoadModel(modelPath: String, nCtx: Int, nThreads: Int): Boolean
    external fun nativeUnloadModel()
    external fun nativeRequestStop()
    external fun nativeGenerate(
        roles: Array<String>,
        contents: Array<String>,
        temperature: Float,
        maxTokens: Int,
        listener: LlamaTokenListener
    ): Int
}
