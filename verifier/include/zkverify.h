/* C ABI over the Longfellow Rust runtime. See verifier/zkverify-ffi/src/lib.rs. */
#ifndef EE_EUDIW_ZKVERIFY_H
#define EE_EUDIW_ZKVERIFY_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define ZKV_OK          0
#define ZKV_ERR_ARGS    1
#define ZKV_ERR_CIRCUIT 2
#define ZKV_ERR_VERIFY  3
#define ZKV_ERR_PANIC   4

/* Verify that a single attribute is present in an mdoc, in zero knowledge.
   Returns ZKV_OK, or an error code with a message written into `err`. */
int zkv_verify(uint32_t version, uint32_t num_attributes,
               const char *pkx, const char *pky,
               const uint8_t *transcript, size_t transcript_len,
               const char *doc_type, const char *namespace_, const char *attr_id,
               const uint8_t *attr_cbor, size_t attr_cbor_len,
               const char *now,
               const uint8_t *proof, size_t proof_len,
               char *err, size_t err_len);

/* Hex circuit hash for (version, num_attributes), for allowlist checks. */
int zkv_circuit_hash(uint32_t version, uint32_t num_attributes,
                     char *out, size_t out_len);

#ifdef __cplusplus
}
#endif
#endif /* EE_EUDIW_ZKVERIFY_H */
