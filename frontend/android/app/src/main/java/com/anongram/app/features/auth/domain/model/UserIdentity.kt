package com.anongram.app.features.auth.domain.model

/**
 * Неизменяемая доменная модель децентрализованной идентичности пользователя в AnonGram.
 *
 * В архитектуре Zero-Knowledge у пользователя нет логина, почты или телефонного номера.
 * Идентичность формируется локально на основе криптографической мнемоники BIP-39.
 *
 * @property publicId Уникальный 32-значный hex-идентификатор пользователя в сети, вычисляемый как SHA-256(MasterVK).
 * @property nickname Публичный псевдоним (отображается только собеседникам в чатах).
 * @property avatarUri Локальный URI или относительный путь к выбранному аватару профиля.
 * @property masterVkHex Hex-представление публичного ключа верификации Ed25519 (Master Verification Key).
 * @property pqMasterVkHex Опциональный пост-квантовый публичный ключ ML-DSA-87 для квантово-устойчивой подписи.
 * @property isBiometricsEnabled Флаг активности биометрической разблокировки устройства.
 * @property isPinSet Флаг наличия установленного локального PIN-кода.
 */
data class UserIdentity(
    val publicId: String,
    val nickname: String,
    val avatarUri: String? = null,
    val masterVkHex: String,
    val pqMasterVkHex: String? = null,
    val isBiometricsEnabled: Boolean = false,
    val isPinSet: Boolean = false
)
