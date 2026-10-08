//! WebRTC Signaling Messages (SDP Offer/Answer & ICE Candidates).
//!
//! Exchanged via secure signaling channel or silent Push notifications.

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum CallSignal {
    /// Initial Call Invitation with SDP Offer
    Offer {
        call_id: [u8; 16],
        sdp: String,
        is_video: bool,
    },
    /// Response with SDP Answer
    Answer {
        call_id: [u8; 16],
        sdp: String,
    },
    /// NAT Traversal Candidate
    IceCandidate {
        call_id: [u8; 16],
        candidate: String,
        sdp_mid: Option<String>,
        sdp_mline_index: u16,
    },
    /// Upgrade 1-on-1 call to multi-party Group Call room
    UpgradeToGroup {
        call_id: [u8; 16],
        room_id: [u8; 32],
        new_participant_id: [u8; 32],
    },
    /// Call Termination
    Hangup {
        call_id: [u8; 16],
        reason: String,
    },
}
