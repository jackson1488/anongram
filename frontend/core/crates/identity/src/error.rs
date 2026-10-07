use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum IdentityError {
    #[error("crypto error: {0}")]
    Crypto(String),
    #[error("signature error")]
    Signature,
    #[error("malformed identity blob or phrase")]
    Malformed,
    #[error("decryption failed")]
    Decrypt,
}
