package jose

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/ecdh"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
)

// The one profile this package speaks (ADR-003). Anything else is
// ErrUnsupported — a refusal, never a fallback.
const (
	algHeader = "ECDH-ES+A256KW"
	encHeader = "A256GCM"
)

// ErrUnsupported marks an algorithm, curve or key type outside the profile.
var ErrUnsupported = errors.New("jose: unsupported outside the ECDH-ES+A256KW / A256GCM / P-256 profile")

// ErrDecrypt covers every integrity failure: bad tag, bad wrap, malformed
// token. The distinctions are in the wrapped error for the log, never in
// behaviour — a caller cannot branch on *why* a token failed.
var ErrDecrypt = errors.New("jose: decrypt failed")

// ErrParse marks a token that is not well-formed compact JWE at all.
var ErrParse = errors.New("jose: not a compact JWE")

// Public returns the public half of a keypair JWK, dropping any private
// material. Used everywhere a key leaves the process.
func (j JWK) Public() JWK {
	j.D = ""
	return j
}

// b64 is the base64url-without-padding encoding JOSE mandates everywhere.
func b64(b []byte) string { return base64.RawURLEncoding.EncodeToString(b) }

func unb64(s string) ([]byte, error) {
	return base64.RawURLEncoding.DecodeString(s)
}

// aesKeyWrap is RFC 3394 with AES-256, exactly as the §4.6 vector pins it.
// in must be a multiple of 8 bytes (the CEK is 32); kek must be 32 bytes.
func aesKeyWrap(kek, in []byte) ([]byte, error) {
	if len(kek) != 32 {
		return nil, fmt.Errorf("jose: key wrap kek is %d bytes, want 32", len(kek))
	}
	if len(in) < 16 || len(in)%8 != 0 {
		return nil, fmt.Errorf("jose: key wrap input is %d bytes, want a multiple of 8 at least 16", len(in))
	}
	block, err := aes.NewCipher(kek)
	if err != nil {
		return nil, fmt.Errorf("jose: key wrap: %w", err)
	}
	n := len(in) / 8
	a := []byte{0xA6, 0xA6, 0xA6, 0xA6, 0xA6, 0xA6, 0xA6, 0xA6} // default IV
	r := make([]byte, len(in))
	copy(r, in)
	buf := make([]byte, 16)
	for j := 0; j <= 5; j++ {
		for i := 1; i <= n; i++ {
			// B = AES(K, A | R[i]); A = MSB64(B) ^ t; R[i] = LSB64(B)
			copy(buf[:8], a)
			copy(buf[8:], r[(i-1)*8:i*8])
			block.Encrypt(buf, buf)
			copy(a, buf[:8])
			t := uint64(n*j + i)
			for s := 0; s < 8; s++ {
				a[7-s] ^= byte(t >> (8 * s))
			}
			copy(r[(i-1)*8:i*8], buf[8:])
		}
	}
	return append(a, r...), nil
}

// aesKeyUnwrap inverts aesKeyWrap and checks the integrity register; any
// mismatch is a hard failure (RFC 3394 §3).
func aesKeyUnwrap(kek, in []byte) ([]byte, error) {
	if len(kek) != 32 {
		return nil, fmt.Errorf("jose: key unwrap kek is %d bytes, want 32", len(kek))
	}
	if len(in) < 24 || len(in)%8 != 0 {
		return nil, fmt.Errorf("jose: key unwrap input is %d bytes, want a multiple of 8 at least 24", len(in))
	}
	block, err := aes.NewCipher(kek)
	if err != nil {
		return nil, fmt.Errorf("jose: key unwrap: %w", err)
	}
	n := len(in)/8 - 1
	a := make([]byte, 8)
	copy(a, in[:8])
	r := make([]byte, len(in)-8)
	copy(r, in[8:])
	buf := make([]byte, 16)
	for j := 5; j >= 0; j-- {
		for i := n; i >= 1; i-- {
			t := uint64(n*j + i)
			copy(buf[:8], a)
			copy(buf[8:], r[(i-1)*8:i*8])
			for s := 0; s < 8; s++ {
				buf[7-s] ^= byte(t >> (8 * s))
			}
			block.Decrypt(buf, buf)
			copy(a, buf[:8])
			copy(r[(i-1)*8:i*8], buf[8:])
		}
	}
	// Constant-time: the integrity register is a MAC-like check (RFC 3394 §2.2.3).
	if subtle.ConstantTimeCompare(a, []byte{0xA6, 0xA6, 0xA6, 0xA6, 0xA6, 0xA6, 0xA6, 0xA6}) != 1 {
		return nil, errors.New("jose: key unwrap integrity check failed")
	}
	return r, nil
}

// parseHeader decodes and checks the received JOSE header: the only shape this
// package will emit, with members whose mere presence is a refusal.

