//! AnonGram Modular Push Subsystem.
//!
//! Provides background push notification decryption and silent command execution.

pub mod error;
pub mod multi_inbox;
pub mod payload;

pub use error::PushError;
pub use multi_inbox::{
    ChatNotificationSummary, MultiInboxNotificationState, NotificationMessageItem,
};
pub use payload::{PushAction, PushProcessor};
