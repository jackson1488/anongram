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

data class AiModelPreset(
    val id: String,
    val displayName: String,
    val repoId: String,
    val filename: String,
    val sizeMb: Int,
    val ramHint: String,
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
            id = "qwen25_coder_15b",
            displayName = "Qwen 2.5 Coder 1.5B",
            repoId = "Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF",
            filename = "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
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
            id = "smolvlm_500m",
            displayName = "SmolVLM 500M (Vision)",
            repoId = "ggml-org/SmolVLM-500M-Instruct-GGUF",
            filename = "SmolVLM-500M-Instruct-Q8_0.gguf",
            sizeMb = 520,
            ramHint = "4+ GB RAM",
            supportsVision = true
        )
    )
}