func parseHeader(raw []byte) (epk JWK, err error) {
	var h struct {
		Alg string          `json:"alg"`
		Enc string          `json:"enc"`
		Epk *JWK            `json:"epk"`
		Kid string          `json:"kid"`
		Zip string          `json:"zip"`
		D   json.RawMessage `json:"d"`
		Jku string          `json:"jku"`
		X5u string          `json:"x5u"`
		Jwk json.RawMessage `json:"jwk"`
	}
	if err := json.Unmarshal(raw, &h); err != nil {
		return JWK{}, fmt.Errorf("%w: header is not valid JSON: %v", ErrParse, err)
	}
	if h.Alg != algHeader || h.Enc != encHeader {
		return JWK{}, fmt.Errorf("%w: alg/enc %q/%q", ErrUnsupported, h.Alg, h.Enc)
	}
	if h.Zip != "" {
		return JWK{}, fmt.Errorf("%w: zip is not part of the profile", ErrUnsupported)
	}
	if h.D != nil {
		return JWK{}, errors.New("jose: header carries a private key member (d)")
	}
	if h.Jku != "" || h.X5u != "" || h.Jwk != nil {
		return JWK{}, fmt.Errorf("%w: remote key members (jku/x5u/jwk) are refused", ErrUnsupported)
	}
	if h.Epk == nil {
		return JWK{}, fmt.Errorf("%w: header has no epk", ErrParse)
	}
	return *h.Epk, nil
}

// Encrypt seals plaintext to the recipient's P-256 public JWK under the
// profile header, returning the compact JWE. kid (the thumbprint b64u) is
// optional but recommended so the wallet can match keys.
func Encrypt(recipientJWK JWK, plaintext []byte) (string, error) {
	tp, err := recipientJWK.Thumbprint()
	if err != nil {
		return "", err
	}
	hdr, err := json.Marshal(struct {
		Alg string `json:"alg"`
		Enc string `json:"enc"`
		Epk *JWK   `json:"epk"`
		Kid string `json:"kid"`
	}{Alg: algHeader, Enc: encHeader, Epk: nil, Kid: b64(tp)})
	if err != nil {
		return "", err
	}
	return encryptWithHeader(recipientJWK, plaintext, hdr)
}

// encryptWithHeader is Encrypt with a caller-chosen protected header — the
// seam the negative tests drive unsupported profiles through. Ephemeral key
// generation and the epk member are handled here, not by the caller.
func encryptWithHeader(recipientJWK JWK, plaintext []byte, hdr []byte) (string, error) {
	// Reject unsupported profiles at encryption time too, before any work.
	var probe struct {
		Alg string          `json:"alg"`
		Enc string          `json:"enc"`
		D   json.RawMessage `json:"d"`
	}
	if err := json.Unmarshal(hdr, &probe); err != nil {
		return "", fmt.Errorf("%w: header is not valid JSON: %v", ErrParse, err)
	}
	if probe.Alg != algHeader || probe.Enc != encHeader {
		return "", fmt.Errorf("%w: alg/enc %q/%q", ErrUnsupported, probe.Alg, probe.Enc)
	}
	if probe.D != nil {
		return "", errors.New("jose: header carries a private key member (d)")
	}

	recipientPub, err := recipientJWK.ECDHKey()
	if err != nil {
		return "", err
	}
	eph, err := ecdh.P256().GenerateKey(rand.Reader)
	if err != nil {
		return "", fmt.Errorf("jose: ephemeral key: %w", err)
	}
	epkJWK := JWK{
		Kty: "EC", Crv: "P-256",
		X: b64(eph.PublicKey().Bytes()[1:33]),
		Y: b64(eph.PublicKey().Bytes()[33:65]),
	}

	// ECDH-ES: the KEK for the wrap is ConcatKDF(sha256, Z, alg, apu=∅, apv=∅) — the
	// header carries neither member, so RFC 7518 §4.6.2 makes both empty.
	shared, err := eph.ECDH(recipientPub)
	if err != nil {
		return "", fmt.Errorf("jose: ecdh: %w", err)
	}
	kek, err := concatKDF(shared, []byte(algHeader), 256)
	if err != nil {
		return "", err
	}

	// Key wrap the fresh CEK.
	cek := make([]byte, 32)
	if _, err := rand.Read(cek); err != nil {
		return "", fmt.Errorf("jose: cek: %w", err)
	}
	wrapped, err := aesKeyWrap(kek, cek)
	if err != nil {
		return "", err
	}

	// Inject the real epk into the header before hashing it.
	var hdrMap map[string]json.RawMessage
	if err := json.Unmarshal(hdr, &hdrMap); err != nil {
		return "", fmt.Errorf("%w: header is not valid JSON: %v", ErrParse, err)
	}
	epkJSON, err := json.Marshal(epkJWK)
	if err != nil {
		return "", err
	}
	hdrMap["epk"] = epkJSON
	hdrJSON, err := json.Marshal(hdrMap)
	if err != nil {
		return "", err
	}
	hdrB64 := b64(hdrJSON)

	// A256GCM over the plaintext with hdrB64 as the extra authenticated data.
	iv := make([]byte, 12)
	if _, err := rand.Read(iv); err != nil {
		return "", fmt.Errorf("jose: iv: %w", err)
	}
	gcm, err := newGCM(cek)
	if err != nil {
		return "", err
	}
	sealed := gcm.Seal(nil, iv, plaintext, []byte(hdrB64))
	ct, tag := sealed[:len(sealed)-16], sealed[len(sealed)-16:]

	return strings.Join([]string{
		hdrB64,
		b64(wrapped),
		b64(iv),
		b64(ct),
		b64(tag),
	}, "."), nil
}

