use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum VpnError {
    #[error("vpn configuration error")]
    Config,
    #[error("vpn handshake failed")]
    Handshake,
    #[error("vpn timeout")]
    Timeout,
    #[error("vpn disconnected")]
    Disconnected,
    #[error("malformed packet")]
    Malformed,
    #[error("crypto error")]
    Crypto,
}
