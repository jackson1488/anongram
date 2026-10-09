import 'package:flutter/material.dart';
import '../../../core/theme/theme_config.dart';

/// Contract for the Theme Engine (ISP / DIP)
abstract class IThemeController extends Listenable {
  AppThemeConfig get config;
  void setPreset(ThemePreset preset);
  void setAccentColor(Color color);
  void setBubbleStyle(BubbleStyle style);
}

/// Concrete ThemeController managing application live theming
class ThemeController extends ChangeNotifier implements IThemeController {
  AppThemeConfig _config;

  ThemeController({AppThemeConfig? initialConfig})
      : _config = initialConfig ?? AppThemeConfig.defaultConfig();

  @override
  AppThemeConfig get config => _config;

  @override
  void setPreset(ThemePreset preset) {
    if (_config.preset == preset) return;
    Color accent = _config.accentColor;
    Color secondary = _config.secondaryColor;

    switch (preset) {
      case ThemePreset.cyber:
        accent = const Color(0xFF00F2FE);
        secondary = const Color(0xFF10B981);
        break;
      case ThemePreset.matrix:
        accent = const Color(0xFF22C55E);
        secondary = const Color(0xFF16A34A);
        break;
      case ThemePreset.cobalt:
        accent = const Color(0xFF3B82F6);
        secondary = const Color(0xFF60A5FA);
        break;
      case ThemePreset.amber:
        accent = const Color(0xFFF59E0B);
        secondary = const Color(0xFFFBBF24);
        break;
      case ThemePreset.ghost:
        accent = const Color(0xFFFFFFFF);
        secondary = const Color(0xFFA3A3A3);
        break;
    }

    _config = _config.copyWith(
      preset: preset,
      accentColor: accent,
      secondaryColor: secondary,
    );
    notifyListeners();
  }

  @override
  void setAccentColor(Color color) {
    _config = _config.copyWith(accentColor: color);
    notifyListeners();
  }

  @override
  void setBubbleStyle(BubbleStyle style) {
    _config = _config.copyWith(bubbleStyle: style);
    notifyListeners();
  }
}
