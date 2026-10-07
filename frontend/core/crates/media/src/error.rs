use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum MediaError {
    #[error("media encryption failed")]
    Encrypt,
    #[error("media decryption failed")]
    Decrypt,
    #[error("malformed media container")]
    Malformed,
    #[error("compression error")]
    Compression,
    #[error("decompression error")]
    Decompression,
}
