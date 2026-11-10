package com.anongram.app.features.auth.data.repository

import android.content.Context
import com.anongram.app.features.auth.data.crypto.Bip39WordList
import com.anongram.app.features.auth.data.crypto.KeyGenerator
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.auth.domain.model.AuthState
import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
import com.anongram.app.features.auth.domain.repository.IAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Основная реализация интерфейса [IAuthRepository].
 *
 * Координирует:
 * - Взаимодействие с аппаратным хранилищем [KeystoreVault];
 * - Криптографическую деривацию через [KeyGenerator] и [Bip39WordList];
 * - Реактивное уведомление UI через [StateFlow] о смене статуса аутентификации;
 * - Публикацию открытого паспорта на Fastify бэкенд слепого релея.
 *
 * @param context Контекст приложения.
 * @param vault Аппаратно-защищённое хранилище учетных записей.
 */
class AuthRepositoryImpl(
    context: Context,
    private val vault: KeystoreVault = KeystoreVault(context)
) : IAuthRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        scope.launch {
            checkExistingSession()
        }
    }

    /**
     * Проверяет наличие сохранённой сессии при старте приложения.
     */
    override suspend fun checkExistingSession(): AuthState = withContext(Dispatchers.IO) {
        if (vault.hasSession()) {
            val identity = vault.loadIdentity()
            if (identity != null) {
                val state = AuthState.Authenticated(identity)
                _authState.value = state
                return@withContext state
            }
        }
        val unauthState = AuthState.Unauthenticated
        _authState.value = unauthState
        unauthState
    }

    /**
     * Генерирует новую анонимную идентичность: 12 слов BIP-39, ключи Ed25519 и Public ID.
     */
    override suspend fun createIdentity(nickname: String): Pair<UserIdentity, SeedPhrase> =
        withContext(Dispatchers.Default) {
            val cleanNickname = nickname.trim().ifEmpty { "Anon_${System.currentTimeMillis() % 10000}" }
            val seedPhrase = Bip39WordList.generate12Words()
            val identity = KeyGenerator.deriveIdentity(cleanNickname, seedPhrase)
            Pair(identity, seedPhrase)
        }

    /**
     * Восстанавливает идентичность по введённой мнемонике после валидации каждого слова по словарю.
     */
    override suspend fun restoreIdentity(seedPhrase: SeedPhrase): Result<UserIdentity> =
        withContext(Dispatchers.Default) {
            try {
                // Валидация каждого слова по словарю BIP-39
                for ((index, word) in seedPhrase.words.withIndex()) {
                    if (!Bip39WordList.isValidWord(word)) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Слово #${index + 1} «$word» не найдено в словаре BIP-39")
                        )
                    }
                }

                // Восстановление профиля
                val identity = KeyGenerator.deriveIdentity("AnonUser", seedPhrase)
                Result.success(identity)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Сохраняет идентичность и мнемонику в Keystore и обновляет статус приложения на Authenticated.
     */
    override suspend fun saveIdentityToKeystore(
        identity: UserIdentity,
        seedPhrase: SeedPhrase,
        pin: String?
    ): Unit = withContext(Dispatchers.IO) {
        vault.saveIdentity(identity, seedPhrase, pin)
        _authState.value = AuthState.Authenticated(identity)
        // Фоновая публикация паспорта на сервер
        scope.launch {
            publishPassportToServer(identity)
        }
        Unit
    }

    /**
     * Публикация паспорта пользователя на слепой релей (/api/v1/passport).
     */
    override suspend fun publishPassportToServer(identity: UserIdentity): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val serverUrl = URL("http://127.0.0.1:3000/api/v1/passport")
                val connection = (serverUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 3000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }

                val jsonPayload = """
                    {
                        "publicId": "${identity.publicId}",
                        "masterVk": "${identity.masterVkHex}",
                        "nickname": "${identity.nickname}",
                        "pqMasterVk": ${identity.pqMasterVkHex?.let { "\"$it\"" } ?: "null"}
                    }
                """.trimIndent()

                connection.outputStream.use { os ->
                    OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
                        writer.write(jsonPayload)
                        writer.flush()
                    }
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Сервер вернул код $responseCode"))
                }
            } catch (e: Exception) {
                // Если сервер временно оффлайн, работа клиента продолжается в локальном автономном режиме
                Result.failure(e)
            }
        }

    /**
     * Безопасный логаут со стиранием всех локальных криптографических ключей.
     */
    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            vault.clear()
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
