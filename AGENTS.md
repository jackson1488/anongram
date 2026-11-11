# Project AnonGram Rules & Guidelines

## 1. Сборка (Build & CI)
- **Строго запрещено запускать локальную сборку (`./gradlew`) на компьютере пользователя**.
- Все сборки Android APK выполняются **исключительно через облачный CI (GitHub Actions)**:
  1. `git commit` и `git push origin main`
  2. Мониторинг запуска через GitHub Actions API
  3. Скачивание артефакта APK из Cloud CI

## 2. Установка на телефон (Device Deployment via ADB)
- **Никогда не открывать и не запускать приложение автоматически после установки**:
  - Выполнять ТОЛЬКО тихую установку обновления: `adb -s <device> install -r <apk>`
  - **ЗАПРЕЩЕНО** запускать приложение командами `monkey`, `am start` и т.д.
  - Пользователь откроет приложение сам, когда ему будет удобно, чтобы не прерывать текущую работу на телефоне.

## 3. Анонимность (AnonGram Core Protocol)
- Пересылка сообщений ВСЕГДА на 100% анонимна (`forwardedFrom = null`).
- Никаких плашек «Переслано от ...».

## 4. Темы, палитры и локализация для всех новых экранов
- **Запрещено** хардкодить цвета (`Color(0xFF...)`, `Color.White`, `ObsidianBlack`, `CardDark`, `TextPrimary`) в новых UI-компонентах.
- Для новых экранов всегда использовать [`AnonGramScreenSurface`](file:///home/jackson/anongram/frontend/android/app/src/main/java/com/anongram/app/core/ui/AnonGramScreenSurface.kt), токены `LocalAnonGramScheme.current.*` (или `MaterialTheme.colorScheme.*`) — тема тогда применяется автоматически к любым тёмным, светлым, Ч/Б и цветным палитрам из единой точки (`core/theme/`).
- Текстовые строки модуля ввода и сопутствующих панелей подключаются через `LocalInputStrings.current` / `InputI18n.current`.

