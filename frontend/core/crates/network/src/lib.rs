//! AnonGram Modular Network Transport Crate.
//!
//! Provides zero-hardcoded dynamic server endpoint management and dual-channel TCP/UDP transport.

pub mod endpoint;
pub mod error;
pub mod transport;

pub use endpoint::{ProtocolScheme, ServerEndpoint};
pub use error::NetworkError;
pub use transport::{NetworkManager, NetworkPacket, TransportChannel};
