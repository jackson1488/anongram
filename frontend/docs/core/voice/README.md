# Real-Time Voice & Video Calling Engine (WebRTC + SFrame E2EE)

## Architecture Overview

The Anongram Voice & Video engine (`anongram-network::voice`) provides military-grade real-time audio and video communication designed specifically for adversarial network environments and high-censorship regimes.

```
+-------------------------------------------------------------------------------+
|                       1-on-1 P2P vs. Multi-Party Group                        |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [ Alice ] <====== Direct P2P (Host / STUN / TURN UDP) ======> [ Bob ]        |
|     |                                                             |           |
|     |               DYNAMIC UPGRADE TO GROUP CALL                 |           |
|     v                                                             v           |
|  [ Alice ] ------------\                             /--------- [ Bob ]       |
|   (SFrame)              \                           /          (SFrame)       |
|                          v                         v                          |
|                     [ Blind SFU Relay (Zero Decryption) ]                     |
|                                     ^                                         |
|                                     |                                         |
|                                  [ Charlie ]                                  |
|                                   (SFrame)                                    |
+-------------------------------------------------------------------------------+
```

---

## 1. NAT Traversal & Censorship Resistance: 3-Tier ICE Fallback

State-level firewalls and commercial ISPs frequently block UDP traffic or inspect STUN binding requests. Anongram implements a 3-tier ICE fallback mechanism in `IceConfiguration`:

1. **Tier 1 (Host P2P & STUN):** Direct UDP host connection or standard RFC 8489 STUN NAT mapping.
2. **Tier 2 (TURN over UDP):** Fallback relay using UDP TURN when symmetric NATs prevent direct binding.
3. **Tier 3 (TURNS over TLS Port 443):** Indistinguishable from standard HTTPS traffic (`turns://cloud.neongram.space:443?transport=tcp`). Bypasses restrictive Deep Packet Inspection (DPI) and enterprise firewalls where only TCP port 443 is permitted.

Configurable policies:
- `IceTransportPolicy::All`: Probes Host $\to$ STUN $\to$ Relay.
- `IceTransportPolicy::RelayOnly`: Privacy-first mode; never reveals client IP address to the peer, forcing all traffic through the relay.

---

## 2. End-to-End Encryption: SFrame (RFC 9605)

Standard WebRTC DTLS-SRTP terminates at the Selective Forwarding Unit (SFU) in group calls, allowing server operators to potentially record or wiretap conversations.

Anongram solves this using **SFrame (RFC 9605)**:
- **Audio & Video Frames Sealed at Sender:** Audio (Opus) and Video (VP8/H.264) payloads are encrypted before WebRTC packetization using `XChaCha20-Poly1305`.
- **Authenticated Additional Data (AAD):** SFrame header contains:
  - `key_id`: Identifies the sender's ratchet key.
  - `frame_seq`: 64-bit frame counter preventing replay attacks.
  - `media_type`: 0x01 (Audio Opus), 0x02 (Video VP8), 0x03 (Video H.264).
- **Zero SFU Decryption:** The SFU relay server only reads the unencrypted SFrame header for packet forwarding. It cannot decrypt the media payload.

---

## 3. Dynamic Mid-Call Upgrade (1-on-1 $\to$ Group)

Users can start a private 1-on-1 call and invite additional participants on-the-fly without hanging up:

1. Session starts as `CallType::OneOnOne` using direct P2P.
2. When a participant is added via `call_session.add_participant(peer_id, key_id, sframe_key)`:
   - Signaling emits `CallSignal::UpgradeToGroup { room_id, new_participants }`.
   - Call state transitions to `CallTransportState::UpgradedToGroupSfu`.
   - Each participant registers individual SFrame ratchet keys.
   - Incoming frames from any participant are verified and decrypted seamlessly.

---

## 4. Ultra-Low RAM Footprint (< 9 MB Target)

The voice engine operates strictly within the core < 9 MB RAM budget:
- Zero dynamic memory bloat during media streaming.
- Frame buffers reuse pre-allocated slices without large vector clones.
- Cryptographic state is bounded per participant.
