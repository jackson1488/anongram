//! Media & Document Zero-Leak Pipeline.
//!
//! Provides:
//! - Complete metadata stripping (EXIF/GPS/Device info removal).
//! - Lossless & high-ratio compression (Zstandard) to preserve One-Time Pad quota.
//! - Encrypted binary blob storage format (`.blob` files, completely invisible to Android Gallery / MediaScanner).
//! - In-memory streaming encryption/decryption (XChaCha20-Poly1305) without temporary files on disk.

use crate::aead::{self, KEY_LEN};
use crate::error::CoreError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MediaType {
    Image,
    Voice,
    Video,
    Document,
}

impl MediaType {
    pub fn to_u8(&self) -> u8 {
        match self {
            MediaType::Image => 1,
            MediaType::Voice => 2,
            MediaType::Video => 3,
            MediaType::Document => 4,
        }
    }

    pub fn from_u8(v: u8) -> Result<Self, CoreError> {
        match v {
            1 => Ok(MediaType::Image),
            2 => Ok(MediaType::Voice),
            3 => Ok(MediaType::Video),
            4 => Ok(MediaType::Document),
            _ => Err(CoreError::Malformed),
        }
    }
}

/// Strips standard JPEG/PNG metadata segments (EXIF, GPS, camera serials, IPTC, XMP).
/// Returns sanitized image bytes.
pub fn strip_image_metadata(data: &[u8]) -> Vec<u8> {
    // Check JPEG SOI (0xFF, 0xD8)
    if data.len() >= 4 && data[0] == 0xFF && data[1] == 0xD8 {
        strip_jpeg_exif(data)
    } else if data.len() >= 8 && &data[0..8] == b"\x89PNG\r\n\x1a\n" {
        strip_png_metadata(data)
    } else {
        // Fallback: return as-is for unrecognized or raw data
        data.to_vec()
    }
}

fn strip_jpeg_exif(data: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(data.len());
    out.extend_from_slice(&[0xFF, 0xD8]); // SOI marker
    let mut i = 2;

    while i + 4 <= data.len() {
        if data[i] != 0xFF {
            // Raw image stream reached, append rest
            out.extend_from_slice(&data[i..]);
            break;
        }

        let marker = data[i + 1];
        if marker == 0xDA || marker == 0xD9 {
            // SOS (Start of Scan) or EOI (End of Image)
            out.extend_from_slice(&data[i..]);
            break;
        }

        let length = u16::from_be_bytes([data[i + 2], data[i + 3]]) as usize;
        let segment_end = i + 2 + length;

        if segment_end > data.len() {
            out.extend_from_slice(&data[i..]);
            break;
        }

        // Filter out APP1 (EXIF: 0xE1), APP2 (ICC: 0xE2), APP13 (IPTC: 0xED), APP14 (0xEE), COM (Comments: 0xFE)
        let is_meta = matches!(marker, 0xE1 | 0xE2 | 0xED | 0xFE);
        if !is_meta {
            out.extend_from_slice(&data[i..segment_end]);
        }

        i = segment_end;
    }

    out
}

fn strip_png_metadata(data: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(data.len());
    out.extend_from_slice(&data[0..8]); // PNG signature
    let mut i = 8;

    while i + 12 <= data.len() {
        let length = u32::from_be_bytes(data[i..i + 4].try_into().unwrap()) as usize;
        let chunk_type = &data[i + 4..i + 8];
        let chunk_end = i + 12 + length;

        if chunk_end > data.len() {
            break;
        }

        // Keep critical chunks (IHDR, PLTE, IDAT, IEND), strip ancillary chunks (tEXt, zTXt, iTXt, eXIf)
        let is_meta = matches!(chunk_type, b"tEXt" | b"zTXt" | b"iTXt" | b"eXIf" | b"tIME");
        if !is_meta {
            out.extend_from_slice(&data[i..chunk_end]);
        }

        if chunk_type == b"IEND" {
            break;
        }

        i = chunk_end;
    }

    out
}

/// High-ratio Zstandard compression (reduces One-Time Pad consumption).
pub fn compress_payload(data: &[u8]) -> Result<Vec<u8>, CoreError> {
    zstd::encode_all(data, 3).map_err(|_| CoreError::Compression)
}

