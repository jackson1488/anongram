import 'package:flutter/material.dart';
import 'theme_config.dart';
import 'theme_extension.dart';

/// Central theme factory and helper for AnonGram
class AnonGramTheme {
  /// Access the active design tokens from BuildContext
  static AnonGramThemeExtension of(BuildContext context) {
    final ext = Theme.of(context).extension<AnonGramThemeExtension>();
    if (ext != null) return ext;
    // Fallback default
    return const AnonGramThemeExtension(
      bgDark: Color(0xFF07090E),
      surfaceDark: Color(0xFF0C1017),
      cardDark: Color(0xFF131A26),
      neonAccent: Color(0xFF00F2FE),
      secondaryAccent: Color(0xFF10B981),
      textPrimary: Color(0xFFFFFFFF),
      textSecondary: Color(0xFF9CA3AF),
      bubbleStyle: BubbleStyle.cyber,
    );
  }

  /// Build complete ThemeData based on the user's config
  static ThemeData buildTheme(AppThemeConfig config) {
    Color bg;
    Color surface;
    Color card;
    Color accent = config.accentColor;
    Color secondary = config.secondaryColor;

    switch (config.preset) {
      case ThemePreset.cyber:
        bg = const Color(0xFF07090E);
        surface = const Color(0xFF0C1017);
        card = const Color(0xFF131A26);
        break;
      case ThemePreset.matrix:
        bg = const Color(0xFF030804);
        surface = const Color(0xFF08140A);
        card = const Color(0xFF0E2211);
        accent = const Color(0xFF22C55E);
        secondary = const Color(0xFF16A34A);
        break;
      case ThemePreset.cobalt:
        bg = const Color(0xFF040814);
        surface = const Color(0xFF0A1024);
        card = const Color(0xFF121B38);
        accent = const Color(0xFF3B82F6);
        secondary = const Color(0xFF60A5FA);
        break;
      case ThemePreset.amber:
        bg = const Color(0xFF0D0A04);
        surface = const Color(0xFF1A1408);
        card = const Color(0xFF291E0A);
        accent = const Color(0xFFF59E0B);
        secondary = const Color(0xFFFBBF24);
        break;
      case ThemePreset.ghost:
        bg = const Color(0xFF0A0A0A);
        surface = const Color(0xFF171717);
        card = const Color(0xFF262626);
        accent = const Color(0xFFFFFFFF);
        secondary = const Color(0xFFA3A3A3);
        break;
    }

    final ext = AnonGramThemeExtension(
      bgDark: bg,
      surfaceDark: surface,
      cardDark: card,
      neonAccent: accent,
      secondaryAccent: secondary,
      textPrimary: const Color(0xFFFFFFFF),
      textSecondary: const Color(0xFF9CA3AF),
      bubbleStyle: config.bubbleStyle,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: bg,
      colorScheme: ColorScheme.dark(
        primary: accent,
        secondary: secondary,
        surface: surface,
      ),
      extensions: [ext],
    );
  }
}
