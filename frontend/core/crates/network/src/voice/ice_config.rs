//! WebRTC STUN/TURN/ICE Configuration with Anti-Censorship Fallbacks.
//!
//! Provides 3-tier ICE fallback:
//! 1. Host P2P (direct device-to-device).
//! 2. STUN NAT Hole Punching.
//! 3. TURN Relay on TLS Port 443 (TURNS) for strict DPI/censorship environments.

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum IceTransportPolicy {
    /// Try direct P2P first, then STUN, then TURN
    All,
    /// Force TURN relay only (for maximum privacy / hiding client IP)
    RelayOnly,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct IceServer {
    pub urls: Vec<String>,
    pub username: Option<String>,
    pub credential: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct IceConfiguration {
    pub ice_servers: Vec<IceServer>,
    pub transport_policy: IceTransportPolicy,
}

impl Default for IceConfiguration {
    fn default() -> Self {
        Self {
            ice_servers: vec![
                // Tier 1: Standard public STUN for NAT discovery
                IceServer {
                    urls: vec!["stun:stun.l.google.com:19302".to_string()],
                    username: None,
                    credential: None,
                },
                // Tier 2: Dedicated secure TURN relay on HTTPS Port 443 (bypasses ISP UDP blocks)
                IceServer {
                    urls: vec![
                        "turn:cloud.neongram.space:3478?transport=udp".to_string(),
                        "turns:cloud.neongram.space:443?transport=tcp".to_string(),
                    ],
                    username: Some("anongram-guest".to_string()),
                    credential: Some("e2ee-session-token".to_string()),
                },
            ],
            transport_policy: IceTransportPolicy::All,
        }
    }
}

impl IceConfiguration {
    /// Forces all traffic exclusively through encrypted TURN relays to prevent IP address exposure.
    pub fn privacy_relay_only() -> Self {
        let mut config = Self::default();
        config.transport_policy = IceTransportPolicy::RelayOnly;
        config
    }
}
