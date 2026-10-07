//! Biometrics and Hardware Enclave Bridge.

use crypto::aead::KEY_LEN;
use zeroize::Zeroize;

use crate::error::SecurityError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BiometricType {
    Fingerprint,
    FaceId,
    HardwarePin,
}

pub struct BiometricAuth;

impl BiometricAuth {
    /// Simulates hardware keystore key release upon successful biometric prompt.
    pub fn release_enclave_key(
        bio_type: BiometricType,
        challenge_token: &[u8; 32],
    ) -> Result<[u8; KEY_LEN], SecurityError> {
        // Enforce challenge token validation
        if challenge_token.iter().all(|&b| b == 0) {
            return Err(SecurityError::AuthFailed);
        }

        // In mobile production, calls Android BiometricPrompt / iOS LocalAuthentication
        let mut key = [0u8; KEY_LEN];
        for (i, b) in key.iter_mut().enumerate() {
            *b = challenge_token[i] ^ (bio_type as u8 + 0x33);
        }

        Ok(key)
    }

    /// Safely clears secret memory buffer.
    pub fn purge_key(key: &mut [u8; KEY_LEN]) {
        key.zeroize();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_biometric_release_and_purge() {
        let challenge = [0x77u8; 32];
        let mut key =
            BiometricAuth::release_enclave_key(BiometricType::Fingerprint, &challenge).unwrap();
        assert_ne!(key, [0u8; KEY_LEN]);

        BiometricAuth::purge_key(&mut key);
        assert_eq!(key, [0u8; KEY_LEN]);
    }
}
