//! C ABI over the Longfellow Rust runtime.
//!
//! Upstream ships a Go verifier that links the C++ `libmdoc_static`. This links
//! the Rust implementation instead, so a Go service needs no C++ toolchain — and
//! so the ecosystem gets a second independent Go binding.
//!
//! Every entry point is `extern "C"`, takes borrowed buffers, writes any error
//! message into a caller-supplied buffer, and returns 0 on success.

use std::os::raw::{c_char, c_int};
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::slice;

use mdoc_zk_runtime::{provider, req_attr, run_mdoc_verifier_inner, RequestedAttribute};

pub const ZKV_OK: c_int = 0;
pub const ZKV_ERR_ARGS: c_int = 1;
pub const ZKV_ERR_CIRCUIT: c_int = 2;
pub const ZKV_ERR_VERIFY: c_int = 3;
pub const ZKV_ERR_PANIC: c_int = 4;

/// Copy `msg` into `buf` as a NUL-terminated string, truncating if needed.
unsafe fn set_err(buf: *mut c_char, len: usize, msg: &str) {
    if buf.is_null() || len == 0 {
        return;
    }
    let bytes = msg.as_bytes();
    let n = bytes.len().min(len - 1);
    std::ptr::copy_nonoverlapping(bytes.as_ptr(), buf.cast::<u8>(), n);
    *buf.add(n) = 0;
}

unsafe fn as_str<'a>(p: *const c_char) -> Option<&'a str> {
    if p.is_null() {
        return None;
    }
    std::ffi::CStr::from_ptr(p).to_str().ok()
}

unsafe fn as_bytes<'a>(p: *const u8, len: usize) -> Option<&'a [u8]> {
    if p.is_null() && len != 0 {
        return None;
    }
    Some(if len == 0 { &[] } else { slice::from_raw_parts(p, len) })
}

/// Byte length of a CBOR text-string header for a string of `n` bytes.
fn cbor_str_header_len(n: usize) -> usize {
    match n {
        0..=23 => 1,
        24..=0xff => 2,
        0x100..=0xffff => 3,
        _ => 5,
    }
}

/// Reject inputs that overflow the fixed-width slots of the legacy input hash
/// (longfellow `legacy/public.rs`). Upstream pads with `(slot - len) * 8`, which
/// wraps on oversize input and aborts the process on the resulting allocation —
/// an abort `catch_unwind` cannot stop. `now` is a holder-supplied timestamp.
fn check_input_sizes(version: u32, now: &str, attr_id: &str, attr_cbor: &[u8]) -> Result<(), &'static str> {
    if now.len() > 20 {
        return Err("timestamp longer than 20 bytes");
    }
    let id_enc = cbor_str_header_len(attr_id.len()) + attr_id.len();
    let fits = if version >= 7 {
        id_enc <= 32 && attr_cbor.len() <= 64
    } else {
        id_enc + cbor_str_header_len(12) + 12 + attr_cbor.len() <= 96
    };
    if !fits {
        return Err("attribute id or value too long for circuit");
    }
    Ok(())
}

