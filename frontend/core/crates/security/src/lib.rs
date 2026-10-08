//! AnonGram Modular Security Subsystem.
//!
//! Provides master password KDF, biometrics bridge, and trusted device management.

pub mod biometrics;
pub mod brute_force;
pub mod credentials;
pub mod device;
pub mod duress;
pub mod error;
pub mod kdf;
pub mod tamper;
pub mod verification;

pub use biometrics::{BiometricAuth, BiometricType};
pub use brute_force::BruteForcePolicy;
pub use credentials::{AuthCredential, CredentialProcessor};
pub use device::{DeviceInfo, DeviceManager};
pub use duress::{DuressAction, DuressConfig};
pub use error::SecurityError;
pub use kdf::PasswordKdf;
pub use tamper::{TamperConfig, TamperDetector};
pub use verification::{
    KeyChangeGuard, KeyChangePolicy, KeyCheckResult, NumericFingerprint, QrSafetyScanner,
    QrVerificationResult, ServerTransparencyWitness, TransparencyRecord, VerifiedContactRecord,
};

