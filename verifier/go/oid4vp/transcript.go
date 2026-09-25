// Package oid4vp implements the verifier half of an OpenID4VP 1.0 presentation
// over ISO/IEC 18013-5 mdocs, including the session transcript the holder's
// device signature is bound to.
package oid4vp

import (
	"crypto/sha256"
	"errors"
	"fmt"
)

// Flow selects one transcript derivation. The two flows are NEVER
// interchangeable — this is the F2 replay lesson made structural: a proof
// bound to the OpenID4VP redirect handover does not verify over the ISO dcapi
// handover and vice versa (asserted byte-for-byte in both language halves),
// and TranscriptForFlow is the only door to the transcript bytes, so a caller
// cannot accidentally mix what it hashes. Carriers declare which flow their
// proofs bind (carrier.go), and the verifier derives the transcript for
// exactly that flow from its own session data — never from the response.
type Flow int

const (
	// FlowRedirectB261 is OpenID4VP 1.0 Appendix B.2.6.1, invocation via
	// redirects: [null, null, ["OpenID4VPHandover", h]] — the transcript
	// both the interim-JSON envelope and the de-facto mso_mdoc_zk CBOR
	// carrier bind (fixture-verified both directions, step 8.7).
	FlowRedirectB261 Flow = iota
	// FlowDcapiISO is ISO/IEC 18013-7 Annex C over the Digital Credentials
	// API: [null, null, ["dcapi", h]] — the transcript the ISO dcapi path
	// binds.
	FlowDcapiISO
)

// TranscriptParams are the verifier-side inputs one flow's derivation needs.
// Which fields matter depends on the flow: FlowRedirectB261 hashes
// client_id/nonce/jwkThumbprint/response_uri, FlowDcapiISO hashes
// encryptionInfoB64/origin. Everything here is a verifier fact from the stored
// session; nothing is ever taken from the response.
type TranscriptParams struct {
	ClientID      string
	Nonce         string
	ResponseURI   string
	JWKThumbprint []byte // RFC 7638 thumbprint; nil for the unencrypted direct_post

	EncryptionInfoB64 string // the dcapi handover's base64url EncryptionInfo
	Origin            string // the dcapi origin the session was created under
}

// TranscriptForFlow is the single entry point over the two transcript
// derivations this verifier knows. Every caller names the flow it wants and
// supplies verifier-side facts; the caller never picks between byte layouts by
// hand. The two flows are never interchangeable: for the same inputs they
// hash different handover bytes, so a proof made for one flow cannot be
// replayed over the other — a transcript-registry test pins that both
// directions fail, and the carrier's TranscriptFlow field keeps response
// handling on the one flow its proof was made for.
func TranscriptForFlow(f Flow, p TranscriptParams) ([]byte, error) {
	switch f {
	case FlowRedirectB261:
		return SessionTranscript(p.ClientID, p.Nonce, p.JWKThumbprint, p.ResponseURI)
	case FlowDcapiISO:
		return ISOTranscript(p.EncryptionInfoB64, p.Origin)
	default:
		return nil, fmt.Errorf("oid4vp: no transcript derivation for flow %d", int(f))
	}
}

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
