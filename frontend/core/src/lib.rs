//! AnonGram core.
//!
//! Planned modules (each added in its own commit, with tests):
//! - `kem`      hybrid X25519 + ML-KEM-1024
//! - `sign`     hybrid Ed25519 + ML-DSA-87
//! - `aead`     ChaCha20-Poly1305 / AES-256-GCM
//! - `identity` BIP-39, key derivation, Passport Blob
//! - `otp`      one-time pad: two-ended pointers, Wegman-Carter MAC, wiping
//! - `manifest` Pad Manifest and protection level labels

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
