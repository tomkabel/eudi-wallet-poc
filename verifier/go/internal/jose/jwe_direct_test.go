package jose

import (
	"crypto/ecdh"
	"crypto/rand"
	"encoding/json"
	"strings"
	"testing"
)

// directToken builds a compact JWE under the HAIP profile the wallet actually
// sends: alg=ECDH-ES (direct key agreement, no key wrap), enc=A256GCM, empty
// encrypted-key segment, CEK = ConcatKDF(SHA-256, Z, "ECDH-ES", keylen=256).
//
// It is assembled here from the RFC 7518 §4.6 steps rather than by calling the
// code under test, so a pass cannot be an artefact of the implementation
// agreeing with itself: the only shared helpers are the RFC-pinned primitives
// (b64, concatKDF, newGCM) whose vectors are tested elsewhere.
func directToken(t *testing.T, recipient JWK, plaintext []byte) (string, error) {
	t.Helper()
	recipPub, err := recipient.ECDHKey()
	if err != nil {
		t.Fatalf("recipient key: %v", err)
	}
	eph, err := ecdh.P256().GenerateKey(rand.Reader)
	if err != nil {
		t.Fatalf("ephemeral: %v", err)
	}
	pub := eph.PublicKey().Bytes()
	epk := JWK{Kty: "EC", Crv: "P-256", X: b64(pub[1:33]), Y: b64(pub[33:65])}

	shared, err := eph.ECDH(recipPub)
	if err != nil {
		t.Fatalf("ecdh: %v", err)
	}
	// Direct agreement: the derived key IS the CEK, and the algorithm
	// identifier in OtherInfo is "ECDH-ES" (not "ECDH-ES+A256KW").
	cek, err := concatKDF(shared, []byte("ECDH-ES"), 256)
	if err != nil {
		t.Fatalf("kdf: %v", err)
	}

	hdrJSON, err := json.Marshal(map[string]any{
		"alg": "ECDH-ES", "enc": "A256GCM", "epk": epk,
	})
	if err != nil {
		t.Fatalf("header: %v", err)
	}
	hdrB64 := b64(hdrJSON)

	iv := make([]byte, 12)
	if _, err := rand.Read(iv); err != nil {
		t.Fatalf("iv: %v", err)
	}
	gcm, err := newGCM(cek)
	if err != nil {
		t.Fatalf("gcm: %v", err)
	}
	sealed := gcm.Seal(nil, iv, plaintext, []byte(hdrB64))
	ct, tag := sealed[:len(sealed)-16], sealed[len(sealed)-16:]

	// The encrypted-key segment is empty under direct agreement.
	return strings.Join([]string{hdrB64, "", b64(iv), b64(ct), b64(tag)}, "."), nil
}

// TestDirectECDHES is the red-first gate for the recorded HAIP deviation: the
// wallet sends alg=ECDH-ES with an empty encrypted-key segment, and the
// verifier must open it.
func TestDirectECDHES(t *testing.T) {
	recipient, err := GenerateKey()
	if err != nil {
		t.Fatalf("keygen: %v", err)
	}
	plaintext := []byte(`{"vp_token":"stub","presentation_submission":{}}`)

	token, err := directToken(t, recipient, plaintext)
	if err != nil {
		t.Fatalf("build: %v", err)
	}
	if parts := strings.Split(token, "."); len(parts) != 5 || parts[1] != "" {
		t.Fatalf("test token is not direct-agreement shaped: %d parts, ek=%d bytes",
			len(parts), len(parts[1]))
	}

	got, err := Decrypt(&recipient, token)
	if err != nil {
		t.Fatalf("Decrypt(direct ECDH-ES) = %v, want the plaintext", err)
	}
	if string(got) != string(plaintext) {
		t.Fatalf("plaintext = %q, want %q", got, plaintext)
	}
}

// TestDirectECDHESRejectsWrappedKey: alg=ECDH-ES with a non-empty encrypted-key
// segment is a contradictory token (direct agreement defines no key wrapping).
// Accepting it would mean two different readings of the same header.
func TestDirectECDHESRejectsWrappedKey(t *testing.T) {
	recipient, err := GenerateKey()
	if err != nil {
		t.Fatalf("keygen: %v", err)
	}
	token, err := directToken(t, recipient, []byte("x"))
	if err != nil {
		t.Fatalf("build: %v", err)
	}
	parts := strings.Split(token, ".")
	parts[1] = b64(make([]byte, 40)) // a wrapped-CEK-shaped segment
	if _, err := Decrypt(&recipient, strings.Join(parts, ".")); err == nil {
		t.Fatal("accepted alg=ECDH-ES with a non-empty encrypted key")
	}
}

// TestEncryptDirectRoundTrip keeps the emitting side symmetric with the
// accepting side so the verifier can encrypt to a wallet that only speaks the
// HAIP profile.
func TestEncryptDirectRoundTrip(t *testing.T) {
	recipient, err := GenerateKey()
	if err != nil {
		t.Fatalf("keygen: %v", err)
	}
	msg := []byte("public key profile pin")
	token, err := EncryptDirect(recipient.Public(), msg)
	if err != nil {
		t.Fatalf("EncryptDirect: %v", err)
	}
	parts := strings.Split(token, ".")
	if len(parts) != 5 || parts[1] != "" {
		t.Fatalf("EncryptDirect produced a wrapped-key JWE: %v parts", len(parts))
	}
	got, err := Decrypt(&recipient, token)
	if err != nil {
		t.Fatalf("Decrypt: %v", err)
	}
	if string(got) != string(msg) {
		t.Fatalf("round trip = %q, want %q", got, msg)
	}
}

// TestKeyWrapProfileStillWorks guards the compatibility promise: existing
// ECDH-ES+A256KW tokens must keep opening after direct agreement is added.
func TestKeyWrapProfileStillWorks(t *testing.T) {
	recipient, err := GenerateKey()
	if err != nil {
		t.Fatalf("keygen: %v", err)
	}
	msg := []byte("legacy profile")
	token, err := Encrypt(recipient.Public(), msg)
	if err != nil {
		t.Fatalf("Encrypt: %v", err)
	}
	got, err := Decrypt(&recipient, token)
	if err != nil {
		t.Fatalf("Decrypt: %v", err)
	}
	if string(got) != string(msg) {
		t.Fatalf("round trip = %q, want %q", got, msg)
	}
}

// TestPublishedKeyDeclaresDirectAlg pins the other half of the recorded HAIP
// deviation: the response key handed to the wallet must name the profile it
// speaks. The RFC 7638 thumbprint must NOT change when it does — alg is not a
// thumbprint member.
func TestPublishedKeyDeclaresDirectAlg(t *testing.T) {
	k, err := GenerateKey()
	if err != nil {
		t.Fatalf("keygen: %v", err)
	}
	pub := k.Public()
	raw, err := json.Marshal(pub)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	var back JWK
	if err := json.Unmarshal(raw, &back); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if back.Alg != "ECDH-ES" {
		t.Fatalf("published key alg = %q, want ECDH-ES", back.Alg)
	}
	if back.D != "" {
		t.Fatal("published key carries private material")
	}
	tp, err := k.Thumbprint()
	if err != nil {
		t.Fatalf("thumbprint: %v", err)
	}
	tp2, err := back.Thumbprint()
	if err != nil {
		t.Fatalf("thumbprint: %v", err)
	}
	if string(tp) != string(tp2) {
		t.Fatal("thumbprint changed when alg was added")
	}
}
