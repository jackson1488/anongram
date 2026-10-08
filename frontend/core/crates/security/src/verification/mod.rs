//! Modular Anti-MITM Verification Subsystem.
//!
//! Provides 4 completely independent, configurable verification methods:
//! 1. `fingerprint`: 60-digit canonical numeric safety numbers (12 blocks of 5 digits).
//! 2. `qr_scanner`: In-person device-to-device QR payload generator and scanner.
//! 3. `key_guard`: Safety number change detector blocking man-in-the-middle key swaps.
//! 4. `server_witness`: Server Key Transparency background consistency auditor.

pub mod fingerprint;
pub mod key_guard;
pub mod qr_scanner;
pub mod server_witness;

pub use fingerprint::NumericFingerprint;
pub use key_guard::{KeyChangeGuard, KeyChangePolicy, KeyCheckResult, VerifiedContactRecord};
pub use qr_scanner::{QrSafetyScanner, QrVerificationResult};
pub use server_witness::{ServerTransparencyWitness, TransparencyRecord};
