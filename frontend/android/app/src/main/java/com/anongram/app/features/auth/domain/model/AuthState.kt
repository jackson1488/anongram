package com.anongram.app.features.auth.domain.model

/**
 * Глобальное состояние аутентификации пользователя в приложении AnonGram.
 *
 * Используется в UI для переключения между экранами входа/регистрации и основным экраном приложения (MainAppShell).
 */
sealed interface AuthState {

    /**
     * Состояние инициализации и проверки сохранённой сессии в Android Keystore.
     */
    data object Loading : AuthState

    /**
     * Пользователь не авторизован (первый запуск приложения или выполнен логаут).
     * Должен отображаться AuthNavGraph (экраны приветствия, создания личности или восстановления).
     */
    data object Unauthenticated : AuthState

    /**
     * Пользователь успешно авторизован, ключи загружены в память.
     *
     * @property identity Данные текущей подтверждённой личности пользователя.
     */
    data class Authenticated(val identity: UserIdentity) : AuthState
}
