package com.anongram.app.features.settings.animations.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

data class AnimationsI18nStrings(
    val isRussian: Boolean,
    val screenTitle: String,
    val messagesSectionTitle: String,
    val messagesAppearToggleTitle: String,
    val messagesAppearToggleDesc: String,
    val messageStyleHeader: String,
    val styleSpringTitle: String,
    val styleSpringDesc: String,
    val styleSlideTitle: String,
    val styleSlideDesc: String,
    val stylePopTitle: String,
    val stylePopDesc: String,
    val styleStreamTitle: String,
    val styleStreamDesc: String,
    val streamSpeedHeader: String,
    val speedSlow: String,
    val speedMedium: String,
    val speedFast: String,
    val speedTurbo: String,
    val listShiftToggleTitle: String,
    val listShiftToggleDesc: String,
    val statusCheckToggleTitle: String,
    val statusCheckToggleDesc: String,
    val previewHeader: String,
    val previewReplayBtn: String,
    val previewIncomingSample: String,
    val previewOutgoingSample: String,
    val messagesRowDisabled: String,
    val deleteStyleHeader: String,
    val deleteParticlesTitle: String,
    val deleteParticlesDesc: String,
    val deletePuzzleTitle: String,
    val deletePuzzleDesc: String,
    val deleteSlideTitle: String,
    val deleteSlideDesc: String,
    val deleteNoneTitle: String,
    val deleteNoneDesc: String
)

private val RuAnimationsStrings = AnimationsI18nStrings(
    isRussian = true,
    screenTitle = "Анимации",
    messagesSectionTitle = "Сообщения в чате",
    messagesAppearToggleTitle = "Анимация отправки и получения",
    messagesAppearToggleDesc = "Плавное появление новых исходящих и входящих сообщений",
    messageStyleHeader = "Стиль появления",
    styleSpringTitle = "Telegram-пружина",
    styleSpringDesc = "Мягкий вылет снизу с лёгкой пружиной",
    styleSlideTitle = "Всплытие",
    styleSlideDesc = "Чистый подъём с плавным появлением",
    stylePopTitle = "Поп от угла",
    stylePopDesc = "Упругое раскрытие от хвостика пузыря",
    styleStreamTitle = "AI-стриминг (токен за токеном)",
    styleStreamDesc = "Плавная печать текста слово за словом как у нейросети",
    streamSpeedHeader = "Скорость токенов стриминга",
    speedSlow = "Медленно",
    speedMedium = "Средне",
    speedFast = "Быстро",
    speedTurbo = "Турбо",
    listShiftToggleTitle = "Плавный сдвиг ленты",
    listShiftToggleDesc = "Мягкое смещение истории при появлении нового сообщения",
    statusCheckToggleTitle = "Анимация галочек (✓ → ✓✓)",
    statusCheckToggleDesc = "Плавный отклик при отправке и прочтении",
    previewHeader = "Предпросмотр",
    previewReplayBtn = "Проверить",
    previewIncomingSample = "Привет! Как работает стриминг сообщений в AnonGram?",
    previewOutgoingSample = "Всё летает локально, токен за токеном и без задержек!",
    messagesRowDisabled = "Отключено",
    deleteStyleHeader = "Анимация удаления сообщений",
    deleteParticlesTitle = "Щелчок Таноса (частицы)",
    deleteParticlesDesc = "Распад сообщения на микро-пыль как в Telegram",
    deletePuzzleTitle = "Пазл",
    deletePuzzleDesc = "Пузырь рассыпается на плитки, разъезжающиеся в стороны",
    deleteSlideTitle = "Уезд в сторону",
    deleteSlideDesc = "Плавный сдвиг влево или вправо с затуханием",
    deleteNoneTitle = "Без анимации",
    deleteNoneDesc = "Мгновенное удаление без эффектов"
)

private val EnAnimationsStrings = AnimationsI18nStrings(
    isRussian = false,
    screenTitle = "Animations",
    messagesSectionTitle = "Chat messages",
    messagesAppearToggleTitle = "Send & receive animation",
    messagesAppearToggleDesc = "Smooth entrance for new outgoing and incoming messages",
    messageStyleHeader = "Appearance style",
    styleSpringTitle = "Telegram Spring",
    styleSpringDesc = "Smooth fly-up from bottom with gentle spring",
    styleSlideTitle = "Slide & Fade",
    styleSlideDesc = "Clean upward slide with soft fade-in",
    stylePopTitle = "Corner Pop",
    stylePopDesc = "Bouncy scale expansion from bubble corner",
    styleStreamTitle = "AI Streaming (token by token)",
    styleStreamDesc = "Smooth word-by-word reveal like an AI assistant",
    streamSpeedHeader = "Token streaming speed",
    speedSlow = "Slow",
    speedMedium = "Medium",
    speedFast = "Fast",
    speedTurbo = "Turbo",
    listShiftToggleTitle = "Smooth list shift",
    listShiftToggleDesc = "Animate message list positions when new messages arrive",
    statusCheckToggleTitle = "Checkmark animation (✓ → ✓✓)",
    statusCheckToggleDesc = "Smooth transition on message send and read",
    previewHeader = "Preview",
    previewReplayBtn = "Replay",
    previewIncomingSample = "Hey! How does message streaming work in AnonGram?",
    previewOutgoingSample = "Everything runs smoothly, token by token with zero lag!",
    messagesRowDisabled = "Disabled",
    deleteStyleHeader = "Message deletion animation",
    deleteParticlesTitle = "Thanos Snap (Particles)",
    deleteParticlesDesc = "Disintegrate the message bubble into drifting dust",
    deletePuzzleTitle = "Puzzle",
    deletePuzzleDesc = "Split the bubble into tiles sliding left and right",
    deleteSlideTitle = "Slide out",
    deleteSlideDesc = "Smoothly slide left or right with fade-out",
    deleteNoneTitle = "None",
    deleteNoneDesc = "Instant deletion without extra effects"
)

object AnimationsI18n {
    val current: AnimationsI18nStrings
        @Composable
        get() {
            val lang = LanguageSettingsStore.language.collectAsState().value
            return if (lang == AppLanguage.RU) RuAnimationsStrings else EnAnimationsStrings
        }
}
