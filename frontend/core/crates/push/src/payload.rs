//! Push Commands and Encrypted Notification Decryptor.
//!
//! Handles background silent push notifications:
//! 1. Decrypts messages from contacts (Alice, Bob) on arrival in the background.
//! 2. Executes silent system commands:
//!    - RotateServerEndpoint (switches IP/Domain/Port without app update)
//!    - WakeAndSync (silent sync wake lock)
//!    - PanicWipe (emergency wipe)

use crypto::aead::{self, KEY_LEN};
use zeroize::Zeroize;

use crate::error::PushError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum PushAction {
    /// Incoming encrypted message for user (e.g. from Alice/Bob)
    IncomingMessage {
        sender_id: [u8; 32],
        plaintext: Vec<u8>,
    },
    /// Silent command to rotate server network endpoint dynamically
    RotateServerEndpoint {
        new_endpoint: String,
    },
    /// Wake up app background worker to pull messages from relay
    WakeAndSync,
    /// Emergency remote wipe signal (e.g. sent from master cold key)
    PanicWipe,
}

pub struct PushProcessor {
    last_nonce: u64,
}

impl Default for PushProcessor {
    fn default() -> Self {
        Self::new()
    }
}

impl PushProcessor {
    pub fn new() -> Self {
        Self { last_nonce: 0 }
    }

    /// Packs and encrypts a push notification payload (Sender side).
    pub fn pack_push_payload(
        session_key: &[u8; KEY_LEN],
        nonce: u64,
        action: &PushAction,
    ) -> Result<Vec<u8>, PushError> {
        let mut buffer = Vec::new();
        buffer.extend_from_slice(&nonce.to_be_bytes());

        match action {
            PushAction::IncomingMessage {
                sender_id,
                plaintext,
            } => {
                buffer.push(0x01); // Tag: Message
                buffer.extend_from_slice(sender_id);
                buffer.extend_from_slice(plaintext);
            }
            PushAction::RotateServerEndpoint { new_endpoint } => {
                buffer.push(0x02); // Tag: RotateEndpoint
                let ep_bytes = new_endpoint.as_bytes();
                buffer.extend_from_slice(&(ep_bytes.len() as u16).to_be_bytes());
                buffer.extend_from_slice(ep_bytes);
            }
            PushAction::WakeAndSync => {
                buffer.push(0x03); // Tag: WakeAndSync
            }
            PushAction::PanicWipe => {
                buffer.push(0xFF); // Tag: PanicWipe
            }
        }

        let aad = b"anongram/push/v1";
        aead::seal(session_key, aad, &buffer).map_err(|_| PushError::Crypto)
    }

    /// Decrypts, verifies replay protection and processes incoming push payload (Receiver background).
    pub fn unpack_push_payload(
        &mut self,
        session_key: &[u8; KEY_LEN],
        encrypted_blob: &[u8],
    ) -> Result<PushAction, PushError> {
        let aad = b"anongram/push/v1";
        let mut plain = aead::open(session_key, aad, encrypted_blob).map_err(|_| PushError::AuthFailed)?;

        if plain.len() < 9 {
            // 8 bytes nonce + 1 byte tag
            plain.zeroize();
            return Err(PushError::Malformed);
        }

        let nonce = u64::from_be_bytes(plain[0..8].try_into().unwrap());
        if nonce <= self.last_nonce {
            plain.zeroize();
            return Err(PushError::ReplayDetected);
        }
        self.last_nonce = nonce;

        let tag = plain[8];
        let action = match tag {
            0x01 => {
                // Incoming message
                if plain.len() < 9 + 32 {
                    plain.zeroize();
                    return Err(PushError::Malformed);
                }
                let mut sender_id = [0u8; 32];
                sender_id.copy_from_slice(&plain[9..41]);
                let plaintext = plain[41..].to_vec();
                PushAction::IncomingMessage {
                    sender_id,
                    plaintext,
                }
            }
            0x02 => {
                // Rotate endpoint
                if plain.len() < 11 {
                    plain.zeroize();
                    return Err(PushError::Malformed);
                }
                let len = u16::from_be_bytes([plain[9], plain[10]]) as usize;
                if plain.len() < 11 + len {
                    plain.zeroize();
                    return Err(PushError::Malformed);
                }
                let ep_str = String::from_utf8(plain[11..11 + len].to_vec())
                    .map_err(|_| PushError::Malformed)?;
                PushAction::RotateServerEndpoint {
                    new_endpoint: ep_str,
                }
            }
            0x03 => PushAction::WakeAndSync,
            0xFF => PushAction::PanicWipe,
            _ => {
                plain.zeroize();
                return Err(PushError::Malformed);
            }
        };

        plain.zeroize();
        Ok(action)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const TEST_KEY: [u8; KEY_LEN] = [0x55; KEY_LEN];

    #[test]
    fn test_push_decrypt_incoming_message_from_alice() {
        let mut processor = PushProcessor::new();
        let alice_id = [0xAA; 32];
        let original_msg = b"Hello from Alice: safe rendezvous at 18:00";

        let action = PushAction::IncomingMessage {
            sender_id: alice_id,
            plaintext: original_msg.to_vec(),
        };

        let blob = PushProcessor::pack_push_payload(&TEST_KEY, 1, &action).unwrap();
        let received_action = processor.unpack_push_payload(&TEST_KEY, &blob).unwrap();

        match received_action {
            PushAction::IncomingMessage {
                sender_id,
                plaintext,
            } => {
                assert_eq!(sender_id, alice_id);
                assert_eq!(plaintext, original_msg);
            }
            _ => panic!("Expected IncomingMessage"),
        }
    }

    #[test]
    fn test_push_command_rotate_endpoint() {
        let mut processor = PushProcessor::new();
        let new_url = "https://backup-relay.anongram.net:8443/api".to_string();

        let action = PushAction::RotateServerEndpoint {
            new_endpoint: new_url.clone(),
        };

        let blob = PushProcessor::pack_push_payload(&TEST_KEY, 10, &action).unwrap();
        let received = processor.unpack_push_payload(&TEST_KEY, &blob).unwrap();

        assert_eq!(
            received,
            PushAction::RotateServerEndpoint {
                new_endpoint: new_url
            }
        );
    }

    #[test]
    fn test_push_replay_attack_rejected() {
        let mut processor = PushProcessor::new();
        let action = PushAction::WakeAndSync;

        let blob = PushProcessor::pack_push_payload(&TEST_KEY, 5, &action).unwrap();
        assert!(processor.unpack_push_payload(&TEST_KEY, &blob).is_ok());

        // Replay of same or older nonce must be rejected
        assert_eq!(
            processor.unpack_push_payload(&TEST_KEY, &blob).unwrap_err(),
            PushError::ReplayDetected
        );
    }
}
