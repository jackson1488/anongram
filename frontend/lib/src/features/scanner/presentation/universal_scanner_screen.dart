import 'package:flutter/material.dart';
import '../../../core/theme/anongram_theme.dart';
import '../core/qr_dispatcher.dart';
import '../core/qr_payload_interface.dart';
import '../friend/friend_qr_handler.dart';
import '../device_link/device_link_qr_handler.dart';
import '../verification/verification_qr_handler.dart';

/// Screen 3: Universal Multi-Purpose QR Scanner
class UniversalScannerScreen extends StatefulWidget {
  const UniversalScannerScreen({super.key});

  @override
  State<UniversalScannerScreen> createState() => _UniversalScannerScreenState();
}

class _UniversalScannerScreenState extends State<UniversalScannerScreen> {
  late final QrPayloadDispatcher _dispatcher;
  bool _isMyCodeMode = false;
  QrDispatchResult? _lastResult;

  @override
  void initState() {
    super.initState();
    _dispatcher = QrPayloadDispatcher([
      FriendQrHandler(),
      DeviceLinkQrHandler(),
      VerificationQrHandler(),
    ]);
  }

  void _testScan(String payload) async {
    final result = await _dispatcher.dispatch(context, payload);
    setState(() => _lastResult = result);
  }

  @override
  Widget build(BuildContext context) {
    final theme = AnonGramTheme.of(context);

    return Scaffold(
      backgroundColor: theme.bgDark,
      appBar: AppBar(
        backgroundColor: theme.surfaceDark,
        elevation: 0,
        title: const Text(
          "Универсальный Сканер",
          style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
        ),
        actions: [
          TextButton.icon(
            icon: Icon(_isMyCodeMode ? Icons.camera_alt : Icons.qr_code, color: theme.neonAccent, size: 16),
            label: Text(
              _isMyCodeMode ? "Камера" : "Мой QR",
              style: TextStyle(color: theme.neonAccent, fontSize: 12),
            ),
            onPressed: () => setState(() {
              _isMyCodeMode = !_isMyCodeMode;
              _lastResult = null;
            }),
          ),
        ],
      ),
      body: _isMyCodeMode ? _buildMyQrView(theme) : _buildScannerView(theme),
    );
  }

  Widget _buildScannerView(AnonGramThemeExtension theme) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Camera Viewfinder Box
        Center(
          child: Container(
            width: 240,
            height: 240,
            decoration: BoxDecoration(
              color: Colors.black,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: theme.secondaryAccent.withOpacity(0.6), width: 2),
            ),
            child: Stack(
              alignment: Alignment.center,
              children: [
                Icon(Icons.qr_code_scanner, size: 72, color: Colors.white.withOpacity(0.15)),
                Positioned(
                  bottom: 16,
                  child: Text(
                    "Наведите камеру на QR-код",
                    style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 11),
                  ),
                ),
              ],
            ),
          ),
        ),

        const SizedBox(height: 20),
        const Text(
          "БЫСТРЫЙ ТЕСТ ПАРСИНГА QR-КОДОВ:",
          style: TextStyle(fontSize: 10, color: Colors.white38, fontFamily: 'monospace'),
        ),
        const SizedBox(height: 8),

        _testButton("👤 Добавить друга", "AGQR:USER:0x98A1B2C3:Bob", theme),
        _testButton("💻 Связать Desktop/Web", "AGQR:LINK:session_token_xyz987", theme),
        _testButton("🛡️ Anti-MITM Проверка ключей", "AGQR:VERIFY:128940092177312", theme),

        if (_lastResult != null) ...[
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: theme.surfaceDark,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: theme.secondaryAccent),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.check_circle, color: theme.secondaryAccent, size: 18),
                    const SizedBox(width: 8),
                    Text(
                      _lastResult!.title,
                      style: TextStyle(color: theme.secondaryAccent, fontWeight: FontWeight.bold, fontSize: 13),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(_lastResult!.description, style: const TextStyle(color: Colors.white70, fontSize: 11)),
              ],
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildMyQrView(AnonGramThemeExtension theme) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(24),
              ),
              child: const Icon(Icons.qr_code, size: 180, color: Colors.black),
            ),
            const SizedBox(height: 20),
            const Text("Мой Анонимный ID", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
            const SizedBox(height: 4),
            Text("0x4D9F...88E1 (Ed25519)", style: TextStyle(color: theme.neonAccent, fontFamily: 'monospace', fontSize: 11)),
            const SizedBox(height: 8),
            const Text(
              "Покажите этот код для добавления в контакты или Anti-MITM проверки ключей",
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.white54, fontSize: 10),
            ),
          ],
        ),
      ),
    );
  }

  Widget _testButton(String title, String payload, AnonGramThemeExtension theme) {
    return Card(
      color: theme.surfaceDark,
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        dense: true,
        title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold)),
        subtitle: Text(payload, style: const TextStyle(color: Colors.white38, fontSize: 9, fontFamily: 'monospace')),
        trailing: Icon(Icons.play_arrow, color: theme.neonAccent, size: 18),
        onTap: () => _testScan(payload),
      ),
    );
  }
}
