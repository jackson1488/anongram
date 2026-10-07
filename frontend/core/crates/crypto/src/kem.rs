//! Hybrid KEM: X25519 + ML-KEM-1024.
//!
//! The shared secret is `HKDF-SHA256(ss_x25519 || ss_mlkem, info = label || transcript)`.
//! The transcript binds both public keys and both ciphertexts, so breaking only one
//! algorithm does not reveal the key, and substitution attacks are blocked.

use hkdf::Hkdf;
use ml_kem::{
    kem::{Decapsulate, DecapsulationKey, Encapsulate, EncapsulationKey},
    Ciphertext, Encoded, EncodedSizeUser, KemCore, MlKem1024, MlKem1024Params,
};
use rand::rngs::OsRng;
use sha2::{Digest, Sha256};
use x25519_dalek::{EphemeralSecret, PublicKey as XPublic, StaticSecret};
use zeroize::Zeroizing;

use crate::error::CryptoError;

const LABEL: &[u8] = b"anongram/hybrid-kem/v1";
pub const X25519_LEN: usize = 32;

/// Public half: share it freely.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct HybridPublicKey {
    pub x25519: [u8; X25519_LEN],
    pub mlkem: Vec<u8>,
}

/// Secret half: never leaves the device.
pub struct HybridSecretKey {
    x25519: StaticSecret,
    mlkem: DecapsulationKey<MlKem1024Params>,
    public: HybridPublicKey,
}

/// What the sender transmits to the recipient.
#[derive(Clone)]
pub struct HybridCiphertext {
    pub x25519_ephemeral: [u8; X25519_LEN],
    pub mlkem: Vec<u8>,
}

pub type SharedSecret = Zeroizing<[u8; 32]>;

pub fn generate_keypair() -> HybridSecretKey {
    let x_secret = StaticSecret::random_from_rng(OsRng);
    let x_public = XPublic::from(&x_secret);
    let (dk, ek) = MlKem1024::generate(&mut OsRng);
    HybridSecretKey {
        x25519: x_secret,
        mlkem: dk,
        public: HybridPublicKey {
            x25519: x_public.to_bytes(),
            mlkem: ek.as_bytes().to_vec(),
        },
    }
}

impl HybridSecretKey {
    pub fn public_key(&self) -> &HybridPublicKey {
        &self.public
    }

    pub fn decapsulate(&self, ct: &HybridCiphertext) -> Result<SharedSecret, CryptoError> {
        let eph = XPublic::from(ct.x25519_ephemeral);
        let ss_x = self.x25519.diffie_hellman(&eph);

        let mlkem_ct: Ciphertext<MlKem1024> = ct
            .mlkem
            .as_slice()
            .try_into()
            .map_err(|_| CryptoError::Malformed)?;
        let ss_m = self
            .mlkem
            .decapsulate(&mlkem_ct)
            .map_err(|_| CryptoError::Kem)?;

        Ok(combine(
            ss_x.as_bytes(),
            ss_m.as_slice(),
            &ct.x25519_ephemeral,
            &ct.mlkem,
            &self.public,
        ))
    }
}

/// Sender side: returns the ciphertext to send and the shared secret to keep.
pub fn encapsulate(pk: &HybridPublicKey) -> Result<(HybridCiphertext, SharedSecret), CryptoError> {
    let eph_secret = EphemeralSecret::random_from_rng(OsRng);
    let eph_public = XPublic::from(&eph_secret);
    let ss_x = eph_secret.diffie_hellman(&XPublic::from(pk.x25519));

    let encoded: Encoded<EncapsulationKey<MlKem1024Params>> = pk
        .mlkem
        .as_slice()
        .try_into()
        .map_err(|_| CryptoError::Malformed)?;
    let ek = EncapsulationKey::<MlKem1024Params>::from_bytes(&encoded);
    let (ct_m, ss_m) = ek.encapsulate(&mut OsRng).map_err(|_| CryptoError::Kem)?;

    let ct = HybridCiphertext {
        x25519_ephemeral: eph_public.to_bytes(),
        mlkem: ct_m.as_slice().to_vec(),
    };
    let secret = combine(
        ss_x.as_bytes(),
        ss_m.as_slice(),
        &ct.x25519_ephemeral,
        &ct.mlkem,
        pk,
    );
    Ok((ct, secret))
}

fn combine(
    ss_x: &[u8],
    ss_m: &[u8],
    eph_x: &[u8],
    ct_m: &[u8],
    recipient: &HybridPublicKey,
) -> SharedSecret {
    let mut ikm = Zeroizing::new(Vec::with_capacity(ss_x.len() + ss_m.len()));
    ikm.extend_from_slice(ss_x);
    ikm.extend_from_slice(ss_m);

    let mut info = Vec::new();
    info.extend_from_slice(LABEL);
    info.extend_from_slice(eph_x);
    info.extend_from_slice(&recipient.x25519);
    info.extend_from_slice(&Sha256::digest(&recipient.mlkem));
    info.extend_from_slice(&Sha256::digest(ct_m));

    let hk = Hkdf::<Sha256>::new(None, &ikm);
    let mut out = Zeroizing::new([0u8; 32]);
    hk.expand(&info, out.as_mut())
        .expect("32 bytes is a valid HKDF output length");
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn both_sides_agree() {
        let sk = generate_keypair();
        let (ct, ss_sender) = encapsulate(sk.public_key()).unwrap();
        let ss_recipient = sk.decapsulate(&ct).unwrap();
        assert_eq!(*ss_sender, *ss_recipient);
    }

    #[test]
    fn different_runs_give_different_secrets() {
        let sk = generate_keypair();
        let (_, a) = encapsulate(sk.public_key()).unwrap();
        let (_, b) = encapsulate(sk.public_key()).unwrap();
        assert_ne!(*a, *b);
    }

    #[test]
    fn wrong_recipient_gets_different_secret() {
        let alice = generate_keypair();
        let eve = generate_keypair();
        let (ct, ss) = encapsulate(alice.public_key()).unwrap();
        let eve_ss = eve.decapsulate(&ct).unwrap();
        assert_ne!(*ss, *eve_ss);
    }

    #[test]
    fn tampered_x25519_part_changes_secret() {
        let sk = generate_keypair();
        let (mut ct, ss) = encapsulate(sk.public_key()).unwrap();
        ct.x25519_ephemeral[0] ^= 1;
        assert_ne!(*ss, *sk.decapsulate(&ct).unwrap());
    }

    #[test]
    fn tampered_mlkem_part_changes_secret() {
        let sk = generate_keypair();
        let (mut ct, ss) = encapsulate(sk.public_key()).unwrap();
        ct.mlkem[0] ^= 1;
        assert_ne!(*ss, *sk.decapsulate(&ct).unwrap());
    }

    #[test]
    fn malformed_lengths_are_rejected() {
        let sk = generate_keypair();
        let (mut ct, _) = encapsulate(sk.public_key()).unwrap();
        ct.mlkem.truncate(10);
        assert_eq!(sk.decapsulate(&ct).err(), Some(CryptoError::Malformed));

        let mut pk = sk.public_key().clone();
        pk.mlkem.truncate(10);
        assert_eq!(encapsulate(&pk).err(), Some(CryptoError::Malformed));
    }
}
