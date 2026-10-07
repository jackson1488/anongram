//! Authenticated encryption: XChaCha20-Poly1305 (256-bit key, 192-bit random nonce).
//!
//! The 192-bit nonce makes random nonces safe. Output layout: `nonce(24) || ciphertext || tag(16)`.

use chacha20poly1305::{
    aead::{Aead, KeyInit, Payload},
    XChaCha20Poly1305, XNonce,
};
use rand::{rngs::OsRng, RngCore};

use crate::error::CryptoError;

pub const KEY_LEN: usize = 32;
pub const NONCE_LEN: usize = 24;
pub const TAG_LEN: usize = 16;

/// Encrypts `plaintext` and binds `aad` (associated data) to the ciphertext.
pub fn seal(key: &[u8; KEY_LEN], aad: &[u8], plaintext: &[u8]) -> Result<Vec<u8>, CryptoError> {
    let cipher = XChaCha20Poly1305::new(key.into());
    let mut nonce = [0u8; NONCE_LEN];
    OsRng.fill_bytes(&mut nonce);
    let ct = cipher
        .encrypt(
            XNonce::from_slice(&nonce),
            Payload {
                msg: plaintext,
                aad,
            },
        )
        .map_err(|_| CryptoError::Encrypt)?;
    let mut out = Vec::with_capacity(NONCE_LEN + ct.len());
    out.extend_from_slice(&nonce);
    out.extend_from_slice(&ct);
    Ok(out)
}

/// Decrypts data produced by [`seal`]. Fails if the key, aad or data were changed.
pub fn open(key: &[u8; KEY_LEN], aad: &[u8], sealed: &[u8]) -> Result<Vec<u8>, CryptoError> {
    if sealed.len() < NONCE_LEN + TAG_LEN {
        return Err(CryptoError::Malformed);
    }
    let (nonce, ct) = sealed.split_at(NONCE_LEN);
    let cipher = XChaCha20Poly1305::new(key.into());
    cipher
        .decrypt(XNonce::from_slice(nonce), Payload { msg: ct, aad })
        .map_err(|_| CryptoError::Decrypt)
}

#[cfg(test)]
mod tests {
    use super::*;

    const KEY: [u8; KEY_LEN] = [7u8; KEY_LEN];

    #[test]
    fn roundtrip() {
        let sealed = seal(&KEY, b"hdr", b"hello").unwrap();
        assert_eq!(open(&KEY, b"hdr", &sealed).unwrap(), b"hello");
    }

    #[test]
    fn wrong_key_fails() {
        let sealed = seal(&KEY, b"", b"hello").unwrap();
        assert_eq!(
            open(&[8u8; KEY_LEN], b"", &sealed),
            Err(CryptoError::Decrypt)
        );
    }

    #[test]
    fn wrong_aad_fails() {
        let sealed = seal(&KEY, b"a", b"hello").unwrap();
        assert_eq!(open(&KEY, b"b", &sealed), Err(CryptoError::Decrypt));
    }

    #[test]
    fn tampering_fails() {
        let mut sealed = seal(&KEY, b"", b"hello").unwrap();
        let last = sealed.len() - 1;
        sealed[last] ^= 1;
        assert_eq!(open(&KEY, b"", &sealed), Err(CryptoError::Decrypt));
    }

    #[test]
    fn too_short_is_malformed() {
        assert_eq!(open(&KEY, b"", &[0u8; 10]), Err(CryptoError::Malformed));
    }

    #[test]
    fn nonces_are_unique() {
        let a = seal(&KEY, b"", b"x").unwrap();
        let b = seal(&KEY, b"", b"x").unwrap();
        assert_ne!(a[..NONCE_LEN], b[..NONCE_LEN]);
    }
}
