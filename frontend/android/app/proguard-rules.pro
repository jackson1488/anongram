# AnonGram R8 / ProGuard universal rules

# Сохраняем все классы и методы, вызываемые из C++ / Rust JNI
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keep class com.anongram.app.core.crypto.NativeCryptoBridge { *; }
-keep class com.anongram.app.features.chats.media.voice.stt.WhisperNativeBridge { *; }

# Bergamot TranslateKit (JNI C++ -> Java callbacks: LanguageResult, TranslationResult)
-keep class io.github.marcosholgado.translatekit.** { *; }

# Vosk + JNA (reflection & native structs)
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.Structure { *; }
-keep class * implements com.sun.jna.Callback { *; }
-keep class * implements com.sun.jna.Library { *; }
-dontwarn com.sun.jna.**
-dontwarn java.awt.**
-dontwarn javax.swing.**

# OSMDroid / ZXing / Coroutines
-dontwarn org.osmdroid.**
-dontwarn org.slf4j.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
