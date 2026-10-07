//! AnonGram Modular Core Facade.
//!
//! Re-exports autonomous sub-crates:
//! - `crypto`: Hybrid Post-Quantum KEM, X25519, ML-DSA, Ed25519, XChaCha20-Poly1305.
//! - `otp`: Two-ended One-Time Pad engine with Wegman-Carter Poly1305 MAC and zeroize.
//! - `media`: 20+ format metadata sanitization, Zstd compression, and encrypted blobs.
//! - `identity`: BIP-39 mnemonic engine and signed Passport Blob.
//! - `vpn`: Multi-line anti-censorship VPN (AmneziaWG, Shadowsocks 2022, VLESS-Reality).

pub use crypto;
pub use identity;
pub use media;
pub use otp;
pub use vpn;

/// Core version string.
pub fn version() -> &'static str {
    env!("CARGO_PKG_VERSION")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn version_is_not_empty() {
        assert!(!version().is_empty());
    }
}
