import 'package:flutter/material.dart';
import 'src/core/theme/anongram_theme.dart';
import 'src/core/di/locator.dart';
import 'src/features/settings/appearance/theme_controller.dart';
import 'src/features/navigation/presentation/main_navigation_shell.dart';

/// Root AnonGram Application Widget with Live Dynamic Theme Listening
class AnonGramApp extends StatelessWidget {
  const AnonGramApp({super.key});

  @override
  Widget build(BuildContext context) {
    final themeController = sl.get<IThemeController>();

    return AnimatedBuilder(
      animation: themeController,
      builder: (context, _) {
        return MaterialApp(
          title: 'AnonGram',
          debugShowCheckedModeBanner: false,
          theme: AnonGramTheme.buildTheme(themeController.config),
          home: const MainNavigationShell(),
        );
      },
    );
  }
}
