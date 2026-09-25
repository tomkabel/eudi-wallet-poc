package oid4vp

import "errors"

// BuildDCAPIEnvelope encodes the response envelope the page posts:
//
//	["dcapi", {enc: bstr, cipherText: bstr}]
//
// enc is the 65-byte HPKE encapsulated key and cipherText the AES-128-GCM
// ciphertext — already split by the caller, because the split point IS the
// DHKEM(P-256) enc length and belongs to the writer, not the reader.
func BuildDCAPIEnvelope(enc, cipherText []byte) []byte {
	m := []byte{0xa2}
	m = append(m, cborText("enc")...)
	m = append(m, cborBytes(enc)...)
	m = append(m, cborText("cipherText")...)
	m = append(m, cborBytes(cipherText)...)
	arr := append([]byte{0x82}, cborText("dcapi")...)
	return append(arr, m...)
}

// ErrNoTestVector keeps test helpers honest about missing fixtures.
var ErrNoTestVector = errors.New("oid4vp: test vector missing")
