# AnonGram: Полная энциклопедия проекта, история создания и Архитектурная спецификация Регистрации / Входа (Jetpack Compose + Core)

> **Agent Handoff & Architecture Master Specification**  
> Проект: **AnonGram (АнонГрам)** — Zero-Knowledge Blind-Relay мессенджер с пост-квантовой защитой  
> Дата: 10 ноября 2026 г.  
> Версия: 2.0 (Complete System Reference & Auth Roadmap)

---

## 1. Миссия и философия AnonGram

**AnonGram** создаётся как бескомпромиссная альтернатива современным централизованным и псевдо-защищённым мессенджерам. 

### Главные инварианты протокола и проекта
1. **Слепой релей (Zero-Knowledge Blind Relay)**:
   - Сервер **никогда не знает** реальных номеров телефонов, email, паролей, открытых текстов сообщений или связей между пользователями.
   - Сервер выступает исключительно в роли «слепого почтового отделения», маршрутизируя сквозным образом зашифрованные конверты (`EncryptedEnvelope`) по обезличенному `public_id` адресата.
2. **Абсолютная анонимность пересылки (AnonGram Core Rule)**:
   - Пересылка сообщений ВСЕГДА на 100% анонимна: `forwardedFrom = null`.
   - В интерфейсе баблов и базы данных строго запрещены любые плашки и метаданные вида «Переслано от ...».
3. **Пост-квантовая гибридная криптография**:
   - Идентификатор пользователя `public_id` формируется как Hex SHA-256 от гибридного мастер-ключа верификации (`Ed25519` + `ML-DSA-87`).
   - Обмен сессионными ключами строится на гибриде `X25519` + `ML-KEM`.
4. **СТРОЖАЙШИЕ ПРАВИЛА СБОРКИ И ДЕПЛОЯ (ОБЯЗАТЕЛЬНО ДЛЯ ВСЕХ АГЕНТОВ)**:
   - **СТРОГО ЗАПРЕЩЕНО запускать локальную сборку (`./gradlew`) на компьютере пользователя**.
   - Все сборки Android APK выполняются **исключительно через облачный CI (GitHub Actions)**:
     1. `git add -A && git commit -m "..." && git push origin main`
     2. Мониторинг Cloud CI через GitHub API (скрипт `scratch/download_apk.py` / `monitor_and_download.py`)
     3. Скачивание артефакта APK `app-debug.apk` в папку `build-artifacts/debug/`
   - **НИКОГДА не запускать приложение автоматически после установки**:
     - Выполнять ТОЛЬКО тихую установку обновления: `adb -t 1 install -r -d build-artifacts/debug/app-debug.apk`
     - **ЗАПРЕЩЕНО** вызывать команды `am start`, `monkey` и любые другие лаунчеры. Пользователь откроет приложение сам на своём телефоне, когда ему будет удобно!

---

## 2. Полная хронологическая история разработки проекта

AnonGram прошёл огромный путь последовательной эволюции:

### Этап 1. Ядро сервера слепого релея (Backend Fastify)
- Разработан ультра-быстрый бэкенд на **Fastify + WebSockets + SQLite**:
  - `backend/src/index.ts`: Точка входа, WebSocket сервер `/ws`, эндпоинты `/health` и `/api/v1/passport`.
  - `SqlitePassportStorage`: Персистентное хранилище подписанных паспортов пользователей с защитой от отката версий (`downgrade attack`).
  - `BlindRelayRouter`: Маршрутизатор зашифрованных конвертов `EncryptedEnvelope` и сигналов звонков `CallSignalMessage` в реальном времени.

### Этап 2. Архитектура Android-клиента (Jetpack Compose & Obsidian Theme)
- Клиент построен на современных принципах Android Clean Architecture:
  - **Edge-to-Edge**: Полноэкранный рендеринг под статус-баром и навигационной полосой.
  - **WindowSizeClass**: Адаптивность интерфейса под смартфоны, складные устройства и планшеты.
  - **Obsidian Theme**: Тёмная неоновая эстетика (`ObsidianBlack = #0D0B14`, глубокие фиолетовые акценты `#8B5CF6`, `#C084FC`).
  - **Navigation**: `MainAppShell` с 4 табами (Чаты, Звонки, Контакты, Профиль) и плавающим островком с центральной кнопкой быстрого QR-сканера.

