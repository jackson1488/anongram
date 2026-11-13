package com.anongram.web.features.auth.domain.repository

import com.anongram.web.features.auth.domain.model.AuthState
import com.anongram.web.features.auth.domain.model.SeedPhrase
import com.anongram.web.features.auth.domain.model.UserIdentity
import kotlinx.coroutines.flow.StateFlow

interface IAuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun checkExistingSession(): AuthState
    suspend fun createIdentity(nickname: String): Pair<UserIdentity, SeedPhrase>
    suspend fun restoreIdentity(seedPhrase: SeedPhrase): Result<UserIdentity>
    suspend fun saveIdentityToKeystore(identity: UserIdentity, seedPhrase: SeedPhrase, pin: String? = null)
    suspend fun publishPassportToServer(identity: UserIdentity): Result<Unit>
    suspend fun logout(): Result<Unit>
}
