use thiserror::Error;

#[derive(Debug, Error)]
pub enum ControlError {
    #[error("Push error: {0}")]
    Push(#[from] push::PushError),
    #[error("Network error: {0}")]
    Network(#[from] network::NetworkError),
    #[error("Storage error: {0}")]
    Storage(#[from] storage::StorageError),
    #[error("Security error: {0}")]
    Security(#[from] security::SecurityError),
    #[error("Core has been purged")]
    Purged,
}
