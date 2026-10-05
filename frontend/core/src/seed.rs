//! Master seed: BIP-39 phrase -> 64-byte seed -> independent per-purpose keys (HKDF-SHA512).
//!
//! The same phrase always yields the same identity, so the account can be restored
//! on a new phone without any server.

use bip39::{Language, Mnemonic};
use hkdf::Hkdf;
use rand::{rngs::OsRng, RngCore};
use sha2::Sha512;
use zeroize::Zeroizing;

use crate::error::CoreError;
use crate::sign::HybridSigningKey;

const SALT: &[u8] = b"anongram/master/v1";

/// Root secret of an identity. Zeroized on drop.
pub struct MasterSeed(Zeroizing<[u8; 64]>);

impl MasterSeed {
    /// Generates a new phrase of 12 or 24 words and the matching seed.
    pub fn generate(words: usize) -> Result<(Zeroizing<String>, Self), CoreError> {
        let entropy_len = match words {
            12 => 16,
            24 => 32,
            _ => return Err(CoreError::Malformed),
        };
        let mut entropy = Zeroizing::new(vec![0u8; entropy_len]);
        OsRng.fill_bytes(&mut entropy);
        let mnemonic = Mnemonic::from_entropy_in(Language::English, &entropy)
            .map_err(|_| CoreError::Malformed)?;
        let phrase = Zeroizing::new(mnemonic.to_string());
        let seed = Self(Zeroizing::new(mnemonic.to_seed("")));
        Ok((phrase, seed))
    }

    /// Restores the seed from a phrase. `passphrase` is the optional BIP-39 passphrase.
    pub fn from_phrase(phrase: &str, passphrase: &str) -> Result<Self, CoreError> {
        let mnemonic = Mnemonic::parse_in_normalized(Language::English, phrase)
            .map_err(|_| CoreError::Malformed)?;
        Ok(Self(Zeroizing::new(mnemonic.to_seed(passphrase))))
    }

    /// Derives a 32-byte key bound to a purpose label. Different labels give unrelated keys.
    pub fn derive(&self, label: &str) -> Zeroizing<[u8; 32]> {
        let hk = Hkdf::<Sha512>::new(Some(SALT), self.0.as_ref());
        let mut out = Zeroizing::new([0u8; 32]);
        hk.expand(label.as_bytes(), out.as_mut())
            .expect("32 bytes is a valid HKDF output length");
        out
    }

    /// Master hybrid signing key (Ed25519 + ML-DSA-87) of this identity.
    pub fn master_signing_key(&self) -> HybridSigningKey {
        let ed = self.derive("identity/ed25519");
        let ml = self.derive("identity/ml-dsa-87");
        HybridSigningKey::from_seeds(&ed, &ml)
    }

    /// Key that protects the profile metadata inside the Passport Blob.
    pub fn profile_key(&self) -> Zeroizing<[u8; 32]> {
        self.derive("identity/profile-key")
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const ABANDON: &str =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

    #[test]
    fn bip39_official_test_vector() {
        // From the BIP-39 reference test vectors (passphrase "TREZOR").
        let m = Mnemonic::parse_in_normalized(Language::English, ABANDON).unwrap();
        let seed = m.to_seed("TREZOR");
        assert_eq!(
            hex(&seed),
            "c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e5349553\
             1f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04"
        );
    }

    #[test]
    fn same_phrase_same_identity() {
        let a = MasterSeed::from_phrase(ABANDON, "").unwrap();
        let b = MasterSeed::from_phrase(ABANDON, "").unwrap();
        assert_eq!(
            a.master_signing_key().verifying_key(),
            b.master_signing_key().verifying_key()
        );
    }

    #[test]
    fn passphrase_changes_identity() {
        let a = MasterSeed::from_phrase(ABANDON, "").unwrap();
        let b = MasterSeed::from_phrase(ABANDON, "x").unwrap();
        assert_ne!(
            a.master_signing_key().verifying_key(),
            b.master_signing_key().verifying_key()
        );
    }

    #[test]
    fn labels_are_independent() {
        let s = MasterSeed::from_phrase(ABANDON, "").unwrap();
        assert_ne!(*s.derive("a"), *s.derive("b"));
        assert_eq!(*s.derive("a"), *s.derive("a"));
    }

    #[test]
    fn generated_phrase_restores_same_seed() {
        for words in [12usize, 24] {
            let (phrase, seed) = MasterSeed::generate(words).unwrap();
            assert_eq!(phrase.split_whitespace().count(), words);
            let restored = MasterSeed::from_phrase(&phrase, "").unwrap();
            assert_eq!(*seed.profile_key(), *restored.profile_key());
        }
    }

    #[test]
    fn bad_checksum_and_word_count_rejected() {
        let bad = ABANDON.replace("about", "abandon");
        assert!(MasterSeed::from_phrase(&bad, "").is_err());
        assert!(MasterSeed::generate(13).is_err());
    }

    fn hex(b: &[u8]) -> String {
        b.iter().map(|x| format!("{x:02x}")).collect()
    }
}
