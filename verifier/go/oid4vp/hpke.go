package oid4vp

import (
	"crypto/ecdh"
	"crypto/hpke"
	"encoding/base64"
)

// HPKE ciphersuite, fixed: DHKEM(P-256, HKDF-SHA256), HKDF-SHA256,
// AES-128-GCM — the suite multipaz's Hpke.CipherSuite
// DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM names on the other side.
var (
	hpkeKEM  = hpke.DHKEM(ecdh.P256())
	hpkeKDF  = hpke.HKDFSHA256()
	hpkeAEAD = hpke.AES128GCM()
)

// hpkeEncSize is the DHKEM(P-256) encapsulated-key length: an X9.62 point.
const hpkeEncSize = 65

func encryptionInfoCBOR(e EncryptionInfo) []byte {
	m := []byte{0xa2}
	m = append(m, cborText("nonce")...)
	m = append(m, cborBytes(e.Nonce)...)
	m = append(m, cborText("recipientPublicKey")...)
	// A bare COSE_Key map, not tag 24 over a bstr: multipaz 0.99.0 writes it
	// bare and reads it with DataItem.getAsCoseKey, which requires a CborMap.
	m = append(m, e.RecipientPublicKey...)
	arr := append([]byte{0x82}, cborText("dcapi")...)
	return append(arr, m...)
}

// Open is the receiving half of the single-shot HPKE the wallet performs:
// ciphertext = enc || ct, info = the SessionTranscript CBOR, aad empty (the
// PoC's own comment at DigitalCredentialsViewModel.kt:424-432 explains why the
// transcript belongs in info and not the aad). One call, one message: the
// stateful Recipient context is not kept, so a second message under the same
// enc is refused rather than opened with a shifted nonce counter.
func Open(privateKey *ecdh.PrivateKey, info, ciphertext []byte) ([]byte, error) {
	if len(ciphertext) < hpkeEncSize+16 {
		return nil, fail("ciphertext is %d bytes, shorter than an enc plus an AES-128-GCM tag",
			len(ciphertext))
	}
	enc := ciphertext[:hpkeEncSize]
	ct := ciphertext[hpkeEncSize:]

	if hpkeKEM == nil || hpkeKDF == nil || hpkeAEAD == nil {
		return nil, fail("crypto/hpke is not available in this build")
	}
	recip, err := hpkeNewRecipient(enc, privateKey, info)
	if err != nil {
		return nil, fail("hpke: %v", err)
	}
	pt, err := recip.Open(nil, ct)
	if err != nil {
		return nil, fail("hpke open failed: %v", err)
	}
	return pt, nil
}

// SealTo is the sending half, used by tests to exercise Open with a fixed
// ephemeral key: it returns the single-shot ciphertext enc||ct.
func SealTo(recipientPublic *ecdh.PublicKey, info, plaintext []byte) ([]byte, error) {
	seal, err := hpke.Seal(mustHPKEPublicKey(recipientPublic), hpkeKDF, hpkeAEAD, info, plaintext)
	if err != nil {
		return nil, fail("hpke seal: %v", err)
	}
	return seal, nil
}

func mustHPKEPublicKey(pub *ecdh.PublicKey) hpke.PublicKey {
	pk, err := hpke.NewDHKEMPublicKey(pub)
	if err != nil {
		panic("hpke: " + err.Error())
	}
	return pk
}

func hpkeNewRecipient(enc []byte, priv *ecdh.PrivateKey, info []byte) (*hpke.Recipient, error) {
	rk, err := hpke.NewDHKEMPrivateKey(priv)
	if err != nil {
		return nil, err
	}
	return hpke.NewRecipient(enc, rk, hpkeKDF, hpkeAEAD, info)
}

// Base64URL encodes with padding absent, which is what the Digital Credentials
// API page and the PoC both use (Base64.UrlSafe.withPadding(ABSENT)).
func Base64URL(b []byte) string { return base64.RawURLEncoding.EncodeToString(b) }

// DecodeBase64URL is the inverse of Base64URL. Padded standard base64 is also
// accepted; anything else is a rejection.
func DecodeBase64URL(s string) ([]byte, error) {
	if b, err := base64.RawURLEncoding.DecodeString(s); err == nil {
		return b, nil
	}
	b, err := base64.StdEncoding.DecodeString(s)
	if err != nil {
		return nil, fail("value is not base64url")
	}
	return b, nil
}