### Этап 3. Эволюция поля ввода и чата
1. **Монолитный плавающий островок ввода (`ChatInputBar.kt`)**:
   - Включает кнопку стикеров/эмодзи, динамическое поле ввода (растущее до 160dp), скрепку вложений и динамическую кнопку отправки/голосового сообщения.
   - Кнопка разворачивания в полный экран `⤢` при вводе длинного текста.
2. **Полноэкранный редактор текста (`ChatFullTextEditorScreen.kt`)**:
   - Undo/Redo стек правок.
   - Плавающий центрированный тулбар над клавиатурой (`TextFormattingToolbar`).
   - Панель стилей `Aa` с подменю заголовков `H1`–`H6` (`HeadingPickerSubmenu`).
   - Меню списков: маркеры `•`, цифры `1.`, римские `I.`, стрелочки `->`, чек-боксы `[ ]`.
   - Панель ввода формул LaTeX (`LatexFormulaInputPanel`) с чистым полем ввода и кнопкой «Готово».
3. **Плавающая панель выделения текста (`TextSelectionFloatingBar.kt`)**:
   - Всплывает точно над выделенным текстом: кнопки `B`, `I`, `U`, `S̶`, `✦` (спойлер), `Aa`.
   - Кнопка `</>` убрана по требованию пользователя для чистоты интерфейса.
   - Форматирование применяется нативно через `SpanStyle` без вставки сырых звёздочек `**` (чистый WYSIWYG).
4. **Рендеринг сообщений в баблах (`features/chats/ui/components/bubble/`)**:
   - `MessageBubble.kt`: 100% непрозрачные баблы (`Color(0xFF281C3D)` и `CardDark`), устранившие просвечивание обоев чата.
   - `BubbleTextMessage.kt`: Инлайн-спойлеры со слоем мерцающих фиолетовых искр (Canvas), раскрывающиеся по клику без разбиения текста на колонки.
   - `BubbleCollapsibleBlock.kt`: Сворачиваемые блоки с шевроном `^`/`v` по макету дизайна.
   - `BubbleLatexFormula.kt`: Математические карточки с бейджем `∑ LaTeX`.
   - Чек-листы: Крупные фиолетовые чекбоксы (22x22dp со скруглением и белой галочкой `✓`).
