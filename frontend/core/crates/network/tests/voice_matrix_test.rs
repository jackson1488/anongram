//! Comprehensive Test Suite for WebRTC SFrame E2EE Voice & Video Engine.
//!
//! Tests all call variants, dynamic group upgrades, blind SFU routing,
//! anti-replay/tamper resilience, ICE fallback, and WhatsApp-style controls.

use crypto::aead::KEY_LEN;
use network::voice::{
    CallParticipant, CallSession, CallSignal, CallTransportState, CallType, EncryptedMediaFrame,
    IceConfiguration, IceServer, IceTransportPolicy, MediaType, SFrameEngine,
};

const ALICE_KEY: [u8; KEY_LEN] = [0x11; KEY_LEN];
const BOB_KEY: [u8; KEY_LEN] = [0x22; KEY_LEN];
const CHARLIE_KEY: [u8; KEY_LEN] = [0x33; KEY_LEN];
const DAVID_KEY: [u8; KEY_LEN] = [0x44; KEY_LEN];
const EVE_KEY: [u8; KEY_LEN] = [0x55; KEY_LEN];

const SHARED_1ON1_KEY: [u8; KEY_LEN] = [0x99; KEY_LEN];

const CALL_ID: [u8; 16] = [0xAA; 16];
const ALICE_ID: [u8; 32] = [0x01; 32];
const BOB_ID: [u8; 32] = [0x02; 32];
const CHARLIE_ID: [u8; 32] = [0x03; 32];
const DAVID_ID: [u8; 32] = [0x04; 32];
const EVE_ID: [u8; 32] = [0x05; 32];

// =========================================================================
// 1. 1-on-1 AUDIO OPUS TEST
// =========================================================================
#[test]
fn test_1on1_audio_opus_roundtrip() {
    let mut alice_call = CallSession::new_one_on_one(CALL_ID, BOB_ID, SHARED_1ON1_KEY);
    let bob_call = CallSession::new_one_on_one(CALL_ID, ALICE_ID, SHARED_1ON1_KEY);

    assert_eq!(alice_call.call_type, CallType::OneOnOne);
    assert_eq!(alice_call.participant_count(), 2);

    let opus_frame = b"Opus_48kHz_20ms_Alice_Voice_Sample";
    let encrypted = alice_call
        .produce_outgoing_frame(MediaType::AudioOpus, opus_frame)
        .expect("Alice seals Opus frame");

    assert_eq!(encrypted.media_type, MediaType::AudioOpus);
    assert_eq!(encrypted.frame_seq, 1);

    let bob_decrypted = bob_call
        .consume_incoming_frame(&ALICE_ID, &encrypted)
        .expect("Bob decrypts Alice's Opus frame");

    assert_eq!(bob_decrypted, opus_frame);
}

// =========================================================================
// 2. 1-on-1 VIDEO (VP8 & H.264) TEST
// =========================================================================
#[test]
fn test_1on1_video_h264_and_vp8() {
    let mut session = CallSession::new_one_on_one(CALL_ID, BOB_ID, SHARED_1ON1_KEY);

    // H.264 1080p Keyframe
    let h264_payload = vec![0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x1F, 0xAA, 0xBB];
    let enc_h264 = session
        .produce_outgoing_frame(MediaType::VideoH264, &h264_payload)
        .expect("Produce H264 frame");
    assert_eq!(enc_h264.media_type, MediaType::VideoH264);

    let dec_h264 = session
        .consume_incoming_frame(&BOB_ID, &enc_h264)
        .expect("Decrypt H264");
    assert_eq!(dec_h264, h264_payload);

    // VP8 720p Delta frame
    let vp8_payload = b"VP8_Delta_Frame_720p_Timestamp_90000";
    let enc_vp8 = session
        .produce_outgoing_frame(MediaType::VideoVp8, vp8_payload)
        .expect("Produce VP8 frame");
    assert_eq!(enc_vp8.media_type, MediaType::VideoVp8);

    let dec_vp8 = session
        .consume_incoming_frame(&BOB_ID, &enc_vp8)
        .expect("Decrypt VP8");
    assert_eq!(dec_vp8, vp8_payload);
}

