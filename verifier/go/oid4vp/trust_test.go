package oid4vp

// Step 6 trust-store acceptance, exercised through the real TrustStore: the
// step 6 fixture's mock issuer key must be accepted for BOTH doctypes — the
// national ee.riik.poa.1 and the EU AV Profile eu.europa.ec.av.1 — because
// EE-POA-003 issues both from the same authoritative source in one issuance
// transaction (plan step 6). A key outside the store is refused for either,
// which is the rule the store exists to enforce.

import (
	"encoding/json"
	"os"
	"testing"
)

// step6TrustStorePath is the fixture trust store; the file's provenance is in
// the sibling GENERATED-BY.
const step6TrustStorePath = "../zk/testdata/step6-device-pubkey/issuers.json"

// Any well-formed 32-byte coordinate pair works as "not in the store"; these
// are the step 0 fixture's issuer key, which this store must not carry.
const (
	foreignPKx = "0xa2a62ede3223470130fce5133e45cbb2bdf9bcd52439cdc7b70cf3758597a019"
	foreignPKy = "0x2a2ef0cfeb2e2ff2d4fd3698684c2cb70a9684bc98280f8f85fecc59716c2e88"
)

func TestStep6TrustStoreSelectsFixtureIssuerForBothDoctypes(t *testing.T) {
	ts, err := LoadTrustStore(step6TrustStorePath)
	if err != nil {
		t.Fatalf("load fixture trust store: %v", err)
	}
	meta := readStep6FixtureIssuer(t)
	for _, dt := range []string{"ee.riik.poa.1", "eu.europa.ec.av.1"} {
		is, err := ts.Select(dt, meta.PKx, meta.PKy)
		if err != nil {
			t.Errorf("fixture issuer key not accepted for %q: %v", dt, err)
			continue
		}
		if is.Namespace != dt {
			t.Errorf("doctype %q entry carries namespace %q", dt, is.Namespace)
		}
		if is.DocType != dt {
			t.Errorf("doctype %q entry carries doc_type %q", dt, is.DocType)
		}
	}
}

func TestStep6TrustStoreRefusesForeignIssuerKey(t *testing.T) {
	ts, err := LoadTrustStore(step6TrustStorePath)
	if err != nil {
		t.Fatalf("load fixture trust store: %v", err)
	}
	for _, dt := range []string{"ee.riik.poa.1", "eu.europa.ec.av.1"} {
		if _, err := ts.Select(dt, foreignPKx, foreignPKy); err == nil {
			t.Errorf("a foreign key was accepted for doctype %q", dt)
		}
	}
}

// readStep6FixtureIssuer returns the fixture's minting issuer key from the
// request.json ee_poa_demo wrote alongside the trust store.
func readStep6FixtureIssuer(t *testing.T) (meta struct {
	PKx string `json:"pkx"`
	PKy string `json:"pky"`
}) {
	t.Helper()
	raw, err := os.ReadFile("../zk/testdata/step6-device-pubkey/request.json")
	if err != nil {
		t.Fatalf("read fixture request.json: %v", err)
	}
	if err := json.Unmarshal(raw, &meta); err != nil {
		t.Fatalf("parse fixture request.json: %v", err)
	}
	return meta
}

// Hex case is not identity: an uppercase store entry must still match the
// lowercase coordinates the presentation path derives, and vice versa.
func TestTrustStoreSelectIgnoresHexCase(t *testing.T) {
	path := t.TempDir() + "/issuers.json"
	body := `{"issuers":[{"name":"up","doc_type":"d","pkx":"0xABCD","pky":"0xEF01"}]}`
	if err := os.WriteFile(path, []byte(body), 0o600); err != nil {
		t.Fatal(err)
	}
	ts, err := LoadTrustStore(path)
	if err != nil {
		t.Fatal(err)
	}
	for _, k := range [][2]string{{"0xabcd", "0xef01"}, {"0xABCD", "0xEF01"}} {
		if _, err := ts.Select("d", k[0], k[1]); err != nil {
			t.Fatalf("Select(%s, %s): %v", k[0], k[1], err)
		}
	}
}
