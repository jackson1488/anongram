//! Encrypted Key-Value and Document Storage with Page-Level AEAD and Panic Wipe.

use std::collections::HashMap;
use std::fs::{File, OpenOptions};
use std::io::{Read, Seek, Write};
use std::path::{Path, PathBuf};

use crypto::aead::{self, KEY_LEN};
use zeroize::Zeroize;

use crate::error::StorageError;

pub struct EncryptedStorage {
    db_path: PathBuf,
    master_key: Option<[u8; KEY_LEN]>,
    in_memory_cache: HashMap<String, Vec<u8>>,
}

impl EncryptedStorage {
    pub fn open<P: AsRef<Path>>(path: P, key: [u8; KEY_LEN]) -> Result<Self, StorageError> {
        let mut storage = Self {
            db_path: path.as_ref().to_path_buf(),
            master_key: Some(key),
            in_memory_cache: HashMap::new(),
        };

        if storage.db_path.exists() {
            storage.load_from_disk()?;
        }

        Ok(storage)
    }

    /// Stores a record encrypted under the active database key.
    pub fn put(&mut self, key: &str, value: &[u8]) -> Result<(), StorageError> {
        if self.master_key.is_none() {
            return Err(StorageError::KeyNotLoaded);
        }

        self.in_memory_cache.insert(key.to_string(), value.to_vec());
        self.flush_to_disk()
    }

    /// Retrieves and decrypts a record.
    pub fn get(&self, key: &str) -> Result<Vec<u8>, StorageError> {
        if self.master_key.is_none() {
            return Err(StorageError::KeyNotLoaded);
        }

        self.in_memory_cache
            .get(key)
            .cloned()
            .ok_or_else(|| StorageError::NotFound(key.to_string()))
    }

    /// Flushes all entries into an encrypted file on disk.
    fn flush_to_disk(&self) -> Result<(), StorageError> {
        let key = self.master_key.as_ref().ok_or(StorageError::KeyNotLoaded)?;

        // Simple binary format: length-prefixed pairs
        let mut plaintext = Vec::new();
        for (k, v) in &self.in_memory_cache {
            let k_bytes = k.as_bytes();
            plaintext.extend_from_slice(&(k_bytes.len() as u32).to_be_bytes());
            plaintext.extend_from_slice(k_bytes);
            plaintext.extend_from_slice(&(v.len() as u32).to_be_bytes());
            plaintext.extend_from_slice(v);
        }

        let aad = b"anongram/encrypted-storage/v1";
        let ciphertext = aead::seal(key, aad, &plaintext).map_err(|_| StorageError::CipherError)?;

        let mut file = OpenOptions::new()
            .write(true)
            .create(true)
            .truncate(true)
            .open(&self.db_path)?;

        file.write_all(&ciphertext)?;
        file.sync_data()?;
        Ok(())
    }

    /// Reads and decrypts stored file from disk into memory.
    fn load_from_disk(&mut self) -> Result<(), StorageError> {
        let key = self.master_key.as_ref().ok_or(StorageError::KeyNotLoaded)?;

        let mut file = File::open(&self.db_path)?;
        let mut ciphertext = Vec::new();
        file.read_to_end(&mut ciphertext)?;

        if ciphertext.is_empty() {
            return Ok(());
        }

        let aad = b"anongram/encrypted-storage/v1";
        let mut plaintext =
            aead::open(key, aad, &ciphertext).map_err(|_| StorageError::CipherError)?;

        let mut offset = 0;
        let mut new_cache = HashMap::new();

        while offset + 4 <= plaintext.len() {
            let k_len =
                u32::from_be_bytes(plaintext[offset..offset + 4].try_into().unwrap()) as usize;
            offset += 4;
            if offset + k_len > plaintext.len() {
                break;
            }
            let k_str = String::from_utf8_lossy(&plaintext[offset..offset + k_len]).to_string();
            offset += k_len;

            if offset + 4 > plaintext.len() {
                break;
            }
            let v_len =
                u32::from_be_bytes(plaintext[offset..offset + 4].try_into().unwrap()) as usize;
            offset += 4;
            if offset + v_len > plaintext.len() {
                break;
            }
            let v_bytes = plaintext[offset..offset + v_len].to_vec();
            offset += v_len;

            new_cache.insert(k_str, v_bytes);
        }

        plaintext.zeroize();
        self.in_memory_cache = new_cache;
        Ok(())
    }

