package com.anongram.llama

fun interface LlamaTokenListener {
    fun onToken(token: String)
}

object LlamaNativeBridge {
    @Volatile
    private var loaded: Boolean = false

    val isAvailable: Boolean
        get() {
            if (loaded) return true
            return runCatching {
                System.loadLibrary("anongram_llama")
                loaded = true
                true
            }.getOrDefault(false)
        }

    fun loadFromPath(absolutePath: String): Boolean {
        if (loaded) return true
        return runCatching {
            System.load(absolutePath)
            loaded = true
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
