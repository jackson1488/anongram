import 'package:flutter/material.dart';
import '../../../core/theme/anongram_theme.dart';
import '../../../core/theme/theme_config.dart';
import '../../../core/di/locator.dart';
import '../appearance/theme_controller.dart';

/// Screen 4: Modular Settings Screen with Live Appearance Customization
class SettingsMenuScreen extends StatelessWidget {
  const SettingsMenuScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = AnonGramTheme.of(context);
    final themeCtrl = sl.get<IThemeController>();

    return Scaffold(
      backgroundColor: theme.bgDark,
      appBar: AppBar(
        backgroundColor: theme.surfaceDark,
        elevation: 0,
        title: const Text(
          "Настройки",
          style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 4.1 SUB-MODULE: APPEARANCE (ОФОРМЛЕНИЕ И ТЕМЫ)
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: theme.surfaceDark,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: theme.neonAccent.withOpacity(0.3)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Text("🎨", style: TextStyle(fontSize: 18)),
                        const SizedBox(width: 8),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text("Оформление и Темы", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
                            Text("settings/appearance/", style: TextStyle(color: theme.neonAccent, fontSize: 9, fontFamily: 'monospace')),
                          ],
                        ),
                      ],
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: theme.neonAccent.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        themeCtrl.config.preset.name.toUpperCase(),
                        style: TextStyle(color: theme.neonAccent, fontSize: 9, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                const Text("ПРЕСЕТЫ ТЕМ (КЛИКНИТЕ ДЛЯ СМЕНЫ):", style: TextStyle(color: Colors.white38, fontSize: 9, fontFamily: 'monospace')),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _themeButton("Cyber Obsidian", ThemePreset.cyber, themeCtrl, const Color(0xFF00F2FE)),
                    _themeButton("Matrix Terminal", ThemePreset.matrix, themeCtrl, const Color(0xFF22C55E)),
                    _themeButton("Midnight Cobalt", ThemePreset.cobalt, themeCtrl, const Color(0xFF3B82F6)),
                    _themeButton("Solar Amber", ThemePreset.amber, themeCtrl, const Color(0xFFF59E0B)),
                    _themeButton("Ghost Monochrome", ThemePreset.ghost, themeCtrl, const Color(0xFFFFFFFF)),
                  ],
                ),
                const SizedBox(height: 14),
                const Text("НЕОНОВЫЙ АКЦЕНТ:", style: TextStyle(color: Colors.white38, fontSize: 9, fontFamily: 'monospace')),
                const SizedBox(height: 8),
                Row(
                  children: [
                    const Color(0xFF00F2FE),
                    const Color(0xFF10B981),
                    const Color(0xFFA855F7),
                    const Color(0xFFF97316),
                    const Color(0xFFFFFFFF),
                  ].map((color) {
                    final isSelected = themeCtrl.config.accentColor == color;
                    return GestureDetector(
                      onTap: () => themeCtrl.setAccentColor(color),
                      child: Container(
                        margin: const EdgeInsets.only(right: 12),
                        width: 26,
                        height: 26,
                        decoration: BoxDecoration(
                          color: color,
                          shape: BoxShape.circle,
                          border: isSelected ? Border.all(color: Colors.white, width: 2) : null,
                          boxShadow: isSelected ? [BoxShadow(color: color.withOpacity(0.6), blurRadius: 8)] : null,
                        ),
                      ),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 14),
                const Text("ФОРМА СООБЩЕНИЙ:", style: TextStyle(color: Colors.white38, fontSize: 9, fontFamily: 'monospace')),
                const SizedBox(height: 8),
                Row(
                  children: [
                    _bubbleBtn("Cyber Cut", BubbleStyle.cyber, themeCtrl),
                    const SizedBox(width: 8),
                    _bubbleBtn("Rounded", BubbleStyle.rounded, themeCtrl),
                    const SizedBox(width: 8),
                    _bubbleBtn("Outline", BubbleStyle.outline, themeCtrl),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // 4.2 SUB-MODULE: SECURITY
          _settingsTile(Icons.security, "Безопасность и Шифрование", "Ключи, таймеры, Anti-Screen Capture", theme),
          // 4.3 SUB-MODULE: PRIVACY
          _settingsTile(Icons.lock, "Приватность и Дуресс", "Биометрия, Пароль под принуждением", theme),
          // 4.4 SUB-MODULE: NETWORK
          _settingsTile(Icons.vpn_lock, "Сеть и Луковые Прокси", "Onion 3-hop, DNS over HTTPS", theme),
          // 4.5 SUB-MODULE: STORAGE
          _settingsTile(Icons.storage, "Хранилище и База Данных", "Шифрование SQLCipher, очистка медиа", theme),
          // 4.6 SUB-MODULE: NOTIFICATIONS
          _settingsTile(Icons.notifications, "Уведомления", "5 уровней адаптивных каналов Android", theme),
        ],
      ),
    );
  }

  Widget _themeButton(String label, ThemePreset preset, IThemeController ctrl, Color color) {
    final isSelected = ctrl.config.preset == preset;
    return InkWell(
      onTap: () => ctrl.setPreset(preset),
      borderRadius: BorderRadius.circular(10),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: isSelected ? color.withOpacity(0.2) : Colors.black45,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: isSelected ? color : Colors.white10),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(width: 8, height: 8, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
            const SizedBox(width: 6),
            Text(label, style: TextStyle(color: isSelected ? Colors.white : Colors.white70, fontSize: 10, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
          ],
        ),
      ),
    );
  }

  Widget _bubbleBtn(String label, BubbleStyle style, IThemeController ctrl) {
    final isSelected = ctrl.config.bubbleStyle == style;
    return Expanded(
      child: OutlinedButton(
        style: OutlinedButton.styleFrom(
          side: BorderSide(color: isSelected ? ctrl.config.accentColor : Colors.white12),
          backgroundColor: isSelected ? ctrl.config.accentColor.withOpacity(0.15) : null,
          padding: const EdgeInsets.symmetric(vertical: 6),
        ),
        onPressed: () => ctrl.setBubbleStyle(style),
        child: Text(label, style: TextStyle(fontSize: 10, color: isSelected ? Colors.white : Colors.white60)),
      ),
    );
  }

  Widget _settingsTile(IconData icon, String title, String subtitle, AnonGramThemeExtension theme) {
    return Card(
      color: theme.surfaceDark,
      margin: const EdgeInsets.only(bottom: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: ListTile(
        leading: Icon(icon, color: theme.neonAccent, size: 20),
        title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold)),
        subtitle: Text(subtitle, style: const TextStyle(color: Colors.white54, fontSize: 10)),
        trailing: const Icon(Icons.chevron_right, color: Colors.white38, size: 18),
        onTap: () {},
      ),
    );
  }
}