5. **Архитектурная защита скрепки ("атаку щит")**:
   - Основной модуль `features/attach/` (`AttachSheet.kt`, `AttachSheetContent.kt`) признан неприкосновенным ядром и **строго защищён от правок**.
   - Для инпута и редактора создаётся выделенный [EditorAttachSheet.kt](file:///home/jackson/anongram/frontend/android/app/src/main/java/com/anongram/app/features/chats/ui/input/editor/EditorAttachSheet.kt), который переиспользует визуальный ряд вложений, но добавляет кнопку подтверждения `✓` («Готово») и складывает медиа в черновик сообщения `attachedMediaList`, не отправляя их сразу в чат.

---

## 3. Архитектура Регистрации и Входа (Аутентификация AnonGram)

### 3.1. Криптографическая концепция: No-Identity & Self-Sovereign Identity
В AnonGram **нет понятия учетной записи на сервере**:
- Нет связки с телефонным номером.
- Нет подтверждения по SMS или Email.
- Пользователь **сам является своим центром сертификации**.
- Единственный источник правды — криптографическая мнемоника **BIP-39 (12 слов)**.

```mermaid
flowchart TD
    Mnemonic["12 слов BIP-39 (Seed Phrase)"] -->|PBKDF2 / SHA-512| Seed["512-bit Master Seed"]
    Seed -->|Ed25519 Derivation| MasterKeyPair["Master KeyPair (Sk, Vk)"]
    Seed -->|ML-DSA-87 Derivation| PQKeyPair["Post-Quantum KeyPair (PQ-Sk, PQ-Vk)"]
    MasterKeyPair & PQKeyPair -->|SHA-256 Hash| PublicID["Public ID (32 hex-символа)"]
    
    PublicID --> Passport["Passport Record (Nickname, Avatar, MasterVK, Signature)"]
    Passport -->|POST /api/v1/passport| ServerDB[("Fastify SQLite Passport DB")]
    
    Seed -->|AES-256-GCM / Biometrics| AndroidKeystore[("Android Keystore Vault")]
```

### 3.2. Спецификация данных и контрактов

#### 1. Модели данных (`com.anongram.app.features.auth.domain.model`)
```kotlin
package com.anongram.app.features.auth.domain.model

/**
 * Идентификатор и локальный профиль пользователя
 */
data class UserIdentity(
    val publicId: String,          // 32-значный hex (SHA-256 от MasterVK)
    val nickname: String,          // Пользовательский псевдоним (напр. "GhostWalker")
    val avatarUri: String?,        // Локальный путь к аватару или null
    val masterVkHex: String,       // Публичный ключ Ed25519
    val pqMasterVkHex: String? = null, // Публичный ключ ML-DSA
    val isBiometricsEnabled: Boolean = false,
    val isPinSet: Boolean = false
)

/**
 * Глобальное состояние аутентификации приложения
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val identity: UserIdentity) : AuthState
}

/**
 * Мнемоническая фраза BIP-39
 */
data class SeedPhrase(
    val words: List<String>
) {
    init {
        require(words.size == 12 || words.size == 24) { "Mnemonic must be 12 or 24 words" }
    }
    
    val text: String get() = words.joinToString(" ")
}
```

#### 2. Интерфейс репозитория (`IAuthRepository.kt`)
```kotlin
package com.anongram.app.features.auth.domain.repository

import com.anongram.app.features.auth.domain.model.AuthState
import com.anongram.app.features.auth.domain.model.SeedPhrase
import com.anongram.app.features.auth.domain.model.UserIdentity
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
```

---

## 4. Пользовательские сценарии и UI в Jetpack Compose

Модуль расположен в `com.anongram.app.features.auth.ui`:

### Сценарий 1. Первый запуск (Приветствие) — `WelcomeScreen.kt`
- **Дизайн**: Футуристический тёмный фон `ObsidianBlack`, анимированный щит AnonGram с фиолетовым неоновым ореолом.
- **Инфографика**:
  - 🛡️ *100% анонимность: без SIM-карт и почты*
  - ⚡ *Слепой сервер: данные видите только вы*
  - 🔐 *Полный контроль над своими ключами*
- **Кнопки**:
  - `«Создать анонимную личность»` (яркая фиолетовая капсула `#8B5CF6`).
  - `«Войти по Seed-фразе или QR»` (контурная кнопка с полупрозрачным фоном).

### Сценарий 2. Создание профиля — `CreateIdentityScreen.kt`
- Ввод псевдонима с ограничением и подсказкой: «Псевдоним видят только ваши собеседники».
- Генератор анонимных аватаров (случайные неоновые геометрические градиенты или выбор фото).
- Анимированный статус генерации ключей:
  - `Генерация мастер-ключа Ed25519...`
  - `Генерация квантового бандла ML-DSA-87...`
  - `Формирование Public ID...`
- Кнопка **«Продолжить к сохранению ключей»**.

### Сценарий 3. Резервное копирование мнемоники — `BackupSeedScreen.kt`
- **Критический экран безопасности**:
  - Плашка предупреждения (красновато-фиолетовый акцент): *«Никогда не передавайте эту фразу третьим лицам. Если вы её потеряете, восстановить переписку будет невозможно!»*.
  - Красивая сетка 3x4 из 12 слов. Каждое слово в карточке: номер (серый `1.`, `2.`) + слово (белый жирный шрифт).
  - Кнопка **«Скопировать в буфер обмена»** (с автоматической очисткой или тостом).
  - Кнопка **«Я сохранил слова»**: запускает интерактивный мини-тест (Memory Quiz), в котором пользователя просят выбрать, например, 3-е, 7-е и 11-е слово из предложенных вариантов.
  - После успешного теста — аккаунт регистрируется, паспорт отправляется на сервер, и открывается `MainAppShell`.

### Сценарий 4. Восстановление аккаунта — `RestoreAccountScreen.kt`
- Удобное поле ввода 12 слов с автокомплитом по официальному словарю BIP-39.
- Кнопка **«Вставить из буфера»** (парсит строку на 12 слов и заполняет чипы).
- Кнопка **«Сканировать резервный QR-код»**.
- Проверка чек-суммы: если слова введены с ошибкой — подсветка красным.
- При успехе — деривация ключей, запрос паспорта с сервера `/api/v1/passport/:publicId` и переход в чаты.

### Сценарий 5. Защита входа — `PinLockScreen.kt`
- 4-значный или 6-значный цифровой код.
- Кнопка быстрой биометрической аутентификации (отпечаток пальца / лицо).

---

## 5. Связка с ядром бэкенда и WebSocket

После того как пользователь авторизован:
1. `UserIdentity.publicId` используется для всех сетевых запросов.
2. Приложение устанавливает WebSocket-соединение с бэкендом (`backend/src/index.ts`):
```json
{
  "type": "auth",
  "publicId": "8f3ab71c90...921"
}
```
3. Сервер регистрирует сокет в `BlindRelayRouter` и отвечает:
```json
{
  "type": "auth_ack",
  "publicId": "8f3ab71c90...921"
}
```
4. Начиная с этого момента клиент готов отправлять и принимать зашифрованные конверты `EncryptedEnvelope` и звонки в реальном времени.

---

---

## 6. СТРОГАЯ КАРТА ФАЙЛОВ И ТРЕБОВАНИЯ К ДОКУМЕНТАЦИИ

### 6.1. Точные пути создаваемых файлов (Strict File Map)

Все файлы модуля Auth должны быть размещены строго по следующим путям (не создавать файлы в случайных папках!):

```
frontend/android/app/src/main/java/com/anongram/app/features/auth/
├── AUTH_CHANGELOG.md                          # Обязательный журнал изменений и описание компонентов
├── domain/
│   ├── model/
│   │   ├── UserIdentity.kt                    # Модель пользователя (publicId, nickname, avatarUri, masterVkHex)
│   │   ├── AuthState.kt                       # Sealed-интерфейс состояний: Loading, Unauthenticated, Authenticated
│   │   └── SeedPhrase.kt                      # Модель мнемоники BIP-39 (12/24 слова)
│   └── repository/
│       └── IAuthRepository.kt                 # Интерфейс репозитория аутентификации
├── data/
│   ├── crypto/
│   │   ├── Bip39WordList.kt                   # Массив 2048 слов официального словаря BIP-39
│   │   ├── KeyGenerator.kt                    # Деривация Seed, Ed25519 ключей и вычисление Public ID (SHA-256)
│   │   └── KeystoreVault.kt                   # Безопасное хранилище в Android Keystore / EncryptedSharedPreferences
│   └── repository/
│       └── AuthRepositoryImpl.kt              # Реализация IAuthRepository с публикацией паспорта на бэкенд
└── ui/
    ├── WelcomeScreen.kt                       # Приветственный экран с кнопками «Создать личность» и «Войти»
    ├── registration/
    │   ├── CreateIdentityScreen.kt            # Ввод псевдонима, выбор аватара, запуск генерации ключей
    │   └── BackupSeedScreen.kt                # Сетка 3x4 из 12 слов, предупреждение о защите, тест памяти
    ├── login/
    │   └── RestoreAccountScreen.kt            # Ввод мнемоники с автокомплитом и кнопкой вставки из буфера
    ├── pin/
    │   └── PinLockScreen.kt                   # Локальный PIN-код и вызов биометрии
    ├── state/
    │   └── AuthViewModel.kt                   # ViewModel с StateFlow<AuthState>
    └── navigation/
        └── AuthNavGraph.kt                    # Навигационный граф переходов между экранами авторизации
```

Файлы для интеграции:
- Точка входа приложения:
  `frontend/android/app/src/main/java/com/anongram/app/MainActivity.kt`
- Каркас навигации (переход в профиль):
  `frontend/android/app/src/main/java/com/anongram/app/features/profile/ui/ProfileScreen.kt`

---

### 6.2. Требования к документированию и комментированию кода (КРИТИЧЕСКИ ВАЖНО)

1. **Подробные KDoc комментарии в каждом файле**:
   - Каждый класс, интерфейс, `data class`, функция и публичное свойство должны сопровождаться чётким KDoc-комментарием на русском языке.
   - Описывать: назначение компонента, архитектурную роль, входные параметры и возвращаемое значение.
   - В сложных алгоритмах (деривация ключей, тест мнемоники, переходы UI) оставлять построчные поясняющие комментарии.
2. **Обязательный файл журнала изменений `AUTH_CHANGELOG.md`**:
   - Агент обязан создать и заполнить файл `frontend/android/app/src/main/java/com/anongram/app/features/auth/AUTH_CHANGELOG.md`.
   - В нём необходимо задокументировать:
     - Список всех созданных файлов с описанием их ответственности.
     - Описание доменных моделей и потока данных.
     - Инструкцию, как работает проверка сессии при старте приложения.
     - Описание связи с бэкендом (публикация Passport Blob).

---

## 7. ГОТОВЫЙ МАСТЕР-ПРОМПТ ДЛЯ ЗАПУСКА АГЕНТА В ОТДЕЛЬНОМ ОКНЕ/ТАБЕ

Скопируйте текст ниже и передайте его агенту для выполнения задачи:

```text
Ты — ведущий Android-разработчик AnonGram (Kotlin, Jetpack Compose, Clean Architecture).

ТВОЯ ЗАДАЧА:
Разработать и подключить модуль Регистрации и Аутентификации (Вход и Регистрация) в строгом соответствии со спецификацией `REGISTRATION_AND_AUTH_SPEC.md` и связать его с `MainActivity.kt`.

СТРОЖАЙШИЕ ПРАВИЛА ПРОЕКТА:
1. СТРОГО ЗАПРЕЩЕНО запускать локальную сборку `./gradlew` на машине пользователя. Все сборки выполняются ИСКЛЮЧИТЕЛЬНО через GitHub Actions Cloud CI (git add -A && git commit -m "..." && git push origin main).
2. При установке APK на устройство (adb -t 1 install -r -d ...) выполнять ТОЛЬКО тихую установку. СТРОГО ЗАПРЕЩЕНО запускать приложение командами monkey, am start и др. Пользователь откроет приложение сам!
3. Все пересылки сообщений строго анонимны (forwardedFrom = null).
4. Основной модуль `features/attach/` ЗАЩИЩЁН и НЕ ДОЛЖЕН модифицироваться.
5. Регистрация полностью децентрализована: 12 слов BIP-39, деривация Ed25519, publicId = SHA256(MasterVK). Никаких номеров телефонов или email!

ТОЧНЫЕ ПУТИ ФАЙЛОВ ДЛЯ СОЗДАНИЯ (СОЗДАВАТЬ СТРОГО ЗДЕСЬ!):
- Журнал изменений:
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/AUTH_CHANGELOG.md`
- Domain Layer:
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/domain/model/UserIdentity.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/domain/model/AuthState.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/domain/model/SeedPhrase.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/domain/repository/IAuthRepository.kt`
- Data Layer:
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/data/crypto/Bip39WordList.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/data/crypto/KeyGenerator.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/data/crypto/KeystoreVault.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/data/repository/AuthRepositoryImpl.kt`
- UI Layer (Jetpack Compose):
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/ui/WelcomeScreen.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/ui/registration/CreateIdentityScreen.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/ui/registration/BackupSeedScreen.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/ui/login/RestoreAccountScreen.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/ui/pin/PinLockScreen.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/ui/state/AuthViewModel.kt`
  `frontend/android/app/src/main/java/com/anongram/app/features/auth/ui/navigation/AuthNavGraph.kt`
- Интеграция:
  `frontend/android/app/src/main/java/com/anongram/app/MainActivity.kt`

ОБЯЗАТЕЛЬНЫЕ ТРЕБОВАНИЯ К КОММЕНТАРИЯМ И ДОКУМЕНТАЦИИ:
1. Каждый файл должен содержать подробные KDoc-комментарии на русском языке с объяснением назначения каждого класса, интерфейса и метода.
2. В файле `AUTH_CHANGELOG.md` подробно задокументируй все сделанные изменения: назначение каждого файла, архитектуру и логику переходов.

ПЛАН РЕАЛИЗАЦИИ:
Шаг 1. Создать доменные модели и интерфейс IAuthRepository с полными KDoc-комментариями.
Шаг 2. Реализовать Bip39WordList (2048 слов), KeyGenerator (вычисление Public ID = SHA256(MasterVK)), KeystoreVault (Android Keystore / EncryptedSharedPreferences) и AuthRepositoryImpl.
Шаг 3. Реализовать экраны Jetpack Compose в теме ObsidianBlack с неоновыми фиолетовыми акцентами (#8B5CF6 / #C084FC):
  - WelcomeScreen: футуристичный экран первого запуска с щитом AnonGram и кнопками «Создать анонимную личность» и «Войти».
  - CreateIdentityScreen: ввод псевдонима, неоновый аватар, анимация генерации ключей.
  - BackupSeedScreen: сетка 3x4 из 12 слов, предупреждение о безопасности, копирование и тест памяти (проверка 3-го и 7-го слова).
  - RestoreAccountScreen: ввод 12 слов с автокомплитом и вставкой из буфера.
  - AuthViewModel и AuthNavGraph.
Шаг 4. Интеграция в MainActivity.kt:
  - Проверять состояние authRepository.checkExistingSession().
  - Если Unauthenticated -> показывать AuthNavGraph.
  - Если Authenticated -> показывать MainAppShell.
Шаг 5. Заполнить `AUTH_CHANGELOG.md`, закоммитить изменения, отправить в main, дождаться Cloud CI сборки, скачать APK и установить тихо через ADB:
  `adb -t 1 install -r -d build-artifacts/debug/app-debug.apk`
  (НЕ открывать приложение через adb!).
```

