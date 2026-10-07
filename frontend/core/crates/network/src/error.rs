use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum NetworkError {
    #[error("Invalid endpoint format: {0}")]
    InvalidEndpoint(String),
    #[error("Network endpoint not configured")]
    Unconfigured,
    #[error("Socket connection failed")]
    ConnectionFailed,
    #[error("Packet transmission error: {0}")]
    TransmissionError(String),
    #[error("Malformed network frame")]
    MalformedFrame,
}
