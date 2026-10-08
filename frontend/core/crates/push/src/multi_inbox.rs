//! Multi-Inbox 5-List Notification Descriptor and Grouping Aggregator.
//!
//! Enforces:
//! 1. Exactly up to 5 conversation lists per single notification.
//! 2. Overflow badge counter (+1, +2) when > 5 chats are active.
//! 3. Preview text for each list, optional media thumbnails (photos/gifs).
//! 4. Individual message history per list for standalone expansion.

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NotificationMessageItem {
    pub message_id: u64,
    pub sender_id: [u8; 32],
    pub sender_name: String,
    pub text: String,
    pub timestamp: u64,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ChatNotificationSummary {
    pub chat_id: [u8; 32],
    pub title: String,
    pub is_group: bool,
    pub unread_count: u32,
    pub last_message_preview: String,
    pub full_messages: Vec<NotificationMessageItem>,
    pub media_thumbnail: Option<Vec<u8>>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct MultiInboxNotificationState {
    pub total_unread_count: u32,
    pub total_conversations_count: u32,
    /// Exactly up to 5 conversation lists displayed inside one notification
    pub displayed_chats: Vec<ChatNotificationSummary>,
    /// Overflow indicator: +1, +2 if total_conversations_count > 5
    pub overflow_count: u32,
}

impl MultiInboxNotificationState {
    pub const MAX_DISPLAYED_CHATS: usize = 5;

    pub fn new() -> Self {
        Self {
            total_unread_count: 0,
            total_conversations_count: 0,
            displayed_chats: Vec::with_capacity(Self::MAX_DISPLAYED_CHATS),
            overflow_count: 0,
        }
    }

    /// Aggregates a list of all active chats into the strict 5-list notification format.
    pub fn from_chats(mut chats: Vec<ChatNotificationSummary>) -> Self {
        let total_conversations_count = chats.len() as u32;
        let mut total_unread_count = 0;
        for c in &chats {
            total_unread_count += c.unread_count;
        }

        let overflow_count = if chats.len() > Self::MAX_DISPLAYED_CHATS {
            (chats.len() - Self::MAX_DISPLAYED_CHATS) as u32
        } else {
            0
        };

        if chats.len() > Self::MAX_DISPLAYED_CHATS {
            chats.truncate(Self::MAX_DISPLAYED_CHATS);
        }

        Self {
            total_unread_count,
            total_conversations_count,
            displayed_chats: chats,
            overflow_count,
        }
    }
}

impl Default for MultiInboxNotificationState {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_multi_inbox_strictly_caps_at_5_lists_and_counts_overflow() {
        let mut test_chats = Vec::new();
        for i in 1..=8 {
            test_chats.push(ChatNotificationSummary {
                chat_id: [i as u8; 32],
                title: format!("Chat {}", i),
                is_group: i % 2 == 0,
                unread_count: i,
                last_message_preview: format!("Last message {}", i),
                full_messages: vec![NotificationMessageItem {
                    message_id: 100 + i as u64,
                    sender_id: [i as u8; 32],
                    sender_name: format!("User {}", i),
                    text: format!("Full message text {}", i),
                    timestamp: 1700000000 + i as u64,
                }],
                media_thumbnail: if i == 2 { Some(vec![0xFF; 64]) } else { None },
            });
        }

        let state = MultiInboxNotificationState::from_chats(test_chats);

        // Must display exactly 5 lists
        assert_eq!(state.displayed_chats.len(), 5);
        // Overflow badge (+3 chats waiting in app)
        assert_eq!(state.overflow_count, 3);
        assert_eq!(state.total_conversations_count, 8);
        // Total unread: 1+2+3+4+5+6+7+8 = 36
        assert_eq!(state.total_unread_count, 36);

        // Chat 2 must preserve media thumbnail
        assert!(state.displayed_chats[1].media_thumbnail.is_some());
    }
}
