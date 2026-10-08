//! Fully Modular Brute-Force Defense Policy.
//!
//! Freedom of configuration:
//! - Toggle ON / OFF completely.
//! - Configurable attempt threshold (e.g. 3, 5, 10, 20).
//! - Configurable cooldown lockouts (seconds).
//! - Optional auto-purge on attempt exhaustion.

use crate::error::SecurityError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct BruteForcePolicy {
    pub enabled: bool,
    pub max_attempts: u32,
    pub failed_attempts: u32,
    pub lockout_duration_secs: u64,
    pub lockout_until_timestamp: u64,
    pub auto_wipe_on_exhaust: bool,
}

impl Default for BruteForcePolicy {
    fn default() -> Self {
        Self {
            enabled: true,
            max_attempts: 10,
            failed_attempts: 0,
            lockout_duration_secs: 60,
            lockout_until_timestamp: 0,
            auto_wipe_on_exhaust: false, // Default is safe: no accidental auto-wipe unless user explicitly enables
        }
    }
}

impl BruteForcePolicy {
    /// Completely disables brute force restriction (total user freedom).
    pub fn disable(&mut self) {
        self.enabled = false;
        self.failed_attempts = 0;
        self.lockout_until_timestamp = 0;
    }

    /// Configures custom attempt threshold and auto-wipe behavior.
    pub fn configure(&mut self, max_attempts: u32, lockout_secs: u64, auto_wipe: bool) {
        self.enabled = true;
        self.max_attempts = max_attempts;
        self.lockout_duration_secs = lockout_secs;
        self.auto_wipe_on_exhaust = auto_wipe;
    }

    /// Checks if current login attempt is permitted under policy.
    pub fn can_attempt(&self, current_timestamp: u64) -> Result<(), SecurityError> {
        if !self.enabled {
            return Ok(());
        }

        if current_timestamp < self.lockout_until_timestamp {
            return Err(SecurityError::AuthFailed);
        }

        Ok(())
    }

    /// Registers a failed login attempt. Returns true if auto-wipe threshold is reached.
    pub fn record_failure(&mut self, current_timestamp: u64) -> bool {
        if !self.enabled {
            return false;
        }

        self.failed_attempts += 1;

        if self.failed_attempts >= self.max_attempts {
            if self.auto_wipe_on_exhaust {
                return true; // Trigger wipe
            } else {
                // Lockout
                self.lockout_until_timestamp = current_timestamp + self.lockout_duration_secs;
            }
        }

        false
    }

    /// Resets failure counter upon successful unlock.
    pub fn record_success(&mut self) {
        self.failed_attempts = 0;
        self.lockout_until_timestamp = 0;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_brute_force_configurable_freedom() {
        let mut policy = BruteForcePolicy::default();
        policy.configure(3, 300, true);

        assert!(policy.can_attempt(1000).is_ok());

        assert!(!policy.record_failure(1000)); // Attempt 1
        assert!(!policy.record_failure(1001)); // Attempt 2
        let triggers_wipe = policy.record_failure(1002); // Attempt 3 -> triggers wipe!
        assert!(triggers_wipe);

        // Turn OFF brute force defense completely
        policy.disable();
        assert!(!policy.record_failure(2000));
        assert!(policy.can_attempt(2000).is_ok());
    }
}
