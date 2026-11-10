package com.anongram.app.features.auth.domain.repository

import com.anongram.app.features.auth.domain.model.AuthState
import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
import kotlinx.coroutines.flow.StateFlow

/**
 * Основной интерфейс репозитория аутентификации и управления децентрализованной идентичностью.
 *
 * Отвечает за:
 * - Проверку существующей сессии пользователя при запуске приложения;
 * - Создание новой анонимной идентичности с генерацией BIP-39 мнемоники;
 * - Восстановление доступа по введённой мнемонике;
 * - Сохранение ключей в защищённое аппаратное хранилище Android Keystore;
 * - Публикацию крипто-паспорта на слепой релей бэкенда;
 * - Безопасный логаут с очисткой ключевого материала.
 */
interface IAuthRepository {

    /**
     * Реактивный поток текущего состояния авторизации в приложении.
     */
    val authState: StateFlow<AuthState>

    /**
     * Проверить наличие сохранённой активной сессии в защищённом хранилище.
     *
     * @return Состояние [AuthState.Authenticated], если ключи найдены, или [AuthState.Unauthenticated].
     */
    suspend fun checkExistingSession(): AuthState

    /**
     * Создать новую анонимную личность с указанным псевдонимом.
     * Генерирует 12 слов BIP-39 и деривирует мастер-ключи.
     *
     * @param nickname Выбранный пользователем псевдоним.
     * @return Пара из созданной модели [UserIdentity] и новой мнемоники [SeedPhrase].
     */
    suspend fun createIdentity(nickname: String): Pair<UserIdentity, SeedPhrase>

    /**
     * Восстановить идентичность по введённой мнемонической фразе BIP-39.
     *
     * @param seedPhrase Введённые 12 слов.
     * @return [Result] с восстановленной моделью [UserIdentity] или ошибкой, если мнемоника невалидна.
     */
    suspend fun restoreIdentity(seedPhrase: SeedPhrase): Result<UserIdentity>

    /**
     * Зафиксировать идентичность и мнемонику в зашифрованном локальном хранилище (Keystore).
     *
     * @param identity Модель личности.
     * @param seedPhrase Мнемоническая фраза.
     * @param pin Необязательный локальный PIN-код (если настроен).
     */
    suspend fun saveIdentityToKeystore(identity: UserIdentity, seedPhrase: SeedPhrase, pin: String? = null)

    /**
     * Опубликовать подписанный криптографический паспорт пользователя на сервер слепого релея.
     *
     * @param identity Личность пользователя с MasterVK.
     * @return Успешный результат или сетевая ошибка.
     */
    suspend fun publishPassportToServer(identity: UserIdentity): Result<Unit>

    /**
     * Выполнить безопасный выход из аккаунта с полным стиранием локального мастер-ключа.
     *
     * @return Результат операции очистки.
     */
    suspend fun logout(): Result<Unit>
}
