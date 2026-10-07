use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum StorageError {
    #[error("Database encryption or decryption failed")]
    CipherError,
    #[error("Key not loaded in memory")]
    KeyNotLoaded,
    #[error("Record not found: {0}")]
    NotFound(String),
    #[error("IO error: {0}")]
    Io(String),
}

impl From<std::io::Error> for StorageError {
    fn from(err: std::io::Error) -> Self {
        StorageError::Io(err.to_string())
    }
}
