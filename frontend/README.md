# AnonGram Frontend: Модульная Архитектура Клиента и UI

AnonGram — децентрализованный мессенджер с квантово-стойким шифрованием (Post-Quantum E2EE), луковым проксированием (Onion Routing) и встроенным VPN.

Архитектура фронтенда построена на принципах **Feature-First Clean Architecture** и **ООП (SOLID)** со строгой **суб-модульной декомпозицией** каждого доменного раздела.

---

## 🏛️ Ключевые Архитектурные Принципы (ООП & SOLID)

1. **S — Single Responsibility (Единая ответственность)**:
   Каждый суб-модуль решает ровно одну задачу. Например:
   - `chats/direct/` отвечает только за попарный Double Ratchet 1-на-1;
   - `chats/group/` отвечает за Sender Keys и пороговые переходы (&le;30 vs &gt;30);
   - `chats/otp/` отвечает за самоуничтожающиеся сообщения и немедленное зануление памяти (`zeroize`).
2. **O — Open / Closed (Открытость к расширению, закрытость к модификации)**:
   - В сканере применен **Strategy Pattern**: базовый контракт `IQrPayloadHandler` с полиморфными наследниками `FriendQrHandler`, `DeviceLinkQrHandler`, `VerificationQrHandler`. Добавление нового типа QR (например, каналы) создается в новой подпапке без изменения существующего кода сканера.
   - В чатах список чатов работает с полиморфным `List<IChatSession>`.
3. **L — Liskov Substitution (Подстановка Лисков)**:
   - Любая реализация сессии чата (`DirectChatSession`, `GroupChatSession`, `OtpBurnSession`) взаимозаменяема для UI.
4. **I — Interface Segregation (Разделение интерфейсов)**:
   - Тонкие интерфейсы: `IThemeController`, `IKeyVault`, `IVpnController`, `IQrScannerService`.
5. **D — Dependency Inversion (Инверсия зависимостей)**:
   - Презентационный слой Flutter UI не зависит от FFI или Rust Core напрямую. Все зависимости инжектятся через интерфейсы (`DI Service Locator`).

---

## 📂 Структура Каталогов Клиента (`frontend/lib/src/`)

```
frontend/lib/src/
├── core/                                   # Общесистемный фундамент
│   ├── ffi/                                # C/Rust FFI мост к libanongram_core.so
│   ├── di/                                 # Dependency Injection Service Locator
│   ├── theme/                              # Динамический ThemeEngine (AMOLED, Neon)
│   └── security/                           # Безопасное хранилище (SecureStorage, SQLCipher)
│
└── features/                               # НЕЗАВИСИМЫЕ МОДУЛИ И СУБ-МОДУЛИ
    │
    ├── chats/                              # 💬 1. МОДУЛЬ: ЧАТЫ
    │   ├── common/                         # Базовые контракты (IChatSession, IMessage)
    │   ├── direct/                         # 1.1 Личные чаты 1-на-1 (Double Ratchet)
    │   ├── group/                          # 1.2 Группы (StrictPairwise &le;30 vs SenderKeys &gt;30)
    │   └── otp/                            # 1.3 Одноразовые OTP-чаты (Burn-after-reading)
    │
    ├── services/                           # 🛡️ 2. МОДУЛЬ: СЕРВИСЫ
    │   ├── vpn/                            # 2.1 Встроенный AnonGram VPN & Kill-Switch
    │   ├── onion/                          # 2.2 3-hop луковые цепочки ретрансляторов
    │   └── storage/                        # 2.3 Zero-Knowledge P2P хранилище
    │
    ├── scanner/                            # ⭐ 3. МОДУЛЬ: ЦЕНТРАЛЬНЫЙ QR-СКАНЕР
    │   ├── core/                           # Видеопоток камеры, лазерный видоискатель
    │   ├── friend/                         # 3.1 Добавление контакта (AGQR:USER:...)
    │   ├── device_link/                    # 3.2 Связка Desktop/Web клиента (AGQR:LINK:...)
    │   ├── verification/                   # 3.3 Anti-MITM сверка отпечатка (AGQR:VERIFY:...)
    │   └── my_code/                        # 3.4 Экран «Мой QR-код»
    │
    ├── settings/                           # ⚙️ 4. МОДУЛЬ: НАСТРОЙКИ
    │   ├── appearance/                     # 4.1 Оформление: темы, неоновые акценты, бабблы
    │   ├── security/                       # 4.2 Ключи, отпечатки, защита от скриншотов
    │   ├── privacy/                        # 4.3 Биометрия, пароль под принуждением (Dumb PIN)
    │   ├── network/                        # 4.4 Прокси, DNS over HTTPS, сокеты
    │   ├── storage/                        # 4.5 База данных SQLCipher, авто-очистка кэша
    │   └── notifications/                  # 4.6 5 уровней адаптивных Android уведомлений
    │
    ├── profile/                            # 👤 5. МОДУЛЬ: ПРОФИЛЬ
    │   ├── identity/                       # 5.1 Анонимный паспорт, 60-значный хэш
    │   ├── sessions/                       # 5.2 Управление подключенными сессиями
    │   └── backup/                         # 5.3 Зашифрованная резервная копия
    │
    ├── auth/                               # 🔑 МОДУЛЬ: АНОНИМНАЯ РЕГИСТРАЦИЯ
    │   ├── seed_phrase/                    # Генерация 12 мнемонических слов (BIP-39)
    │   ├── keys_derivation/                # Вывод квантово-стойких ключей (Ed25519/Kyber)
    │   ├── biometrics/                     # Мастер-PIN и биометрический анклав
    │   └── duress/                         # Пароль под принуждением
    │
    └── calls/                              # 📞 МОДУЛЬ: E2EE ЗВОНКИ
        ├── domain/                         # WebRTC сессия, SAS-эмодзи верификация
        └── presentation/                   # Полноэкранный вызов, плавающее окно (PiP)
```

