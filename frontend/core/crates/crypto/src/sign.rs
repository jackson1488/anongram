//! Hybrid signatures: Ed25519 + ML-DSA-87.
//!
//! A signature is valid only if BOTH component signatures verify, so forging one
//! requires breaking both algorithms. Keys can be rebuilt from two 32-byte seeds,
//! which lets the identity module derive them deterministically from a BIP-39 phrase.

use ed25519_dalek::{
    Signature as EdSignature, Signer as EdSigner, SigningKey as EdSigningKey,
    VerifyingKey as EdVerifyingKey,
};
use ml_dsa::{
    Keypair, MlDsa87, Signature as MlSignature, Signer as MlSigner, SigningKey as MlSigningKey,
    VerifyingKey as MlVerifyingKey,
};
use rand::{rngs::OsRng, RngCore};
use zeroize::Zeroizing;

use crate::error::CryptoError;

const DOMAIN: &[u8] = b"anongram/sig/v1";
pub const ED25519_PK_LEN: usize = 32;
pub const ED25519_SIG_LEN: usize = 64;
pub const SEED_LEN: usize = 32;

fn framed(msg: &[u8]) -> Vec<u8> {
    let mut m = Vec::with_capacity(DOMAIN.len() + msg.len());
    m.extend_from_slice(DOMAIN);
    m.extend_from_slice(msg);
    m
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct HybridVerifyingKey {
    pub ed25519: [u8; ED25519_PK_LEN],
    pub mldsa: Vec<u8>,
}

#[derive(Clone, PartialEq, Eq, Debug)]
pub struct HybridSignature {
    pub ed25519: [u8; ED25519_SIG_LEN],
    pub mldsa: Vec<u8>,
}

pub struct HybridSigningKey {
    ed: EdSigningKey,
    ml: MlSigningKey<MlDsa87>,
    public: HybridVerifyingKey,
}

impl HybridSigningKey {
    pub fn generate() -> Self {
        let mut ed_seed = Zeroizing::new([0u8; SEED_LEN]);
        let mut ml_seed = Zeroizing::new([0u8; SEED_LEN]);
        OsRng.fill_bytes(ed_seed.as_mut());
        OsRng.fill_bytes(ml_seed.as_mut());
        Self::from_seeds(&ed_seed, &ml_seed)
    }

    /// Deterministic construction from two seeds.
    pub fn from_seeds(ed_seed: &[u8; SEED_LEN], ml_seed: &[u8; SEED_LEN]) -> Self {
        let ed = EdSigningKey::from_bytes(ed_seed);
        let ml = MlSigningKey::<MlDsa87>::from_seed(&(*ml_seed).into());
        let public = HybridVerifyingKey {
            ed25519: ed.verifying_key().to_bytes(),
            mldsa: ml.verifying_key().encode().as_slice().to_vec(),
        };
        Self { ed, ml, public }
    }

    pub fn verifying_key(&self) -> &HybridVerifyingKey {
        &self.public
    }

    pub fn sign(&self, msg: &[u8]) -> HybridSignature {
        let m = framed(msg);
        let ed_sig = self.ed.sign(&m);
        let ml_sig = self.ml.sign(&m);
        HybridSignature {
            ed25519: ed_sig.to_bytes(),
            mldsa: ml_sig.encode().as_slice().to_vec(),
        }
    }
}

impl HybridVerifyingKey {
    /// Returns `Ok(())` only if both signatures are valid.
    pub fn verify(&self, msg: &[u8], sig: &HybridSignature) -> Result<(), CoreError> {
        let m = framed(msg);

        let ed_vk = EdVerifyingKey::from_bytes(&self.ed25519).map_err(|_| CoreError::Malformed)?;
        let ed_sig = EdSignature::from_bytes(&sig.ed25519);
        let ed_ok = ed_vk.verify_strict(&m, &ed_sig).is_ok();

        let ml_ok = self.verify_mldsa(&m, &sig.mldsa)?;

        if ed_ok && ml_ok {
            Ok(())
        } else {
            Err(CoreError::Signature)
        }
    }

    fn verify_mldsa(&self, framed_msg: &[u8], sig: &[u8]) -> Result<bool, CoreError> {
        use ml_dsa::Verifier;
        let enc = self
            .mldsa
            .as_slice()
            .try_into()
            .map_err(|_| CoreError::Malformed)?;
        let vk = MlVerifyingKey::<MlDsa87>::decode(&enc);
        let sig = MlSignature::<MlDsa87>::try_from(sig).map_err(|_| CoreError::Malformed)?;
        Ok(vk.verify(framed_msg, &sig).is_ok())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sign_and_verify() {
        let sk = HybridSigningKey::generate();
        let sig = sk.sign(b"hello");
        assert!(sk.verifying_key().verify(b"hello", &sig).is_ok());
    }

    #[test]
    fn wrong_message_fails() {
        let sk = HybridSigningKey::generate();
        let sig = sk.sign(b"hello");
        assert_eq!(
            sk.verifying_key().verify(b"hellp", &sig),
            Err(CoreError::Signature)
        );
    }

    #[test]
    fn wrong_key_fails() {
        let a = HybridSigningKey::generate();
        let b = HybridSigningKey::generate();
        let sig = a.sign(b"m");
        assert_eq!(
            b.verifying_key().verify(b"m", &sig),
            Err(CoreError::Signature)
        );
    }

    #[test]
    fn broken_ed25519_part_fails() {
        let sk = HybridSigningKey::generate();
        let mut sig = sk.sign(b"m");
        sig.ed25519[0] ^= 1;
        assert!(sk.verifying_key().verify(b"m", &sig).is_err());
    }

    #[test]
    fn broken_mldsa_part_fails() {
        let sk = HybridSigningKey::generate();
        let mut sig = sk.sign(b"m");
        sig.mldsa[0] ^= 1;
        assert!(sk.verifying_key().verify(b"m", &sig).is_err());
    }

    #[test]
    fn same_seeds_give_same_keys() {
        let a = HybridSigningKey::from_seeds(&[1u8; 32], &[2u8; 32]);
        let b = HybridSigningKey::from_seeds(&[1u8; 32], &[2u8; 32]);
        assert_eq!(a.verifying_key(), b.verifying_key());
        let c = HybridSigningKey::from_seeds(&[1u8; 32], &[3u8; 32]);
        assert_ne!(a.verifying_key(), c.verifying_key());
    }

    #[test]
    fn malformed_key_is_rejected() {
        let sk = HybridSigningKey::generate();
        let sig = sk.sign(b"m");
        let mut vk = sk.verifying_key().clone();
        vk.mldsa.truncate(5);
        assert_eq!(vk.verify(b"m", &sig), Err(CoreError::Malformed));
    }
}
