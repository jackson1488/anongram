# Архитектура и Документация Ядра: Модуль `ffi` (C-ABI & Flutter/Dart Bridge)

## 1. Для чего и зачем нужен этот модуль?
Ядро AnonGram написано на языке Rust для обеспечения предельной производительности, безопасности работы с памятью и постквантовой криптографии.
Однако пользовательский интерфейс мессенджера (UI) создается на кроссплатформенном фреймворке (Flutter / Dart) под Android, iOS, Windows, macOS и Linux.

Чтобы Flutter мог вызывать функции ядра **напрямую в оперативной памяти с околонулевыми задержками (Zero-Copy)** без прохождения через медленные сериализаторы JSON или системные каналы Platform Channels, необходим **C-ABI FFI мост** (`Foreign Function Interface`).

---

## 2. Что делает модуль (Анатомия и Архитектура)?

### 1. Безопасная структура буфера (`ByteBuffer`)
В Си и Dart нет встроенных векторов Rust (`Vec<u8>`). Передача данных через границу FFI осуществляется через плоскую структуру с выравниванием `#[repr(C)]`:
```rust
#[repr(C)]
pub struct ByteBuffer {
    pub ptr: *mut u8,
    pub len: usize,
    pub capacity: usize,
}
```
- **Zero-Copy**: Данные не дублируются в оперативной памяти. Dart получает прямой указатель на память в RAM.
- **Предотвращение утечек памяти (Memory Leaks)**: Функция `anongram_free_buffer(buf)` возвращает владение вектором обратно в Rust allocator, где память гарантированно освобождается или затирается нулями `zeroize`.

### 2. Экспортируемые C-API интерфейсы
Модуль предоставляет чистый Си-интерфейс ко всем компонентам ядра:
- **Криптография (`crypto`)**:
  - `anongram_crypto_aead_seal()` — шифрование XChaCha20-Poly1305.
  - `anongram_crypto_aead_open()` — расшифровка и проверка аутентичности Poly1305.
- **Идентичность (`identity`)**:
  - `anongram_identity_from_mnemonic()` — валидация BIP-39 мнемоники и инициализация паспорта.
- **Медиа (`media`)**:
  - `anongram_media_seal()` — стриппинг EXIF/GPS, сжатие Zstd и шифрование.
  - `anongram_media_open()` — распаковка и декомпрессия медиафайла.
- **Сеть и Эндпоинты (`network`)**:
  - `anongram_network_parse_endpoint()` — универсальный разбор произвольных серверных адресов (IP, домен, порт, /api).
- **Скрытые Push-уведомления (`push`)**:
  - `anongram_push_unpack()` — фоновая расшифровка сообщений и извлечение приказов (RotateEndpoint, PanicWipe).
- **Безопасность (`security`)**:
  - `anongram_security_derive_key()` — деривация мастер-ключа из пароля через Argon2id/KDF.

---

## 3. Интеграция с Dart / Flutter (`dart:ffi`)
Пример вызова из Flutter:
```dart
import 'dart:ffi' as ffi;

final DynamicLibrary nativeLib = Platform.isAndroid
    ? DynamicLibrary.open("libanongram_ffi.so")
    : DynamicLibrary.process();

// Прямой вызов функции ядра за 0.001 миллисекунды:
final version = nativeLib.lookupFunction<...>("anongram_version")();
```
Модуль компилируется в виде динамической библиотеки `cdylib` (`.so` для Android, `.dylib` для macOS/iOS, `.dll` для Windows), что позволяет использовать **Hot-Reload / динамический dlopen** в процессе мобильной разработки.
