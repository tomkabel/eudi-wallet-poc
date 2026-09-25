package oid4vp

import (
	"crypto/ecdh"
	"fmt"

	"github.com/tomkabel/ee-eudiw/verifier/go/circuits"
)

// The Digital Credentials API extension of the presentation session, plan §5.1
// step 1. Everything the ISO 18013-7 Annex C path verifies against is minted
// at session creation: the HPKE key pair, the EncryptionInfo the wallet will
// hash, and the ZkSystemSpecs the wallet may name in its response. A response
// that names anything else is refused before any crypto runs.

// ISOExtension carries the dcapi-specific session state.
type ISOExtension struct {
	// HPKEPrivate is the session's HPKE P-256 private key.
	HPKEPrivate *ecdh.PrivateKey
	// EncryptionInfoB64 is the base64url of the encoded EncryptionInfo —
	// exactly the string the page sends the wallet and the handover hashes.
	EncryptionInfoB64 string
	// OfferedSpecIDs are the ZkSystemSpec ids offered in THIS session, in the
	// order they were offered. A zkDocument naming an id outside this list is
	// refused before the FFI is touched (plan §5.3).
	OfferedSpecIDs []string
	// BySpecID maps each offered id to its registry entry.
	BySpecID map[string]circuits.Circuit
}

// SessionISO returns the dcapi extension of a session, or nil for a session
// created without one.
func (s *Session) SessionISO() *ISOExtension {
	if s == nil || s.iso == nil {
		return nil
	}
	return s.iso
}

// ISOOrigin returns the configured dcapi origin a session was created under.
func (s *Session) ISOOrigin() string { return s.origin }

// NewISO creates a presentation session carrying the dcapi extension. The
// redirect path keeps using New; nothing about New changes.
func (st *Store) NewISO(clientID, responseURIBase string, q DCQL, origin string, offered []circuits.Circuit) (*Session, error) {
	if origin == "" {
		return nil, fmt.Errorf("oid4vp: the dcapi origin is required (it is hashed into the handover)")
	}
	s, err := st.New(clientID, responseURIBase, q)
	if err != nil {
		return nil, err
	}
	priv, _, infoB64, err := NewHPKEKeyPair()
	if err != nil {
		return nil, err
	}
	ext := &ISOExtension{
		HPKEPrivate:       priv,
		EncryptionInfoB64: infoB64,
		OfferedSpecIDs:    make([]string, 0, len(offered)),
		BySpecID:          make(map[string]circuits.Circuit, len(offered)),
	}
	for _, c := range offered {
		id := c.SpecID()
		if _, dup := ext.BySpecID[id]; dup {
			return nil, fmt.Errorf("oid4vp: duplicate offered spec id %q", id)
		}
		ext.BySpecID[id] = c
		ext.OfferedSpecIDs = append(ext.OfferedSpecIDs, id)
	}
	if err := st.AttachISO(s.ID, ext, origin); err != nil {
		return nil, err
	}
	// Re-read: s is a snapshot from New, so it lacks the extension. The stored
	// session has it; hand the caller the fresh snapshot.
	return st.Get(s.ID)
}
