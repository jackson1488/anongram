package com.anongram.app.features.services.ai.data

import android.net.Uri

enum class AiReasoningLevel(val temperatureHint: Float) {
    OFF(0.5f),
    LOW(0.4f),
    MEDIUM(0.7f),
    HIGH(0.8f)
}

enum class AiSystemPromptPreset {
    DEFAULT,
    CODER,
    ANALYST,
    TRANSLATOR,
    CONCISE,
    CUSTOM
}

enum class AiModelCategory {
    ALL,
    GEMMA,
    DEEPSEEK,
    GPT_OSS,
    UNCENSORED
}

data class AiModelPreset(
    val id: String,
    val displayName: String,
    val repoId: String,
    val filename: String,
    val sizeMb: Int,
    val ramHint: String,
    val category: AiModelCategory = AiModelCategory.ALL,
    val supportsThinking: Boolean = false,
    val supportsVision: Boolean = false
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$repoId/resolve/main/$filename"
}

data class HfModelFile(
    val filename: String,
    val sizeBytes: Long,
    val downloadUrl: String
)

data class HfRepoSummary(
    val repoId: String,
    val downloads: Long,
    val likes: Int
)

data class InstalledAiModel(
    val id: String,
    val displayName: String,
    val filename: String,
    val sizeBytes: Long,
    val repoId: String? = null,
    val supportsThinking: Boolean = false,
    val supportsVision: Boolean = false
)

data class AiAttachment(
    val id: String,
    val name: String,
    val uriString: String,
    val isImage: Boolean,
    val mimeType: String,
    val textContent: String? = null
) {
    val uri: Uri get() = Uri.parse(uriString)
}

data class AiMessage(
    val id: String,
    val isUser: Boolean,
    val content: String,
    val thinking: String = "",
    val thinkingDurationSec: Float = 0f,
    val attachments: List<AiAttachment> = emptyList(),
    val searchStatus: String? = null,
    val webSources: List<WebSearchResult> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis()
)

data class AiChatSession(
    val id: String,
    val title: String,
    val isPinned: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis(),
    val messages: List<AiMessage> = emptyList()
)

object AiCatalog {
    val presets: List<AiModelPreset> = listOf(
        AiModelPreset(
            id = "smollm2_360m",
            displayName = "SmolLM2 360M",
            repoId = "HuggingFaceTB/SmolLM2-360M-Instruct-GGUF",
            filename = "smollm2-360m-instruct-q8_0.gguf",
            sizeMb = 386,
            ramHint = "2+ GB RAM"
        ),
        AiModelPreset(
            id = "qwen25_05b",
            displayName = "Qwen 2.5 0.5B",
            repoId = "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            filename = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            sizeMb = 398,
            ramHint = "3+ GB RAM"
        ),
        AiModelPreset(
            id = "gemma3_1b",
            displayName = "Gemma 3 1B",
            repoId = "ggml-org/gemma-3-1b-it-GGUF",
            filename = "gemma-3-1b-it-Q4_K_M.gguf",
            sizeMb = 768,
            ramHint = "3+ GB RAM",
            category = AiModelCategory.GEMMA
        ),
        AiModelPreset(
            id = "deepseek_r1_15b",
            displayName = "DeepSeek R1 1.5B",
            repoId = "bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
            filename = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_0.gguf",
            sizeMb = 1019,
            ramHint = "4+ GB RAM",
            category = AiModelCategory.DEEPSEEK,
            supportsThinking = true
        ),
        AiModelPreset(
            id = "qwen3_06b",
            displayName = "Qwen 3 0.6B (Think)",
            repoId = "Qwen/Qwen3-0.6B-GGUF",
            filename = "Qwen3-0.6B-Q8_0.gguf",
            sizeMb = 639,
            ramHint = "3+ GB RAM",
            supportsThinking = true
        ),
        AiModelPreset(
            id = "llama32_1b",
            displayName = "Llama 3.2 1B",
            repoId = "bartowski/Llama-3.2-1B-Instruct-GGUF",
            filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            sizeMb = 808,
            ramHint = "4+ GB RAM"
        ),
        AiModelPreset(
            id = "qwen25_15b",
            displayName = "Qwen 2.5 1.5B",
            repoId = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeMb = 1120,
            ramHint = "6+ GB RAM"
        ),
        AiModelPreset(
            id = "qwen3_17b",
            displayName = "Qwen 3 1.7B (Think)",
            repoId = "Qwen/Qwen3-1.7B-GGUF",
            filename = "Qwen3-1.7B-Q4_K_M.gguf",
            sizeMb = 1280,
            ramHint = "6+ GB RAM",
            supportsThinking = true
        ),
        AiModelPreset(
            id = "gpt_oss_20b",
            displayName = "GPT-OSS 20B",
            repoId = "unsloth/gpt-oss-20b-GGUF",
            filename = "gpt-oss-20b-Q4_0.gguf",
            sizeMb = 10968,
            ramHint = "16+ GB RAM",
            category = AiModelCategory.GPT_OSS
        ),
        // 5 проверенных лёгких моделей без цензуры для телефона
        AiModelPreset(
            id = "dolphin3_1b_uncensored",
            displayName = "Dolphin 3.0 Llama 3.2 1B",
            repoId = "bartowski/Dolphin3.0-Llama3.2-1B-GGUF",
            filename = "Dolphin3.0-Llama3.2-1B-Q4_0.gguf",
            sizeMb = 737,
            ramHint = "3+ GB RAM",
            category = AiModelCategory.UNCENSORED
        ),
        AiModelPreset(
            id = "qwen25_15b_abliterated",
            displayName = "Qwen 2.5 1.5B Abliterated",
            repoId = "mradermacher/Qwen2.5-1.5B-Instruct-abliterated-GGUF",
            filename = "Qwen2.5-1.5B-Instruct-abliterated.Q4_0_4_4.gguf",
            sizeMb = 891,
            ramHint = "3+ GB RAM",
            category = AiModelCategory.UNCENSORED
        ),
        AiModelPreset(
            id = "qwen25_coder_15b_abliterated",
            displayName = "Qwen 2.5 Coder 1.5B Abliterated",
            repoId = "bartowski/Qwen2.5-Coder-1.5B-Instruct-abliterated-GGUF",
            filename = "Qwen2.5-Coder-1.5B-Instruct-abliterated-Q4_0.gguf",
            sizeMb = 1019,
            ramHint = "4+ GB RAM",
            category = AiModelCategory.UNCENSORED
        ),
        AiModelPreset(
            id = "deepseek_r1_15b_abliterated",
            displayName = "DeepSeek R1 1.5B Abliterated",
            repoId = "mradermacher/DeepSeek-R1-Distill-Qwen-1.5B-abliterated-GGUF",
            filename = "DeepSeek-R1-Distill-Qwen-1.5B-abliterated.Q4_K_M.gguf",
            sizeMb = 1065,
            ramHint = "4+ GB RAM",
            category = AiModelCategory.UNCENSORED,
            supportsThinking = true
        ),
        AiModelPreset(
            id = "dolphin3_3b_uncensored",
            displayName = "Dolphin 3.0 Llama 3.2 3B",
            repoId = "bartowski/Dolphin3.0-Llama3.2-3B-GGUF",
            filename = "Dolphin3.0-Llama3.2-3B-Q4_0.gguf",
            sizeMb = 1832,
            ramHint = "6+ GB RAM",
            category = AiModelCategory.UNCENSORED
        )
    )
}
