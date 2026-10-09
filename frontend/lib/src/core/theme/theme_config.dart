import 'package:flutter/material.dart';

/// Available visual theme presets for AnonGram
enum ThemePreset {
  cyber,    // AMOLED Obsidian with Neon Cyan & Emerald
  matrix,   // Hacker Terminal Phosphor Green
  cobalt,   // Deep Ultramarine Sapphire Blue
  amber,    // Cyberpunk Solar Gold & Amber
  ghost,    // Minimalist Stealth Monochrome
}

/// Available chat bubble boundary styles
enum BubbleStyle {
  cyber,    // Chamfered / cut cyber corners
  rounded,  // Smooth modern rounded corners
  outline,  // Stealth transparent background with neon border
}

/// Immutable configuration of application appearance
class AppThemeConfig {
  final ThemePreset preset;
  final Color accentColor;
  final Color secondaryColor;
  final BubbleStyle bubbleStyle;
  final bool isAmoledPureBlack;

  const AppThemeConfig({
    required this.preset,
    required this.accentColor,
    required this.secondaryColor,
    required this.bubbleStyle,
    this.isAmoledPureBlack = true,
  });

  /// Default canonical AnonGram cyber theme
  factory AppThemeConfig.defaultConfig() {
    return const AppThemeConfig(
      preset: ThemePreset.cyber,
      accentColor: Color(0xFF00F2FE),
      secondaryColor: Color(0xFF10B981),
      bubbleStyle: BubbleStyle.cyber,
      isAmoledPureBlack: true,
    );
  }

  AppThemeConfig copyWith({
    ThemePreset? preset,
    Color? accentColor,
    Color? secondaryColor,
    BubbleStyle? bubbleStyle,
    bool? isAmoledPureBlack,
  }) {
    return AppThemeConfig(
      preset: preset ?? this.preset,
      accentColor: accentColor ?? this.accentColor,
      secondaryColor: secondaryColor ?? this.secondaryColor,
      bubbleStyle: bubbleStyle ?? this.bubbleStyle,
      isAmoledPureBlack: isAmoledPureBlack ?? this.isAmoledPureBlack,
    );
  }
}
