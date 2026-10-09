import 'package:flutter/material.dart';
import '../../../core/theme/anongram_theme.dart';

/// Screen 5: Profile & Identity Passport Screen
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = AnonGramTheme.of(context);

    return Scaffold(
      backgroundColor: theme.bgDark,
      appBar: AppBar(
        backgroundColor: theme.surfaceDark,
        elevation: 0,
        title: const Text(
          "Мой Профиль",
          style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Identity Passport Card
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: theme.surfaceDark,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: Colors.white10),
            ),
            child: Column(
              children: [
                CircleAvatar(
                  radius: 36,
                  backgroundColor: theme.cardDark,
                  child: const Text("🦊", style: TextStyle(fontSize: 36)),
                ),
                const SizedBox(height: 12),
                const Text(
                  "Cypherpunk #77",
                  style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                ),
                const SizedBox(height: 4),
                Text(
                  "0x7F2A...9C14 (Ed25519)",
                  style: TextStyle(color: theme.neonAccent, fontFamily: 'monospace', fontSize: 11),
                ),
                const SizedBox(height: 10),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                  decoration: BoxDecoration(
                    color: theme.secondaryAccent.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: theme.secondaryAccent.withOpacity(0.3)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.shield, size: 12, color: theme.secondaryAccent),
                      const SizedBox(width: 4),
                      Text(
                        "Kyber-1024 Post-Quantum Ready",
                        style: TextStyle(color: theme.secondaryAccent, fontSize: 9, fontFamily: 'monospace'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // 60-digit Fingerprint Block
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: theme.surfaceDark,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: Colors.white10),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  "60-ЗНАЧНЫЙ ОТПЕЧАТОК БЕЗОПАСНОСТИ",
                  style: TextStyle(fontSize: 9, color: Colors.white38, fontFamily: 'monospace'),
                ),
                const SizedBox(height: 8),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.black,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    "12894 00921 77312 99401\n44021 88319 23145 90124\n55120 78192 34109 88127",
                    style: TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 11,
                      color: theme.secondaryAccent,
                      height: 1.5,
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                const Text(
                  "Используется для сверки при личной встрече или защищенном звонке",
                  style: TextStyle(color: Colors.white38, fontSize: 9),
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // Seed phrase export button
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: theme.surfaceDark,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: const BorderSide(color: Colors.white12),
              ),
            ),
            icon: const Icon(Icons.key, size: 16),
            label: const Text("Экспорт Резервной Seed-Фразы", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
            onPressed: () {},
          ),
        ],
      ),
    );
  }
}
