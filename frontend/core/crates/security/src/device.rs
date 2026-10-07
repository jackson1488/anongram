//! Trusted Device Registry and Hardware Revocation.

use std::collections::HashMap;

use crate::error::SecurityError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DeviceInfo {
    pub device_id: [u8; 16],
    pub name: String,
    pub public_key: [u8; 32],
    pub registered_at: u64,
    pub is_revoked: bool,
}

pub struct DeviceManager {
    devices: HashMap<[u8; 16], DeviceInfo>,
}

impl Default for DeviceManager {
    fn default() -> Self {
        Self::new()
    }
}

impl DeviceManager {
    pub fn new() -> Self {
        Self {
            devices: HashMap::new(),
        }
    }

    /// Registers a newly authorized trusted device.
    pub fn register_device(
        &mut self,
        device_id: [u8; 16],
        name: &str,
        public_key: [u8; 32],
        timestamp: u64,
    ) {
        let info = DeviceInfo {
            device_id,
            name: name.to_string(),
            public_key,
            registered_at: timestamp,
            is_revoked: false,
        };
        self.devices.insert(device_id, info);
    }

    /// Checks if device is authorized and not revoked.
    pub fn is_trusted(&self, device_id: &[u8; 16]) -> Result<bool, SecurityError> {
        match self.devices.get(device_id) {
            Some(dev) => {
                if dev.is_revoked {
                    Err(SecurityError::DeviceRevoked)
                } else {
                    Ok(true)
                }
            }
            None => Err(SecurityError::DeviceUnauthorized),
        }
    }

    /// Revokes compromised device immediately.
    pub fn revoke_device(&mut self, device_id: &[u8; 16]) -> Result<(), SecurityError> {
        match self.devices.get_mut(device_id) {
            Some(dev) => {
                dev.is_revoked = true;
                Ok(())
            }
            None => Err(SecurityError::DeviceUnauthorized),
        }
    }

    pub fn list_devices(&self) -> Vec<&DeviceInfo> {
        self.devices.values().collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_device_registry_and_revocation() {
        let mut mgr = DeviceManager::new();
        let dev1_id = [1u8; 16];
        let dev2_id = [2u8; 16];

        mgr.register_device(dev1_id, "Alice Pixel 9 Pro", [0xAA; 32], 1700000000);
        mgr.register_device(dev2_id, "Alice Secondary Tablet", [0xBB; 32], 1700000100);

        assert_eq!(mgr.is_trusted(&dev1_id).unwrap(), true);
        assert_eq!(mgr.is_trusted(&dev2_id).unwrap(), true);

        // Revoke stolen tablet
        mgr.revoke_device(&dev2_id).unwrap();
        assert_eq!(mgr.is_trusted(&dev2_id).err(), Some(SecurityError::DeviceRevoked));

        // Unauthorized device
        let unknown = [99u8; 16];
        assert_eq!(mgr.is_trusted(&unknown).err(), Some(SecurityError::DeviceUnauthorized));
    }
}
