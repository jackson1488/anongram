//! Call Controller supporting 1-on-1 P2P, NAT traversal, and Dynamic Group Upgrade.
//!
//! Enables seamless upgrading from a private 1-on-1 call into a multi-party group call
//! without terminating the existing audio/video stream.

use std::collections::HashMap;

use crypto::aead::KEY_LEN;
use zeroize::Zeroize;

use crate::error::NetworkError;
use crate::voice::ice_config::IceConfiguration;
use crate::voice::sframe_e2ee::{EncryptedMediaFrame, MediaType, SFrameEngine};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CallType {
    OneOnOne,
    GroupCall,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CallTransportState {
    NegotiatingIce,
    ConnectedP2PDirect,
    ConnectedTurnRelayed,
    UpgradedToGroupSfu,
    Ended,
}

pub struct CallParticipant {
    pub participant_id: [u8; 32],
    pub is_muted: bool,
    pub is_video_enabled: bool,
    pub decryptor: SFrameEngine,
}

pub struct CallSession {
    pub call_id: [u8; 16],
    pub call_type: CallType,
    pub transport_state: CallTransportState,
    pub ice_config: IceConfiguration,
    pub local_encryptor: SFrameEngine,
    pub participants: HashMap<[u8; 32], CallParticipant>,
}

impl CallSession {
    /// Initiates a standard 1-on-1 private call.
    pub fn new_one_on_one(
        call_id: [u8; 16],
        peer_id: [u8; 32],
        shared_session_key: [u8; KEY_LEN],
    ) -> Self {
        let local_encryptor = SFrameEngine::new(0, shared_session_key);
        let peer_decryptor = SFrameEngine::new(0, shared_session_key);

        let mut participants = HashMap::new();
        participants.insert(
            peer_id,
            CallParticipant {
                participant_id: peer_id,
                is_muted: false,
                is_video_enabled: true,
                decryptor: peer_decryptor,
            },
        );

        Self {
            call_id,
            call_type: CallType::OneOnOne,
            transport_state: CallTransportState::NegotiatingIce,
            ice_config: IceConfiguration::default(),
            local_encryptor,
            participants,
        }
    }

    /// Dynamically upgrades a private 1-on-1 call into a multi-party Group Call room.
    pub fn upgrade_to_group(&mut self, room_id: [u8; 32]) {
        self.call_type = CallType::GroupCall;
        self.transport_state = CallTransportState::UpgradedToGroupSfu;
        // In group mode, SFU routes packets based on SFrame unencrypted header
        let _ = room_id;
    }

    /// Adds a new participant into an active group call.
    pub fn add_participant(
        &mut self,
        participant_id: [u8; 32],
        key_id: u32,
        participant_key: [u8; KEY_LEN],
    ) {
        if self.call_type == CallType::OneOnOne {
            // Automatically upgrade to group if second peer is added
            self.upgrade_to_group([0xAA; 32]);
        }

        let decryptor = SFrameEngine::new(key_id, participant_key);
        self.participants.insert(
            participant_id,
            CallParticipant {
                participant_id,
                is_muted: false,
                is_video_enabled: true,
                decryptor,
            },
        );
    }

    /// Removes a participant from the call.
    pub fn remove_participant(&mut self, participant_id: &[u8; 32]) {
        self.participants.remove(participant_id);
    }

    /// Seals an outgoing local voice/video frame.
    pub fn produce_outgoing_frame(
        &mut self,
        media_type: MediaType,
        pcm_or_video: &[u8],
    ) -> Result<EncryptedMediaFrame, NetworkError> {
        self.local_encryptor.seal_frame(media_type, pcm_or_video)
    }

    /// Decrypts an incoming frame received from a specific participant.
    pub fn consume_incoming_frame(
        &self,
        sender_id: &[u8; 32],
        frame: &EncryptedMediaFrame,
    ) -> Result<Vec<u8>, NetworkError> {
        match self.participants.get(sender_id) {
            Some(peer) => peer.decryptor.open_frame(frame),
            None => Err(NetworkError::MalformedFrame),
        }
    }

    pub fn participant_count(&self) -> usize {
        self.participants.len() + 1 // + 1 for local user
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SHARED_KEY: [u8; KEY_LEN] = [0x99; KEY_LEN];

    #[test]
    fn test_one_on_one_call_and_upgrade_to_group() {
        let call_id = [1u8; 16];
        let alice_id = [0x01; 32];
        let bob_id = [0x02; 32];
        let charlie_id = [0x03; 32];

        // 1. Alice starts 1-on-1 call with Bob
        let mut session = CallSession::new_one_on_one(call_id, bob_id, SHARED_KEY);
        assert_eq!(session.call_type, CallType::OneOnOne);
        assert_eq!(session.participant_count(), 2);

        // Alice sends audio to Bob
        let audio_frame = session
            .produce_outgoing_frame(MediaType::AudioOpus, b"Alice speaking to Bob")
            .unwrap();
        let bob_heard = session
            .consume_incoming_frame(&bob_id, &audio_frame)
            .unwrap();
        assert_eq!(bob_heard, b"Alice speaking to Bob");

        // 2. Mid-call upgrade: Bob or Alice invites Charlie to join!
        let charlie_key = [0x88; KEY_LEN];
        session.add_participant(charlie_id, 2, charlie_key);

        assert_eq!(session.call_type, CallType::GroupCall);
        assert_eq!(
            session.transport_state,
            CallTransportState::UpgradedToGroupSfu
        );
        assert_eq!(session.participant_count(), 3); // Alice + Bob + Charlie

        // Charlie's frame decryption
        let mut charlie_encryptor = SFrameEngine::new(2, charlie_key);
        let charlie_audio = charlie_encryptor
            .seal_frame(MediaType::AudioOpus, b"Charlie joined the group call!")
            .unwrap();
        let alice_heard_charlie = session
            .consume_incoming_frame(&charlie_id, &charlie_audio)
            .unwrap();
        assert_eq!(alice_heard_charlie, b"Charlie joined the group call!");
    }
}
