//! Pad Manifest: metadata describing physical pad identity, security rating, and bounds.
//!
//! Labels:
//! - Cable (Direct USB/OTG): Gold Shield (Level 3 Absolute Information-Theoretic)
//! - Wireless Direct (WiFi-Direct / Bluetooth with QR secret handshake): Blue Shield (High Security PQ-Hybrid)

use sha2::{Digest, Sha256};
use crate::error::CoreError;
use crate::otp::PadSide;
use crate::sign::{HybridSignature, HybridSigningKey, HybridVerifyingKey};

const MANIFEST_MAGIC: &[u8; 4] = b"AGPM";
const MANIFEST_VERSION: u8 = 1;
const PREFIX: &[u8] = b"anongram/manifest/v1";

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TransferMethod {
    Cable,           // Absolute Level 3 (Gold)
    WifiDirectQr,    // High Level PQ (Blue)
    BluetoothQr,     // High Level PQ (Blue)
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SecurityShieldLevel {
    GoldAbsolute,
    BlueHigh,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PadManifest {
    pub pad_id: [u8; 32],
    pub total_size: u64,
    pub reserve_pct: u8,
    pub partner_id: String,
    pub local_side: PadSide,
    pub transfer_method: TransferMethod,
    pub created_at: u64,
    pub head_offset: u64,
    pub tail_offset: u64,
}

impl PadManifest {
    pub fn shield_level(&self) -> SecurityShieldLevel {
        match self.transfer_method {
            TransferMethod::Cable => SecurityShieldLevel::GoldAbsolute,
            TransferMethod::WifiDirectQr | TransferMethod::BluetoothQr => {
                SecurityShieldLevel::BlueHigh
            }
        }
    }

    pub fn is_exhausted(&self) -> bool {
        self.head_offset >= self.tail_offset
    }

    pub fn serialize_body(&self) -> Vec<u8> {
        let mut out = Vec::new();
        out.extend_from_slice(MANIFEST_MAGIC);
        out.push(MANIFEST_VERSION);
        out.extend_from_slice(&self.pad_id);
        out.extend_from_slice(&self.total_size.to_be_bytes());
        out.push(self.reserve_pct);

        let p_bytes = self.partner_id.as_bytes();
        out.extend_from_slice(&(p_bytes.len() as u32).to_be_bytes());
        out.extend_from_slice(p_bytes);

        out.push(match self.local_side {
            PadSide::SideA => 0,
            PadSide::SideB => 1,
        });

        out.push(match self.transfer_method {
            TransferMethod::Cable => 0,
            TransferMethod::WifiDirectQr => 1,
            TransferMethod::BluetoothQr => 2,
        });

        out.extend_from_slice(&self.created_at.to_be_bytes());
        out.extend_from_slice(&self.head_offset.to_be_bytes());
        out.extend_from_slice(&self.tail_offset.to_be_bytes());
        out
    }

    pub fn sign(&self, signing_key: &HybridSigningKey) -> SignedManifest {
        let body = self.serialize_body();
        let mut msg = Vec::with_capacity(PREFIX.len() + body.len());
        msg.extend_from_slice(PREFIX);
        msg.extend_from_slice(&body);
        let signature = signing_key.sign(&msg);
        SignedManifest {
            manifest: self.clone(),
            signature,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SignedManifest {
    pub manifest: PadManifest,
    pub signature: HybridSignature,
}

impl SignedManifest {
    pub fn verify(&self, verifying_key: &HybridVerifyingKey) -> Result<(), CoreError> {
        let body = self.manifest.serialize_body();
        let mut msg = Vec::with_capacity(PREFIX.len() + body.len());
        msg.extend_from_slice(PREFIX);
        msg.extend_from_slice(&body);
        verifying_key.verify(&msg, &self.signature)
    }

    pub fn manifest_fingerprint(&self) -> [u8; 32] {
        let body = self.manifest.serialize_body();
        let mut hasher = Sha256::new();
        hasher.update(&body);
        let res = hasher.finalize();
        let mut out = [0u8; 32];
        out.copy_from_slice(&res);
        out
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_manifest_sign_and_shield_level() {
        let sk = HybridSigningKey::generate();
        let manifest = PadManifest {
            pad_id: [7u8; 32],
            total_size: 1024 * 1024 * 100, // 100 MB
            reserve_pct: 20,
            partner_id: "alice_pub_key".to_string(),
            local_side: PadSide::SideA,
            transfer_method: TransferMethod::Cable,
            created_at: 1700000000,
            head_offset: 0,
            tail_offset: 1024 * 1024 * 100,
        };

        assert_eq!(manifest.shield_level(), SecurityShieldLevel::GoldAbsolute);

        let signed = manifest.sign(&sk);
        assert!(signed.verify(sk.verifying_key()).is_ok());

        let fake_key = HybridSigningKey::generate();
        assert!(signed.verify(fake_key.verifying_key()).is_err());
    }

    #[test]
    fn test_wireless_shield_level() {
        let manifest = PadManifest {
            pad_id: [1u8; 32],
            total_size: 500,
            reserve_pct: 10,
            partner_id: "bob".to_string(),
            local_side: PadSide::SideB,
            transfer_method: TransferMethod::WifiDirectQr,
            created_at: 1700000000,
            head_offset: 0,
            tail_offset: 500,
        };

        assert_eq!(manifest.shield_level(), SecurityShieldLevel::BlueHigh);
        assert!(!manifest.is_exhausted());
    }
}