/// Verify a Longfellow proof that a single attribute is present in an mdoc.
///
/// `version` / `num_attributes` select the circuit; the caller is expected to
/// have already checked the circuit hash against its own allowlist
/// (see `zkv_circuit_hash`).
///
/// # Safety
/// All pointers must be valid for the given lengths for the duration of the call.
#[no_mangle]
pub unsafe extern "C" fn zkv_verify(
    version: u32,
    num_attributes: u32,
    pkx: *const c_char,
    pky: *const c_char,
    transcript: *const u8,
    transcript_len: usize,
    doc_type: *const c_char,
    namespace: *const c_char,
    attr_id: *const c_char,
    attr_cbor: *const u8,
    attr_cbor_len: usize,
    now: *const c_char,
    proof: *const u8,
    proof_len: usize,
    err: *mut c_char,
    err_len: usize,
) -> c_int {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let (Some(pkx), Some(pky), Some(doc_type), Some(namespace), Some(attr_id), Some(now)) = (
            as_str(pkx),
            as_str(pky),
            as_str(doc_type),
            as_str(namespace),
            as_str(attr_id),
            as_str(now),
        ) else {
            return (ZKV_ERR_ARGS, "null or non-UTF-8 string argument".to_string());
        };
        let (Some(transcript), Some(attr_cbor), Some(proof)) = (
            as_bytes(transcript, transcript_len),
            as_bytes(attr_cbor, attr_cbor_len),
            as_bytes(proof, proof_len),
        ) else {
            return (ZKV_ERR_ARGS, "null buffer argument".to_string());
        };
        if let Err(msg) = check_input_sizes(version, now, attr_id, attr_cbor) {
            return (ZKV_ERR_ARGS, msg.to_string());
        }

        let circuit = match provider::materialize(version as usize, num_attributes as usize) {
            Ok(c) => c,
            Err(e) => {
                return (
                    ZKV_ERR_CIRCUIT,
                    format!("no circuit for version {version} / {num_attributes} attrs: {e:?}"),
                )
            }
        };

        let attrs: Vec<RequestedAttribute> = vec![req_attr(namespace, attr_id, attr_cbor)];
        match run_mdoc_verifier_inner(
            &circuit.spec,
            &circuit.compressed,
            pkx,
            pky,
            transcript,
            &attrs,
            now,
            doc_type,
            proof,
        ) {
            Ok(()) => (ZKV_OK, String::new()),
            Err(e) => (ZKV_ERR_VERIFY, format!("{e:?}")),
        }
    }));

    match result {
        Ok((ZKV_OK, _)) => ZKV_OK,
        Ok((code, msg)) => {
            set_err(err, err_len, &msg);
            code
        }
        Err(_) => {
            set_err(err, err_len, "panic in verifier");
            ZKV_ERR_PANIC
        }
    }
}

/// Write the hex circuit hash for `(version, num_attributes)` into `out`.
///
/// A relying party is required to check this against a published allowlist
/// before trusting a proof (EE-EUDIW-TS-1.0, EE-ZKP-023).
///
/// # Safety
/// `out` must be valid for `out_len` bytes.
#[no_mangle]
pub unsafe extern "C" fn zkv_circuit_hash(
    version: u32,
    num_attributes: u32,
    out: *mut c_char,
    out_len: usize,
) -> c_int {
    let result = catch_unwind(AssertUnwindSafe(|| {
        provider::materialize(version as usize, num_attributes as usize)
            .map(|c| c.spec.combined_hash_hex())
    }));
    match result {
        Ok(Ok(hash)) => {
            if out_len <= hash.len() {
                return ZKV_ERR_ARGS;
            }
            set_err(out, out_len, &hash);
            ZKV_OK
        }
        Ok(Err(_)) => ZKV_ERR_CIRCUIT,
        Err(_) => ZKV_ERR_PANIC,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn oversize_inputs_are_rejected_not_aborted() {
        let mut err = [0 as c_char; 128];
        let call = |now: &str, id: &str, val: &[u8], err: &mut [c_char; 128]| unsafe {
            let c = |s: &str| std::ffi::CString::new(s).unwrap();
            let (pk, dt, ns, id, now) = (c("0x1"), c("org.iso.18013.5.1.mDL"), c("org.iso.18013.5.1"), c(id), c(now));
            zkv_verify(7, 1, pk.as_ptr(), pk.as_ptr(), [0u8].as_ptr(), 1, dt.as_ptr(), ns.as_ptr(),
                id.as_ptr(), val.as_ptr(), val.len(), now.as_ptr(), [0u8].as_ptr(), 1, err.as_mut_ptr(), 128)
        };
        assert_eq!(call("2026-09-25T09:00:00+03:00", "age_over_18", &[0xf5], &mut err), ZKV_ERR_ARGS);
        assert_eq!(call("2026-09-25T09:00:00Z", &"a".repeat(31), &[0xf5], &mut err), ZKV_ERR_ARGS);
        assert_eq!(call("2026-09-25T09:00:00Z", "age_over_18", &[0u8; 65], &mut err), ZKV_ERR_ARGS);
        assert!(check_input_sizes(7, "2026-09-25T09:00:00Z", &"a".repeat(30), &[0u8; 64]).is_ok());
    }
}