// Decrypt opens a compact JWE encrypted to recipient under the profile.
// It returns the plaintext whole or an error; there is no partial result.
func Decrypt(recipient *JWK, token string) ([]byte, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 5 {
		return nil, fmt.Errorf("%w: %d segments, want 5", ErrParse, len(parts))
	}
	hdrJSON, err := unb64(parts[0])
	if err != nil {
		return nil, fmt.Errorf("%w: header is not base64url: %v", ErrParse, err)
	}
	epk, err := parseHeader(hdrJSON)
	if err != nil {
		return nil, err
	}
	wrapped, err := unb64(parts[1])
	if err != nil {
		return nil, fmt.Errorf("%w: encrypted key is not base64url: %v", ErrParse, err)
	}
	iv, err := unb64(parts[2])
	if err != nil {
		return nil, fmt.Errorf("%w: iv is not base64url: %v", ErrParse, err)
	}
	ct, err := unb64(parts[3])
	if err != nil {
		return nil, fmt.Errorf("%w: ciphertext is not base64url: %v", ErrParse, err)
	}
	tag, err := unb64(parts[4])
	if err != nil {
		return nil, fmt.Errorf("%w: tag is not base64url: %v", ErrParse, err)
	}

	if len(iv) != 12 {
		return nil, fmt.Errorf("%w: iv is %d bytes, want 12", ErrDecrypt, len(iv))
	}
	if len(tag) != 16 {
		return nil, fmt.Errorf("%w: tag is %d bytes, want 16", ErrDecrypt, len(tag))
	}
	if len(wrapped) != 40 { // 32-byte CEK + 8-byte wrap register
		return nil, fmt.Errorf("%w: wrapped key is %d bytes, want 40", ErrDecrypt, len(wrapped))
	}

	priv, err := recipient.PrivateECDHKey()
	if err != nil {
		return nil, err
	}
	ephPub, err := epk.ECDHKey()
	if err != nil {
		return nil, fmt.Errorf("%w: %v", ErrDecrypt, err)
	}
	shared, err := priv.ECDH(ephPub)
	if err != nil {
		return nil, fmt.Errorf("%w: ecdh: %v", ErrDecrypt, err)
	}
	// ConcatKDF's Otherinfo: alg | apu(∅) | apv(∅) — neither member is in the header.
	kek, err := concatKDF(shared, []byte(algHeader), 256)
	if err != nil {
		return nil, err
	}
	cek, err := aesKeyUnwrap(kek, wrapped)
	if err != nil {
		return nil, fmt.Errorf("%w: %v", ErrDecrypt, err)
	}

	gcm, err := newGCM(cek)
	if err != nil {
		return nil, err
	}
	plain, err := gcm.Open(nil, iv, append(append([]byte{}, ct...), tag...), []byte(parts[0]))
	if err != nil {
		return nil, fmt.Errorf("%w: authentication failed", ErrDecrypt)
	}
	return plain, nil
}

func newGCM(key []byte) (cipher.AEAD, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, fmt.Errorf("jose: gcm: %w", err)
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return nil, fmt.Errorf("jose: gcm: %w", err)
	}
	return aead, nil
}

// concatKDF is the NIST SP 800-56A Concat KDF per RFC 7518 §4.6.2 with
// SHA-256. Otherinfo = len(alg) | alg | len(apu)=0 | apu(∅) | len(apv)=0 |
// apv(∅) | SuppPubInfo = 32-bit key length — every field of §4.6.2 is
// present, including the key-length round-trip jwcrypto and every
// conformant JOSE library write.
func concatKDF(z, alg []byte, keyLenBits int) ([]byte, error) {
	otherinfo := make([]byte, 0, 4+len(alg)+4+4+4)
	var u32 [4]byte
	binary.BigEndian.PutUint32(u32[:], uint32(len(alg)))
	otherinfo = append(otherinfo, u32[:]...)
	otherinfo = append(otherinfo, alg...)
	binary.BigEndian.PutUint32(u32[:], 0) // apu length: empty
	otherinfo = append(otherinfo, u32[:]...)
	binary.BigEndian.PutUint32(u32[:], 0) // apv length: empty
	otherinfo = append(otherinfo, u32[:]...)
	binary.BigEndian.PutUint32(u32[:], uint32(keyLenBits)) // SuppPubInfo
	otherinfo = append(otherinfo, u32[:]...)

	keyLen := keyLenBits / 8
	out := make([]byte, 0, keyLen)
	for counter := 1; len(out) < keyLen; counter++ {
		h := sha256.New()
		var c [4]byte
		c[3] = byte(counter)
		h.Write(c[:])
		h.Write(z)
		h.Write(otherinfo)
		out = h.Sum(out)
	}
	return out[:keyLen], nil
}
