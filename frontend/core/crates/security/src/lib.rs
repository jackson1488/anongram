//! AnonGram Modular Security Subsystem.
//!
//! Provides master password KDF, biometrics bridge, and trusted device management.

pub mod biometrics;
pub mod device;
pub mod error;
pub mod kdf;

pub use biometrics::{BiometricAuth, BiometricType};
pub use device::{DeviceInfo, DeviceManager};
pub use error::SecurityError;
pub use kdf::PasswordKdf;
