//! AnonGram core.
//!
//! Planned modules (each added in its own commit, with tests):
//! - `aead`     XChaCha20-Poly1305 (done)
//! - `kem`      hybrid X25519 + ML-KEM-1024
//! - `sign`     hybrid Ed25519 + ML-DSA-87
//! - `identity` BIP-39, key derivation, Passport Blob
//! - `otp`      one-time pad: two-ended pointers, Wegman-Carter MAC, wiping
//! - `manifest` Pad Manifest and protection level labels

pub mod aead;
pub mod error;
pub mod kem;
pub mod passport;
pub mod seed;
pub mod sign;

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
