use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum CryptoError {
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
    #[error("io error: {0}")]
    Io(String),
}
