//! Anti-Debugging, Memory Inspection, and Process Integrity Detector.
//!
//! Provides paranoid runtime protection against dynamic analysis, Frida hooks,
//! ptrace debugger attachments, and LD_PRELOAD injections.
//!
//! Completely configurable: user has 100% freedom to toggle ON / OFF.

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TamperConfig {
    pub enabled: bool,
    pub block_on_debugger: bool,
    pub block_on_injection: bool,
}

impl Default for TamperConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            block_on_debugger: true,
            block_on_injection: true,
        }
    }
}

pub struct TamperDetector;

impl TamperDetector {
    /// Inspects system environment and returns true if active tampering or debugger is present.
    pub fn is_tampered(config: &TamperConfig) -> bool {
        if !config.enabled {
            return false;
        }

        // 1. Check TracerPid (Linux / Android ptrace detector)
        if config.block_on_debugger && Self::check_tracer_pid() {
            return true;
        }

        // 2. Check Frida / Substrate injection environment variables
        if config.block_on_injection && Self::check_injection_env() {
            return true;
        }

        false
    }

    fn check_tracer_pid() -> bool {
        if let Ok(status) = std::fs::read_to_string("/proc/self/status") {
            for line in status.lines() {
                if line.starts_with("TracerPid:") {
                    let parts: Vec<&str> = line.split_whitespace().collect();
                    if parts.len() >= 2 {
                        if let Ok(pid) = parts[1].parse::<u32>() {
                            if pid > 0 {
                                return true; // Debugger attached!
                            }
                        }
                    }
                }
            }
        }
        false
    }

    fn check_injection_env() -> bool {
        let suspicious_vars = ["FRIDA_SERVER", "XPOSED_BRIDGE", "LD_PRELOAD"];
        for var in &suspicious_vars {
            if let Ok(val) = std::env::var(var) {
                if !val.is_empty() {
                    return true;
                }
            }
        }
        false
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_tamper_config_freedom() {
        let mut config = TamperConfig::default();
        assert!(config.enabled);

        // Turn OFF detector completely
        config.enabled = false;
        assert!(!TamperDetector::is_tampered(&config));
    }
}
