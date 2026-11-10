# Журнал изменений и Архитектурная документация модуля Auth

## 1. Общие сведения
- **Модуль**: `com.anongram.app.features.auth`
- **Версия**: 1.0.0
- **Статус**: Реализован и интегрирован в `MainActivity.kt`
- **Философия**: Zero-Knowledge, децентрализованная крипто-идентичность без номеров телефонов, почты и паролей.

---

## 2. Структура созданных файлов и папок

```
frontend/android/app/src/main/java/com/anongram/app/features/auth/
├── AUTH_CHANGELOG.md                         # Данный журнал изменений и архитектурный справочник
│
├── domain/                                   # Доменный слой (Clean Architecture)
│   ├── model/
│   │   ├── UserIdentity.kt                   # Модель личности (publicId, nickname, avatarUri, masterVkHex, pqMasterVkHex)
│   │   ├── AuthState.kt                      # Sealed-состояния: Loading, Unauthenticated, Authenticated
│   │   └── SeedPhrase.kt                     # Модель мнемоники BIP-39 (12 слов, валидация, форматирование)
│   └── repository/
│       └── IAuthRepository.kt                # Интерфейс репозитория аутентификации
│
├── data/                                     # Слой данных и криптографии
│   ├── crypto/
│   │   ├── Bip39WordList.kt                  # 2048 слов BIP-39, генератор SecureRandom, автодополнение
│   │   ├── KeyGenerator.kt                   # PBKDF2-HMAC-SHA512, Ed25519 Curve25519, Public ID = SHA256(MasterVK)
│   │   └── KeystoreVault.kt                  # Аппаратное хранилище Android Keystore (AES-256-GCM)
│   └── repository/
│       └── AuthRepositoryImpl.kt             # Реализация IAuthRepository с публикацией на Fastify релей
│
└── ui/                                       # Слой пользовательского интерфейса Jetpack Compose
    ├── WelcomeScreen.kt                      # Шлюз верхнего уровня к экрану приветствия
    ├── welcome/
    │   ├── WelcomeScreen.kt                  # Экран первого запуска: неоновый щит, карточки приватности, навигация
    │   └── WelcomeShieldAnimation.kt         # Анимированный неоновый щит AnonGram на Canvas с пульсацией ореола
    │
    ├── registration/
    │   ├── CreateIdentityScreen.kt           # Ввод никнейма, неоновые аватары, анимация крипто-конвейера
    │   └── BackupSeedScreen.kt               # Сетка 3x4 из 12 слов, предупреждение, квиз памяти 3-го и 7-го слова
    │
    ├── login/
    │   └── RestoreAccountScreen.kt           # Ввод 12 слов с автокомплитом BIP-39 и быстрой вставкой из буфера
    │
    ├── state/
    │   └── AuthViewModel.kt                  # ViewModel управления сессией, регистрацией и восстановлением
    │
    └── navigation/
        ├── AuthNavGraph.kt                   # Навигационный граф переходов между экранами с BackHandler
        └── AuthViewModel.kt                  # Псевдоним доступа к ViewModel из пакета навигации
```

Файлы интеграции:
- `frontend/android/app/src/main/java/com/anongram/app/MainActivity.kt` — бесшовная точка входа, проверка сессии, переключение между `AuthNavGraph` и `MainAppShell`.
- `frontend/android/app/src/main/java/com/anongram/app/features/profile/ui/ProfileScreen.kt` — отображение деривированного `publicId`, никнейма и кнопка безопасного выхода.

---

## 3. Криптографические алгоритмы и протоколы

### 3.1. Генерация мнемоники BIP-39 (`Bip39WordList.kt`)
- Официальный английский словарь BIP-39 из 2048 слов.
- Генерация 12 слов через криптографически стойкий генератор `java.security.SecureRandom`.
- Быстрый поиск вариантов автодополнения слов при вводе в поле восстановления `findSuggestions(prefix)`.
- Валидация каждого слова на принадлежность словарю `isValidWord(word)`.

### 3.2. Деривация ключей (`KeyGenerator.kt`)
1. **Master Seed**:
   - Алгоритм: `PBKDF2-HMAC-SHA512`.
   - Пароль: текст 12 слов мнемоники через пробел (UTF-8).
   - Соль: `"mnemonic"` (стандарт BIP-39).
   - Итерации: `2048`.
   - Размер выхода: `64 байта` (512 бит).
2. **Мастер-ключ Ed25519 (RFC 8032)**:
   - Приватный ключ: первые 32 байта мастер-сида.
   - Клампинг скаляра: `a[0] &= 248`, `a[31] &= 127`, `a[31] |= 64`.
   - Точка публичного ключа $A = a \cdot B$ на эллиптической кривой Эдвардса Curve25519 (модуль $P = 2^{255} - 19$).
   - Сжатие точки: координата $Y$ в формате little-endian (32 байта) со знаком $X$ в старшем бите.
3. **Public ID**:
   - `publicId = SHA-256(MasterVK).take(32)` (первые 32 символа шестнадцатеричного дайджеста).
4. **Пост-квантовая подготовка ML-DSA-87**:
   - Детерминированная деривация от второй половины 512-битного сида (`masterSeed[32..63]`).

### 3.3. Аппаратная защита Android Keystore (`KeystoreVault.kt`)
- Мастер-ключ создаётся в аппаратном модуле `AndroidKeyStore` (TEE / StrongBox) с флагом `KeyProperties.KEY_ALGORITHM_AES`, размер 256 бит.
- Режим шифрования: `AES/GCM/NoPadding` со случайным 12-байтным вектором инициализации (IV).
- Сохраняемые данные: `UserIdentity` (publicId, nickname, avatarUri, masterVk, pqMasterVk) и зашифрованная строка мнемоники `SeedPhrase`.

---

## 4. Пользовательские сценарии и переходы UI

1. **Запуск приложения**:
   - `MainActivity` запрашивает `checkExistingSession()`.
   - Если сессия найдена -> мгновенный вход в `MainAppShell`.
   - Если не найдена -> открытие экрана `WelcomeScreen`.
2. **Сценарий регистрации**:
   - `WelcomeScreen` -> кнопка «Создать анонимную личность» -> `CreateIdentityScreen`.
   - Пользователь вводит ник, выбирает неоновый аватар, наблюдает анимацию деривации ключей.
   - Нажатие «Продолжить к сохранению Seed-фразы» -> `BackupSeedScreen`.
   - Просмотр 12 слов, возможность копирования в буфер обмена.
   - Нажатие «Я сохранил слова» -> запуск диалога **Memory Quiz**: проверка знания 3-го и 7-го слова.
   - После успешного ответа — ключи сохраняются в `KeystoreVault`, и приложение переходит в `MainAppShell`.
3. **Сценарий входа / восстановления**:
   - `WelcomeScreen` -> кнопка «Войти по Seed-фразе» -> `RestoreAccountScreen`.
   - 12 слотов для ввода слов с подсказками BIP-39 на лету.
   - Кнопка «Вставить из буфера» для моментального заполнения всех 12 слотов.
   - Валидация слов -> деривация идентичности -> сохранение в `KeystoreVault` -> переход в `MainAppShell`.
