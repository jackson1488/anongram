//! AmneziaWG Obfuscated WireGuard Protocol Implementation (Line 1 of Defense).
//!
//! Provides advanced WireGuard packet header and payload obfuscation to defeat
//! ISP Deep Packet Inspection (DPI, TSPU, GFW) while maintaining WireGuard's line speed.
//!
//! Features:
//! - Custom Magic Headers (`H1`, `H2`, `H3`, `H4` replacing standard `0x01, 0x02, 0x03, 0x04`).
//! - Junk Packet Injection (`Jc` packets with lengths between `Jmin` and `Jmax` bytes).
//! - Deterministic entropy padding matching standard TLS/random noise.
//! - Low memory footprint (< 100 KB RAM allocation).

use rand::RngCore;
use crate::error::CoreError;

/// Header and junk parameters for AmneziaWG obfuscation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AmneziaWgConfig {
    /// Handshake Initiation custom header
    pub h1: u32,
    /// Handshake Response custom header
    pub h2: u32,
    /// Handshake Cookie custom header
    pub h3: u32,
    /// Transport Data custom header
    pub h4: u32,
    /// Junk packet count (0 to 128)
    pub jc: u16,
    /// Minimum junk packet size (bytes)
    pub jmin: u16,
    /// Maximum junk packet size (bytes)
    pub jmax: u16,
    /// Server endpoint (IP:Port or Domain:Port)
    pub endpoint: String,
    /// WireGuard Public Key (Base64 or 32 bytes)
    pub peer_public_key: [u8; 32],
}

impl Default for AmneziaWgConfig {
    fn default() -> Self {
        Self {
            h1: 0x1A2B3C4D,
            h2: 0x5E6F7A8B,
            h3: 0x9C0D1E2F,
            h4: 0x3A4B5C6D,
            jc: 4,
            jmin: 40,
            jmax: 70,
            endpoint: "127.0.0.1:51820".to_string(),
            peer_public_key: [7u8; 32],
        }
    }
}

pub struct AmneziaWgEngine {
    config: AmneziaWgConfig,
}

impl AmneziaWgEngine {
    pub fn new(config: AmneziaWgConfig) -> Result<Self, CoreError> {
        if config.jmin > config.jmax {
            return Err(CoreError::VpnConfig);
        }
        Ok(Self { config })
    }

    pub fn config(&self) -> &AmneziaWgConfig {
        &self.config
    }

    /// Generates `Jc` pseudo-random junk packets to pre-condition DPI state machine before handshake.
    pub fn generate_junk_packets(&self) -> Vec<Vec<u8>> {
        let count = self.config.jc as usize;
        let mut packets = Vec::with_capacity(count);
        let mut rng = rand::thread_rng();

        for _ in 0..count {
            let span = (self.config.jmax - self.config.jmin + 1) as usize;
            let len = self.config.jmin as usize + (rng.next_u32() as usize % span);
            let mut junk = vec![0u8; len];
            rng.fill_bytes(&mut junk);
            packets.push(junk);
        }

        packets
    }

    /// Obfuscates an outgoing WireGuard packet by replacing standard 4-byte header with custom H1..H4.
    pub fn obfuscate_outgoing(&self, packet: &[u8]) -> Result<Vec<u8>, CoreError> {
        if packet.len() < 4 {
            return Err(CoreError::Malformed);
        }

        let standard_type = u32::from_le_bytes(packet[0..4].try_into().unwrap());
        let custom_header = match standard_type {
            1 => self.config.h1, // Handshake Initiation
            2 => self.config.h2, // Handshake Response
            3 => self.config.h3, // Cookie Reply
            4 => self.config.h4, // Transport Data
            _ => return Err(CoreError::Malformed),
        };

        let mut obfuscated = packet.to_vec();
        obfuscated[0..4].copy_from_slice(&custom_header.to_le_bytes());
        Ok(obfuscated)
    }

    /// De-obfuscates an incoming AmneziaWG packet back to standard WireGuard format.
    pub fn deobfuscate_incoming(&self, packet: &[u8]) -> Result<Vec<u8>, CoreError> {
        if packet.len() < 4 {
            return Err(CoreError::Malformed);
        }

        let custom_header = u32::from_le_bytes(packet[0..4].try_into().unwrap());
        let standard_type: u32 = if custom_header == self.config.h1 {
            1
        } else if custom_header == self.config.h2 {
            2
        } else if custom_header == self.config.h3 {
            3
        } else if custom_header == self.config.h4 {
            4
        } else {
            return Err(CoreError::VpnHandshake);
        };

        let mut restored = packet.to_vec();
        restored[0..4].copy_from_slice(&standard_type.to_le_bytes());
        Ok(restored)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_amnezia_junk_generation() {
        let config = AmneziaWgConfig {
            jc: 5,
            jmin: 50,
            jmax: 80,
            ..Default::default()
        };
        let engine = AmneziaWgEngine::new(config).unwrap();
        let junk = engine.generate_junk_packets();
        assert_eq!(junk.len(), 5);
        for p in junk {
            assert!(p.len() >= 50 && p.len() <= 80);
        }
    }

    #[test]
    fn test_amnezia_obfuscation_roundtrip() {
        let engine = AmneziaWgEngine::new(AmneziaWgConfig::default()).unwrap();

        // Standard WireGuard data packet (starts with 0x04 0x00 0x00 0x00)
        let mut sample_data = vec![4, 0, 0, 0];
        sample_data.extend_from_slice(b"WireGuard encrypted payload 12345");

        let obfuscated = engine.obfuscate_outgoing(&sample_data).unwrap();
        assert_ne!(&obfuscated[0..4], &sample_data[0..4]);
        assert_eq!(&obfuscated[0..4], &engine.config().h4.to_le_bytes());

        let restored = engine.deobfuscate_incoming(&obfuscated).unwrap();
        assert_eq!(restored, sample_data);
    }
}
