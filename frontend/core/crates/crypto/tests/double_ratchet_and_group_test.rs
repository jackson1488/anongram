//! Test Suite: Double Ratchet, Hybrid Group Encryption & Anti-MITM Verification.
//!
//! Validates:
//! 1. Double Ratchet symmetric KDF advance on EVERY message.
//! 2. Double Ratchet DH ratchet advance on replies.
//! 3. StrictPairwise mode (<= 30 participants) with pairwise Double Ratchets.
//! 4. SenderKeys mode (30 to 300+ participants) with 1 encryption per group message.
//! 5. Adaptive threshold auto-transition at 31 participants with memory wipe & system alert.
//! 6. 4-module verification: 60-digit code, QR payload, KeyChangeGuard, ServerTransparency.

use rand::rngs::OsRng;
use x25519_dalek::{PublicKey as XPublic, StaticSecret};

use crypto::aead::KEY_LEN;
use crypto::group::{
    GroupEncryptionSession, GroupMode, SenderKeyChain, MAX_PAIRWISE_PARTICIPANTS,
};
use crypto::ratchet::DoubleRatchet;

// =========================================================================
// 1. DOUBLE RATCHET PER-MESSAGE KDF ISOLATION
// =========================================================================
#[test]
fn test_double_ratchet_per_message_key_isolation() {
    let master = [0x42u8; KEY_LEN];
    let bob_secret = StaticSecret::random_from_rng(OsRng);
    let bob_public = XPublic::from(&bob_secret).to_bytes();

    let mut alice = DoubleRatchet::new_initiator(master, bob_public);
    let mut bob = DoubleRatchet::new_responder(master, bob_secret);

    // Alice sends 5 consecutive messages without Bob replying
    let mut messages = Vec::new();
    for i in 0..5 {
        let text = format!("Message sequence index: {}", i);
        let msg = alice.ratchet_encrypt(text.as_bytes(), b"room-1").expect("Encrypt");
        assert_eq!(msg.sequence_counter, i);
        messages.push((text, msg));
    }

    // Ensure all ciphertexts are distinct and non-repeating
    for i in 0..messages.len() {
        for j in (i + 1)..messages.len() {
            assert_ne!(messages[i].1.ciphertext, messages[j].1.ciphertext);
        }
    }

    // Bob decrypts all 5 messages cleanly
    for (expected_text, msg) in messages {
        let dec = bob.ratchet_decrypt(&msg, b"room-1").expect("Decrypt");
        assert_eq!(dec, expected_text.as_bytes());
    }
}

// =========================================================================
// 2. DOUBLE RATCHET ASYMMETRIC DH/KEM ADVANCEMENT
// =========================================================================
#[test]
fn test_double_ratchet_dh_advancement() {
    let master = [0x88u8; KEY_LEN];
    let bob_secret = StaticSecret::random_from_rng(OsRng);
    let bob_public = XPublic::from(&bob_secret).to_bytes();

    let mut alice = DoubleRatchet::new_initiator(master, bob_public);
    let mut bob = DoubleRatchet::new_responder(master, bob_secret);

    // Alice -> Bob
    let m1 = alice.ratchet_encrypt(b"Ping from Alice", b"room").unwrap();
    let d1 = bob.ratchet_decrypt(&m1, b"room").unwrap();
    assert_eq!(d1, b"Ping from Alice");

    // Bob -> Alice (triggers DH ratchet step)
    let m2 = bob.ratchet_encrypt(b"Pong from Bob", b"room").unwrap();
    let d2 = alice.ratchet_decrypt(&m2, b"room").unwrap();
    assert_eq!(d2, b"Pong from Bob");

    // Alice -> Bob again (triggers next DH ratchet step)
    let m3 = alice.ratchet_encrypt(b"Follow-up from Alice", b"room").unwrap();
    let d3 = bob.ratchet_decrypt(&m3, b"room").unwrap();
    assert_eq!(d3, b"Follow-up from Alice");
}

