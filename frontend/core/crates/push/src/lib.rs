//! AnonGram Modular Push Subsystem.
//!
//! Provides background push notification decryption and silent command execution.

pub mod error;
pub mod payload;

pub use error::PushError;
pub use payload::{PushAction, PushProcessor};
