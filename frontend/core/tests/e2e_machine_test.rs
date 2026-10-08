//! End-to-End Integration Machine Test for AnonGram Core.
#![allow(clippy::all)]
//!
//! Tests the full autonomous lifecycle of AnonGram functioning as a single unified machine:
//! 1. Identity Genesis: Master BIP-39 mnemonic seed derivation.
//! 2. Sovereign Passport: Passport creation with monotonic versioning and hybrid signature (Ed25519 + ML-DSA-87).
//! 3. Two-Ended OTP Pairing: Pad manifest authorization (Gold Shield) and bidirectional OTP messaging with Wegman-Carter Poly1305 MAC and disk zeroization.
//! 4. Media Sanitization & Compression: EXIF/GPS metadata stripping and high-speed Zstd compression.
//! 5. Post-Quantum KEM Ratchet: ML-KEM-1024 + X25519 session encapsulation.
//! 6. Multi-Protocol VPN Transport: Encapsulation into VLESS-Reality TLS 1.3 disguise and AmneziaWG obfuscated transport.

use std::io::Write;
use tempfile::NamedTempFile;

use anongram_core::crypto::aead::KEY_LEN;
use anongram_core::crypto::kem;
use anongram_core::identity::manifest::{PadManifest, SecurityShieldLevel, TransferMethod};
use anongram_core::identity::passport::{DeviceEntry, Passport, PassportBody};
use anongram_core::identity::seed::MasterSeed;
use anongram_core::media::{open_media, seal_media, MediaType};
use anongram_core::otp::{OtpStorage, PadSide};
use anongram_core::vpn::amnezia::AmneziaWgConfig;
use anongram_core::vpn::shadowsocks::ShadowsocksConfig;
use anongram_core::vpn::vless::VlessRealityConfig;
use anongram_core::vpn::{CensorshipProfile, VpnOrchestrator, VpnProtocol};

const MNEMONIC_ALICE: &str =
    "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
const MNEMONIC_BOB: &str =
    "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo vote";

