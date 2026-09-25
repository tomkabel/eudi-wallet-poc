// Package oid4vp implements the verifier half of an OpenID4VP 1.0 presentation
// over ISO/IEC 18013-5 mdocs, including the session transcript the holder's
// device signature is bound to.
package oid4vp

import (
	"crypto/sha256"
	"errors"
	"fmt"
)

// SessionTranscript builds the CBOR structure defined in OpenID4VP 1.0
// Appendix B.2.6.1 (invocation via redirects):
//
//	SessionTranscript = [null, null, OpenID4VPHandover]
//
//	OpenID4VPHandover = [
//	  "OpenID4VPHandover",
//	  OpenID4VPHandoverInfoHash        ; sha-256 of OpenID4VPHandoverInfoBytes
//	]
//	OpenID4VPHandoverInfoBytes = bstr .cbor OpenID4VPHandoverInfo
//	OpenID4VPHandoverInfo = [clientId, nonce, jwkThumbprint, responseUri]
//
// jwkThumbprint is the RFC 7638 SHA-256 thumbprint of the verifier's response
// encryption key when the response is encrypted (direct_post.jwt), and null
// otherwise — pass nil for the unencrypted case.
//
// This is what binds a presentation to one verifier and one nonce: a proof made
// for another client_id, another response_uri or another nonce hashes to a
// different transcript and will not verify.
func SessionTranscript(clientID, nonce string, jwkThumbprint []byte, responseURI string) ([]byte, error) {
	if clientID == "" || nonce == "" || responseURI == "" {
		return nil, errors.New("oid4vp: clientID, nonce and responseURI are all required")
	}
	info, err := handoverInfo(clientID, nonce, jwkThumbprint, responseURI)
	if err != nil {
		return nil, err
	}
	sum := sha256.Sum256(info)

	// OpenID4VPHandover = ["OpenID4VPHandover", h]
	handover := append([]byte{0x82}, cborText("OpenID4VPHandover")...)
	handover = append(handover, cborBytes(sum[:])...)

	// SessionTranscript = [null, null, OpenID4VPHandover]
	st := []byte{0x83, 0xf6, 0xf6}
	return append(st, handover...), nil
}

// handoverInfo returns the CBOR encoding of OpenID4VPHandoverInfo — the exact
// bytes that get hashed.
func handoverInfo(clientID, nonce string, jwkThumbprint []byte, responseURI string) ([]byte, error) {
	out := []byte{0x84} // array(4)
	out = append(out, cborText(clientID)...)
	out = append(out, cborText(nonce)...)
	if jwkThumbprint == nil {
		out = append(out, 0xf6) // null: response is not encrypted
	} else {
		if len(jwkThumbprint) != sha256.Size {
			return nil, fmt.Errorf("oid4vp: jwkThumbprint must be %d bytes, got %d",
				sha256.Size, len(jwkThumbprint))
		}
		out = append(out, cborBytes(jwkThumbprint)...)
	}
	out = append(out, cborText(responseURI)...)
	return out, nil
}

// cborHead encodes a CBOR major type and argument using the shortest form,
// which is what deterministic encoding requires.
func cborHead(major byte, n uint64) []byte {
	mt := major << 5
	switch {
	case n < 24:
		return []byte{mt | byte(n)}
	case n < 1<<8:
		return []byte{mt | 24, byte(n)}
	case n < 1<<16:
		return []byte{mt | 25, byte(n >> 8), byte(n)}
	case n < 1<<32:
		return []byte{mt | 26, byte(n >> 24), byte(n >> 16), byte(n >> 8), byte(n)}
	default:
		b := []byte{mt | 27}
		for s := 56; s >= 0; s -= 8 {
			b = append(b, byte(n>>uint(s)))
		}
		return b
	}
}

func cborText(s string) []byte  { return append(cborHead(3, uint64(len(s))), s...) }
func cborBytes(b []byte) []byte { return append(cborHead(2, uint64(len(b))), b...) }
