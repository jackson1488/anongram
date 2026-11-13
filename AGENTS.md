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

## 4. Темы, палитры и обязательная локализация (i18n)
- **Запрещено** хардкодить цвета (`Color(0xFF...)`, `Color.White`, `ObsidianBlack`, `CardDark`, `TextPrimary`, `TextSecondary`, `IslandBackground`, `SurfaceDark`, `Attach*`) в UI-компонентах.
- Для всех экранов, оверлеев, меню (`⋮`), диалогов, плеера и меню вложений всегда использовать [`AnonGramScreenSurface`](file:///home/jackson/anongram/frontend/android/app/src/main/java/com/anongram/app/core/ui/AnonGramScreenSurface.kt) и токены `LocalAnonGramScheme.current.*` (`scheme.background`, `scheme.surface`, `scheme.card`, `scheme.islandBg`, `scheme.islandBorder`, `scheme.textPrimary`, `scheme.textSecondary`, `scheme.textMuted`, `scheme.accent`, `scheme.onAccent`, `scheme.outgoingText`, `scheme.incomingText`, `scheme.divider`), чтобы тема автоматически применялась ко всем тёмным, светлым, Ч/Б и кастомным палитрам.
- **Правило i18n (строго обязательно)**: при **каждом** изменении или добавлении пользовательского текста в интерфейсе (кнопки, заголовки, пункты меню, диалоги, тосты, плейсхолдеры) обязательно сразу обновлять соответствующие словари локализации (RU + EN через `core/i18n/` и модульные `i18n/`-контракты, например `LocalInputStrings.current` / `InputI18n.current`), не оставляя захардкоженных строк в UI.

## 5. Git-дисциплина и изоляция параллельной работы
- **Строго запрещено** использовать `git add -A` или `git add .` — всегда индексировать (`git add <file1> <file2> ...`) **только свои** изменённые или созданные файлы.
- Над `frontend/web/` ведётся параллельная работа другим агентом: **запрещено** изменять, удалять или добавлять в коммит любые файлы из `frontend/web/`, если пользователь явно об этом не просил.

## 6. Минификация / R8 (`isMinifyEnabled`)
- Файл `frontend/android/app/proguard-rules.pro` уже подготовлен, но **флаг `isMinifyEnabled = true` сейчас НЕ включать** (`isMinifyEnabled = false` в `frontend/android/app/build.gradle.kts` не менять), пока пользователь явно не скажет включить его для финального релиза.

