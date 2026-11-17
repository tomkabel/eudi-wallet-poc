// circuit-list signs the community-maintained circuit document. It reads only
// an operator-supplied PKCS#8 Ed25519 key; no private key is generated in-tree.
package main

import (
	"crypto/ed25519"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

func main() {
	source := flag.String("source", "", "unsigned document JSON, including envelope")
	keyPath := flag.String("key", "", "PKCS#8 Ed25519 PEM outside checkout")
	keyID := flag.String("key-id", "", "public release key identifier")
	output := flag.String("out", "", "signed document output path")
	publicOut := flag.String("public-out", "", "public pin JSON output path")
	flag.Parse()
	if *source == "" || *keyPath == "" || *keyID == "" || *output == "" || *publicOut == "" {
		fatal(fmt.Errorf("source, key, key-id, out and public-out are required"))
	}
	data, err := os.ReadFile(*source)
	if err != nil {
		fatal(err)
	}
	doc, err := circuits.DecodeDocument(data)
	if err != nil {
		fatal(err)
	}
	raw, err := os.ReadFile(*keyPath)
	if err != nil {
		fatal(err)
	}
	block, _ := pem.Decode(raw)
	if block == nil {
		fatal(fmt.Errorf("expected PEM private key"))
	}
	parsed, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		fatal(err)
	}
	key, ok := parsed.(ed25519.PrivateKey)
	if !ok {
		fatal(fmt.Errorf("expected Ed25519 private key"))
	}
	// Sign exactly the normalized representation emitted below. In particular,
	// omitempty removes explicitly empty optional source fields.
	normalized, err := json.Marshal(doc)
	if err != nil {
		fatal(err)
	}
	payload, err := circuits.CanonicalPayload(normalized)
	if err != nil {
		fatal(err)
	}
	doc.Signatures = []circuits.Signature{{KeyID: *keyID, Sig: base64.RawURLEncoding.EncodeToString(ed25519.Sign(key, payload))}}
	signed, err := json.MarshalIndent(doc, "", "  ")
	if err != nil {
		fatal(err)
	}
	public, err := json.MarshalIndent(circuits.PublicKey{KeyID: *keyID, Key: base64.RawURLEncoding.EncodeToString(key.Public().(ed25519.PublicKey))}, "", "  ")
	if err != nil {
		fatal(err)
	}
	if _, err := circuits.ValidateDocument(signed, public, doc.MinLibraryVersion, mustTime(doc.IssuedAt)); err != nil {
		fatal(err)
	}
	if err := os.WriteFile(*output, append(signed, '\n'), 0644); err != nil {
		fatal(err)
	}
	if err := os.WriteFile(*publicOut, append(public, '\n'), 0644); err != nil {
		fatal(err)
	}
}
func fatal(err error) { fmt.Fprintln(os.Stderr, err); os.Exit(1) }
func mustTime(s string) time.Time {
	t, e := time.Parse(time.RFC3339, s)
	if e != nil {
		fatal(e)
	}
	return t
}
