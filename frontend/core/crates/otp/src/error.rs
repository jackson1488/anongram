use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum OtpError {
    #[error("one-time pad exhausted or collision with reserved bounds")]
    PadExhausted,
    #[error("operation out of pad bounds or invalid offset")]
    OutOfBounds,
    #[error("authentication or verification failed")]
    AuthFailed,
    #[error("malformed buffer or input")]
    Malformed,
    #[error("io error")]
    Io,
}
