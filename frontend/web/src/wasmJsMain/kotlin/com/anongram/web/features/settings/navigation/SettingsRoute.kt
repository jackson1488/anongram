package com.anongram.web.features.settings.navigation

sealed interface SettingsRoute {
    data object Main : SettingsRoute
    data object Search : SettingsRoute
    data object MyQr : SettingsRoute
    data object Account : SettingsRoute
    data object ChatSettings : SettingsRoute
    data object Privacy : SettingsRoute
    data object Security : SettingsRoute
    data object Notifications : SettingsRoute
    data object DataStorage : SettingsRoute
    data object Animations : SettingsRoute
    data object AnimationsMessages : SettingsRoute
    data object Devices : SettingsRoute
    data object Battery : SettingsRoute
    data object Language : SettingsRoute
    data object Modules : SettingsRoute
    data object ModulesVoice : SettingsRoute
    data object ModulesTranslation : SettingsRoute
}