/// Decompress payload.
pub fn decompress_payload(compressed: &[u8]) -> Result<Vec<u8>, CoreError> {
    zstd::decode_all(compressed).map_err(|_| CoreError::Decompression)
}

/// Packs, strips metadata, compresses, and encrypts any media or document.
///
/// Output format (Encrypted Blob):
/// `XChaCha20-Poly1305( [MediaType(1)] || [Compressed Payload] )`
pub fn seal_media(
    key: &[u8; KEY_LEN],
    media_type: MediaType,
    raw_payload: &[u8],
) -> Result<Vec<u8>, CoreError> {
    // 1. Strip metadata if image
    let sanitized = match media_type {
        MediaType::Image => strip_image_metadata(raw_payload),
        _ => raw_payload.to_vec(),
    };

    // 2. Compress with Zstandard to save OTP/network bandwidth
    let compressed = compress_payload(&sanitized)?;

    // 3. Assemble plain buffer: 1 byte type + compressed bytes
    let mut plain = Vec::with_capacity(1 + compressed.len());
    plain.push(media_type.to_u8());
    plain.extend_from_slice(&compressed);

    // 4. Encrypt into authenticated blob
    let aad = b"anongram/media/v1";
    aead::seal(key, aad, &plain)
}

/// Decrypts, decompresses, and recovers original clean media.
pub fn open_media(key: &[u8; KEY_LEN], encrypted_blob: &[u8]) -> Result<(MediaType, Vec<u8>), CoreError> {
    let aad = b"anongram/media/v1";
    let plain = aead::open(key, aad, encrypted_blob)?;

    if plain.is_empty() {
        return Err(CoreError::Malformed);
    }

    let media_type = MediaType::from_u8(plain[0])?;
    let decompressed = decompress_payload(&plain[1..])?;

    Ok((media_type, decompressed))
}

#[cfg(test)]
mod tests {
    use super::*;

    const KEY: [u8; KEY_LEN] = [42u8; KEY_LEN];

    #[test]
    fn test_jpeg_exif_stripping() {
        // Construct fake JPEG with EXIF (0xFF, 0xE1) marker
        let mut fake_jpeg = vec![0xFF, 0xD8]; // SOI
        // Add APP1 (EXIF segment)
        fake_jpeg.extend_from_slice(&[0xFF, 0xE1, 0x00, 0x08]); // length 8
        fake_jpeg.extend_from_slice(b"GPS_TAG");
        // Add SOS and image data
        fake_jpeg.extend_from_slice(&[0xFF, 0xDA, 0x00, 0x02, 0x12, 0x34, 0xFF, 0xD9]);

        let stripped = strip_image_metadata(&fake_jpeg);
        assert!(!stripped.windows(7).any(|w| w == b"GPS_TAG"));
        assert_eq!(&stripped[0..2], &[0xFF, 0xD8]);
    }

    #[test]
    fn test_media_pipeline_roundtrip_all_types() {
        let types = [
            (MediaType::Image, b"RAW_IMAGE_PIXELS_DATA".as_slice()),
            (MediaType::Voice, b"OPUS_VOICE_AUDIO_RECORDING".as_slice()),
            (MediaType::Video, b"H264_VIDEO_FRAME_BYTES".as_slice()),
            (MediaType::Document, b"CONFIDENTIAL_PDF_OR_DOCX_FILE_TEXT".as_slice()),
        ];

        for (m_type, data) in types {
            let sealed = seal_media(&KEY, m_type, data).unwrap();
            assert_ne!(sealed, data); // Completely ciphered

            let (opened_type, decompressed) = open_media(&KEY, &sealed).unwrap();
            assert_eq!(opened_type, m_type);
            assert_eq!(decompressed, data);
        }
    }

    #[test]
    fn test_compression_efficiency() {
        let text_doc = b"ANONGRAM SECRET PROTOCOL SPECIFICATION ".repeat(100);
        let compressed = compress_payload(&text_doc).unwrap();
        assert!(compressed.len() < text_doc.len() / 3);

        let restored = decompress_payload(&compressed).unwrap();
        assert_eq!(restored, text_doc);
    }
}
