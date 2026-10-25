package com.anongram.app.features.chats.common

/** Уровень криптозащиты сессии. Чисто доменная модель — без UI-зависимостей. */
enum class ChatSecurityTier(val label: String) {
    STANDARD("X25519 · ChaCha20-Poly1305"),
    POST_QUANTUM("ML-KEM-1024 · Double Ratchet"),
    OTP_PAD("OTP · Physical Pad")
}
