//! 60-Digit Canonical Safety Number (12 Blocks of 5 Digits).
//!
//! Generates identical numeric fingerprints on both peers' devices (like WhatsApp and Signal)
//! for verbal or manual comparison against Man-In-The-Middle (MITM) attacks.

use sha2::{Digest, Sha512};

pub struct NumericFingerprint;

impl NumericFingerprint {
    /// Computes deterministic 60-digit safety code from two public keys and peer IDs.
    /// Order of participants does not matter; results are canonical and symmetric.
    pub fn compute(
        local_key: &[u8; 32],
        local_id: &[u8; 32],
        remote_key: &[u8; 32],
        remote_id: &[u8; 32],
    ) -> String {
        let mut hasher = Sha512::new();

        // Sort keys lexicographically for symmetric canonical output
        if local_key <= remote_key {
            hasher.update(local_key);
            hasher.update(remote_key);
        } else {
            hasher.update(remote_key);
            hasher.update(local_key);
        }

        if local_id <= remote_id {
            hasher.update(local_id);
            hasher.update(remote_id);
        } else {
            hasher.update(remote_id);
            hasher.update(local_id);
        }

        let hash = hasher.finalize();

        // Convert 64-byte SHA-512 digest into 60 decimal digits (12 blocks of 5 digits)
        let mut digits = String::with_capacity(71); // 60 digits + 11 spaces

        for chunk_idx in 0..12 {
            // Take 5 bytes per block to produce 5 decimal digits
            let b0 = hash[chunk_idx * 5] as u64;
            let b1 = hash[chunk_idx * 5 + 1] as u64;
            let b2 = hash[chunk_idx * 5 + 2] as u64;
            let b3 = hash[chunk_idx * 5 + 3] as u64;
            let b4 = hash[chunk_idx * 5 + 4] as u64;

            let val = (b0 << 32) | (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
            let block_num = val % 100_000;

            if chunk_idx > 0 {
                digits.push(' ');
            }
            digits.push_str(&format!("{:05}", block_num));
        }

        digits
    }

    /// Verifies that two safety code strings match (ignoring whitespace).
    pub fn verify(code_a: &str, code_b: &str) -> bool {
        let clean_a: String = code_a.chars().filter(|c| !c.is_whitespace()).collect();
        let clean_b: String = code_b.chars().filter(|c| !c.is_whitespace()).collect();
        !clean_a.is_empty() && clean_a == clean_b
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_canonical_symmetry() {
        let key_alice = [0x11u8; 32];
        let id_alice = [0xAAu8; 32];
        let key_bob = [0x22u8; 32];
        let id_bob = [0xBBu8; 32];

        // Alice computes code with Bob
        let code_from_alice = NumericFingerprint::compute(&key_alice, &id_alice, &key_bob, &id_bob);
        // Bob computes code with Alice (reversed arguments)
        let code_from_bob = NumericFingerprint::compute(&key_bob, &id_bob, &key_alice, &id_alice);

        assert_eq!(code_from_alice, code_from_bob, "Safety numbers must be identical symmetrically");
        assert_eq!(code_from_alice.len(), 71); // 12 * 5 digits + 11 spaces = 71 chars

        // Split into 12 blocks
        let blocks: Vec<&str> = code_from_alice.split(' ').collect();
        assert_eq!(blocks.len(), 12);
        for block in blocks {
            assert_eq!(block.len(), 5);
            assert!(block.chars().all(|c| c.is_ascii_digit()));
        }

        assert!(NumericFingerprint::verify(&code_from_alice, &code_from_bob));
    }
}
