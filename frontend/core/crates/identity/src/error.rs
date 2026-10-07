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

impl From<crypto::CryptoError> for IdentityError {
    fn from(err: crypto::CryptoError) -> Self {
        match err {
            crypto::CryptoError::Signature => IdentityError::Signature,
            crypto::CryptoError::Malformed => IdentityError::Malformed,
            crypto::CryptoError::Decrypt => IdentityError::Decrypt,
            _ => IdentityError::Crypto(err.to_string()),
        }
    }
}
