//! Android JNI bridge for `com.anongram.app.core.crypto.NativeCryptoBridge`.

use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jint, jstring};
use jni::JNIEnv;

use crypto::aead::{self, KEY_LEN};
use media::{open_media, seal_media, strip_image_metadata, MediaType};

#[no_mangle]
pub extern "system" fn Java_com_anongram_app_core_crypto_NativeCryptoBridge_nativeVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    match env.new_string("0.1.0-modular-core") {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

use sha2::{Digest, Sha256};

#[no_mangle]
pub extern "system" fn Java_com_anongram_app_core_crypto_NativeCryptoBridge_nativeDeriveFingerprint(
    env: JNIEnv,
    _class: JClass,
    public_key: JByteArray,
) -> jstring {
    let Ok(bytes) = env.convert_byte_array(public_key) else {
        return std::ptr::null_mut();
    };
    let hash = Sha256::digest(&bytes);
    let fp = format!(
        "0x{:02X}{:02X}…{:02X}{:02X}",
        hash[0], hash[1], hash[30], hash[31]
    );
    match env.new_string(fp) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_anongram_app_core_crypto_NativeCryptoBridge_nativeAeadSeal(
    env: JNIEnv,
    _class: JClass,
    key: JByteArray,
    aad: JByteArray,
    plain: JByteArray,
) -> jbyteArray {
    let Ok(key_bytes) = env.convert_byte_array(key) else {
        return std::ptr::null_mut();
    };
    if key_bytes.len() != KEY_LEN {
        return std::ptr::null_mut();
    }
    let mut k = [0u8; KEY_LEN];
    k.copy_from_slice(&key_bytes);

    let aad_bytes = env.convert_byte_array(aad).unwrap_or_default();
    let Ok(plain_bytes) = env.convert_byte_array(plain) else {
        return std::ptr::null_mut();
    };

    match aead::seal(&k, &aad_bytes, &plain_bytes) {
        Ok(sealed) => env
            .byte_array_from_slice(&sealed)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_anongram_app_core_crypto_NativeCryptoBridge_nativeAeadOpen(
    env: JNIEnv,
    _class: JClass,
    key: JByteArray,
    aad: JByteArray,
    cipher: JByteArray,
) -> jbyteArray {
    let Ok(key_bytes) = env.convert_byte_array(key) else {
        return std::ptr::null_mut();
    };
    if key_bytes.len() != KEY_LEN {
        return std::ptr::null_mut();
    }
    let mut k = [0u8; KEY_LEN];
    k.copy_from_slice(&key_bytes);

    let aad_bytes = env.convert_byte_array(aad).unwrap_or_default();
    let Ok(cipher_bytes) = env.convert_byte_array(cipher) else {
        return std::ptr::null_mut();
    };

    match aead::open(&k, &aad_bytes, &cipher_bytes) {
        Ok(opened) => env
            .byte_array_from_slice(&opened)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_anongram_app_core_crypto_NativeCryptoBridge_nativeStripImageMetadata(
    env: JNIEnv,
    _class: JClass,
    data: JByteArray,
) -> jbyteArray {
    let Ok(raw) = env.convert_byte_array(data) else {
        return std::ptr::null_mut();
    };
    let clean = strip_image_metadata(&raw);
    env.byte_array_from_slice(&clean)
        .map(|a| a.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_anongram_app_core_crypto_NativeCryptoBridge_nativeSealMedia(
    env: JNIEnv,
    _class: JClass,
    key: JByteArray,
    media_type: jint,
    data: JByteArray,
) -> jbyteArray {
    let Ok(key_bytes) = env.convert_byte_array(key) else {
        return std::ptr::null_mut();
    };
    if key_bytes.len() != KEY_LEN {
        return std::ptr::null_mut();
    }
    let mut k = [0u8; KEY_LEN];
    k.copy_from_slice(&key_bytes);

    let Ok(mt) = MediaType::from_u8(media_type as u8) else {
        return std::ptr::null_mut();
    };
    let Ok(raw) = env.convert_byte_array(data) else {
        return std::ptr::null_mut();
    };

    match seal_media(&k, mt, &raw) {
        Ok(sealed) => env
            .byte_array_from_slice(&sealed)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_com_anongram_app_core_crypto_NativeCryptoBridge_nativeOpenMedia(
    env: JNIEnv,
    _class: JClass,
    key: JByteArray,
    sealed: JByteArray,
) -> jbyteArray {
    let Ok(key_bytes) = env.convert_byte_array(key) else {
        return std::ptr::null_mut();
    };
    if key_bytes.len() != KEY_LEN {
        return std::ptr::null_mut();
    }
    let mut k = [0u8; KEY_LEN];
    k.copy_from_slice(&key_bytes);

    let Ok(blob) = env.convert_byte_array(sealed) else {
        return std::ptr::null_mut();
    };

    match open_media(&k, &blob) {
        Ok((_mt, opened)) => env
            .byte_array_from_slice(&opened)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
        Err(_) => std::ptr::null_mut(),
    }
}
