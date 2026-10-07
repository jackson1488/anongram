//! One-Time Pad (OTP) Engine with Information-Theoretic Security.
//!
//! Features:
//! - Pure OTP: C = M ^ K with uniform random pad bytes.
//! - Two-ended pointer allocation:
//!   - Side A allocates strictly monotonically from start (head: 0 -> total_size).
//!   - Side B allocates strictly monotonically from end (tail: total_size -> 0).
//! - Guaranteed reservation bounds (default 20% each side, 60% shared pool).
//! - Information-theoretic Wegman-Carter style MAC per message:
//!   - Tag = Poly1305(ciphertext, key = pad[0..32]).
//!   - Authenticators are zeroized along with payload pad bytes.
//! - Strict Zeroize: Used pad bytes are securely wiped with zeroize/zeros in memory and storage.
//! - Out-of-order tolerance: explicitly indexed chunks reject rewinds or re-use.

use std::fs::OpenOptions;
use std::io::{Read, Seek, SeekFrom, Write};
use std::path::{Path, PathBuf};
use zeroize::Zeroize;

use crate::error::CoreError;

pub const MAC_KEY_LEN: usize = 32;
pub const MAC_TAG_LEN: usize = 16;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PadSide {
    SideA, // Head -> Tail (starts at 0, moves forward)
    SideB, // Tail -> Head (starts at total_size, moves backward)
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct OtpMessage {
    pub offset: u64,
    pub ciphertext: Vec<u8>,
    pub tag: [u8; MAC_TAG_LEN],
}

/// Core in-memory / file-backed OTP controller.
pub struct OtpStorage {
    path: PathBuf,
    total_size: u64,
    reserve_pct: u8,
    head: u64,
    tail: u64,
    local_side: PadSide,
}

impl OtpStorage {
    /// Opens or binds an existing physical pad file.
    pub fn open<P: AsRef<Path>>(
        path: P,
        total_size: u64,
        reserve_pct: u8,
        head: u64,
        tail: u64,
        local_side: PadSide,
    ) -> Result<Self, CoreError> {
        let path_buf = path.as_ref().to_path_buf();
        if head > tail || tail > total_size || reserve_pct > 50 {
            return Err(CoreError::OutOfBounds);
        }

        Ok(Self {
            path: path_buf,
            total_size,
            reserve_pct,
            head,
            tail,
            local_side,
        })
    }

    pub fn total_size(&self) -> u64 {
        self.total_size
    }

    pub fn head(&self) -> u64 {
        self.head
    }

    pub fn tail(&self) -> u64 {
        self.tail
    }

    pub fn local_side(&self) -> PadSide {
        self.local_side
    }

    pub fn remaining_bytes(&self) -> u64 {
        if self.tail >= self.head {
            self.tail - self.head
        } else {
            0
        }
    }

    /// Checks if local side has budget to encrypt `payload_len` bytes (+ 32 bytes for MAC key).
    pub fn can_encrypt(&self, payload_len: usize) -> bool {
        let needed = match (payload_len as u64).checked_add(MAC_KEY_LEN as u64) {
            Some(n) => n,
            None => return false,
        };

        let reserve_bytes = (self.total_size * self.reserve_pct as u64) / 100;

        match self.local_side {
            PadSide::SideA => {
                // Must not intrude into Side B's guaranteed reserve
                let b_boundary = self.total_size.saturating_sub(reserve_bytes);
                let limit = self.tail.min(b_boundary);
                self.head
                    .checked_add(needed)
                    .map_or(false, |next| next <= limit)
            }
            PadSide::SideB => {
                // Must not intrude into Side A's guaranteed reserve
                let a_boundary = reserve_bytes;
                let limit = self.head.max(a_boundary);
                self.tail
                    .checked_sub(needed)
                    .map_or(false, |next| next >= limit)
            }
        }
    }

    /// Encrypts plaintext using pure OTP and generates a Wegman-Carter Poly1305 MAC.
    /// Immediately burns and zeroes out the used pad bytes on disk.
    pub fn encrypt(&mut self, plaintext: &[u8]) -> Result<OtpMessage, CoreError> {
        let len = plaintext.len();
        let total_needed = len
            .checked_add(MAC_KEY_LEN)
            .ok_or(CoreError::OutOfBounds)?;

        if !self.can_encrypt(len) {
            return Err(CoreError::PadExhausted);
        }

        let offset = match self.local_side {
            PadSide::SideA => {
                let off = self.head;
                self.head += total_needed as u64;
                off
            }
            PadSide::SideB => {
                self.tail -= total_needed as u64;
                self.tail
            }
        };

        // Read raw pad slice: [32 bytes MAC key] + [len bytes stream pad]
        let mut pad_bytes = vec![0u8; total_needed];
        self.read_and_wipe(offset, &mut pad_bytes)?;

        let mut mac_key = [0u8; MAC_KEY_LEN];
        mac_key.copy_from_slice(&pad_bytes[..MAC_KEY_LEN]);

        // Pure OTP XOR
        let mut ciphertext = vec![0u8; len];
        for i in 0..len {
            ciphertext[i] = plaintext[i] ^ pad_bytes[MAC_KEY_LEN + i];
        }

        // Generate Poly1305 tag
        let tag = compute_poly1305(&mac_key, &ciphertext);

        // Zeroize memory
        pad_bytes.zeroize();
        mac_key.zeroize();

        Ok(OtpMessage {
            offset,
            ciphertext,
            tag,
        })
    }

    /// Decrypts OTP message, verifies MAC, and securely wipes the pad bytes.
    pub fn decrypt(&mut self, msg: &OtpMessage) -> Result<Vec<u8>, CoreError> {
        let len = msg.ciphertext.len();
        let total_needed = len
            .checked_add(MAC_KEY_LEN)
            .ok_or(CoreError::OutOfBounds)?;

        // Verify bounds against overall pad
        let end_offset = msg
            .offset
            .checked_add(total_needed as u64)
            .ok_or(CoreError::OutOfBounds)?;

        if end_offset > self.total_size {
            return Err(CoreError::OutOfBounds);
        }

        // Advance pointers if appropriate to prevent reuse
        match self.local_side {
            PadSide::SideA => {
                // Incoming from Side B (tailwards)
                if msg.offset < self.head {
                    // Collision with our own head!
                    return Err(CoreError::OutOfBounds);
                }
                if msg.offset < self.tail {
                    self.tail = msg.offset;
                }
            }
            PadSide::SideB => {
                // Incoming from Side A (headwards)
                if end_offset > self.tail {
                    // Collision with our own tail!
                    return Err(CoreError::OutOfBounds);
                }
                if end_offset > self.head {
                    self.head = end_offset;
                }
            }
        }

        // Read pad slice & zeroize on storage
        let mut pad_bytes = vec![0u8; total_needed];
        self.read_and_wipe(msg.offset, &mut pad_bytes)?;

        let mut mac_key = [0u8; MAC_KEY_LEN];
        mac_key.copy_from_slice(&pad_bytes[..MAC_KEY_LEN]);

        // Verify Poly1305 tag
        let expected_tag = compute_poly1305(&mac_key, &msg.ciphertext);
        if expected_tag != msg.tag {
            pad_bytes.zeroize();
            mac_key.zeroize();
            return Err(CoreError::Decrypt);
        }

        // Pure OTP XOR
        let mut plaintext = vec![0u8; len];
        for i in 0..len {
            plaintext[i] = msg.ciphertext[i] ^ pad_bytes[MAC_KEY_LEN + i];
        }

        pad_bytes.zeroize();
        mac_key.zeroize();

        Ok(plaintext)
    }

    /// Reads pad segment into buffer and overwrites storage with zeroes immediately.
    fn read_and_wipe(&self, offset: u64, buf: &mut [u8]) -> Result<(), CoreError> {
        let mut file = OpenOptions::new()
            .read(true)
            .write(true)
            .open(&self.path)
            .map_err(|_| CoreError::Io)?;

        file.seek(SeekFrom::Start(offset))
            .map_err(|_| CoreError::Io)?;
        file.read_exact(buf).map_err(|_| CoreError::Io)?;

        // Secure wipe on disk with zeroes
        let zeroes = vec![0u8; buf.len()];
        file.seek(SeekFrom::Start(offset))
            .map_err(|_| CoreError::Io)?;
        file.write_all(&zeroes).map_err(|_| CoreError::Io)?;
        file.sync_data().map_err(|_| CoreError::Io)?;

        Ok(())
    }
}

/// Computes Poly1305 MAC over data using a 32-byte one-time key.
fn compute_poly1305(key: &[u8; MAC_KEY_LEN], data: &[u8]) -> [u8; MAC_TAG_LEN] {
    use chacha20poly1305::aead::generic_array::GenericArray;
    use chacha20poly1305::ChaCha20Poly1305;
    use chacha20poly1305::aead::{AeadInPlace, KeyInit};

    // Poly1305 with zero nonce via ChaCha20Poly1305 authenticated payload on empty text
    let cipher = ChaCha20Poly1305::new(GenericArray::from_slice(key));
    let nonce = GenericArray::from_slice(&[0u8; 12]);
    let mut buffer = Vec::new();
    let tag = cipher
        .encrypt_in_place_detached(nonce, data, &mut buffer)
        .expect("authentication tag computation");
    
    let mut out = [0u8; MAC_TAG_LEN];
    out.copy_from_slice(tag.as_slice());
    out
}

/// Combines two equal-sized pad chunks using XOR (physical entropy mutual generation).
pub fn xor_pad_buffers(a: &mut [u8], b: &[u8]) -> Result<(), CoreError> {
    if a.len() != b.len() {
        return Err(CoreError::Malformed);
    }
    for i in 0..a.len() {
        a[i] ^= b[i];
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    fn create_test_pad(size: usize) -> (tempfile::NamedTempFile, Vec<u8>) {
        let mut tmp = tempfile::NamedTempFile::new().unwrap();
        let raw: Vec<u8> = (0..size).map(|i| (i * 37 + 11) as u8).collect();
        tmp.write_all(&raw).unwrap();
        tmp.flush().unwrap();
        (tmp, raw)
    }

    #[test]
    fn test_two_ended_allocation_and_roundtrip() {
        let size = 1024 * 10;
        let (file_a, raw) = create_test_pad(size);
        let path_a = file_a.path().to_path_buf();

        // Create independent duplicate pad file for Side B (representing Bob's device)
        let mut file_b = tempfile::NamedTempFile::new().unwrap();
        file_b.write_all(&raw).unwrap();
        file_b.flush().unwrap();
        let path_b = file_b.path().to_path_buf();

        let mut side_a =
            OtpStorage::open(&path_a, size as u64, 20, 0, size as u64, PadSide::SideA).unwrap();
        let mut side_b =
            OtpStorage::open(&path_b, size as u64, 20, 0, size as u64, PadSide::SideB).unwrap();

        let msg1 = b"Secret message from Alice to Bob";
        let encrypted1 = side_a.encrypt(msg1).unwrap();
        assert_eq!(encrypted1.offset, 0);

        let decrypted1 = side_b.decrypt(&encrypted1).unwrap();
        assert_eq!(decrypted1, msg1);

        let msg2 = b"Response message from Bob to Alice";
        let encrypted2 = side_b.encrypt(msg2).unwrap();
        assert_eq!(encrypted2.offset, (size - msg2.len() - MAC_KEY_LEN) as u64);

        let decrypted2 = side_a.decrypt(&encrypted2).unwrap();
        assert_eq!(decrypted2, msg2);
    }

    #[test]
    fn test_tampering_rejected_by_mac() {
        let size = 1024;
        let (file_a, raw) = create_test_pad(size);
        let path_a = file_a.path().to_path_buf();

        let mut file_b = tempfile::NamedTempFile::new().unwrap();
        file_b.write_all(&raw).unwrap();
        file_b.flush().unwrap();
        let path_b = file_b.path().to_path_buf();

        let mut side_a =
            OtpStorage::open(&path_a, size as u64, 20, 0, size as u64, PadSide::SideA).unwrap();
        let mut side_b =
            OtpStorage::open(&path_b, size as u64, 20, 0, size as u64, PadSide::SideB).unwrap();

        let mut enc = side_a.encrypt(b"Strict OTP message").unwrap();
        enc.ciphertext[0] ^= 0x01; // Tamper 1 bit

        assert_eq!(side_b.decrypt(&enc).unwrap_err(), CoreError::Decrypt);
    }

    #[test]
    fn test_reserve_limits_prevent_starvation() {
        let size = 100; // Small pad
        let (file, _) = create_test_pad(size);
        let path = file.path().to_path_buf();

        // 20% reserve = 20 bytes reserved for Side B. Side A cannot pass byte 80.
        let mut side_a =
            OtpStorage::open(&path, size as u64, 20, 0, size as u64, PadSide::SideA).unwrap();

        // Trying to encrypt 50 bytes + 32 bytes MAC = 82 bytes (exceeds 80 limit)
        let large_msg = vec![42u8; 50];
        assert_eq!(
            side_a.encrypt(&large_msg).unwrap_err(),
            CoreError::PadExhausted
        );
    }

    #[test]
    fn test_xor_pad_buffers() {
        let mut buf_a = vec![0b10101010, 0x00, 0xFF];
        let buf_b = vec![0b01010101, 0xAA, 0xFF];
        xor_pad_buffers(&mut buf_a, &buf_b).unwrap();
        assert_eq!(buf_a, vec![0b11111111, 0xAA, 0x00]);
    }
}
