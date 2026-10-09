# AnonGram: Спецификация Модульной ООП Архитектуры

Данный документ описывает интерфейсные контракты, иерархию классов и паттерны проектирования клиента AnonGram.

---

## 1. Паттерны Проектирования по Модулям

### 1.1 Модуль Чатов (`chats/`): Polymorphic Session & Factory Pattern

```
                       ┌─────────────────────────┐
                       │      IChatSession       │ (Интерфейс)
                       │─────────────────────────│
                       │ + id: String            │
                       │ + title: String         │
                       │ + type: ChatType        │
                       │ + lastMessage: IMessage │
                       │ + sendMessage(...)      │
                       └────────────┬────────────┘
                                    │
         ┌──────────────────────────┼──────────────────────────┐
         ▼                          ▼                          ▼
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│ DirectChatSession│       │ GroupChatSession │       │  OtpBurnSession  │
│──────────────────│       │──────────────────│       │──────────────────│
│ + peerId: String │       │ + memberCount    │       │ + burnSeconds:int│
│ + ratchetState   │       │ + isStrictPair   │       │ + zeroizeKeys()  │
│ + sasEmoji       │       │ + senderKeyId    │       │ + autoDestroy()  │
└──────────────────┘       └──────────────────┘       └──────────────────┘
```

#### Фабрика Создания Сессий:
```dart
abstract class IChatSessionFactory {
  IChatSession createDirectSession({required String peerId, required String title});
  IChatSession createGroupSession({required String groupId, required List<String> members});
  IChatSession createOtpSession({required String peerId, required Duration ttl});
}
```

---

### 1.2 Модуль Сканера (`scanner/`): Strategy Pattern

```
                          ┌─────────────────────────┐
                          │    IQrPayloadHandler    │ (Strategy Contract)
                          │─────────────────────────│
                          │ + canHandle(raw): bool  │
                          │ + handle(context, raw)  │
                          └────────────┬────────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            ▼                          ▼                          ▼
┌───────────────────────┐  ┌───────────────────────┐  ┌───────────────────────┐
│    FriendQrHandler    │  │  DeviceLinkQrHandler  │  │ VerificationQrHandler │
│───────────────────────│  │───────────────────────│  │───────────────────────│
│ Prefix: AGQR:USER:... │  │ Prefix: AGQR:LINK:... │  │Prefix: AGQR:VERIFY:...│
│ Action: Add Contact   │  │ Action: Pair Desktop  │  │ Action: Anti-MITM SAS │
└───────────────────────┘  └───────────────────────┘  └───────────────────────┘
```

#### Диспетчер Сканера:
```dart
class QrPayloadDispatcher {
  final List<IQrPayloadHandler> _handlers;

  QrPayloadDispatcher(this._handlers);

  Future<QrDispatchResult> dispatch(BuildContext context, String rawQrData) async {
    for (final handler in _handlers) {
      if (handler.canHandle(rawQrData)) {
        return await handler.handle(context, rawQrData);
      }
    }
    return QrDispatchResult.unknownFormat(rawQrData);
  }
}
```

---

### 1.3 Модуль Настроек Оформления (`settings/appearance/`): Observer / ThemeExtension

```dart
/// Конфигурация оформления (Immutable State)
class AppThemeConfig {
  final ThemePreset preset;
  final Color accentColor;
  final BubbleStyle bubbleStyle;
  final bool isAmoledPureBlack;

  const AppThemeConfig({
    required this.preset,
    required this.accentColor,
    required this.bubbleStyle,
    this.isAmoledPureBlack = true,
  });

  AppThemeConfig copyWith({
    ThemePreset? preset,
    Color? accentColor,
    BubbleStyle? bubbleStyle,
    bool? isAmoledPureBlack,
  }) {
    return AppThemeConfig(
      preset: preset ?? this.preset,
      accentColor: accentColor ?? this.accentColor,
      bubbleStyle: bubbleStyle ?? this.bubbleStyle,
      isAmoledPureBlack: isAmoledPureBlack ?? this.isAmoledPureBlack,
    );
  }
}

/// Контроллер Темы (Observer)
abstract class IThemeController extends Listenable {
  AppThemeConfig get config;
  Future<void> updatePreset(ThemePreset preset);
  Future<void> updateAccentColor(Color color);
  Future<void> updateBubbleStyle(BubbleStyle style);
}
```

---

### 1.4 Модуль Анонимной Регистрации (`auth/`): Clean Crypto Identity

1. **`auth/seed_phrase/`**:
   - `Bip39MnemonicGenerator`: генерация энтропии (128 бит = 12 слов, 256 бит = 24 слова).
   - Валидация контрольной суммы BIP-39.
2. **`auth/keys_derivation/`**:
   - Вывод мастер-сид ключа через PBKDF2/Argon2id.
   - Генерация открытого паспорта:
     - Ed25519 (цифровая подпись сообщений).
     - X25519 + Kyber-1024 (пост-квантовый KEM для Double Ratchet).
3. **`auth/duress/`**:
   - Опция «Пароль под принуждением» (Dumb PIN).
   - При вводе альтернативного PIN-кода приложение загружает изолированную пустую базу без реальных чатов и ключей.

---

## 2. Инверсия Зависимостей (Dependency Inversion & DI)

Все сервисы регистрируются в контейнере зависимостей (`ServiceLocator`):

```dart
void setupDependencies() {
  // Core FFI
  sl.registerLazySingleton<IAnongramCoreBridge>(() => AnongramFfiBridge());

  // Repositories
  sl.registerLazySingleton<IThemeRepository>(() => ThemeRepositoryImpl(sl()));
  sl.registerLazySingleton<IChatRepository>(() => FfiChatRepository(sl()));
  sl.registerLazySingleton<IVpnController>(() => FfiVpnController(sl()));

  // Controllers / Blocs
  sl.registerLazySingleton<IThemeController>(() => ThemeController(sl()));
  sl.registerFactory<QrPayloadDispatcher>(() => QrPayloadDispatcher([
    FriendQrHandler(sl()),
    DeviceLinkQrHandler(sl()),
    VerificationQrHandler(sl()),
  ]));
}
```
