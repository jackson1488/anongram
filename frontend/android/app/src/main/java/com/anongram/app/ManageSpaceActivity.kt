package com.anongram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.AnonGramTheme
import com.anongram.app.core.theme.SharedPrefsThemeRepository
import com.anongram.app.core.theme.ThemeController
import com.anongram.app.features.settings.data_storage.ui.DataStorageSettingsScreen
import com.anongram.app.features.settings.security.data.PasscodeStore
import com.anongram.app.features.settings.security.ui.components.PasscodeLockOverlay
import com.anongram.app.features.settings.security.ui.components.PasscodeScreenMode

/**
 * Изолированная Activity для системной кнопки «Управление местом / Очистить данные»
 * из настроек Android (android:manageSpaceActivity).
 *
 * При включённом код-пароле сначала требует его ввода и не открывает
 * экран «Данные и память» (и не читает размеры данных) до разблокировки.
 */
class ManageSpaceActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeController = remember {
                ThemeController(SharedPrefsThemeRepository(applicationContext))
            }
            val scheme by themeController.scheme.collectAsState()
            val lang by LanguageSettingsStore.language.collectAsState()
            val isRu = lang == AppLanguage.RU

            val requiresPasscode = remember { PasscodeStore.isPasscodeEnabled(applicationContext) }
            var isUnlocked by remember { mutableStateOf(!requiresPasscode) }

            AnonGramTheme(scheme = scheme) {
                if (!isUnlocked) {
                    PasscodeLockOverlay(
                        mode = PasscodeScreenMode.UNLOCK,
                        subtitleOverride = if (isRu) {
                            "Введите код-пароль, чтобы открыть «Данные и память»"
                        } else {
                            "Enter passcode to open Data and Storage"
                        },
                        onSuccess = { isUnlocked = true },
                        onBack = { finish() }
                    )
                } else {
                    DataStorageSettingsScreen(
                        onBack = { finish() },
                        allowClearAllLocalData = true,
                        onAllLocalDataCleared = { finish() }
                    )
                }
            }
        }
    }
}
