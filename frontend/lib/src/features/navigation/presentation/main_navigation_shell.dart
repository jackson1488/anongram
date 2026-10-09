import 'package:flutter/material.dart';
import '../../../core/theme/anongram_theme.dart';
import '../../chats/presentation/chats_list_screen.dart';
import '../../services/presentation/services_hub_screen.dart';
import '../../scanner/presentation/universal_scanner_screen.dart';
import '../../settings/presentation/settings_menu_screen.dart';
import '../../profile/presentation/profile_screen.dart';

/// Central Navigation Shell with 5 elements and center elevated QR Scanner button
class MainNavigationShell extends StatefulWidget {
  const MainNavigationShell({super.key});

  @override
  State<MainNavigationShell> createState() => _MainNavigationShellState();
}

class _MainNavigationShellState extends State<MainNavigationShell> {
  int _currentIndex = 0;

  final List<Widget> _screens = const [
    ChatsListScreen(),          // Index 0: 💬 Chats
    ServicesHubScreen(),        // Index 1: 🛡️ Services (VPN)
    UniversalScannerScreen(),   // Index 2: ⭐ Universal QR Scanner
    SettingsMenuScreen(),       // Index 3: ⚙️ Settings
    ProfileScreen(),            // Index 4: 👤 Profile
  ];

  @override
  Widget build(BuildContext context) {
    final theme = AnonGramTheme.of(context);

    return Scaffold(
      backgroundColor: theme.bgDark,
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: Container(
        height: 64,
        decoration: BoxDecoration(
          color: theme.surfaceDark,
          border: Border(top: BorderSide(color: Colors.white.withOpacity(0.08))),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            // 1. Chats
            _navItem(Icons.chat_bubble_outline, "Чаты", 0, theme),
            // 2. Services
            _navItem(Icons.shield_outlined, "Сервисы", 1, theme),

            // 3. CENTER ELEVATED FLOATING QR SCANNER BUTTON
            Transform.translate(
              offset: const Offset(0, -14),
              child: GestureDetector(
                onTap: () => setState(() => _currentIndex = 2),
                child: Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [theme.neonAccent, theme.secondaryAccent],
                    ),
                    borderRadius: BorderRadius.circular(18),
                    boxShadow: [
                      BoxShadow(
                        color: theme.neonAccent.withOpacity(0.4),
                        blurRadius: 16,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                  child: const Center(
                    child: Icon(Icons.qr_code_scanner, color: Colors.black, size: 28),
                  ),
                ),
              ),
            ),

            // 4. Settings
            _navItem(Icons.settings_outlined, "Настройки", 3, theme),
            // 5. Profile
            _navItem(Icons.person_outline, "Профиль", 4, theme),
          ],
        ),
      ),
    );
  }

  Widget _navItem(IconData icon, String label, int index, AnonGramThemeExtension theme) {
    final isSelected = _currentIndex == index;
    return InkWell(
      onTap: () => setState(() => _currentIndex = index),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 20,
              color: isSelected ? theme.secondaryAccent : Colors.white54,
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: TextStyle(
                fontSize: 10,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                color: isSelected ? theme.secondaryAccent : Colors.white54,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
