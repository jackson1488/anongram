//! Hybrid Group Encryption: StrictPairwise (<= 30) + Scalable Sender Keys (30-300+).
//!
//! Provides two distinct modes with adaptive transition and key erasure:
//! - `StrictPairwise`: Individual Double Ratchet per participant (max 30 members).
//! - `SenderKeys`: Signal v2 Group Protocol with single encryption for 300+ members.
//! - `Adaptive`: Automatic threshold transition with key wiping and system chat alerts.

use std::collections::HashMap;

use hkdf::Hkdf;
use rand::{rngs::OsRng, RngCore};
use sha2::Sha256;
use zeroize::Zeroize;

use crate::aead::{self, KEY_LEN};
use crate::error::CryptoError;
use crate::ratchet::{DoubleRatchet, RatchetMessage};

pub const MAX_PAIRWISE_PARTICIPANTS: usize = 30;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum GroupMode {
    /// Up to 30 members: individual Double Ratchet per peer.
    StrictPairwise,
    /// 30 to 300+ members: 1 encryption on sender key per message.
    SenderKeys,
    /// Automatic transition at 31 members.
    Adaptive,
}

#[derive(Clone)]
pub struct SenderKeyChain {
    pub chain_id: u32,
    pub iteration: u32,
    chain_key: [u8; KEY_LEN],
}

impl SenderKeyChain {
    pub fn new(chain_id: u32) -> Self {
        let mut key = [0u8; KEY_LEN];
        OsRng.fill_bytes(&mut key);
        Self {
            chain_id,
            iteration: 0,
            chain_key: key,
        }
    }

    pub fn from_received_key(chain_id: u32, key: [u8; KEY_LEN]) -> Self {
        Self {
            chain_id,
            iteration: 0,
            chain_key: key,
        }
    }

    /// Advances the chain and derives a single-use message key.
    pub fn ratchet_step(&mut self) -> (u32, [u8; KEY_LEN]) {
        let current_iter = self.iteration;
        self.iteration += 1;

        let hk = Hkdf::<Sha256>::new(None, &self.chain_key);
        let mut next_chain = [0u8; KEY_LEN];
        let mut msg_key = [0u8; KEY_LEN];

        hk.expand(b"ANONGRAM/SK/chain", &mut next_chain)
            .expect("HKDF");
        hk.expand(b"ANONGRAM/SK/msg", &mut msg_key).expect("HKDF");

        self.chain_key = next_chain;
        (current_iter, msg_key)
    }

    pub fn current_chain_key(&self) -> [u8; KEY_LEN] {
        self.chain_key
    }
}

