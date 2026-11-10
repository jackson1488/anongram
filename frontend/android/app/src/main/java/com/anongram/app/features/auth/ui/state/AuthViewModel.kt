package com.anongram.app.features.auth.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anongram.app.features.auth.domain.model.AuthState
import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
import com.anongram.app.features.auth.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Единая ViewModel для управления состоянием модуля Регистрации и Аутентификации AnonGram.
 *
 * Связывает слои UI с доменным репозиторием [IAuthRepository]:
 * - Предоставляет реактивный поток [authState];
 * - Выполняет создание новой анонимной идентичности;
 * - Выполняет восстановление по мнемонике BIP-39;
 * - Фиксирует аккаунт в защищённом хранилище Android Keystore.
 *
 * @param authRepository Реализация репозитория аутентификации.
 */
class AuthViewModel(
    private val authRepository: IAuthRepository
) : ViewModel() {

    /**
     * Текущий статус аутентификации пользователя (Loading, Unauthenticated, Authenticated).
     */
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    /**
     * Создать новую анонимную личность с указанным никнеймом.
     */
    suspend fun createIdentity(nickname: String): Pair<UserIdentity, SeedPhrase> {
        return authRepository.createIdentity(nickname)
    }

    /**
     * Восстановить личность по мнемонической фразе.
     */
    suspend fun restoreAccount(seedPhrase: SeedPhrase): Result<UserIdentity> {
        val result = authRepository.restoreIdentity(seedPhrase)
        result.onSuccess { identity ->
            authRepository.saveIdentityToKeystore(identity, seedPhrase)
        }
        return result
    }

    /**
     * Зафиксировать созданную при регистрации личность в защищённом Keystore.
     */
    fun saveRegistration(identity: UserIdentity, seedPhrase: SeedPhrase) {
        viewModelScope.launch {
            authRepository.saveIdentityToKeystore(identity, seedPhrase)
        }
    }

    /**
     * Выход из аккаунта со стиранием всех локальных криптографических ключей.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
