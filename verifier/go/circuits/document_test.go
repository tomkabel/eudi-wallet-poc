package circuits

import (
	"crypto/ed25519"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"testing"
	"time"
)

const pinnedRevision = "61a8a735964d1b22bccf79bf14ef6767249cdf92"

func signedTestDocument(t *testing.T, mutate func(*Document)) ([]byte, []byte) {
	t.Helper()
	pub, private, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	doc := Document{Schema: "ee-circuits/1", ListVersion: "99", IssuedAt: "2026-11-01T00:00:00Z", ExpiresAt: "2027-01-01T00:00:00Z", MinLibraryVersion: pinnedRevision, Maintainer: "test-only ephemeral key", Systems: []string{"longfellow-libzk-v1", "org.iso.mdoc.zk"}, Circuits: []Circuit{baseCircuit()}}
	if mutate != nil {
		mutate(&doc)
	}
	b, _ := json.Marshal(doc)
	payload, err := CanonicalPayload(b)
	if err != nil {
		t.Fatal(err)
	}
	doc.Signatures = []Signature{{KeyID: "test", Sig: base64.RawURLEncoding.EncodeToString(ed25519.Sign(private, payload))}}
	b, _ = json.Marshal(doc)
	key, _ := json.Marshal(PublicKey{KeyID: "test", Key: base64.RawURLEncoding.EncodeToString(pub)})
	return b, key
}
func TestSignedDocumentRefusals(t *testing.T) {
	now := time.Date(2026, 11, 13, 12, 0, 0, 0, time.UTC)
	for _, tc := range []struct {
		name, want string
		mutate     func(*Document)
		runtime    string
	}{
		{name: "stale", want: "stale-list", mutate: func(d *Document) { d.ExpiresAt = "2026-11-02T00:00:00Z" }},
		{name: "incompatible", want: "incompatible-library-version", runtime: strings.Repeat("f", 40)},
		{name: "duplicate-tuple", want: "duplicate live circuit", mutate: func(d *Document) {
			c := d.Circuits[0]
			c.Hash = strings.Repeat("b", 64)
			d.Circuits = append(d.Circuits, c)
		}},
		{name: "duplicate-hash", want: "duplicate circuit hash", mutate: func(d *Document) { c := d.Circuits[0]; c.NumAttributes++; d.Circuits = append(d.Circuits, c) }},
	} {
		t.Run(tc.name, func(t *testing.T) {
			b, k := signedTestDocument(t, tc.mutate)
			runtime := tc.runtime
			if runtime == "" {
				runtime = pinnedRevision
			}
			if _, err := ValidateDocument(b, k, runtime, now); err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("got %v, want %s", err, tc.want)
			}
		})
	}
	b, k := signedTestDocument(t, nil)
	for _, tc := range []struct {
		name      string
		data, key []byte
	}{{"tamper", []byte(strings.Replace(string(b), "99", "98", 1)), k}, {"unknown-key", b, []byte(strings.Replace(string(k), "test", "other", 1))}, {"missing-signature", []byte(strings.Replace(string(b), "signatures", "not_signatures", 1)), k}} {
		t.Run(tc.name, func(t *testing.T) {
			if _, err := ValidateDocument(tc.data, tc.key, pinnedRevision, now); err == nil {
				t.Fatal("invalid document accepted")
			}
		})
	}
	if _, err := LoadSigned(t.TempDir()+"/missing", k, pinnedRevision, now); err == nil || !strings.Contains(err.Error(), "missing-local-list") {
		t.Fatal(err)
	}
	r, err := ValidateDocument(b, k, pinnedRevision, now)
	if err != nil {
		t.Fatal(err)
	}
	r.now = func() time.Time { return time.Date(2027, 1, 1, 0, 0, 0, 0, time.UTC) }
	if _, ok := r.Lookup(7, 1); ok || len(r.Accepted()) != 0 {
		t.Fatal("expired cached registry still usable")
	}
}
func TestSignedDocumentDeprecation(t *testing.T) {
	b, k := signedTestDocument(t, func(d *Document) {
		dep := d.Circuits[0]
		dep.DeprecatedOn = "2026-01-01"
		d.Circuits = append(d.Circuits, dep)
	})
	r, e := ValidateDocument(b, k, pinnedRevision, time.Date(2026, 11, 13, 0, 0, 0, 0, time.UTC))
	if e != nil || len(r.Accepted()) != 1 {
		t.Fatalf("deprecation transition: %v", e)
	}
}
func TestJCSIntegerProfile(t *testing.T) {
	for _, bad := range []string{`{"a":1,"a":2}`, `{"a":1.5}`, `{"a":9007199254740992}`, `{"a":1} {}`, `{"x":"\ud800"}`} {
		if _, e := CanonicalPayload([]byte(bad)); e == nil {
			t.Fatalf("accepted %s", bad)
		}
	}
	got, e := CanonicalPayload([]byte(`{"z":"<&>\n","a":1,"signatures":[]}`))
	if e != nil || string(got) != `{"a":1,"z":"<&>\n"}` {
		t.Fatalf("canonical %s: %v", got, e)
	}
}
func TestPinnedOfflineAcceptance(t *testing.T) {
	b, e := os.ReadFile("../../circuits.json")
	if e != nil {
		t.Fatal(e)
	}
	revision, e := os.ReadFile("../../zkverify-ffi/longfellow-rev.txt")
	if e != nil {
		t.Fatal(e)
	}
	r, e := LoadSigned("../../circuits.json", PinnedKey, strings.TrimSpace(string(revision)), time.Now())
	if e != nil {
		t.Fatal(e)
	}
	c, ok := r.Lookup(7, 1)
	if !ok || len(r.Accepted()) != 1 {
		t.Fatal("no usable circuit")
	}
	sum := sha256.Sum256(b)
	observation, _ := json.Marshal(map[string]any{"consumer": "verifier", "list_version": r.ListVersion(), "spec_id": c.SpecID(), "fetched": false, "document_sha256": fmt.Sprintf("%x", sum), "signature_valid": true})
	fmt.Println("ESTONIAN_FALLBACK=" + string(observation))
}

// Shared vectors keep optional metadata interpretation identical to the wallet.
func TestOptionalMetadataSharedVectors(t *testing.T) {
	data, err := os.ReadFile("../../../tests/vectors/circuits/optional-metadata.json")
	if err != nil {
		t.Fatal(err)
	}
	var cases []struct {
		Name, Field       string
		Value             json.RawMessage
		Valid, Deprecated bool
	}
	if err := json.Unmarshal(data, &cases); err != nil {
		t.Fatal(err)
	}
	for _, tc := range cases {
		t.Run(tc.Name, func(t *testing.T) {
			original, _ := signedTestDocument(t, nil)
			var doc map[string]any
			if err := json.Unmarshal(original, &doc); err != nil {
				t.Fatal(err)
			}
			var value any
			if err := json.Unmarshal(tc.Value, &value); err != nil {
				t.Fatal(err)
			}
			doc["circuits"].([]any)[0].(map[string]any)[tc.Field] = value
			data, err := json.Marshal(doc)
			if err != nil {
				t.Fatal(err)
			}
			decoded, err := DecodeDocument(data)
			if (err == nil) != tc.Valid {
				t.Fatalf("valid=%t: %v", tc.Valid, err)
			}
			if err != nil {
				return
			}
			r, err := NewRegistry(decoded.Circuits)
			if err != nil {
				t.Fatal(err)
			}
			if (len(r.Accepted()) == 0) != tc.Deprecated {
				t.Fatalf("deprecated=%t, live=%d", tc.Deprecated, len(r.Accepted()))
			}
		})
	}
}