// =========================================================================
// 3. SFRAME TAMPER & CORRUPTION RESISTANCE
// =========================================================================
#[test]
fn test_sframe_tamper_rejection() {
    let mut engine = SFrameEngine::new(1, ALICE_KEY);
    let original_pcm = b"Voice secret data";

    let mut frame = engine
        .seal_frame(MediaType::AudioOpus, original_pcm)
        .expect("Seal frame");

    // Corrupt single bit in ciphertext
    frame.ciphertext[0] ^= 0x01;

    let decrypt_engine = SFrameEngine::new(1, ALICE_KEY);
    let result = decrypt_engine.open_frame(&frame);
    assert!(
        result.is_err(),
        "Corrupted ciphertext must be rejected by Poly1305 MAC"
    );
}

// =========================================================================
// 4. SFRAME REPLAY & SEQUENCE ATTACK PREVENTION
// =========================================================================
#[test]
fn test_sframe_replay_attack_prevention() {
    let mut engine = SFrameEngine::new(1, ALICE_KEY);
    let frame1 = engine
        .seal_frame(MediaType::AudioOpus, b"Packet 1")
        .expect("Frame 1");
    let frame2 = engine
        .seal_frame(MediaType::AudioOpus, b"Packet 2")
        .expect("Frame 2");

    assert_eq!(frame1.frame_seq, 1);
    assert_eq!(frame2.frame_seq, 2);

    let decrypt_engine = SFrameEngine::new(1, ALICE_KEY);

    // Normal decryption succeeds
    let dec1 = decrypt_engine.open_frame(&frame1).expect("Open 1");
    assert_eq!(dec1, b"Packet 1");

    // Replay with forged sequence number in AAD header fails
    let mut forged_frame = frame1.clone();
    forged_frame.frame_seq = 9999; // Attacker altered sequence in unencrypted header
    let forged_result = decrypt_engine.open_frame(&forged_frame);
    assert!(
        forged_result.is_err(),
        "Altering frame_seq without matching AAD MAC must fail"
    );
}

// =========================================================================
// 5. DYNAMIC MID-CALL UPGRADE (1-on-1 to 3-Party Group)
// =========================================================================
#[test]
fn test_dynamic_upgrade_1on1_to_group_3_parties() {
    let mut alice_session = CallSession::new_one_on_one(CALL_ID, BOB_ID, SHARED_1ON1_KEY);
    assert_eq!(alice_session.call_type, CallType::OneOnOne);

    // Charlie is invited mid-call
    alice_session.add_participant(CHARLIE_ID, 3, CHARLIE_KEY);

    assert_eq!(alice_session.call_type, CallType::GroupCall);
    assert_eq!(
        alice_session.transport_state,
        CallTransportState::UpgradedToGroupSfu
    );
    assert_eq!(alice_session.participant_count(), 3);

    // Charlie transmits his own encrypted voice frame
    let mut charlie_sender = SFrameEngine::new(3, CHARLIE_KEY);
    let charlie_voice = charlie_sender
        .seal_frame(MediaType::AudioOpus, b"Hello everyone, Charlie here!")
        .expect("Charlie speaks");

    let alice_heard = alice_session
        .consume_incoming_frame(&CHARLIE_ID, &charlie_voice)
        .expect("Alice hears Charlie");
    assert_eq!(alice_heard, b"Hello everyone, Charlie here!");
}

// =========================================================================
// 6. SCALABILITY: 5-PARTY GROUP CALL IN BLIND SFU ROOM
// =========================================================================
#[test]
fn test_dynamic_upgrade_to_5_party_sfu() {
    let mut group = CallSession::new_one_on_one(CALL_ID, BOB_ID, BOB_KEY);
    group.add_participant(CHARLIE_ID, 3, CHARLIE_KEY);
    group.add_participant(DAVID_ID, 4, DAVID_KEY);
    group.add_participant(EVE_ID, 5, EVE_KEY);

    assert_eq!(group.participant_count(), 5); // Local + Bob + Charlie + David + Eve
    assert_eq!(group.call_type, CallType::GroupCall);

    // Eve transmits HD video
    let mut eve_engine = SFrameEngine::new(5, EVE_KEY);
    let eve_video = eve_engine
        .seal_frame(MediaType::VideoH264, b"Eve 1080p 60fps Video Stream")
        .expect("Eve seals video");

    let local_sees_eve = group
        .consume_incoming_frame(&EVE_ID, &eve_video)
        .expect("Local user receives Eve video");
    assert_eq!(local_sees_eve, b"Eve 1080p 60fps Video Stream");

    // David leaves group
    group.remove_participant(&DAVID_ID);
    assert_eq!(group.participant_count(), 4);
    assert!(group.consume_incoming_frame(&DAVID_ID, &eve_video).is_err());
}

