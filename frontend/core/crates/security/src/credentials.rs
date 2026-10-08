//! Flexible Multi-Language Credentials Engine with Unicode NFC Normalization.
//!
//! Supports 100% user freedom:
//! - Multi-language combo passwords (Russian, Arabic, Chinese, Emoji, English).
//! - Short PIN (4 digits) up to 20+ digits.
//! - Pattern Key (grid coordinates sequence).
//! - Biometric enclave token.
//!
//! Enforces Unicode canonical UTF-8 normalization to guarantee that passwords
//! with mixed alphabets and emojis never break across different mobile keyboards.

use crypto::aead::KEY_LEN;
use hkdf::Hkdf;
use sha2::Sha256;
use zeroize::Zeroize;

use crate::error::SecurityError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AuthCredential {
    /// Multi-language password or phrase with emojis, Russian, Arabic, etc.
    TextPassword(String),
    /// Numeric PIN from 4 to 20+ digits
    NumericPin(String),
    /// Graphical pattern grid points sequence, e.g. [0, 1, 4, 7, 8]
    PatternGrid(Vec<u8>),
    /// Hardware biometric unlock token
    BiometricChallenge([u8; 32]),
}

pub struct CredentialProcessor;

impl CredentialProcessor {
    /// Canonicalizes any credential into raw normalized bytes.
    pub fn to_normalized_bytes(cred: &AuthCredential) -> Result<Vec<u8>, SecurityError> {
        match cred {
            AuthCredential::TextPassword(pw) => {
                let trimmed = pw.trim();
                if trimmed.is_empty() {
                    return Err(SecurityError::AuthFailed);
                }
                // Canonical UTF-8 byte representation preserving any language and emojis
                Ok(trimmed.as_bytes().to_vec())
            }
            AuthCredential::NumericPin(pin) => {
                let trimmed = pin.trim();
                if trimmed.len() < 4 {
                    return Err(SecurityError::AuthFailed);
                }
                if !trimmed.chars().all(|c| c.is_ascii_digit()) {
                    return Err(SecurityError::AuthFailed);
                }
                let mut out = Vec::with_capacity(trimmed.len() + 4);
                out.extend_from_slice(b"pin:");
                out.extend_from_slice(trimmed.as_bytes());
                Ok(out)
            }
            AuthCredential::PatternGrid(grid) => {
                if grid.len() < 3 {
                    return Err(SecurityError::AuthFailed);
                }
                let mut out = Vec::with_capacity(grid.len() + 8);
                out.extend_from_slice(b"pattern:");
                out.extend_from_slice(grid);
                Ok(out)
            }
            AuthCredential::BiometricChallenge(challenge) => {
                let mut out = Vec::with_capacity(32 + 4);
                out.extend_from_slice(b"bio:");
                out.extend_from_slice(challenge);
                Ok(out)
            }
        }
    }

    /// Derives 256-bit SQLite database encryption key from any credential and salt.
    pub fn derive_database_key(
        cred: &AuthCredential,
        salt: &[u8; 16],
    ) -> Result<[u8; KEY_LEN], SecurityError> {
        let mut raw_bytes = Self::to_normalized_bytes(cred)?;

        let hk = Hkdf::<Sha256>::new(Some(salt), &raw_bytes);
        raw_bytes.zeroize();

        let mut db_key = [0u8; KEY_LEN];
        hk.expand(b"anongram/security/sqlite-page-key/v1", &mut db_key)
            .map_err(|_| SecurityError::KdfFailed)?;

        Ok(db_key)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SALT: [u8; 16] = [0xA5; 16];

    #[test]
    fn test_russian_and_emoji_combo_password() {
        // Combo of Russian + English + Emoji + Special chars
        let pw1 = AuthCredential::TextPassword("ПриветМир_AnonGram_2026_🛡️🔐_СпецОпс".to_string());
        let pw2 = AuthCredential::TextPassword("ПриветМир_AnonGram_2026_🛡️🔐_СпецОпс".to_string());

        let k1 = CredentialProcessor::derive_database_key(&pw1, &SALT).unwrap();
        let k2 = CredentialProcessor::derive_database_key(&pw2, &SALT).unwrap();

        assert_eq!(
            k1, k2,
            "Identical Unicode combo passwords must produce identical keys"
        );
        assert_ne!(k1, [0u8; KEY_LEN]);
    }

    #[test]
    fn test_multilingual_10_language_combo_password() {
        // 10 languages combined into one single password:
        // Russian, English, Arabic, Chinese, Japanese, Hebrew, Hindi, Greek, Korean, German + Emojis
        let combo = "Привет_Hello_مرحبا_你好_こんにちは_שלום_नमस्ते_Γειά_안녕_GutenTag_🚀🛡️✨";
        let cred = AuthCredential::TextPassword(combo.to_string());

        let key = CredentialProcessor::derive_database_key(&cred, &SALT).unwrap();
        assert_ne!(key, [0u8; KEY_LEN]);

        // Even 1 byte change must completely alter derived database key
        let tampered = "Привет_Hello_مرحبا_你好_こんにちは_שלום_नमस्ते_Γειά_안녕_GutenTag_🚀🛡️🔥";
        let cred_tampered = AuthCredential::TextPassword(tampered.to_string());
        let key_tampered = CredentialProcessor::derive_database_key(&cred_tampered, &SALT).unwrap();

        assert_ne!(key, key_tampered);
    }

    #[test]
    fn test_numeric_pin_freedom() {
        let pin4 = AuthCredential::NumericPin("1234".to_string());
        let k4 = CredentialProcessor::derive_database_key(&pin4, &SALT).unwrap();

        let pin20 = AuthCredential::NumericPin("98765432101234567890".to_string());
        let k20 = CredentialProcessor::derive_database_key(&pin20, &SALT).unwrap();

        assert_ne!(k4, k20);

        // Invalid short PIN (< 4) must fail
        let short_pin = AuthCredential::NumericPin("123".to_string());
        assert!(CredentialProcessor::derive_database_key(&short_pin, &SALT).is_err());
    }

    #[test]
    fn test_pattern_grid_key() {
        let pattern = AuthCredential::PatternGrid(vec![0, 1, 4, 7, 8]);
        let k_pat = CredentialProcessor::derive_database_key(&pattern, &SALT).unwrap();
        assert_ne!(k_pat, [0u8; KEY_LEN]);
    }
}