impl Drop for SenderKeyChain {
    fn drop(&mut self) {
        self.chain_key.zeroize();
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GroupSenderKeyMessage {
    pub chain_id: u32,
    pub iteration: u32,
    pub ciphertext: Vec<u8>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GroupMessagePayload {
    pub mode: GroupMode,
    pub sender_id: [u8; 32],
    pub pairwise_payloads: Option<HashMap<[u8; 32], RatchetMessage>>,
    pub sender_key_message: Option<GroupSenderKeyMessage>,
}

pub struct GroupEncryptionSession {
    pub group_id: [u8; 32],
    pub local_id: [u8; 32],
    pub mode: GroupMode,
    pub participants: HashMap<[u8; 32], [u8; 32]>, // peer_id -> public_key
    pub pairwise_sessions: HashMap<[u8; 32], DoubleRatchet>,
    pub remote_sender_keys: HashMap<[u8; 32], SenderKeyChain>,
    pub our_sender_key: SenderKeyChain,
    pub pending_system_notification: Option<String>,
}

impl GroupEncryptionSession {
    pub fn new(group_id: [u8; 32], local_id: [u8; 32], mode: GroupMode) -> Self {
        Self {
            group_id,
            local_id,
            mode,
            participants: HashMap::new(),
            pairwise_sessions: HashMap::new(),
            remote_sender_keys: HashMap::new(),
            our_sender_key: SenderKeyChain::new(1),
            pending_system_notification: None,
        }
    }

    /// Adds a participant into the group session.
    pub fn add_participant(&mut self, peer_id: [u8; 32], public_key: [u8; 32]) {
        self.participants.insert(peer_id, public_key);

        // If in Adaptive mode and count exceeds 30, trigger transition to SenderKeys
        if self.mode == GroupMode::Adaptive && self.participants.len() > MAX_PAIRWISE_PARTICIPANTS {
            self.transition_to_sender_keys();
        }
    }

    /// Removes a participant from the group session and rotates sender keys.
    pub fn remove_participant(&mut self, peer_id: &[u8; 32]) {
        self.participants.remove(peer_id);
        self.pairwise_sessions.remove(peer_id);
        self.remote_sender_keys.remove(peer_id);

        // Immediate rotation of local sender key for Forward Secrecy
        self.our_sender_key = SenderKeyChain::new(self.our_sender_key.chain_id + 1);

        // If in Adaptive mode and count drops <= 30, enable potential downgrade
        if self.mode == GroupMode::Adaptive && self.participants.len() <= MAX_PAIRWISE_PARTICIPANTS
        {
            self.pending_system_notification = Some(
                "Участников стало <= 30. Доступен режим максимальной паранойи (StrictPairwise)."
                    .to_string(),
            );
        }
    }

    /// Forces or triggers transition from pairwise to Sender Keys protocol.
    pub fn transition_to_sender_keys(&mut self) {
        // 1. Wipe all old pairwise sessions from memory
        self.pairwise_sessions.clear();

        // 2. Fresh sender key generation
        self.our_sender_key = SenderKeyChain::new(self.our_sender_key.chain_id + 1);

        // 3. Set alert
        self.pending_system_notification = Some(
            "В группе более 30 участников. Режим шифрования автоматически переключен на высокоскоростной Sender Keys. Старые ключи уничтожены."
                .to_string(),
        );
    }

    /// Encrypts an outgoing group message.
    pub fn seal_message(&mut self, plaintext: &[u8]) -> Result<GroupMessagePayload, CryptoError> {
        let is_sender_keys = match self.mode {
            GroupMode::StrictPairwise => false,
            GroupMode::SenderKeys => true,
            GroupMode::Adaptive => self.participants.len() > MAX_PAIRWISE_PARTICIPANTS,
        };

        if is_sender_keys {
            // 1 encryption for all 300+ members!
            let (iter, mut msg_key) = self.our_sender_key.ratchet_step();
            let mut aad = Vec::with_capacity(40);
            aad.extend_from_slice(&self.group_id);
            aad.extend_from_slice(&self.local_id);
            aad.extend_from_slice(&iter.to_be_bytes());

            let ct = aead::seal(&msg_key, &aad, plaintext)?;
            msg_key.zeroize();

            Ok(GroupMessagePayload {
                mode: GroupMode::SenderKeys,
                sender_id: self.local_id,
                pairwise_payloads: None,
                sender_key_message: Some(GroupSenderKeyMessage {
                    chain_id: self.our_sender_key.chain_id,
                    iteration: iter,
                    ciphertext: ct,
                }),
            })
        } else {
            // Pairwise mode: separate message per participant
            let mut payloads = HashMap::new();
            for (peer_id, session) in self.pairwise_sessions.iter_mut() {
                let msg = session.ratchet_encrypt(plaintext, &self.group_id)?;
                payloads.insert(*peer_id, msg);
            }

            Ok(GroupMessagePayload {
                mode: GroupMode::StrictPairwise,
                sender_id: self.local_id,
                pairwise_payloads: Some(payloads),
                sender_key_message: None,
            })
        }
    }

    /// Decrypts an incoming group message.
    pub fn open_message(&mut self, payload: &GroupMessagePayload) -> Result<Vec<u8>, CryptoError> {
        if let Some(sk_msg) = &payload.sender_key_message {
            let chain = self
                .remote_sender_keys
                .get_mut(&payload.sender_id)
                .ok_or(CryptoError::Decrypt)?;

            let (_, mut msg_key) = chain.ratchet_step();
            let mut aad = Vec::with_capacity(40);
            aad.extend_from_slice(&self.group_id);
            aad.extend_from_slice(&payload.sender_id);
            aad.extend_from_slice(&sk_msg.iteration.to_be_bytes());

            let pt = aead::open(&msg_key, &aad, &sk_msg.ciphertext)?;
            msg_key.zeroize();
            Ok(pt)
        } else if let Some(pairwise_map) = &payload.pairwise_payloads {
            let my_msg = pairwise_map
                .get(&self.local_id)
                .ok_or(CryptoError::Decrypt)?;
            let session = self
                .pairwise_sessions
                .get_mut(&payload.sender_id)
                .ok_or(CryptoError::Decrypt)?;

            session.ratchet_decrypt(my_msg, &self.group_id)
        } else {
            Err(CryptoError::Malformed)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_sender_keys_group_roundtrip() {
        let group_id = [0x77u8; 32];
        let alice_id = [0x01u8; 32];
        let bob_id = [0x02u8; 32];

        let mut alice_session =
            GroupEncryptionSession::new(group_id, alice_id, GroupMode::SenderKeys);
        let mut bob_session = GroupEncryptionSession::new(group_id, bob_id, GroupMode::SenderKeys);

        // Alice shares her initial sender key with Bob
        bob_session.remote_sender_keys.insert(
            alice_id,
            SenderKeyChain::from_received_key(1, alice_session.our_sender_key.current_chain_key()),
        );

        // Alice sends message to group
        let msg = alice_session
            .seal_message(b"Hello Group via Sender Keys!")
            .unwrap();

        // Bob opens it
        let dec = bob_session.open_message(&msg).unwrap();
        assert_eq!(dec, b"Hello Group via Sender Keys!");
    }

    #[test]
    fn test_adaptive_threshold_transition_at_31() {
        let group_id = [0x88u8; 32];
        let alice_id = [0x01u8; 32];

        let mut group = GroupEncryptionSession::new(group_id, alice_id, GroupMode::Adaptive);

        // Add 30 members -> remains pairwise
        for i in 1..=30 {
            let mut peer_id = [0u8; 32];
            peer_id[0] = i as u8;
            group.add_participant(peer_id, [0xAA; 32]);
        }
        assert_eq!(group.participants.len(), 30);
        assert!(group.pending_system_notification.is_none());

        // Add 31st member -> triggers transition & alert!
        let mut peer_31 = [0u8; 32];
        peer_31[0] = 31;
        group.add_participant(peer_31, [0xAA; 32]);

        assert_eq!(group.participants.len(), 31);
        assert!(group.pending_system_notification.is_some());
        assert!(group
            .pending_system_notification
            .unwrap()
            .contains("В группе более 30 участников"));
    }
}
