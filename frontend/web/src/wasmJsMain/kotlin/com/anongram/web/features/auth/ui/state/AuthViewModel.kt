package com.anongram.web.features.auth.ui.state

import com.anongram.web.features.auth.domain.model.AuthState
import com.anongram.web.features.auth.domain.model.SeedPhrase
import com.anongram.web.features.auth.domain.model.UserIdentity
import com.anongram.web.features.auth.domain.repository.IAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: IAuthRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    val authState: StateFlow<AuthState> = authRepository.authState

    suspend fun createIdentity(nickname: String): Pair<UserIdentity, SeedPhrase> {
        return authRepository.createIdentity(nickname)
    }

    suspend fun restoreAccount(seedPhrase: SeedPhrase): Result<UserIdentity> {
        val result = authRepository.restoreIdentity(seedPhrase)
        result.onSuccess { identity ->
            authRepository.saveIdentityToKeystore(identity, seedPhrase)
        }
        return result
    }

    fun saveRegistration(identity: UserIdentity, seedPhrase: SeedPhrase) {
        scope.launch {
            authRepository.saveIdentityToKeystore(identity, seedPhrase)
        }
    }

    fun logout() {
        scope.launch {
            authRepository.logout()
        }
    }
}
