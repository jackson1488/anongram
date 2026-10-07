//! Passport Blob: the signed, portable description of an identity.
//!
//! Contents: master verifying key, device keys (with revocation flags), a long-term
//! hybrid KEM prekey and an encrypted profile. The whole body is signed by the master
//! hybrid key. A `Passport` value can only exist if its signature was verified, so
//! unverified data is never handled as a passport.
//!
//! The monotonic `version` lets clients refuse a rolled-back (older) passport.

use sha2::{Digest, Sha256};

use crate::error::IdentityError;
use crypto::aead;
use crypto::kem::HybridPublicKey;
use crypto::sign::{HybridSignature, HybridSigningKey, HybridVerifyingKey};

const MAGIC: &[u8; 4] = b"AGPB";
const FORMAT: u8 = 1;
const SIGN_PREFIX: &[u8] = b"anongram/passport/v1";
const MAX_BLOB: usize = 1 << 20;
const MAX_DEVICES: usize = 32;
const MAX_PROFILE: usize = 64 * 1024;

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct DeviceEntry {
    pub device_id: [u8; 16],
    pub key: HybridVerifyingKey,
    pub added_at: u64,
    pub revoked: bool,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct PassportBody {
    pub version: u64,
    pub created_at: u64,
    pub master: HybridVerifyingKey,
    pub devices: Vec<DeviceEntry>,
    pub prekey: HybridPublicKey,
    pub encrypted_profile: Vec<u8>,
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Passport {
    body: PassportBody,
    body_bytes: Vec<u8>,
    signature: HybridSignature,
}

impl PassportBody {
    /// Encrypts `profile` for this passport. The aad ties the ciphertext to the
    /// master key and version, so it cannot be copied into another passport.
    pub fn seal_profile(
        master: &HybridVerifyingKey,
        version: u64,
        profile_key: &[u8; 32],
        profile: &[u8],
    ) -> Result<Vec<u8>, CoreError> {
        if profile.len() > MAX_PROFILE {
            return Err(IdentityError::Malformed);
        }
        aead::seal(profile_key, &profile_aad(master, version), profile)
    }
}

impl Passport {
    /// Signs `body` with the master key. The key must match `body.master`.
    pub fn sign(body: PassportBody, master: &HybridSigningKey) -> Result<Self, CoreError> {
        if master.verifying_key() != &body.master {
            return Err(IdentityError::Signature);
        }
        if body.devices.len() > MAX_DEVICES {
            return Err(IdentityError::Malformed);
        }
        let body_bytes = encode_body(&body);
        let signature = master.sign(&signed_message(&body_bytes));
        Ok(Self {
            body,
            body_bytes,
            signature,
        })
    }

    pub fn body(&self) -> &PassportBody {
        &self.body
    }

    pub fn version(&self) -> u64 {
        self.body.version
    }

    /// Stable identifier: hex SHA-256 of the master verifying key.
    pub fn public_id(&self) -> String {
        public_id_of(&self.body.master)
    }

    /// True if `self` is a newer passport of the same identity than `other`.
    pub fn supersedes(&self, other: &Passport) -> bool {
        self.body.master == other.body.master && self.body.version > other.body.version
    }

    pub fn decrypt_profile(&self, profile_key: &[u8; 32]) -> Result<Vec<u8>, CoreError> {
        let aad = profile_aad(&self.body.master, self.body.version);
        aead::open(profile_key, &aad, &self.body.encrypted_profile)
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        let mut w = Writer::default();
        w.raw(MAGIC);
        w.u8(FORMAT);
        w.bytes(&self.body_bytes);
        w.raw(&self.signature.ed25519);
        w.bytes(&self.signature.mldsa);
        w.0
    }

    /// Parses and verifies. Any problem (format, size, signature) returns an error.
    pub fn from_bytes(data: &[u8]) -> Result<Self, CoreError> {
        if data.len() > MAX_BLOB {
            return Err(IdentityError::Malformed);
        }
        let mut r = Reader::new(data);
        if r.take(4)? != MAGIC || r.u8()? != FORMAT {
            return Err(IdentityError::Malformed);
        }
        let body_bytes = r.bytes(MAX_BLOB)?;
        let signature = HybridSignature {
            ed25519: r.array::<64>()?,
            mldsa: r.bytes(MAX_BLOB)?,
        };
        r.finish()?;

        let body = decode_body(&body_bytes)?;
        let msg = signed_message(&body_bytes);
        body.master.verify(&msg, &signature)?;
        Ok(Self {
            body,
            body_bytes,
            signature,
        })
    }
}

pub fn public_id_of(master: &HybridVerifyingKey) -> String {
    let mut w = Writer::default();
    put_vk(&mut w, master);
    let digest = Sha256::digest(&w.0);
    digest.iter().map(|b| format!("{b:02x}")).collect()
}

fn signed_message(body_bytes: &[u8]) -> Vec<u8> {
    let mut m = Vec::with_capacity(SIGN_PREFIX.len() + body_bytes.len());
    m.extend_from_slice(SIGN_PREFIX);
    m.extend_from_slice(body_bytes);
    m
}

fn profile_aad(master: &HybridVerifyingKey, version: u64) -> Vec<u8> {
    let mut w = Writer::default();
    w.raw(b"anongram/passport/profile/v1");
    put_vk(&mut w, master);
    w.u64(version);
    w.0
}

// ---- binary encoding -------------------------------------------------------

#[derive(Default)]
struct Writer(Vec<u8>);

impl Writer {
    fn u8(&mut self, v: u8) {
        self.0.push(v);
    }
    fn u64(&mut self, v: u64) {
        self.0.extend_from_slice(&v.to_be_bytes());
    }
    fn raw(&mut self, v: &[u8]) {
        self.0.extend_from_slice(v);
    }
    fn bytes(&mut self, v: &[u8]) {
        self.0.extend_from_slice(&(v.len() as u32).to_be_bytes());
        self.0.extend_from_slice(v);
    }
}

struct Reader<'a> {
    data: &'a [u8],
    pos: usize,
}

impl<'a> Reader<'a> {
    fn new(data: &'a [u8]) -> Self {
        Self { data, pos: 0 }
    }
    fn take(&mut self, n: usize) -> Result<&'a [u8], CoreError> {
        let end = self.pos.checked_add(n).ok_or(IdentityError::Malformed)?;
        if end > self.data.len() {
            return Err(IdentityError::Malformed);
        }
        let s = &self.data[self.pos..end];
        self.pos = end;
        Ok(s)
    }
    fn u8(&mut self) -> Result<u8, CoreError> {
        Ok(self.take(1)?[0])
    }
    fn u64(&mut self) -> Result<u64, CoreError> {
        let b: [u8; 8] = self
            .take(8)?
            .try_into()
            .map_err(|_| IdentityError::Malformed)?;
        Ok(u64::from_be_bytes(b))
    }
    fn array<const N: usize>(&mut self) -> Result<[u8; N], CoreError> {
        self.take(N)?
            .try_into()
            .map_err(|_| IdentityError::Malformed)
    }
    fn bytes(&mut self, max: usize) -> Result<Vec<u8>, CoreError> {
        let len = u32::from_be_bytes(self.array::<4>()?) as usize;
        if len > max {
            return Err(IdentityError::Malformed);
        }
        Ok(self.take(len)?.to_vec())
    }
    fn finish(&self) -> Result<(), CoreError> {
        if self.pos == self.data.len() {
            Ok(())
        } else {
            Err(IdentityError::Malformed)
        }
    }
}

