//! SFrame (Secure Frame RFC 9605) End-to-End Encryption Engine.
//!
//! Encrypts media frames (Opus Audio / VP8/H264 Video) at the client level before
//! packetization and transmission through WebRTC (P2P, TURN, or SFU).
//! Ensures that intermediate servers (TURN / SFU) NEVER have access to audio/video plaintext.

use crypto::aead::{self, KEY_LEN};
use zeroize::Zeroize;

use crate::error::NetworkError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MediaType {
    AudioOpus,
    VideoVp8,
    VideoH264,
}

impl MediaType {
    pub fn to_u8(&self) -> u8 {
        match self {
            MediaType::AudioOpus => 1,
            MediaType::VideoVp8 => 2,
            MediaType::VideoH264 => 3,
        }
    }

    pub fn from_u8(v: u8) -> Result<Self, NetworkError> {
        match v {
            1 => Ok(MediaType::AudioOpus),
            2 => Ok(MediaType::VideoVp8),
            3 => Ok(MediaType::VideoH264),
            _ => Err(NetworkError::MalformedFrame),
        }
    }
}


#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EncryptedMediaFrame {
    pub key_id: u32,
    pub frame_seq: u64,
    pub media_type: MediaType,
    pub ciphertext: Vec<u8>,
}

pub struct SFrameEngine {
    key_id: u32,
    frame_counter: u64,
    ratchet_key: [u8; KEY_LEN],
}

impl SFrameEngine {
    pub fn new(key_id: u32, session_key: [u8; KEY_LEN]) -> Self {
        Self {
            key_id,
            frame_counter: 0,
            ratchet_key: session_key,
        }
    }

    /// Encrypts raw Opus or Video frame before sending to WebRTC.
    pub fn seal_frame(
        &mut self,
        media_type: MediaType,
        raw_frame: &[u8],
    ) -> Result<EncryptedMediaFrame, NetworkError> {
        self.frame_counter += 1;

        // Construct SFrame unencrypted header for AAD authentication
        let mut aad = Vec::with_capacity(16);
        aad.extend_from_slice(&self.key_id.to_be_bytes());
        aad.extend_from_slice(&self.frame_counter.to_be_bytes());
        aad.push(match media_type {
            MediaType::AudioOpus => 1,
            MediaType::VideoVp8 => 2,
            MediaType::VideoH264 => 3,
        });

        let ciphertext = aead::seal(&self.ratchet_key, &aad, raw_frame)
            .map_err(|_| NetworkError::TransmissionError("SFrame encryption failed".into()))?;

        Ok(EncryptedMediaFrame {
            key_id: self.key_id,
            frame_seq: self.frame_counter,
            media_type,
            ciphertext,
        })
    }

    /// Decrypts received SFrame after depacketization from WebRTC.
    pub fn open_frame(&self, frame: &EncryptedMediaFrame) -> Result<Vec<u8>, NetworkError> {
        let mut aad = Vec::with_capacity(16);
        aad.extend_from_slice(&frame.key_id.to_be_bytes());
        aad.extend_from_slice(&frame.frame_seq.to_be_bytes());
        aad.push(match frame.media_type {
            MediaType::AudioOpus => 1,
            MediaType::VideoVp8 => 2,
            MediaType::VideoH264 => 3,
        });

        aead::open(&self.ratchet_key, &aad, &frame.ciphertext)
            .map_err(|_| NetworkError::MalformedFrame)
    }
}

impl Drop for SFrameEngine {
    fn drop(&mut self) {
        self.ratchet_key.zeroize();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_sframe_roundtrip_audio_and_video() {
        let key = [0x55u8; KEY_LEN];
        let mut sender = SFrameEngine::new(1, key);
        let receiver = SFrameEngine::new(1, key);

        // 1. Audio Frame (20ms Opus raw payload)
        let opus_pcm = b"OPUS_ENCODED_20MS_AUDIO_FRAME_VOICE_DATA";
        let encrypted_audio = sender.seal_frame(MediaType::AudioOpus, opus_pcm).unwrap();
        assert_eq!(encrypted_audio.key_id, 1);
        assert_eq!(encrypted_audio.frame_seq, 1);

        let decrypted_audio = receiver.open_frame(&encrypted_audio).unwrap();
        assert_eq!(decrypted_audio, opus_pcm);

        // 2. Video Frame (VP8 Keyframe)
        let vp8_data = b"VP8_VIDEO_KEYFRAME_1080P_PAYLOAD";
        let encrypted_video = sender.seal_frame(MediaType::VideoVp8, vp8_data).unwrap();
        assert_eq!(encrypted_video.frame_seq, 2);

        let decrypted_video = receiver.open_frame(&encrypted_video).unwrap();
        assert_eq!(decrypted_video, vp8_data);

        // 3. Tampering must fail authentication
        let mut tampered = encrypted_audio.clone();
        tampered.ciphertext[0] ^= 0x01;
        assert!(receiver.open_frame(&tampered).is_err());
    }
}
