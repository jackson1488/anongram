import 'package:flutter/material.dart';
import 'theme_config.dart';

/// AnonGram custom theme extension for type-safe, non-hardcoded design tokens
class AnonGramThemeExtension extends ThemeExtension<AnonGramThemeExtension> {
  final Color bgDark;
  final Color surfaceDark;
  final Color cardDark;
  final Color neonAccent;
  final Color secondaryAccent;
  final Color textPrimary;
  final Color textSecondary;
  final BubbleStyle bubbleStyle;

  const AnonGramThemeExtension({
    required this.bgDark,
    required this.surfaceDark,
    required this.cardDark,
    required this.neonAccent,
    required this.secondaryAccent,
    required this.textPrimary,
    required this.textSecondary,
    required this.bubbleStyle,
  });

  @override
  ThemeExtension<AnonGramThemeExtension> copyWith({
    Color? bgDark,
    Color? surfaceDark,
    Color? cardDark,
    Color? neonAccent,
    Color? secondaryAccent,
    Color? textPrimary,
    Color? textSecondary,
    BubbleStyle? bubbleStyle,
  }) {
    return AnonGramThemeExtension(
      bgDark: bgDark ?? this.bgDark,
      surfaceDark: surfaceDark ?? this.surfaceDark,
      cardDark: cardDark ?? this.cardDark,
      neonAccent: neonAccent ?? this.neonAccent,
      secondaryAccent: secondaryAccent ?? this.secondaryAccent,
      textPrimary: textPrimary ?? this.textPrimary,
      textSecondary: textSecondary ?? this.textSecondary,
      bubbleStyle: bubbleStyle ?? this.bubbleStyle,
    );
  }

  @override
  ThemeExtension<AnonGramThemeExtension> lerp(
    covariant ThemeExtension<AnonGramThemeExtension>? other,
    double t,
  ) {
    if (other is! AnonGramThemeExtension) return this;
    return AnonGramThemeExtension(
      bgDark: Color.lerp(bgDark, other.bgDark, t)!,
      surfaceDark: Color.lerp(surfaceDark, other.surfaceDark, t)!,
      cardDark: Color.lerp(cardDark, other.cardDark, t)!,
      neonAccent: Color.lerp(neonAccent, other.neonAccent, t)!,
      secondaryAccent: Color.lerp(secondaryAccent, other.secondaryAccent, t)!,
      textPrimary: Color.lerp(textPrimary, other.textPrimary, t)!,
      textSecondary: Color.lerp(textSecondary, other.textSecondary, t)!,
      bubbleStyle: t < 0.5 ? bubbleStyle : other.bubbleStyle,
    );
  }
}
