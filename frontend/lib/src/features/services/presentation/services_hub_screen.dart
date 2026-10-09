import 'package:flutter/material.dart';
import '../../../core/theme/anongram_theme.dart';
import '../vpn/vpn_controller.dart';

/// Screen 2: Services & VPN Hub
class ServicesHubScreen extends StatefulWidget {
  const ServicesHubScreen({super.key});

  @override
  State<ServicesHubScreen> createState() => _ServicesHubScreenState();
}

class _ServicesHubScreenState extends State<ServicesHubScreen> {
  final VpnController _vpn = VpnController();

  @override
  Widget build(BuildContext context) {
    final theme = AnonGramTheme.of(context);

    return Scaffold(
      backgroundColor: theme.bgDark,
      appBar: AppBar(
        backgroundColor: theme.surfaceDark,
        elevation: 0,
        title: const Text(
          "Сервисы Приватности",
          style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: Colors.white),
        ),
      ),
      body: AnimatedBuilder(
        animation: _vpn,
        builder: (context, _) {
          final isConnected = _vpn.state == VpnTunnelState.connected;

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // VPN Big Dashboard Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: theme.surfaceDark,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: isConnected ? theme.secondaryAccent.withOpacity(0.5) : Colors.white12,
                  ),
                ),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Icon(Icons.shield, color: isConnected ? theme.secondaryAccent : Colors.grey, size: 24),
                            const SizedBox(width: 8),
                            const Text("AnonGram VPN", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: isConnected ? theme.secondaryAccent.withOpacity(0.2) : Colors.grey.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            isConnected ? "ПОДКЛЮЧЕНО" : "ОТКЛЮЧЕНО",
                            style: TextStyle(color: isConnected ? theme.secondaryAccent : Colors.grey, fontSize: 10, fontWeight: FontWeight.bold),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _metricItem("Шифрование", "ChaCha20-Poly1305", theme),
                        _metricItem("Трасса", _vpn.activeNodeLocation, theme),
                        _metricItem("Пинг", "${_vpn.currentPingMs} ms", theme),
                      ],
                    ),
                    const Divider(color: Colors.white10, height: 24),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text("Аварийный Kill-Switch", style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
                            Text("Блокировать утечки IP при сбое", style: TextStyle(color: Colors.white54, fontSize: 10)),
                          ],
                        ),
                        Switch(
                          value: _vpn.isKillSwitchEnabled,
                          activeColor: theme.secondaryAccent,
                          onChanged: (val) => _vpn.setKillSwitch(val),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: isConnected ? Colors.red.withOpacity(0.2) : theme.secondaryAccent,
                          foregroundColor: isConnected ? Colors.redAccent : Colors.black,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        onPressed: () {
                          if (isConnected) {
                            _vpn.disconnect();
                          } else {
                            _vpn.connect();
                          }
                        },
                        child: Text(
                          isConnected ? "Отключить туннель" : "Включить VPN",
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 20),
              const Text("ДЕЦЕНТРАЛИЗОВАННЫЕ ИНСТРУМЕНТЫ", style: TextStyle(fontSize: 10, color: Colors.white38, fontFamily: 'monospace')),
              const SizedBox(height: 10),

              _toolTile(Icons.storage, "Zero-Knowledge Хранилище", "P2P зашифрованные файлы до 10 ГБ", theme),
              _toolTile(Icons.smart_toy, "Анонимные AI-Боты", "Локальный запуск без передачи промптов", theme),
              _toolTile(Icons.wifi_tethering, "Mesh-Сеть (Без Интернета)", "BLE и Wi-Fi Direct E2EE передача", theme),
            ],
          );
        },
      ),
    );
  }

  Widget _metricItem(String label, String value, AnonGramThemeExtension theme) {
    return Column(
      children: [
        Text(label, style: const TextStyle(color: Colors.white38, fontSize: 9)),
        const SizedBox(height: 2),
        Text(value, style: TextStyle(color: theme.neonAccent, fontSize: 11, fontFamily: 'monospace', fontWeight: FontWeight.bold)),
      ],
    );
  }

  Widget _toolTile(IconData icon, String title, String subtitle, AnonGramThemeExtension theme) {
    return Card(
      color: theme.surfaceDark,
      margin: const EdgeInsets.only(bottom: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: ListTile(
        leading: Icon(icon, color: theme.neonAccent, size: 22),
        title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold)),
        subtitle: Text(subtitle, style: const TextStyle(color: Colors.white54, fontSize: 10)),
        trailing: const Icon(Icons.chevron_right, color: Colors.white38, size: 18),
        onTap: () {},
      ),
    );
  }
}
