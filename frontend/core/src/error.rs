use thiserror::Error;

/// Errors returned by the core. Messages never contain secret material.
#[derive(Debug, Error, PartialEq, Eq)]
pub enum CoreError {
    #[error("encryption failed")]
    Encrypt,
    #[error("decryption or authentication failed")]
    Decrypt,
    #[error("malformed input")]
    Malformed,
    #[error("key encapsulation failed")]
    Kem,
    #[error("signature verification failed")]
    Signature,
    #[error("one-time pad exhausted or collision with reserved bounds")]
    PadExhausted,
    #[error("operation out of pad bounds or invalid offset")]
    OutOfBounds,
    #[error("io error")]
    Io,
    #[error("compression error")]
    Compression,
    #[error("decompression error")]
    Decompression,
    #[error("vpn configuration error")]
    VpnConfig,
    #[error("vpn handshake failed")]
    VpnHandshake,
    #[error("vpn timeout")]
    VpnTimeout,
    #[error("vpn disconnected")]
    VpnDisconnected,
}
