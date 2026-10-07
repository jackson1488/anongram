use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum PushError {
    #[error("Authentication failed: invalid MAC or signature")]
    AuthFailed,
    #[error("Replay attack detected: stale timestamp or nonce")]
    ReplayDetected,
    #[error("Malformed push payload")]
    Malformed,
    #[error("Crypto error")]
    Crypto,
    #[error("Network error: {0}")]
    Network(#[from] network::NetworkError),
}
