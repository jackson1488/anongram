package com.anongram.app.features.settings.navigation

/**
 * Маршруты навигации внутри модульного хаба «Настройки» (`features/settings/`).
 */
sealed interface SettingsRoute {
    data object Main : SettingsRoute
    data object Search : SettingsRoute
    data object MyQr : SettingsRoute
    data object QrScanner : SettingsRoute
    data object Account : SettingsRoute
    data object ChatSettings : SettingsRoute
    data object Privacy : SettingsRoute
    data object Security : SettingsRoute
    data object Notifications : SettingsRoute
    data object DataStorage : SettingsRoute
    data object Animations : SettingsRoute
    data object Devices : SettingsRoute
    data object Battery : SettingsRoute
    data object Language : SettingsRoute
    data object Modules : SettingsRoute
}
