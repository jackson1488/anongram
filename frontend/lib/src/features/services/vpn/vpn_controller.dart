import 'package:flutter/foundation.dart';

/// Current lifecycle state of the background VPN tunnel
enum VpnTunnelState {
  disconnected,
  connecting,
  connected,
  killswitchActive,
}

/// Contract for controlling the integrated Onion VPN tunnel
abstract class IVpnController extends Listenable {
  VpnTunnelState get state;
  bool get isKillSwitchEnabled;
  int get currentPingMs;
  String get activeNodeLocation;

  Future<void> connect();
  Future<void> disconnect();
  void setKillSwitch(bool enabled);
}

/// Concrete implementation of the VPN controller
class VpnController extends ChangeNotifier implements IVpnController {
  VpnTunnelState _state = VpnTunnelState.connected;
  bool _killSwitch = true;
  int _ping = 24;
  String _location = "Rotterdam (Onion 3-Hop)";

  @override
  VpnTunnelState get state => _state;
  @override
  bool get isKillSwitchEnabled => _killSwitch;
  @override
  int get currentPingMs => _ping;
  @override
  String get activeNodeLocation => _location;

  @override
  Future<void> connect() async {
    _state = VpnTunnelState.connecting;
    notifyListeners();

    await Future.delayed(const Duration(milliseconds: 300));
    _state = VpnTunnelState.connected;
    _ping = 24;
    notifyListeners();
  }

  @override
  Future<void> disconnect() async {
    _state = VpnTunnelState.disconnected;
    _ping = 0;
    notifyListeners();
  }

  @override
  void setKillSwitch(bool enabled) {
    _killSwitch = enabled;
    if (!enabled && _state == VpnTunnelState.killswitchActive) {
      _state = VpnTunnelState.disconnected;
    }
    notifyListeners();
  }

  void triggerKillSwitchAlarm() {
    _state = VpnTunnelState.killswitchActive;
    notifyListeners();
  }
}
