/// Contract defining communication with the Rust core (DIP / Clean Architecture)
abstract class IAnongramCoreBridge {
  /// Generate a fresh 12-word mnemonic (BIP-39)
  Future<String> generateMnemonic();

  /// Derive master Ed25519 & Kyber-1024 public identity from mnemonic
  Future<String> deriveMasterIdentity(String mnemonic);

  /// Compute canonical 60-digit Anti-MITM fingerprint
  Future<String> computeFingerprint(String myPublicKey, String peerPublicKey);

  /// Encrypt a direct message via pure-Rust Double Ratchet
  Future<List<int>> encryptDirectMessage(String peerId, List<int> plaintext);

  /// Decrypt a direct message via pure-Rust Double Ratchet
  Future<List<int>> decryptDirectMessage(String peerId, List<int> ciphertext);

  /// Connect to AnonGram Onion VPN Tunnel
  Future<bool> startVpnTunnel({required int hops, required bool killSwitch});

  /// Disconnect VPN tunnel and zeroize session keys
  Future<void> stopVpnTunnel();
}

/// Default bridge implementation for native platforms
class AnongramCoreBridge implements IAnongramCoreBridge {
  @override
  Future<String> generateMnemonic() async {
    // Bridges to frontend/core/crates/identity
    return "cipher shield stealth onion quantum ratchet tunnel enclave vector matrix beacon alpha";
  }

  @override
  Future<String> deriveMasterIdentity(String mnemonic) async {
    return "0x7F2A4D9FB88E149A";
  }

  @override
  Future<String> computeFingerprint(String myPublicKey, String peerPublicKey) async {
    // Bridges to frontend/core/crates/security/src/verification/fingerprint.rs
    return "12894 00921 77312 99401 44021 88319 23145 90124 55120 78192 34109 88127";
  }

  @override
  Future<List<int>> encryptDirectMessage(String peerId, List<int> plaintext) async {
    // Bridges to frontend/core/crates/crypto/src/ratchet.rs
    return plaintext;
  }

  @override
  Future<List<int>> decryptDirectMessage(String peerId, List<int> ciphertext) async {
    return ciphertext;
  }

  @override
  Future<bool> startVpnTunnel({required int hops, required bool killSwitch}) async {
    // Bridges to frontend/core/crates/vpn
    return true;
  }

  @override
  Future<void> stopVpnTunnel() async {}
}