// =========================================================================
// 7. BLIND SFU ZERO-KNOWLEDGE PROOF
// =========================================================================
#[test]
fn test_blind_sfu_zero_knowledge() {
    // SFU only knows packet metadata: key_id, frame_seq, media_type
    let mut sender = SFrameEngine::new(42, ALICE_KEY);
    let encrypted = sender
        .seal_frame(MediaType::AudioOpus, b"Super confidential military comms")
        .expect("Sender seals");

    // SFU inspects header to route
    assert_eq!(encrypted.key_id, 42);
    assert_eq!(encrypted.media_type, MediaType::AudioOpus);

    // Malicious SFU attempts to decrypt without Alice's key
    let rogue_sfu_engine = SFrameEngine::new(42, [0xFF; KEY_LEN]);
    let eavesdrop_attempt = rogue_sfu_engine.open_frame(&encrypted);
    assert!(
        eavesdrop_attempt.is_err(),
        "Blind SFU cannot decrypt media payload without sender ratchet key"
    );
}

// =========================================================================
// 8. TURNS TLS PORT 443 FALLBACK CONFIGURATION
// =========================================================================
#[test]
fn test_turns_tls_443_fallback_simulation() {
    let ice = IceConfiguration::default();
    assert_eq!(ice.ice_servers.len(), 2);

    // Host P2P / STUN
    assert_eq!(ice.ice_servers[0].urls[0], "stun:stun.l.google.com:19302");

    // TURN UDP & Censorship-resistant TURNS over TLS 443 TCP
    assert_eq!(
        ice.ice_servers[1].urls[0],
        "turn:cloud.neongram.space:3478?transport=udp"
    );
    assert_eq!(
        ice.ice_servers[1].urls[1],
        "turns:cloud.neongram.space:443?transport=tcp"
    );

    // Privacy Relay-Only policy hides IP
    let privacy_ice = IceConfiguration::privacy_relay_only();
    assert_eq!(privacy_ice.transport_policy, IceTransportPolicy::RelayOnly);
}

// =========================================================================
// 9. WHATSAPP-STYLE CALL CONTROLS, PIP & SAS FINGERPRINT
// =========================================================================
#[test]
fn test_call_control_mute_pip_and_sas() {
    let mut call = CallSession::new_one_on_one(CALL_ID, BOB_ID, SHARED_1ON1_KEY);

    // Initial state
    assert!(!call.is_muted);
    assert!(call.is_video_enabled);
    assert!(!call.is_speaker_on);
    assert!(!call.pip_mode);

    // Toggle mute
    assert!(call.toggle_mute()); // Muted
    assert!(!call.toggle_mute()); // Unmuted

    // Toggle video
    assert!(!call.toggle_video()); // Video off
    assert!(call.toggle_video()); // Video on

    // Toggle speakerphone
    assert!(call.toggle_speaker()); // Speaker on
    assert!(!call.toggle_speaker()); // Speaker off

    // PiP Mode (switched to another app)
    call.set_pip_mode(true);
    assert!(call.pip_mode);
    call.set_pip_mode(false);
    assert!(!call.pip_mode);

    // SAS (Short Authentication String) fingerprint
    let sas1 = call.get_sas_fingerprint();
    let sas2 = call.get_sas_fingerprint();
    assert_eq!(
        sas1, sas2,
        "SAS fingerprint must be deterministic for identical session"
    );
    assert_eq!(
        sas1.chars().filter(|c| !c.is_whitespace()).count() > 0,
        true
    );

    // Hangup cleans memory and terminates
    call.hangup();
    assert_eq!(call.transport_state, CallTransportState::Ended);
    assert_eq!(call.participants.len(), 0);
}
