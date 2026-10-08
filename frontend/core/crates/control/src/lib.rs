//! AnonGram Kernel Control & Orchestration Subsystem.
//!
//! Provides the central `KernelCommander` connecting `push`, `network`, `storage`, and `security`.

pub mod commander;
pub mod error;

pub use commander::{CoreLifecycleState, KernelCommander};
pub use error::ControlError;
