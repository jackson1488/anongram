//! Duress Policy & Decoy Vault Configuration.
//!
//! Provides 100% modular user freedom:
//! - Enable/disable Duress system entirely.
//! - Choice of 3 actions:
//!   1. DecoyVault (shows fake harmless profile, zaps real keys in RAM).
//!   2. SilentSosServerWipe (alerts server, purges user data and triggers friend push wipes).
//!   3. ScorchedEarth (total local + remote annihilation).

use crypto::aead::KEY_LEN;
use zeroize::Zeroize;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DuressAction {
    /// Option 1: Opens fake innocent account, real keys zeroized in RAM
    DecoyVault,
    /// Option 2: Sends emergency silent SOS packet to server to wipe account & friend history
    SilentSosServerWipe,
    /// Option 3: Full local disk annihilation + remote server wipe
    ScorchedEarth,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DuressConfig {
    pub enabled: bool,
    pub duress_pin_hash: Option<[u8; KEY_LEN]>,
    pub action: DuressAction,
    pub server_sos_payload: Vec<u8>,
}

impl Default for DuressConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            duress_pin_hash: None,
            action: DuressAction::DecoyVault,
            server_sos_payload: Vec::new(),
        }
    }
}

impl DuressConfig {
    pub fn verify_duress_pin(&self, input_hash: &[u8; KEY_LEN]) -> bool {
        if !self.enabled {
            return false;
        }
        if let Some(expected) = &self.duress_pin_hash {
            expected == input_hash
        } else {
            false
        }
    }

    pub fn set_duress_pin(&mut self, hash: [u8; KEY_LEN], action: DuressAction) {
        self.enabled = true;
        self.duress_pin_hash = Some(hash);
        self.action = action;
    }

    pub fn disable(&mut self) {
        self.enabled = false;
        if let Some(mut h) = self.duress_pin_hash.take() {
            h.zeroize();
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_duress_config_toggle_and_actions() {
        let mut cfg = DuressConfig::default();
        assert!(!cfg.enabled);

        let pin_hash = [0x77u8; KEY_LEN];
        cfg.set_duress_pin(pin_hash, DuressAction::ScorchedEarth);
        assert!(cfg.enabled);
        assert!(cfg.verify_duress_pin(&pin_hash));
        assert_eq!(cfg.action, DuressAction::ScorchedEarth);

        cfg.disable();
        assert!(!cfg.enabled);
        assert!(!cfg.verify_duress_pin(&pin_hash));
    }
}
