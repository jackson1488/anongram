package com.anongram.app.features.auth.domain.model

/**
 * Криптографическая мнемоническая фраза по стандарту BIP-39.
 *
 * Является единственным источником мастер-ключа для децентрализованной учетной записи AnonGram.
 * Из 12 слов генерируется 512-битный seed, из которого затем деривируются ключи Ed25519 и ML-DSA-87.
 *
 * @property words Список слов мнемоники (строго 12 или 24 слова).
 */
data class SeedPhrase(
    val words: List<String>
) {
    init {
        require(words.size == 12 || words.size == 24) {
            "Мнемоника BIP-39 должна содержать ровно 12 или 24 слова, получено: ${words.size}"
        }
    }

    /**
     * Текстовое представление фразы, разделённое пробелами.
     */
    val text: String get() = words.joinToString(" ")

    /**
     * Получить слово по 1-индексированному номеру (1..12).
     *
     * @param index Номер слова от 1 до размера фразы.
     * @return Слово мнемоники.
     */
    fun wordAt(index: Int): String {
        require(index in 1..words.size) { "Индекс слова должен быть от 1 до ${words.size}" }
        return words[index - 1]
    }
}
