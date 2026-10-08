//! Kernel Master Commander and Cascading Emergency Purge Bus.
//!
//! Orchestrates interactions between all core subsystems:
//! - Directs incoming Push actions (IncomingMessage, RotateServerEndpoint, PanicWipe).
//! - Executes Cascading Emergency Wipe across RAM, Sockets, OTP Pads, and Encrypted DB.

use std::path::PathBuf;

use crypto::aead::KEY_LEN;
use network::NetworkManager;
use push::{PushAction, PushProcessor};
use security::DeviceManager;
use storage::EncryptedStorage;
use zeroize::Zeroize;

use crate::error::ControlError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CoreLifecycleState {
    Active,
    RotatingEndpoint,
    Purged,
}

pub struct KernelCommander {
    state: CoreLifecycleState,
    push_processor: PushProcessor,
    network_manager: NetworkManager,
    device_manager: DeviceManager,
    storage: Option<EncryptedStorage>,
    active_pad_paths: Vec<PathBuf>,
}

impl KernelCommander {
    pub fn new(
        network_manager: NetworkManager,
        device_manager: DeviceManager,
        storage: Option<EncryptedStorage>,
    ) -> Self {
        Self {
            state: CoreLifecycleState::Active,
            push_processor: PushProcessor::new(),
            network_manager,
            device_manager,
            storage,
            active_pad_paths: Vec::new(),
        }
    }

    pub fn state(&self) -> CoreLifecycleState {
        self.state
    }

    pub fn is_purged(&self) -> bool {
        self.state == CoreLifecycleState::Purged
    }

    pub fn register_pad_path(&mut self, path: PathBuf) {
        self.active_pad_paths.push(path);
    }

    pub fn network(&self) -> &NetworkManager {
        &self.network_manager
    }

    pub fn network_mut(&mut self) -> &mut NetworkManager {
        &mut self.network_manager
    }

    pub fn storage(&self) -> Option<&EncryptedStorage> {
        self.storage.as_ref()
    }

    pub fn storage_mut(&mut self) -> Option<&mut EncryptedStorage> {
        self.storage.as_mut()
    }

    /// Primary entrypoint: Processes an encrypted Push notification from OS background
    /// and dispatches orders to all appropriate subsystems.
    pub fn handle_encrypted_push(
        &mut self,
        session_key: &[u8; KEY_LEN],
        encrypted_blob: &[u8],
    ) -> Result<PushAction, ControlError> {
        if self.state == CoreLifecycleState::Purged {
            return Err(ControlError::Purged);
        }

        let action = self
            .push_processor
            .unpack_push_payload(session_key, encrypted_blob)?;

        match &action {
            PushAction::IncomingMessage {
                sender_id,
                plaintext,
            } => {
                // Store message into encrypted DB if open
                if let Some(storage) = self.storage.as_mut() {
                    let key = format!("msg_{}", hex_encode(sender_id));
                    let _ = storage.put(&key, plaintext);
                }
            }
            PushAction::RotateServerEndpoint { new_endpoint } => {
                self.state = CoreLifecycleState::RotatingEndpoint;
                self.network_manager
                    .rotate_endpoint_from_str(new_endpoint)?;
                self.state = CoreLifecycleState::Active;
            }
            PushAction::WakeAndSync => {
                // Background worker signaled
            }
            PushAction::PanicWipe => {
                // Execute cascading purge across all subsystems
                self.execute_cascading_purge()?;
            }
        }

        Ok(action)
    }

    /// Cascading Emergency Wipe protocol:
    /// 1. Disconnects and resets network sockets.
    /// 2. Overwrites all OTP pads on disk with zeros (fsync) and deletes them.
    /// 3. Executes Panic Wipe on local encrypted database (RAM + Disk zeroize).
    /// 4. Transitions core state into permanently Purged.
    pub fn execute_cascading_purge(&mut self) -> Result<(), ControlError> {
        // Step 1: Network shutdown
        self.network_manager = NetworkManager::new();

        // Step 2: Overwrite and wipe OTP pads on disk
        for pad_path in self.active_pad_paths.drain(..) {
            if pad_path.exists() {
                if let Ok(meta) = std::fs::metadata(&pad_path) {
                    let len = meta.len() as usize;
                    let mut zeros = vec![0u8; len];
                    if let Ok(mut f) = std::fs::OpenOptions::new().write(true).open(&pad_path) {
                        use std::io::Write;
                        let _ = f.write_all(&zeros);
                        let _ = f.sync_data();
                    }
                    zeros.zeroize();
                }
                let _ = std::fs::remove_file(&pad_path);
            }
        }

        // Step 3: Panic wipe database
        if let Some(mut storage) = self.storage.take() {
            let _ = storage.panic_wipe();
        }

        // Step 4: Lock down core state
        self.state = CoreLifecycleState::Purged;
        Ok(())
    }
}

fn hex_encode(bytes: &[u8]) -> String {
    bytes.iter().map(|b| format!("{:02x}", b)).collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use push::PushProcessor;
    use std::io::Write;
    use tempfile::NamedTempFile;

    const TEST_KEY: [u8; KEY_LEN] = [0x88; KEY_LEN];

    #[test]
    fn test_push_triggers_endpoint_rotation_in_network() {
        let net = NetworkManager::new();
        let dev = DeviceManager::new();
        let mut commander = KernelCommander::new(net, dev, None);

        // Pack rotate command in push
        let action = PushAction::RotateServerEndpoint {
            new_endpoint: "https://stealth-node.space:8443/ws".to_string(),
        };
        let packed = PushProcessor::pack_push_payload(&TEST_KEY, 1, &action).unwrap();

        // Commander processes push
        let result = commander.handle_encrypted_push(&TEST_KEY, &packed).unwrap();
        assert_eq!(result, action);

        // Verify network endpoint has been updated on the fly
        let ep = commander.network().endpoint().unwrap();
        assert_eq!(ep.host, "stealth-node.space");
        assert_eq!(ep.port, 8443);
        assert_eq!(ep.path, "/ws");
        assert_eq!(commander.state(), CoreLifecycleState::Active);
    }

    #[test]
    fn test_push_triggers_cascading_panic_wipe() {
        let net = NetworkManager::new();
        let dev = DeviceManager::new();

        // Prepare test DB
        let db_file = NamedTempFile::new().unwrap();
        let db_path = db_file.path().to_path_buf();
        let storage = EncryptedStorage::open(&db_path, [0x11; KEY_LEN]).unwrap();

        // Prepare test OTP pad
        let mut pad_file = NamedTempFile::new().unwrap();
        pad_file.write_all(&vec![0xAA; 512]).unwrap();
        pad_file.flush().unwrap();
        let pad_path = pad_file.path().to_path_buf();

        let mut commander = KernelCommander::new(net, dev, Some(storage));
        commander.register_pad_path(pad_path.clone());

        assert!(db_path.exists());
        assert!(pad_path.exists());

        // Send PanicWipe via Push
        let panic_action = PushAction::PanicWipe;
        let packed = PushProcessor::pack_push_payload(&TEST_KEY, 10, &panic_action).unwrap();

        let res = commander.handle_encrypted_push(&TEST_KEY, &packed).unwrap();
        assert_eq!(res, PushAction::PanicWipe);

        // Verify cascading destruction:
        assert!(commander.is_purged());
        assert!(!db_path.exists(), "DB file must be wiped and deleted");
        assert!(!pad_path.exists(), "OTP pad file must be wiped and deleted");

        // Subsequent pushes must be blocked
        assert!(commander.handle_encrypted_push(&TEST_KEY, &packed).is_err());
    }
}
