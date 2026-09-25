// Package zk verifies Longfellow zero-knowledge proofs over ISO/IEC 18013-5
// mdocs by calling the Longfellow Rust runtime through a small C ABI.
//
// Upstream's reference verifier links the C++ libmdoc_static. This links the
// Rust implementation instead, so the service builds with a Rust toolchain and
// cgo but no C++ toolchain.
package zk

/*
#cgo CFLAGS: -I${SRCDIR}/../../include
#cgo LDFLAGS: -L${SRCDIR}/../../zkverify-ffi/target/release -lzkverify -lm -ldl -lpthread
#include <stdlib.h>
#include "zkverify.h"
*/
import "C"

import (
	"errors"
	"fmt"
	"unsafe"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

const errBufLen = 512

// Request is everything needed to check one proof.
type Request struct {
	Version       uint32 `json:"version"`
	NumAttributes uint32 `json:"num_attributes"`
	PKx           string `json:"pkx"`
	PKy           string `json:"pky"`
	DocType       string `json:"doc_type"`
	Namespace     string `json:"namespace"`
	AttrID        string `json:"attr_id"`
	AttrCBOR      []byte `json:"-"`
	Now           string `json:"now"`
	Transcript    []byte `json:"-"`
	Proof         []byte `json:"-"`
}

// Errors returned by Verify, distinguishable so a caller can tell "this proof is
// not valid" from "this verifier is misconfigured".
var (
	ErrArgs    = errors.New("zk: invalid arguments")
	ErrCircuit = errors.New("zk: unknown circuit")
	ErrInvalid = errors.New("zk: proof did not verify")
	ErrPanic   = errors.New("zk: panic in verifier")
)

// bytePtr returns a pointer usable from C for a possibly empty slice.
func bytePtr(b []byte) *C.uint8_t {
	if len(b) == 0 {
		return nil
	}
	return (*C.uint8_t)(unsafe.Pointer(&b[0]))
}

// Verify checks the proof. A nil error means the proof is valid.
func Verify(r Request) error {
	cPKx, cPKy := C.CString(r.PKx), C.CString(r.PKy)
	cDoc, cNS := C.CString(r.DocType), C.CString(r.Namespace)
	cAttr, cNow := C.CString(r.AttrID), C.CString(r.Now)
	defer func() {
		for _, p := range []*C.char{cPKx, cPKy, cDoc, cNS, cAttr, cNow} {
			C.free(unsafe.Pointer(p))
		}
	}()

	errBuf := make([]byte, errBufLen)
	rc := C.zkv_verify(
		C.uint32_t(r.Version), C.uint32_t(r.NumAttributes),
		cPKx, cPKy,
		bytePtr(r.Transcript), C.size_t(len(r.Transcript)),
		cDoc, cNS, cAttr,
		bytePtr(r.AttrCBOR), C.size_t(len(r.AttrCBOR)),
		cNow,
		bytePtr(r.Proof), C.size_t(len(r.Proof)),
		(*C.char)(unsafe.Pointer(&errBuf[0])), C.size_t(errBufLen),
	)
	if rc == C.ZKV_OK {
		return nil
	}

	msg := C.GoString((*C.char)(unsafe.Pointer(&errBuf[0])))
	var base error
	switch rc {
	case C.ZKV_ERR_ARGS:
		base = ErrArgs
	case C.ZKV_ERR_CIRCUIT:
		base = ErrCircuit
	case C.ZKV_ERR_VERIFY:
		base = ErrInvalid
	default:
		base = ErrPanic
	}
	if msg == "" {
		return base
	}
	return fmt.Errorf("%w: %s", base, msg)
}

// CircuitHash returns the hex hash identifying a circuit, for allowlist checks.
func CircuitHash(version, numAttributes uint32) (string, error) {
	buf := make([]byte, 129)
	rc := C.zkv_circuit_hash(C.uint32_t(version), C.uint32_t(numAttributes),
		(*C.char)(unsafe.Pointer(&buf[0])), C.size_t(len(buf)))
	if rc != C.ZKV_OK {
		return "", fmt.Errorf("%w: version %d / %d attrs", ErrCircuit, version, numAttributes)
	}
	return C.GoString((*C.char)(unsafe.Pointer(&buf[0]))), nil
}

// CheckCircuit resolves the circuit for (version, numAttributes) and reports
// whether reg accepts it. It returns the hash only when a registry entry
// matched (so a caller can log a hash mismatch); an unmatched tuple is refused
// without hashing, and the hash is "".
//
// The (version, numAttributes) pair is matched against the registry BEFORE any
// hash is computed: CircuitHash shells into the FFI, and letting holder-supplied
// numbers drive that call made the redirect path a free CPU and crash-surface
// for unauthenticated input (plan defect S11, fixed 2026-09-24).
func CheckCircuit(reg *circuits.Registry, version, numAttributes uint32) (string, error) {
	// Match on the published tuple first; only an accepted entry earns an FFI
	// call to confirm its hash.
	found, ok := reg.Lookup(version, numAttributes)
	if !ok {
		return "", fmt.Errorf("%w: no accepted circuit for version %d with %d attributes",
			ErrCircuit, version, numAttributes)
	}
	hash, err := CircuitHash(version, numAttributes)
	if err != nil {
		return "", err
	}
	if hash != found.Hash {
		return hash, fmt.Errorf("%w: circuit for (%d, %d) hashes to %s, registry says %s",
			ErrCircuit, version, numAttributes, hash, found.Hash)
	}
	return hash, nil
}
