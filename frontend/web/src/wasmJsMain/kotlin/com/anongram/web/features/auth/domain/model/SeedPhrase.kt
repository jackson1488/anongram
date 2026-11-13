package com.anongram.web.features.auth.domain.model

data class SeedPhrase(
    val words: List<String>
) {
    init {
        require(words.size == 12 || words.size == 24) {
            "Мнемоника BIP-39 должна содержать ровно 12 или 24 слова, получено: ${words.size}"
        }
    }

    val text: String get() = words.joinToString(" ")

    fun wordAt(index: Int): String {
        require(index in 1..words.size) { "Индекс слова должен быть от 1 до ${words.size}" }
        return words[index - 1]
    }
}
