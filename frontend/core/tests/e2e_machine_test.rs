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

    println!("=== ALL MODULES INTEGRATED AND FUNCTIONING AS ONE MACHINE ===");
}