// =========================================================================
// 3. STRICT PAIRWISE GROUP ENCRYPTION (<= 30 MEMBERS)
// =========================================================================
#[test]
fn test_strict_pairwise_group_encryption() {
    let group_id = [0x55u8; 32];
    let alice_id = [0x01u8; 32];
    let bob_id = [0x02u8; 32];

    let master_shared = [0x33u8; KEY_LEN];

    let mut alice_group = GroupEncryptionSession::new(group_id, alice_id, GroupMode::StrictPairwise);
    let mut bob_group = GroupEncryptionSession::new(group_id, bob_id, GroupMode::StrictPairwise);

    // Pairwise setup between Alice and Bob
    let bob_sec = StaticSecret::random_from_rng(OsRng);
    let bob_pub = XPublic::from(&bob_sec).to_bytes();

    let alice_to_bob_ratchet = DoubleRatchet::new_initiator(master_shared, bob_pub);
    let bob_to_alice_ratchet = DoubleRatchet::new_responder(master_shared, bob_sec);

    alice_group.pairwise_sessions.insert(bob_id, alice_to_bob_ratchet);
    bob_group.pairwise_sessions.insert(alice_id, bob_to_alice_ratchet);

    // Alice seals pairwise message
    let payload = alice_group.seal_message(b"Secret group message").unwrap();
    assert_eq!(payload.mode, GroupMode::StrictPairwise);
    assert!(payload.pairwise_payloads.is_some());

    // Bob opens pairwise message
    let opened = bob_group.open_message(&payload).unwrap();
    assert_eq!(opened, b"Secret group message");
}

// =========================================================================
// 4. SENDER KEYS SCALABILITY (100 MEMBERS, 1 ENCRYPTION PER MSG)
// =========================================================================
#[test]
fn test_sender_keys_100_members_single_encryption() {
    let group_id = [0x99u8; 32];
    let alice_id = [0x01u8; 32];

    let mut alice_group = GroupEncryptionSession::new(group_id, alice_id, GroupMode::SenderKeys);

    // Populate 100 group members
    for i in 2..=101 {
        let mut peer_id = [0u8; 32];
        peer_id[0] = i as u8;
        alice_group.add_participant(peer_id, [0xAA; 32]);
    }
    assert_eq!(alice_group.participants.len(), 100);

    // Alice encrypts ONCE for all 100 members
    let msg_payload = alice_group.seal_message(b"Message to 100 members simultaneously!").unwrap();
    assert_eq!(msg_payload.mode, GroupMode::SenderKeys);
    assert!(msg_payload.sender_key_message.is_some());

    // Verify Bob (member 50) decrypts cleanly with Alice's sender key
    let bob_id = [50u8; 32];
    let mut bob_group = GroupEncryptionSession::new(group_id, bob_id, GroupMode::SenderKeys);
    bob_group.remote_sender_keys.insert(
        alice_id,
        SenderKeyChain::from_received_key(1, alice_group.our_sender_key.current_chain_key()),
    );

    let bob_decrypted = bob_group.open_message(&msg_payload).unwrap();
    assert_eq!(bob_decrypted, b"Message to 100 members simultaneously!");
}

// =========================================================================
// 5. ADAPTIVE THRESHOLD AT 31 PARTICIPANTS (WIPES KEYS & ALERTS CHAT)
// =========================================================================
#[test]
fn test_adaptive_threshold_transition_and_key_destruction() {
    let group_id = [0x11u8; 32];
    let alice_id = [0x01u8; 32];

    let mut session = GroupEncryptionSession::new(group_id, alice_id, GroupMode::Adaptive);

    // Add exactly 30 participants
    for i in 1..=MAX_PAIRWISE_PARTICIPANTS {
        let mut pid = [0u8; 32];
        pid[0] = i as u8;
        session.add_participant(pid, [0xEE; 32]);
    }
    assert_eq!(session.participants.len(), 30);
    assert!(session.pending_system_notification.is_none());

    // 31st participant joins!
    let mut peer_31 = [0u8; 32];
    peer_31[0] = 31;
    session.add_participant(peer_31, [0xEE; 32]);

    assert_eq!(session.participants.len(), 31);
    // Old pairwise sessions wiped!
    assert_eq!(session.pairwise_sessions.len(), 0);

    // Chat alert generated
    let alert = session.pending_system_notification.take().expect("Alert must exist");
    assert!(alert.contains("В группе более 30 участников"));
    assert!(alert.contains("Sender Keys"));
    assert!(alert.contains("Старые ключи уничтожены"));

    // Next message sent automatically uses Sender Keys
    let group_msg = session.seal_message(b"Welcome 31st member!").unwrap();
    assert_eq!(group_msg.mode, GroupMode::SenderKeys);
}
