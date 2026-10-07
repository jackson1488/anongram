//! Master Password KDF (Key Derivation Function).
//!
//! Synthesizes secure 256-bit database encryption and session unlock keys from master passphrase.

use crypto::aead::KEY_LEN;
use hkdf::Hkdf;
use sha2::Sha256;
use zeroize::Zeroize;

use crate::error::SecurityError;

pub struct PasswordKdf;

impl PasswordKdf {
    /// Derives 256-bit database encryption key from password and salt.
    pub fn derive_key(password: &str, salt: &[u8; 16]) -> Result<[u8; KEY_LEN], SecurityError> {
        if password.is_empty() {
            return Err(SecurityError::AuthFailed);
        }

        let hk = Hkdf::<Sha256>::new(Some(salt), password.as_bytes());
        let mut okm = [0u8; KEY_LEN];
        hk.expand(b"anongram/security/password-kdf/v1", &mut okm)
            .map_err(|_| SecurityError::KdfFailed)?;

        Ok(okm)
    }

    /// Verifies password against stored verification hash.
    pub fn verify_password(password: &str, salt: &[u8; 16], expected: &[u8; KEY_LEN]) -> bool {
        match Self::derive_key(password, salt) {
            Ok(mut derived) => {
                let matches = &derived == expected;
                derived.zeroize();
                matches
            }
            Err(_) => false,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_password_derivation_deterministic() {
        let salt = [0x42u8; 16];
        let k1 = PasswordKdf::derive_key("SuperSecretPassword123!", &salt).unwrap();
        let k2 = PasswordKdf::derive_key("SuperSecretPassword123!", &salt).unwrap();
        assert_eq!(k1, k2);

        let k3 = PasswordKdf::derive_key("WrongPassword", &salt).unwrap();
        assert_ne!(k1, k3);
    }
}
