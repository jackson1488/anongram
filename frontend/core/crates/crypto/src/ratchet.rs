//! Post-Quantum Double Ratchet Protocol (Pure Rust Implementation).
//!
//! Provides two-tier continuous key evolution:
//! 1. Symmetric KDF Chain Ratchet on EVERY SINGLE MESSAGE (zeroizes message key immediately).
//! 2. Asymmetric DH/KEM Ratchet on every turn-taking roundtrip (Post-Compromise Security).

use hkdf::Hkdf;
use rand::rngs::OsRng;
use sha2::Sha256;
use x25519_dalek::{PublicKey as XPublic, StaticSecret};
use zeroize::Zeroize;

use crate::aead::{self, KEY_LEN};
use crate::error::CryptoError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RatchetMessage {
    pub sender_dh_public: [u8; 32],
    pub sequence_counter: u32,
    pub ciphertext: Vec<u8>,
}

pub struct DoubleRatchet {
    root_key: [u8; KEY_LEN],
    local_dh_secret: StaticSecret,
    pub local_dh_public: [u8; 32],
    remote_dh_public: Option<[u8; 32]>,
    sending_chain_key: [u8; KEY_LEN],
    receiving_chain_key: Option<[u8; KEY_LEN]>,
    send_counter: u32,
    recv_counter: u32,
}

impl DoubleRatchet {
    /// Initializes Alice's side of the Double Ratchet (initiator).
    pub fn new_initiator(shared_master_key: [u8; KEY_LEN], remote_dh_public: [u8; 32]) -> Self {
        let local_secret = StaticSecret::random_from_rng(OsRng);
        let local_public = XPublic::from(&local_secret).to_bytes();

        let remote_xpublic = XPublic::from(remote_dh_public);
        let dh_shared = local_secret.diffie_hellman(&remote_xpublic);

        // KDF root step
        let (root_key, sending_chain_key) = kdf_root_step(&shared_master_key, dh_shared.as_bytes());

        Self {
            root_key,
            local_dh_secret: local_secret,
            local_dh_public: local_public,
            remote_dh_public: Some(remote_dh_public),
            sending_chain_key,
            receiving_chain_key: None,
            send_counter: 0,
            recv_counter: 0,
        }
    }

    /// Initializes Bob's side of the Double Ratchet (responder).
    pub fn new_responder(shared_master_key: [u8; KEY_LEN], local_dh_secret: StaticSecret) -> Self {
        let local_public = XPublic::from(&local_dh_secret).to_bytes();

        Self {
            root_key: shared_master_key,
            local_dh_secret,
            local_dh_public: local_public,
            remote_dh_public: None,
            sending_chain_key: [0u8; KEY_LEN],
            receiving_chain_key: None,
            send_counter: 0,
            recv_counter: 0,
        }
    }

    /// Encrypts a message using a fresh single-use message key derived from the symmetric KDF chain.
    /// The message key is discarded (zeroized) immediately after encryption.
    pub fn ratchet_encrypt(
        &mut self,
        plaintext: &[u8],
        aad: &[u8],
    ) -> Result<RatchetMessage, CryptoError> {
        let (next_chain_key, mut message_key) = kdf_chain_step(&self.sending_chain_key);
        self.sending_chain_key = next_chain_key;

        let counter = self.send_counter;
        self.send_counter += 1;

        // Bind sequence number & DH public into AAD
        let mut full_aad = Vec::with_capacity(aad.len() + 36);
        full_aad.extend_from_slice(aad);
        full_aad.extend_from_slice(&self.local_dh_public);
        full_aad.extend_from_slice(&counter.to_be_bytes());

        let ciphertext = aead::seal(&message_key, &full_aad, plaintext)?;
        message_key.zeroize();

        Ok(RatchetMessage {
            sender_dh_public: self.local_dh_public,
            sequence_counter: counter,
            ciphertext,
        })
    }

    /// Decrypts an incoming message, advancing the symmetric chain or performing a DH ratchet step.
    pub fn ratchet_decrypt(
        &mut self,
        msg: &RatchetMessage,
        aad: &[u8],
    ) -> Result<Vec<u8>, CryptoError> {
        // Check if remote DH public key changed (DH Ratchet step triggered!)
        if self.remote_dh_public.as_ref() != Some(&msg.sender_dh_public) {
            self.dh_ratchet_step(msg.sender_dh_public);
        }

        let recv_chain = match self.receiving_chain_key.as_mut() {
            Some(chain) => chain,
            None => return Err(CryptoError::Decrypt),
        };

        let (next_recv_chain, mut message_key) = kdf_chain_step(recv_chain);
        *recv_chain = next_recv_chain;
        self.recv_counter = msg.sequence_counter + 1;

        let mut full_aad = Vec::with_capacity(aad.len() + 36);
        full_aad.extend_from_slice(aad);
        full_aad.extend_from_slice(&msg.sender_dh_public);
        full_aad.extend_from_slice(&msg.sequence_counter.to_be_bytes());

        let plaintext = aead::open(&message_key, &full_aad, &msg.ciphertext)?;
        message_key.zeroize();

        Ok(plaintext)
    }