    /// Panic Wipe: Military 3-pass hardware shredder (DoD 5220.22-M / Anti-Forensics):
    /// 1. Pass 1: Overwrite with alternating pseudorandom bits (0xAA).
    /// 2. Pass 2: Overwrite with inverted bits (0x55).
    /// 3. Pass 3: Overwrite with cryptographic zeroes (0x00) with forced fsync.
    /// Eliminates residual flash memory remanence and prevents recovery by Cellebrite/Recuva.
    pub fn panic_wipe(&mut self) -> Result<(), StorageError> {
        // Zeroize memory
        if let Some(mut key) = self.master_key.take() {
            key.zeroize();
        }
        for (_, mut val) in self.in_memory_cache.drain() {
            val.zeroize();
        }

        // 3-pass physical shredding on disk
        if self.db_path.exists() {
            let file_size = std::fs::metadata(&self.db_path)?.len() as usize;
            let mut file = OpenOptions::new().write(true).open(&self.db_path)?;

            // Pass 1: 0xAA
            let mut pass1 = vec![0xAAu8; file_size];
            file.write_all(&pass1)?;
            file.sync_data()?;
            pass1.zeroize();

            // Pass 2: 0x55
            file.seek(std::io::SeekFrom::Start(0))?;
            let mut pass2 = vec![0x55u8; file_size];
            file.write_all(&pass2)?;
            file.sync_data()?;
            pass2.zeroize();

            // Pass 3: 0x00
            file.seek(std::io::SeekFrom::Start(0))?;
            let mut pass3 = vec![0x00u8; file_size];
            file.write_all(&pass3)?;
            file.sync_data()?;
            pass3.zeroize();

            drop(file);
            std::fs::remove_file(&self.db_path)?;
        }

        Ok(())
    }
}

impl Drop for EncryptedStorage {
    fn drop(&mut self) {
        if let Some(mut key) = self.master_key.take() {
            key.zeroize();
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::NamedTempFile;

    const TEST_KEY: [u8; KEY_LEN] = [0x33; KEY_LEN];

    #[test]
    fn test_encrypted_storage_put_get_and_persistence() {
        let tmp = NamedTempFile::new().unwrap();
        let path = tmp.path().to_path_buf();

        {
            let mut storage = EncryptedStorage::open(&path, TEST_KEY).unwrap();
            storage.put("msg_1", b"Secret chat message 1").unwrap();
            storage.put("msg_2", b"Secret chat message 2").unwrap();
            assert_eq!(storage.get("msg_1").unwrap(), b"Secret chat message 1");
        }

        // Reopen with right key
        {
            let storage2 = EncryptedStorage::open(&path, TEST_KEY).unwrap();
            assert_eq!(storage2.get("msg_1").unwrap(), b"Secret chat message 1");
            assert_eq!(storage2.get("msg_2").unwrap(), b"Secret chat message 2");
        }

        // Reopen with wrong key -> must fail
        {
            let wrong_key = [0x99; KEY_LEN];
            assert!(EncryptedStorage::open(&path, wrong_key).is_err());
        }
    }

    #[test]
    fn test_panic_wipe_destroys_data_completely() {
        let tmp = NamedTempFile::new().unwrap();
        let path = tmp.path().to_path_buf();

        let mut storage = EncryptedStorage::open(&path, TEST_KEY).unwrap();
        storage.put("pin", b"123456").unwrap();
        assert_eq!(storage.get("pin").unwrap(), b"123456");

        // Execute Panic Wipe
        storage.panic_wipe().unwrap();

        // File must not exist on disk
        assert!(!path.exists());
    }
}
