package com.anongram.app.features.chats.viewer.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

/**
 * Словарь локализации для модуля просмотрщика медиа (RU + EN).
 */
data class ViewerI18nStrings(
    val isRussian: Boolean,
    val back: String,
    val forward: String,
    val settings: String,
    val menu: String,
    val saveToGallery: String,
    val showInChat: String,
    val reply: String,
    val share: String,
    val openIn: String,
    val delete: String,
    val showAllCaption: String,
    val collapseCaption: String,
    val counterOf: (Int, Int) -> String,
    val forwardDialogTitle: String,
    val forwardDialogMessage: String,
    val cancel: String,
    val forwardSingleMedia: String,
    val forwardAllMedia: (Int) -> String,
    val speedTitle: String,
    val speedVerySlow: String,
    val speedSlow: String,
    val speedNormal: String,
    val speedFast: String,
    val speedVeryFast: String,
    val loopVideo: String,
    val shareMediaChooser: String,
    val openWithChooser: String,
    val noAppToOpen: String,
    val todayAt: (String) -> String,
    val yesterdayAt: (String) -> String,
    val weekdayOrDateAt: (String, String) -> String,
    val secondsUnit: String,
    val play: String,
    val pause: String,
    val toggleFill: String
)

private val RuViewerStrings = ViewerI18nStrings(
    isRussian = true,
    back = "Назад",
    forward = "Переслать",
    settings = "Настройки видео",
    menu = "Меню",
    saveToGallery = "Сохранить в галерею",
    showInChat = "Показать в чате",
    reply = "Ответить",
    share = "Поделиться",
    openIn = "Открыть в...",
    delete = "Удалить",
    showAllCaption = "показать полностью...",
    collapseCaption = "свернуть",
    counterOf = { current, total -> "$current из $total" },
    forwardDialogTitle = "Переслать медиа",
    forwardDialogMessage = "Вы хотите переслать...",
    cancel = "Отмена",
    forwardSingleMedia = "1 медиафайл",
    forwardAllMedia = { count -> "$count медиа" },
    speedTitle = "Скорость",
    speedVerySlow = "Очень медленно",
    speedSlow = "Медленно",
    speedNormal = "Стандартно",
    speedFast = "Быстро",
    speedVeryFast = "Очень быстро",
    loopVideo = "Зациклить",
    shareMediaChooser = "Поделиться медиа",
    openWithChooser = "Открыть с помощью",
    noAppToOpen = "Нет подходящего приложения для открытия",
    todayAt = { time -> "сегодня в $time" },
    yesterdayAt = { time -> "вчера в $time" },
    weekdayOrDateAt = { dayOrDate, time -> "$dayOrDate в $time" },
    secondsUnit = "с",
    play = "Воспроизвести",
    pause = "Пауза",
    toggleFill = "Масштаб экрана"
)

private val EnViewerStrings = ViewerI18nStrings(
    isRussian = false,
    back = "Back",
    forward = "Forward",
    settings = "Video settings",
    menu = "Menu",
    saveToGallery = "Save to gallery",
    showInChat = "Show in chat",
    reply = "Reply",
    share = "Share",
    openIn = "Open in...",
    delete = "Delete",
    showAllCaption = "show more...",
    collapseCaption = "collapse",
    counterOf = { current, total -> "$current of $total" },
    forwardDialogTitle = "Forward media",
    forwardDialogMessage = "Do you want to forward...",
    cancel = "Cancel",
    forwardSingleMedia = "1 media file",
    forwardAllMedia = { count -> "$count media" },
    speedTitle = "Speed",
    speedVerySlow = "Very slow",
    speedSlow = "Slow",
    speedNormal = "Normal",
    speedFast = "Fast",
    speedVeryFast = "Very fast",
    loopVideo = "Loop",
    shareMediaChooser = "Share media",
    openWithChooser = "Open with",
    noAppToOpen = "No app found to open this file",
    todayAt = { time -> "today at $time" },
    yesterdayAt = { time -> "yesterday at $time" },
    weekdayOrDateAt = { dayOrDate, time -> "$dayOrDate at $time" },
    secondsUnit = "s",
    play = "Play",
    pause = "Pause",
    toggleFill = "Toggle fill"
)

val LocalViewerStrings = staticCompositionLocalOf { RuViewerStrings }

object ViewerI18n {
    val current: ViewerI18nStrings
        @Composable
        @ReadOnlyComposable
        get() = if (LanguageSettingsStore.language == AppLanguage.RU) RuViewerStrings else EnViewerStrings
}