fn put_vk(w: &mut Writer, k: &HybridVerifyingKey) {
    w.raw(&k.ed25519);
    w.bytes(&k.mldsa);
}

fn get_vk(r: &mut Reader) -> Result<HybridVerifyingKey, CoreError> {
    Ok(HybridVerifyingKey {
        ed25519: r.array::<32>()?,
        mldsa: r.bytes(MAX_BLOB)?,
    })
}

fn encode_body(b: &PassportBody) -> Vec<u8> {
    let mut w = Writer::default();
    w.u64(b.version);
    w.u64(b.created_at);
    put_vk(&mut w, &b.master);
    w.u8(b.devices.len() as u8);
    for d in &b.devices {
        w.raw(&d.device_id);
        put_vk(&mut w, &d.key);
        w.u64(d.added_at);
        w.u8(d.revoked as u8);
    }
    w.raw(&b.prekey.x25519);
    w.bytes(&b.prekey.mlkem);
    w.bytes(&b.encrypted_profile);
    w.0
}

fn decode_body(data: &[u8]) -> Result<PassportBody, CoreError> {
    let mut r = Reader::new(data);
    let version = r.u64()?;
    let created_at = r.u64()?;
    let master = get_vk(&mut r)?;
    let count = r.u8()? as usize;
    if count > MAX_DEVICES {
        return Err(IdentityError::Malformed);
    }
    let mut devices = Vec::with_capacity(count);
    for _ in 0..count {
        let device_id = r.array::<16>()?;
        let key = get_vk(&mut r)?;
        let added_at = r.u64()?;
        let revoked = match r.u8()? {
            0 => false,
            1 => true,
            _ => return Err(IdentityError::Malformed),
        };
        devices.push(DeviceEntry {
            device_id,
            key,
            added_at,
            revoked,
        });
    }
    let prekey = HybridPublicKey {
        x25519: r.array::<32>()?,
        mlkem: r.bytes(MAX_BLOB)?,
    };
    let encrypted_profile = r.bytes(MAX_PROFILE + 64)?;
    r.finish()?;
    Ok(PassportBody {
        version,
        created_at,
        master,
        devices,
        prekey,
        encrypted_profile,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::kem::generate_keypair;
    use crate::seed::MasterSeed;

    const PHRASE: &str =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";

    fn make(version: u64, profile: &[u8]) -> (Passport, MasterSeed) {
        let seed = MasterSeed::from_phrase(PHRASE, "").unwrap();
        let master = seed.master_signing_key();
        let device = HybridSigningKey::generate();
        let prekey = generate_keypair();
        let vk = master.verifying_key();
        let pkey = seed.profile_key();
        let encrypted_profile = PassportBody::seal_profile(vk, version, &pkey, profile).unwrap();
        let body = PassportBody {
            version,
            created_at: 1_700_000_000,
            master: master.verifying_key().clone(),
            devices: vec![DeviceEntry {
                device_id: [9u8; 16],
                key: device.verifying_key().clone(),
                added_at: 1_700_000_001,
                revoked: false,
            }],
            prekey: prekey.public_key().clone(),
            encrypted_profile,
        };
        (Passport::sign(body, &master).unwrap(), seed)
    }

    #[test]
    fn roundtrip_and_verify() {
        let (p, _) = make(1, b"alice");
        let parsed = Passport::from_bytes(&p.to_bytes()).unwrap();
        assert_eq!(parsed, p);
        assert_eq!(parsed.public_id().len(), 64);
    }

    #[test]
    fn restore_on_new_device_gives_same_public_id() {
        let (p, _) = make(1, b"alice");
        let other = MasterSeed::from_phrase(PHRASE, "").unwrap();
        let id = public_id_of(other.master_signing_key().verifying_key());
        assert_eq!(p.public_id(), id);
    }

    #[test]
    fn profile_decrypts_only_with_right_key() {
        let (p, seed) = make(1, b"alice");
        assert_eq!(p.decrypt_profile(&seed.profile_key()).unwrap(), b"alice");
        assert_eq!(p.decrypt_profile(&[1u8; 32]), Err(IdentityError::Decrypt));
    }

    #[test]
    fn tampering_is_always_detected() {
        let (p, _) = make(1, b"alice");
        let bytes = p.to_bytes();
        let mut i = 0;
        while i < bytes.len() {
            let mut t = bytes.clone();
            t[i] ^= 1;
            assert!(Passport::from_bytes(&t).is_err(), "byte {i} not protected");
            i += 197;
        }
        let mut truncated = bytes.clone();
        truncated.pop();
        assert!(Passport::from_bytes(&truncated).is_err());
        let mut extended = bytes;
        extended.push(0);
        assert!(Passport::from_bytes(&extended).is_err());
    }

    #[test]
    fn newer_version_supersedes_older_only() {
        let (v1, _) = make(1, b"a");
        let (v2, _) = make(2, b"a");
        assert!(v2.supersedes(&v1));
        assert!(!v1.supersedes(&v2));
        assert!(!v1.supersedes(&v1));
    }

    #[test]
    fn foreign_key_cannot_sign() {
        let (p, _) = make(1, b"a");
        let stranger = HybridSigningKey::generate();
        assert!(Passport::sign(p.body().clone(), &stranger).is_err());
    }

    #[test]
    fn profile_cannot_move_to_another_version() {
        let (v1, seed) = make(1, b"a");
        let mut body = v1.body().clone();
        body.version = 2;
        let master = seed.master_signing_key();
        let moved = Passport::sign(body, &master).unwrap();
        assert!(moved.decrypt_profile(&seed.profile_key()).is_err());
    }
}
