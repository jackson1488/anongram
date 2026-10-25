package com.anongram.app.features.chats.common

/**
 * Стратегия чат-сессии (Паттерн «Стратегия»).
 *
 * Direct / Group / OTP-Burn — независимые классы-стратегии. Карточка, панели
 * управления и экран чата рендерятся чисто полиморфно по свойствам интерфейса —
 * без ветвлений по типу сессии. Новые типы чатов добавляются как новый класс,
 * не затрагивая существующий UI.
 */
interface IChatSession {
    val id: String
    val title: String
    val fingerprint: String
    val lastMessagePreview: String
    val lastMessageEpochMs: Long
    val unreadCount: Int
    val isPinned: Boolean
    val isOnline: Boolean
    val isArchived: Boolean

    /** Уровень криптозащиты сессии. */
    val tier: ChatSecurityTier

    /** Короткая строка о шифровании для подписи карточки и шапки чата. */
    val securityLabel: String get() = tier.label

    /** Семантический глиф аватара стратегии; UI маппит его в иконку/монограмму. */
    val avatarGlyph: SessionGlyph

    /** Монограмма для глифа MONOGRAM; null для иконных глифов. */
    val avatarMonogram: String? get() = null

    /** true — удаление необратимо (физический OTP-пад). */
    val burnsWithEmbers: Boolean get() = false

    /** Подпись разрушительного действия в диалоге подтверждения. */
    val deleteActionLabel: String get() = "Удалить чат"

    /** Остаток страниц OTP-пада; актуально только для OTP-стратегии. */
    val padPagesLeft: Int? get() = null

    /**
     * Момент окончания мьюта: null — уведомления включены,
     * 0L — навсегда, иначе epoch millis.
     */
    val mutedUntil: Long?

    val isMuted: Boolean get() = mutedUntil != null

    fun withPinned(pinned: Boolean): IChatSession
    fun withArchived(archived: Boolean): IChatSession
    fun withMutedUntil(untilEpochMs: Long?): IChatSession

    /** Локальная очистка истории: превью и счётчик непрочитанных сбрасываются. */
    fun withHistoryCleared(): IChatSession
}
