//! C-ABI Raw Buffer for safe Zero-Copy memory passing between Rust and Dart/Flutter.

use std::slice;

#[repr(C)]
pub struct ByteBuffer {
    pub ptr: *mut u8,
    pub len: usize,
    pub capacity: usize,
}

impl ByteBuffer {
    pub fn from_vec(mut v: Vec<u8>) -> Self {
        let ptr = v.as_mut_ptr();
        let len = v.len();
        let capacity = v.capacity();
        std::mem::forget(v);
        Self { ptr, len, capacity }
    }

    pub fn empty() -> Self {
        Self {
            ptr: std::ptr::null_mut(),
            len: 0,
            capacity: 0,
        }
    }

    /// Converts raw C pointer and length safely into Rust slice.
    ///
    /// # Safety
    /// Caller must ensure `ptr` points to valid memory of at least `len` bytes.
    pub unsafe fn as_slice<'a>(ptr: *const u8, len: usize) -> &'a [u8] {
        if ptr.is_null() || len == 0 {
            &[]
        } else {
            slice::from_raw_parts(ptr, len)
        }
    }
}

/// Frees buffer allocated by Rust.
///
/// # Safety
/// Must only be called on `ByteBuffer` allocated by Rust.
#[no_mangle]
pub unsafe extern "C" fn anongram_free_buffer(buf: ByteBuffer) {
    if !buf.ptr.is_null() && buf.capacity > 0 {
        let _ = Vec::from_raw_parts(buf.ptr, buf.len, buf.capacity);
    }
}
