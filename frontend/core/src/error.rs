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
}