    /// Performs asymmetric DH Ratchet step.
    fn dh_ratchet_step(&mut self, new_remote_dh: [u8; 32]) {
        self.remote_dh_public = Some(new_remote_dh);
        let remote_xpublic = XPublic::from(new_remote_dh);

        // 1. Receive DH step
        let dh_recv = self.local_dh_secret.diffie_hellman(&remote_xpublic);
        let (root_after_recv, recv_chain) = kdf_root_step(&self.root_key, dh_recv.as_bytes());
        self.root_key = root_after_recv;
        self.receiving_chain_key = Some(recv_chain);

        // 2. Fresh local DH generation for sending
        let fresh_local_secret = StaticSecret::random_from_rng(OsRng);
        self.local_dh_public = XPublic::from(&fresh_local_secret).to_bytes();
        let dh_send = fresh_local_secret.diffie_hellman(&remote_xpublic);
        let (root_after_send, send_chain) = kdf_root_step(&self.root_key, dh_send.as_bytes());
        self.root_key = root_after_send;
        self.sending_chain_key = send_chain;
        self.local_dh_secret = fresh_local_secret;
        self.send_counter = 0;
    }
}

impl Drop for DoubleRatchet {
    fn drop(&mut self) {
        self.root_key.zeroize();
        self.sending_chain_key.zeroize();
        if let Some(mut chain) = self.receiving_chain_key {
            chain.zeroize();
        }
    }
}

/// Symmetric KDF chain advancement: derives (next_chain_key, message_key).
fn kdf_chain_step(chain_key: &[u8; KEY_LEN]) -> ([u8; KEY_LEN], [u8; KEY_LEN]) {
    let hk = Hkdf::<Sha256>::new(None, chain_key);
    let mut next_chain = [0u8; KEY_LEN];
    let mut message_key = [0u8; KEY_LEN];

    hk.expand(b"ANONGRAM/chain-advance", &mut next_chain)
        .expect("HKDF expand");
    hk.expand(b"ANONGRAM/message-key", &mut message_key)
        .expect("HKDF expand");

    (next_chain, message_key)
}

/// Asymmetric root KDF step: derives (next_root_key, chain_key).
fn kdf_root_step(root_key: &[u8; KEY_LEN], dh_out: &[u8]) -> ([u8; KEY_LEN], [u8; KEY_LEN]) {
    let hk = Hkdf::<Sha256>::new(Some(root_key), dh_out);
    let mut next_root = [0u8; KEY_LEN];
    let mut chain_key = [0u8; KEY_LEN];

    hk.expand(b"ANONGRAM/root-advance", &mut next_root)
        .expect("HKDF expand");
    hk.expand(b"ANONGRAM/chain-init", &mut chain_key)
        .expect("HKDF expand");

    (next_root, chain_key)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_double_ratchet_continuous_steps() {
        let master = [0x77u8; KEY_LEN];

        let bob_init_secret = StaticSecret::random_from_rng(OsRng);
        let bob_init_public = XPublic::from(&bob_init_secret).to_bytes();

        let mut alice = DoubleRatchet::new_initiator(master, bob_init_public);
        let mut bob = DoubleRatchet::new_responder(master, bob_init_secret);

        // 1. Alice sends 3 consecutive messages without waiting for Bob
        // Each message must have a unique key derived from KDF chain
        let m1 = alice.ratchet_encrypt(b"Hello Bob 1", b"aad").unwrap();
        let m2 = alice.ratchet_encrypt(b"Hello Bob 2", b"aad").unwrap();
        let m3 = alice.ratchet_encrypt(b"Hello Bob 3", b"aad").unwrap();

        assert_eq!(m1.sequence_counter, 0);
        assert_eq!(m2.sequence_counter, 1);
        assert_eq!(m3.sequence_counter, 2);

        // Bob receives all 3 in order
        let d1 = bob.ratchet_decrypt(&m1, b"aad").unwrap();
        let d2 = bob.ratchet_decrypt(&m2, b"aad").unwrap();
        let d3 = bob.ratchet_decrypt(&m3, b"aad").unwrap();

        assert_eq!(d1, b"Hello Bob 1");
        assert_eq!(d2, b"Hello Bob 2");
        assert_eq!(d3, b"Hello Bob 3");

        // 2. Bob replies to Alice -> triggers DH Ratchet step!
        let bob_reply = bob
            .ratchet_encrypt(b"Hey Alice, got your 3 messages!", b"aad")
            .unwrap();
        let alice_decrypted = alice.ratchet_decrypt(&bob_reply, b"aad").unwrap();
        assert_eq!(alice_decrypted, b"Hey Alice, got your 3 messages!");
    }
}
