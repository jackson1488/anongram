//! AnonGram Modular Storage Subsystem.
//!
//! Provides encrypted local database storage with panic wipe.

pub mod error;
pub mod store;

pub use error::StorageError;
pub use store::EncryptedStorage;
