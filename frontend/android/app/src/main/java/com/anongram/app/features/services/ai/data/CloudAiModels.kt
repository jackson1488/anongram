package com.anongram.app.features.services.ai.data

enum class AiEngineMode {
    LOCAL_GGUF,
    CLOUD_OPENAI
}

data class CloudModelInfo(
    val id: String,
    val displayName: String,
    val supportsVision: Boolean = true,
    val supportsImageOutput: Boolean = true,
    val supportsTools: Boolean = true
)

data class CloudProviderProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val models: List<CloudModelInfo> = emptyList(),
    val isPreset: Boolean = false
)

object CloudProviderPresets {
    fun defaultPresets(): List<CloudProviderProfile> = listOf(
        CloudProviderProfile(
            id = "iishko",
            name = "Iishko",
            baseUrl = "https://api.reformboss.com/v1",
            apiKey = "",
            isPreset = true,
            models = listOf(
                CloudModelInfo("auto", "Auto", supportsVision = true),
                CloudModelInfo("gpt-6-luna", "GPT-6 Luna", supportsVision = true),
                CloudModelInfo("gpt-5.6-luna", "GPT-5.6 Luna", supportsVision = true),
                CloudModelInfo("grok-chat-fast", "Grok Chat Fast", supportsVision = true),
                CloudModelInfo("gemini-auto", "Gemini Auto", supportsVision = true),
                CloudModelInfo("qwen-plus", "Qwen Plus", supportsVision = true),
                CloudModelInfo("qwen3.8-max", "Qwen 3.8 Max", supportsVision = true),
                CloudModelInfo("qwen3.7-max", "Qwen 3.7 Max", supportsVision = true),
                CloudModelInfo("qwen3.8-flash", "Qwen 3.8 Flash", supportsVision = true),
                CloudModelInfo("qwen3.8-27b", "Qwen 3.8 27B", supportsVision = true),
                CloudModelInfo("deepseek-ai/DeepSeek-V4.1-Flash", "DeepSeek V4.1 Flash", supportsVision = true),
                CloudModelInfo("deepseek-v4.1-flash-1017", "DeepSeek V4.1 Flash 1017", supportsVision = true),
                CloudModelInfo("deepseek-v4.1-flash", "DeepSeek V4.1 Flash", supportsVision = true),
                CloudModelInfo("glm-5.3", "GLM 5.3", supportsVision = true),
                CloudModelInfo("glm-5.3-flash", "GLM 5.3 Flash", supportsVision = true),
                CloudModelInfo("kimi-k3", "Kimi K3", supportsVision = true),
                CloudModelInfo("zai-org/GLM-5.3", "ZAI GLM 5.3", supportsVision = true)
            )
        ),
        CloudProviderProfile(
            id = "openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            apiKey = "",
            isPreset = true,
            models = listOf(
                CloudModelInfo("gpt-4o", "GPT-4o", supportsVision = true),
                CloudModelInfo("gpt-4o-mini", "GPT-4o mini", supportsVision = true),
                CloudModelInfo("o3-mini", "o3-mini", supportsVision = false)
            )
        ),
        CloudProviderProfile(
            id = "gemini",
            name = "Google Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            apiKey = "",
            isPreset = true,
            models = listOf(
                CloudModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", supportsVision = true),
                CloudModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", supportsVision = true)
            )
        ),
        CloudProviderProfile(
            id = "deepseek",
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = "",
            isPreset = true,
            models = listOf(
                CloudModelInfo("deepseek-chat", "DeepSeek Chat", supportsVision = false),
                CloudModelInfo("deepseek-reasoner", "DeepSeek Reasoner", supportsVision = false)
            )
        ),
        CloudProviderProfile(
            id = "openrouter",
            name = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = "",
            isPreset = true,
            models = listOf(
                CloudModelInfo("openai/gpt-4o-mini", "GPT-4o mini", supportsVision = true),
                CloudModelInfo("google/gemini-2.5-flash", "Gemini 2.5 Flash", supportsVision = true),
                CloudModelInfo("deepseek/deepseek-chat-v3-0324", "DeepSeek V3", supportsVision = false)
            )
        )
    )
}
