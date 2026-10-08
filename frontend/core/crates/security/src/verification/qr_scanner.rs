//! In-Person QR Code Safety Scanner & Generator.
//!
//! Encodes cryptographic identity parameters into a compact QR binary payload,
//! and verifies peer credentials scanned by the device camera.

use crate::error::SecurityError;

const QR_MAGIC: &[u8; 4] = b"AGQR";
const QR_VERSION: u8 = 1;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct QrVerificationResult {
    pub peer_id: [u8; 32],
    pub public_key: [u8; 32],
    pub timestamp: u64,
    pub is_verified: bool,
}

pub struct QrSafetyScanner;

impl QrSafetyScanner {
    /// Generates compact binary QR payload for the user's screen.
    pub fn generate_payload(local_id: &[u8; 32], public_key: &[u8; 32], timestamp: u64) -> Vec<u8> {
        let mut payload = Vec::with_capacity(4 + 1 + 32 + 32 + 8);
        payload.extend_from_slice(QR_MAGIC);
        payload.push(QR_VERSION);
        payload.extend_from_slice(local_id);
        payload.extend_from_slice(public_key);
        payload.extend_from_slice(&timestamp.to_be_bytes());
        payload
    }

    /// Validates QR code scanned from peer's device camera against expected peer ID and public key.
    pub fn validate_scanned(
        scanned_bytes: &[u8],
        expected_peer_id: &[u8; 32],
        expected_public_key: &[u8; 32],
    ) -> Result<QrVerificationResult, SecurityError> {
        if scanned_bytes.len() < 77 {
            return Err(SecurityError::InvalidQrPayload);
        }

        // Validate Magic
        if &scanned_bytes[0..4] != QR_MAGIC {
            return Err(SecurityError::InvalidQrPayload);
        }

        // Validate Version
        if scanned_bytes[4] != QR_VERSION {
            return Err(SecurityError::InvalidQrPayload);
        }

        let mut peer_id = [0u8; 32];
        peer_id.copy_from_slice(&scanned_bytes[5..37]);

        let mut public_key = [0u8; 32];
        public_key.copy_from_slice(&scanned_bytes[37..69]);

        let mut ts_bytes = [0u8; 8];
        ts_bytes.copy_from_slice(&scanned_bytes[69..77]);
        let timestamp = u64::from_be_bytes(ts_bytes);

        // Verification matches
        let is_verified = (&peer_id == expected_peer_id) && (&public_key == expected_public_key);

        if !is_verified {
            return Err(SecurityError::FingerprintMismatch);
        }

        Ok(QrVerificationResult {
            peer_id,
            public_key,
            timestamp,
            is_verified: true,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_qr_roundtrip_verification() {
        let bob_id = [0x42u8; 32];
        let bob_key = [0x99u8; 32];
        let ts = 1728470000;

        // Bob shows his QR code on his phone
        let qr_payload = QrSafetyScanner::generate_payload(&bob_id, &bob_key, ts);

        // Alice scans Bob's phone with camera
        let result = QrSafetyScanner::validate_scanned(&qr_payload, &bob_id, &bob_key)
            .expect("Valid QR scan");
        assert!(result.is_verified);
        assert_eq!(result.peer_id, bob_id);
        assert_eq!(result.public_key, bob_key);

        // Eve tries to show a fake QR with different key
        let eve_key = [0x66u8; 32];
        let fake_payload = QrSafetyScanner::generate_payload(&bob_id, &eve_key, ts);
        let err = QrSafetyScanner::validate_scanned(&fake_payload, &bob_id, &bob_key);
        assert_eq!(err, Err(SecurityError::FingerprintMismatch));
    }
}
