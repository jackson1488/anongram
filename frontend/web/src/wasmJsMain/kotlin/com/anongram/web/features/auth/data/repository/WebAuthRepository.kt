package com.anongram.web.features.auth.data.repository

import com.anongram.web.features.auth.data.crypto.Bip39WordList
import com.anongram.web.features.auth.data.crypto.KeyGenerator
import com.anongram.web.features.auth.domain.model.AuthState
import com.anongram.web.features.auth.domain.model.SeedPhrase
import com.anongram.web.features.auth.domain.model.UserIdentity
import com.anongram.web.features.auth.domain.repository.IAuthRepository
import com.anongram.web.platform.WebStorage
import com.anongram.web.platform.currentTimeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebAuthRepository : IAuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        loadSavedSync()
    }

    private fun loadSavedSync() {
        val publicId = WebStorage.getString(KEY_PUBLIC_ID)
        val nickname = WebStorage.getString(KEY_NICKNAME)
        val masterVk = WebStorage.getString(KEY_MASTER_VK)
        if (!publicId.isNullOrBlank() && !nickname.isNullOrBlank() && !masterVk.isNullOrBlank()) {
            _authState.value = AuthState.Authenticated(
                UserIdentity(
                    publicId = publicId,
                    nickname = nickname,
                    avatarUri = null,
                    masterVkHex = masterVk,
                    pqMasterVkHex = WebStorage.getString(KEY_PQ_VK),
                    isBiometricsEnabled = false,
                    isPinSet = WebStorage.getBoolean(KEY_PIN_SET, false)
                )
            )
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun checkExistingSession(): AuthState {
        loadSavedSync()
        return _authState.value
    }

    override suspend fun createIdentity(nickname: String): Pair<UserIdentity, SeedPhrase> {
        val cleanNickname = nickname.trim().ifEmpty { "Anon_${currentTimeMillis() % 10000}" }
        val seedPhrase = Bip39WordList.generate12Words()
        val identity = KeyGenerator.deriveIdentity(cleanNickname, seedPhrase)
        return identity to seedPhrase
    }

    override suspend fun restoreIdentity(seedPhrase: SeedPhrase): Result<UserIdentity> {
        for ((index, word) in seedPhrase.words.withIndex()) {
            if (!Bip39WordList.isValidWord(word)) {
                return Result.failure(
                    IllegalArgumentException("Слово #${index + 1} «$word» не найдено в словаре BIP-39")
                )
            }
        }
        return Result.success(KeyGenerator.deriveIdentity("AnonUser", seedPhrase))
    }

    override suspend fun saveIdentityToKeystore(identity: UserIdentity, seedPhrase: SeedPhrase, pin: String?) {
        WebStorage.putString(KEY_PUBLIC_ID, identity.publicId)
        WebStorage.putString(KEY_NICKNAME, identity.nickname)
        WebStorage.putString(KEY_MASTER_VK, identity.masterVkHex)
        identity.pqMasterVkHex?.let { WebStorage.putString(KEY_PQ_VK, it) }
        WebStorage.putString(KEY_SEED, seedPhrase.text)
        WebStorage.putBoolean(KEY_PIN_SET, !pin.isNullOrBlank())
        _authState.value = AuthState.Authenticated(identity.copy(isPinSet = !pin.isNullOrBlank()))
    }

    override suspend fun publishPassportToServer(identity: UserIdentity): Result<Unit> = Result.success(Unit)

    override suspend fun logout(): Result<Unit> {
        WebStorage.remove(KEY_PUBLIC_ID)
        WebStorage.remove(KEY_NICKNAME)
        WebStorage.remove(KEY_MASTER_VK)
        WebStorage.remove(KEY_PQ_VK)
        WebStorage.remove(KEY_SEED)
        WebStorage.remove(KEY_PIN_SET)
        _authState.value = AuthState.Unauthenticated
        return Result.success(Unit)
    }

    private companion object {
        const val KEY_PUBLIC_ID = "anongram_auth_public_id"
        const val KEY_NICKNAME = "anongram_auth_nickname"
        const val KEY_MASTER_VK = "anongram_auth_master_vk"
        const val KEY_PQ_VK = "anongram_auth_pq_vk"
        const val KEY_SEED = "anongram_auth_seed"
        const val KEY_PIN_SET = "anongram_auth_pin_set"
    }
}
