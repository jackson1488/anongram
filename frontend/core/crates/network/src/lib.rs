//! AnonGram Modular Network Transport Crate.
//!
//! Provides zero-hardcoded dynamic server endpoint management and dual-channel TCP/UDP transport.

pub mod endpoint;
pub mod error;
pub mod socket;
pub mod transport;

pub use endpoint::{ProtocolScheme, ServerEndpoint};
pub use error::NetworkError;
pub use socket::{AsyncTcpClient, AsyncUdpClient, HeartbeatKeeper};
pub use transport::{NetworkManager, NetworkPacket, TransportChannel};