---

## 🎨 Реактивный Движок Оформления (Real-Time Live Theming Engine)

### Как работает мгновенная смена оформления:
1. В суб-модуле `settings/appearance/` пользователь кликает по пресету темы или акцентному цвету.
2. `ThemeController` (паттерн **Observer**) обновляет состояние `AppThemeConfig` и вызывает `notifyListeners()`.
3. Состояние сохраняется в зашифрованное хранилище `ISecureStorage`.
4. Корневой `MaterialApp` и `AnonGramThemeExtension` получают сигнал об изменении.
5. **Все экраны (Чаты, Сервисы, Сканер, Профиль) моментально перерисовываются без лагов и без перезапуска приложения!**

### Доступные пресеты тем:
* **Cyber Obsidian** — Фирменный глубокий AMOLED Black (`#07090E`) с неоновым цианом (`#00F2FE`) и изумрудом (`#10B981`).
* **Matrix Terminal** — Зеленый хакерский фосфор (`#22C55E`) на обсидиане (`#030804`).
* **Midnight Cobalt** — Ультрамариновый сапфир (`#3B82F6`) на глубоком синем (`#040814`).
* **Solar Amber** — Киберпанк янтарь и золото (`#F59E0B`) на титане (`#0D0A04`).
* **Ghost Monochrome** — Стелс черно-белый (`#FFFFFF` / `#71717A`).

### Кастомизация формы сообщений (Chat Bubbles):
* **Cyber Cut** — Скошенные технологичные углы.
* **Modern Rounded** — Мягкие скругленные углы.
* **Stealth Outline** — Прозрачный фон с неоновым контуром.

---

## 📱 Интерактивные Прототипы и Макеты
Все интерактивные HTML-макеты доступны в папке [`frontend/mockups/`](mockups/):
* **`anongram_app_prototype.html`** — Живой полноэкранный мобильный клиент с 5 вкладками, центральным QR-сканером, живым переключателем тем оформления и модальным окном регистрации мнемоники.
* **`anongram_branding_showcase.html`** — Бренд-пак: адаптивная иконка, статус-бар Android, сплэш-экран и 6 типов векторных уведомлений (`24x24dp`).
* **`whatsapp_call_ui_mockup.html`** — Защищенный голосовой/видео звонок с SAS-эмодзи и плавающим PiP окном.
