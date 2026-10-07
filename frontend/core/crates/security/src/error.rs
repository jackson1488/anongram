use thiserror::Error;

#[derive(Debug, Error, PartialEq, Eq)]
pub enum SecurityError {
    #[error("Authentication failed: invalid password or biometric challenge")]
    AuthFailed,
    #[error("Device not authorized in passport")]
    DeviceUnauthorized,
    #[error("Device is revoked")]
    DeviceRevoked,
    #[error("Key derivation failed")]
    KdfFailed,
    #[error("Hardware enclave error")]
    EnclaveError,
    #[error("Identity error: {0}")]
    Identity(#[from] identity::IdentityError),
}