#[test]
fn test_full_autonomous_core_machine() {
    println!("=== 1. IDENTITY GENESIS ===");
    let alice_seed = MasterSeed::from_phrase(MNEMONIC_ALICE, "").expect("Alice seed");
    let bob_seed = MasterSeed::from_phrase(MNEMONIC_BOB, "").expect("Bob seed");

    let alice_master_key = alice_seed.master_signing_key();
    let bob_master_key = bob_seed.master_signing_key();

    assert_eq!(alice_seed.entropy_bits(), 128);

    println!("=== 2. SOVEREIGN PASSPORT ISSUANCE ===");
    let alice_prekey = kem::generate_keypair();
    let alice_profile_key = alice_seed.profile_key();
    let alice_vk = alice_master_key.verifying_key();

    let alice_enc_profile =
        PassportBody::seal_profile(alice_vk, 1, &alice_profile_key, b"Alice SecOps")
            .expect("Seal profile");

    let alice_body = PassportBody {
        version: 1,
        created_at: 1_700_000_000,
        master: alice_vk.clone(),
        devices: vec![DeviceEntry {
            device_id: [1u8; 16],
            key: alice_master_key.verifying_key().clone(),
            added_at: 1_700_000_000,
            revoked: false,
        }],
        prekey: alice_prekey.public_key().clone(),
        encrypted_profile: alice_enc_profile,
    };

    let alice_passport = Passport::sign(alice_body, &alice_master_key).expect("Sign passport");
    let passport_bytes = alice_passport.to_bytes();
    let verified_passport = Passport::from_bytes(&passport_bytes).expect("Verify passport");
    assert_eq!(verified_passport.version(), 1);
    assert_eq!(
        verified_passport
            .decrypt_profile(&alice_profile_key)
            .expect("Decrypt profile"),
        b"Alice SecOps"
    );

    println!("=== 3. OTP PAIRING & MANIFEST AUTHORIZATION ===");
    let pad_id = [0x5Au8; 32];
    let pad_len = 4096u64;

    let manifest = PadManifest {
        pad_id,
        total_size: pad_len,
        reserve_pct: 20,
        partner_id: "bob_identity".to_string(),
        local_side: PadSide::SideA,
        transfer_method: TransferMethod::Cable,
        created_at: 1_700_000_000,
        head_offset: 0,
        tail_offset: pad_len,
    };

    assert_eq!(manifest.shield_level(), SecurityShieldLevel::GoldAbsolute);

    let signed_manifest = manifest.sign(&alice_master_key);
    assert!(signed_manifest.verify(alice_vk).is_ok());

    println!("=== 4. BIDIRECTIONAL TWO-ENDED OTP EXCHANGE ===");
    // Create shared random pad file
    let mut random_pad = vec![0u8; pad_len as usize];
    for (i, b) in random_pad.iter_mut().enumerate() {
        *b = ((i * 37 + 13) % 256) as u8;
    }

    let mut pad_file_alice = NamedTempFile::new().unwrap();
    pad_file_alice.write_all(&random_pad).unwrap();
    pad_file_alice.flush().unwrap();

    let mut pad_file_bob = NamedTempFile::new().unwrap();
    pad_file_bob.write_all(&random_pad).unwrap();
    pad_file_bob.flush().unwrap();

    let mut storage_alice = OtpStorage::open(
        pad_file_alice.path(),
        pad_len,
        20,
        0,
        pad_len,
        PadSide::SideA,
    )
    .unwrap();

    let mut storage_bob =
        OtpStorage::open(pad_file_bob.path(), pad_len, 20, 0, pad_len, PadSide::SideB).unwrap();

    // Alice sends OTP message (head towards tail)
    let secret_msg = b"TOP_SECRET_COORDINATES_55.75_37.61";
    let encrypted_otp = storage_alice.encrypt(secret_msg).unwrap();
    assert_eq!(encrypted_otp.offset, 0);

    // Bob receives and decrypts
    let decrypted_msg = storage_bob.decrypt(&encrypted_otp).unwrap();
    assert_eq!(decrypted_msg, secret_msg);

    // Bob replies from tail
    let reply_msg = b"ROGER_COORDINATES_RECEIVED_DESTROYING_PAD";
    let encrypted_reply = storage_bob.encrypt(reply_msg).unwrap();
    let decrypted_reply = storage_alice.decrypt(&encrypted_reply).unwrap();
    assert_eq!(decrypted_reply, reply_msg);

    println!("=== 5. MEDIA SANITIZATION, ZSTD COMPRESSION & SEALING ===");
    // Photo with dirty EXIF metadata
    let dirty_photo =
        b"\xFF\xD8\xFF\xE1\x00\x18Exif\x00\x00II*\x00GPS:LAT_LON_SERIAL_NUMBER\xFF\xD9";
    let media_key: [u8; KEY_LEN] = [0x99; KEY_LEN];

    let sealed_media = seal_media(&media_key, MediaType::Photo, dirty_photo).expect("Seal media");
    let (opened_type, sanitized_photo) = open_media(&media_key, &sealed_media).expect("Open media");

    assert_eq!(opened_type, MediaType::Photo);
    assert!(!sanitized_photo.windows(4).any(|w| w == b"Exif"));
    assert!(!sanitized_photo.windows(3).any(|w| w == b"GPS"));

    println!("=== 6. POST-QUANTUM KEM SESSION DERIVATION ===");
    let (kem_ct, alice_shared_secret) =
        kem::encapsulate(alice_prekey.public_key()).expect("KEM encapsulate");
    let bob_derived_secret = alice_prekey.decapsulate(&kem_ct).expect("KEM decapsulate");
    assert_eq!(*alice_shared_secret, *bob_derived_secret);

    println!("=== 7. MULTI-PROTOCOL VPN TRANSPORT ENCAPSULATION ===");
    let mut orchestrator = VpnOrchestrator::new(
        AmneziaWgConfig::default(),
        ShadowsocksConfig::default(),
        VlessRealityConfig::default(),
    )
    .expect("VPN orchestrator");

    // Profile Turbo -> AmneziaWG
    orchestrator.set_profile(CensorshipProfile::Turbo);
    assert_eq!(orchestrator.active_protocol(), VpnProtocol::AmneziaWg);
    let wrapped_wg = orchestrator
        .wrap_outgoing_packet(&sealed_media)
        .expect("Wrap WG");
    let unwrapped_wg = orchestrator
        .unwrap_incoming_packet(&wrapped_wg)
        .expect("Unwrap WG");
    assert_eq!(unwrapped_wg, sealed_media);

    // DPI Block simulation -> Auto Fallback to Shadowsocks 2022
    let fallback_proto = orchestrator.trigger_dpi_fallback();
    assert_eq!(fallback_proto, VpnProtocol::Shadowsocks2022);
    let wrapped_ss = orchestrator
        .wrap_outgoing_packet(&sealed_media)
        .expect("Wrap SS");
    let unwrapped_ss = orchestrator
        .unwrap_incoming_packet(&wrapped_ss)
        .expect("Unwrap SS");
    assert_eq!(unwrapped_ss, sealed_media);

    // Deep DPI / GFW Block simulation -> Fallback to VLESS-Reality TLS 1.3
    let stealth_proto = orchestrator.trigger_dpi_fallback();
    assert_eq!(stealth_proto, VpnProtocol::VlessReality);
    let wrapped_reality = orchestrator
        .wrap_outgoing_packet(&sealed_media)
        .expect("Wrap Reality");
    let unwrapped_reality = orchestrator
        .unwrap_incoming_packet(&wrapped_reality)
        .expect("Unwrap Reality");
    assert_eq!(unwrapped_reality, sealed_media);

    println!("=== 8. NETWORK DUAL-CHANNEL & ZERO-HARDCODED ENDPOINTS ===");
    use anongram_core::network::{NetworkManager, TransportChannel};
    let mut network = NetworkManager::new();
    // Configure dynamically from arbitrary string
    network
        .rotate_endpoint_from_str("130.162.254.32:8443")
        .expect("Rotate IP");
    assert_eq!(network.endpoint().unwrap().host, "130.162.254.32");
    assert_eq!(network.endpoint().unwrap().port, 8443);

    let udp_packet = network.send_packet(b"UDP_FAST_DATA").unwrap();
    assert_eq!(udp_packet.channel, TransportChannel::UdpDatagram);

    // DPI block UDP -> failover to TCP
    network.failover_to_tcp();
    let tcp_packet = network.send_packet(b"TCP_STREAM_DATA").unwrap();
    assert_eq!(tcp_packet.channel, TransportChannel::TcpStream);

    println!("=== 9. STEALTH PUSH NOTIFICATION & COMMAND DISPATCH ===");
    use anongram_core::push::{PushAction, PushProcessor};
    let mut push_proc = PushProcessor::new();
    let push_session_key: [u8; KEY_LEN] = [0x7A; KEY_LEN];

    // Push Action 1: Incoming message from Alice
    let push_msg = PushAction::IncomingMessage {
        sender_id: [0x01; 32],
        plaintext: b"Alice: Meet at safehouse B".to_vec(),
    };
    let packed_push = PushProcessor::pack_push_payload(&push_session_key, 1, &push_msg).unwrap();
    let unpacked_push = push_proc
        .unpack_push_payload(&push_session_key, &packed_push)
        .unwrap();
    match unpacked_push {
        PushAction::IncomingMessage { plaintext, .. } => {
            assert_eq!(plaintext, b"Alice: Meet at safehouse B");
        }
        _ => panic!("Expected incoming message"),
    }

    // Push Action 2: Silent command to rotate server endpoint
    let rotate_cmd = PushAction::RotateServerEndpoint {
        new_endpoint: "cloud.neongram.space:443/api/v2".to_string(),
    };
    let packed_cmd = PushProcessor::pack_push_payload(&push_session_key, 2, &rotate_cmd).unwrap();
    let unpacked_cmd = push_proc
        .unpack_push_payload(&push_session_key, &packed_cmd)
        .unwrap();
    if let PushAction::RotateServerEndpoint { new_endpoint } = unpacked_cmd {
        network.rotate_endpoint_from_str(&new_endpoint).unwrap();
        assert_eq!(network.endpoint().unwrap().host, "cloud.neongram.space");
        assert_eq!(network.endpoint().unwrap().path, "/api/v2");
    } else {
        panic!("Expected rotate endpoint command");
    }

    println!("=== 10. LOCAL SECURITY & TRUSTED DEVICE MANAGEMENT ===");
    use anongram_core::security::{BiometricAuth, BiometricType, DeviceManager, PasswordKdf};
    let salt = [0x55u8; 16];
    let db_pass_key =
        PasswordKdf::derive_key("UserSecureMasterPassword!", &salt).expect("KDF derive");
    let bio_challenge = [0x33u8; 32];
    let mut enclave_key =
        BiometricAuth::release_enclave_key(BiometricType::Fingerprint, &bio_challenge)
            .expect("Biometric release");
    assert_ne!(enclave_key, [0u8; KEY_LEN]);
    BiometricAuth::purge_key(&mut enclave_key);

    let mut dev_mgr = DeviceManager::new();
    let dev_phone = [1u8; 16];
    dev_mgr.register_device(dev_phone, "Pixel 9", [0x11; 32], 1700000000);
    assert_eq!(dev_mgr.is_trusted(&dev_phone).unwrap(), true);

    println!("=== 11. ENCRYPTED DATABASE & PANIC WIPE ===");
    use anongram_core::storage::EncryptedStorage;
    let storage_file = NamedTempFile::new().unwrap();
    let storage_path = storage_file.path().to_path_buf();

    let mut db = EncryptedStorage::open(&storage_path, db_pass_key).expect("Open encrypted db");
    db.put("secret_contact", b"Agent 007").unwrap();
    assert_eq!(db.get("secret_contact").unwrap(), b"Agent 007");

    // Execute Panic Wipe
    db.panic_wipe().expect("Panic wipe");
    assert!(!storage_path.exists());

    println!("=== 12. CENTRAL CONTROL ORCHESTRATOR & PUSH CASCADE WIPE ===");
    use anongram_core::control::{CoreLifecycleState, KernelCommander};
    let test_db_file = NamedTempFile::new().unwrap();
    let test_db_path = test_db_file.path().to_path_buf();
    let test_db = EncryptedStorage::open(&test_db_path, [0x44; KEY_LEN]).unwrap();

    let mut test_pad_file = NamedTempFile::new().unwrap();
    test_pad_file.write_all(&[0xFF; 256]).unwrap();
    test_pad_file.flush().unwrap();
    let test_pad_path = test_pad_file.path().to_path_buf();

    let mut commander = KernelCommander::new(network, dev_mgr, Some(test_db));
    commander.register_pad_path(test_pad_path.clone());

    // Push triggers cascading panic wipe across ALL modules
    let commander_push_key: [u8; KEY_LEN] = [0x55; KEY_LEN];
    let wipe_push =
        PushProcessor::pack_push_payload(&commander_push_key, 99, &PushAction::PanicWipe).unwrap();
    let processed_action = commander
        .handle_encrypted_push(&commander_push_key, &wipe_push)
        .unwrap();
    assert_eq!(processed_action, PushAction::PanicWipe);

    assert_eq!(commander.state(), CoreLifecycleState::Purged);
    assert!(!test_db_path.exists(), "DB must be wiped by commander");
    assert!(!test_pad_path.exists(), "Pad must be wiped by commander");

    println!("=== 13. WEBRTC SFrame E2EE VOICE/VIDEO & DYNAMIC GROUP UPGRADE ===");
    use anongram_core::network::voice::{
        CallSession, CallTransportState, CallType, MediaType, SFrameEngine,
    };
    let call_id = [0x77u8; 16];
    let peer_bob = [0x02u8; 32];
    let peer_charlie = [0x03u8; 32];
    let call_shared_key = [0x33u8; KEY_LEN];

    // 1. Private 1-on-1 Call (P2P with STUN/TURN fallback)
    let mut call = CallSession::new_one_on_one(call_id, peer_bob, call_shared_key);
    assert_eq!(call.call_type, CallType::OneOnOne);
    assert_eq!(call.participant_count(), 2);

    let encrypted_audio = call
        .produce_outgoing_frame(MediaType::AudioOpus, b"Alice speaking to Bob: Secure Voice")
        .expect("Produce audio frame");
    let bob_heard = call
        .consume_incoming_frame(&peer_bob, &encrypted_audio)
        .expect("Decrypt audio frame");
    assert_eq!(bob_heard, b"Alice speaking to Bob: Secure Voice");

    // Verify WhatsApp-style controls & SAS emoji fingerprint
    let sas = call.get_sas_fingerprint();
    assert!(!sas.is_empty(), "SAS emoji fingerprint must be generated");
    assert!(call.toggle_mute(), "Mute toggle must return true");

    // 2. Dynamic Upgrade to Multi-Party Group Call
    let charlie_key = [0x44u8; KEY_LEN];
    call.add_participant(peer_charlie, 2, charlie_key);
    assert_eq!(call.call_type, CallType::GroupCall);
    assert_eq!(call.transport_state, CallTransportState::UpgradedToGroupSfu);
    assert_eq!(call.participant_count(), 3);

    // Charlie transmits video frame to the group
    let mut charlie_engine = SFrameEngine::new(2, charlie_key);
    let charlie_video = charlie_engine
        .seal_frame(MediaType::VideoH264, b"Charlie 1080p Video Keyframe")
        .expect("Seal video");
    let alice_saw = call
        .consume_incoming_frame(&peer_charlie, &charlie_video)
        .expect("Alice opens video");
    assert_eq!(alice_saw, b"Charlie 1080p Video Keyframe");

    println!("=== 14. HIGH-THROUGHPUT STREAMING ENCRYPTION (10 GB+ UNLIMITED FILE SCALE) ===");
    use anongram_core::crypto::StreamingAead;
    use std::io::Cursor;

    let large_stream_key = [0x88u8; KEY_LEN];
    // Simulate streaming 128 KB with 16 KB chunk boundaries
    let mock_large_payload = vec![0x37u8; 128 * 1024];

    let mut enc_stream_buf = Vec::new();
    let enc_bytes = StreamingAead::encrypt_stream(
        &large_stream_key,
        Cursor::new(&mock_large_payload),
        &mut enc_stream_buf,
        16 * 1024,
    )
    .expect("Streaming encryption must succeed with bounded RAM");
    assert_eq!(enc_bytes, (128 * 1024) as u64);

    let mut dec_stream_buf = Vec::new();
    let dec_bytes = StreamingAead::decrypt_stream(
        &large_stream_key,
        Cursor::new(&enc_stream_buf),
        &mut dec_stream_buf,
    )
    .expect("Streaming decryption must verify all chunk tags");
    assert_eq!(dec_bytes, (128 * 1024) as u64);
    assert_eq!(dec_stream_buf, mock_large_payload);

    println!("=== ALL MODULES + HIGH-SPEED STREAMING PIPELINE FULLY OPERATIONAL ===");
}
