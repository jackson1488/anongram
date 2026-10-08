//! Key Change Guard (Anti-MITM Safety Number Change Detector).
//!
//! Protects against rogue servers or state interception silently swapping
//! public keys for existing contacts. Blocks communication and raises alerts.

use std::collections::HashMap;

use crate::error::SecurityError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum KeyChangePolicy {
    /// Strict: completely block sending messages until user re-verifies.
    BlockSending,
    /// Warning: show prominent safety alert banner but allow sending.
    BannerWarning,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct VerifiedContactRecord {
    pub peer_id: [u8; 32],
    pub pinned_public_key: [u8; 32],
    pub is_verified_by_qr: bool,
    pub verified_at: u64,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum KeyCheckResult {
    /// Key matches previously recorded key. Safe to proceed.
    Match,
    /// First time seeing this peer; key is recorded.
    NewContactPinned,
    /// Key mismatch! Potential active MITM attack detected.
    KeyChangedAlert {
        old_key: [u8; 32],
        new_key: [u8; 32],
    },
}

pub struct KeyChangeGuard {
    pub policy: KeyChangePolicy,
    records: HashMap<[u8; 32], VerifiedContactRecord>,
}

impl KeyChangeGuard {
    pub fn new(policy: KeyChangePolicy) -> Self {
        Self {
            policy,
            records: HashMap::new(),
        }
    }

    /// Evaluates incoming public key for a contact.
    pub fn check_peer_key(
        &mut self,
        peer_id: &[u8; 32],
        incoming_key: &[u8; 32],
        timestamp: u64,
    ) -> Result<KeyCheckResult, SecurityError> {
        match self.records.get_mut(peer_id) {
            Some(existing) => {
                if &existing.pinned_public_key == incoming_key {
                    Ok(KeyCheckResult::Match)
                } else {
                    let old_key = existing.pinned_public_key;
                    let new_key = *incoming_key;

                    if self.policy == KeyChangePolicy::BlockSending {
                        Err(SecurityError::KeyChangeDetected)
                    } else {
                        Ok(KeyCheckResult::KeyChangedAlert { old_key, new_key })
                    }
                }
            }
            None => {
                // Pin initial contact key
                self.records.insert(
                    *peer_id,
                    VerifiedContactRecord {
                        peer_id: *peer_id,
                        pinned_public_key: *incoming_key,
                        is_verified_by_qr: false,
                        verified_at: timestamp,
                    },
                );
                Ok(KeyCheckResult::NewContactPinned)
            }
        }
    }

    /// Explicit user override after verifying peer identity (e.g., peer got a new phone).
    pub fn accept_key_rotation(
        &mut self,
        peer_id: &[u8; 32],
        new_key: &[u8; 32],
        verified_by_qr: bool,
        timestamp: u64,
    ) {
        self.records.insert(
            *peer_id,
            VerifiedContactRecord {
                peer_id: *peer_id,
                pinned_public_key: *new_key,
                is_verified_by_qr,
                verified_at: timestamp,
            },
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_mitm_key_change_detection() {
        let mut guard = KeyChangeGuard::new(KeyChangePolicy::BlockSending);
        let bob_id = [0x02u8; 32];
        let original_key = [0xAAu8; 32];
        let attacker_key = [0xFFu8; 32];

        // 1. Initial pin
        let res1 = guard.check_peer_key(&bob_id, &original_key, 1000).unwrap();
        assert_eq!(res1, KeyCheckResult::NewContactPinned);

        // 2. Normal check matches
        let res2 = guard.check_peer_key(&bob_id, &original_key, 1001).unwrap();
        assert_eq!(res2, KeyCheckResult::Match);

        // 3. Attacker / Server tries to replace Bob's key
        let res3 = guard.check_peer_key(&bob_id, &attacker_key, 1002);
        assert_eq!(res3, Err(SecurityError::KeyChangeDetected));

        // 4. Bob bought a new phone and Alice scans his QR code to accept rotation
        guard.accept_key_rotation(&bob_id, &attacker_key, true, 1003);
        let res4 = guard.check_peer_key(&bob_id, &attacker_key, 1004).unwrap();
        assert_eq!(res4, KeyCheckResult::Match);
    }
}
