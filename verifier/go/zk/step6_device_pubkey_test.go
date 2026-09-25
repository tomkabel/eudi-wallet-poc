package zk

// Step 6 of docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md §4, the part on
// this repository's side: ee.riik.poa.1 minted over a wallet-generated device
// key. The fixture under testdata/step6-device-pubkey was minted by
// issuer/mint_ee_poa.py --device-public-key (finding F12) with --over 18 (spec
// finding S10), re-signed with the holder's own private key, and proven by
// ee_poa_demo; its GENERATED-BY records the commands. The holder's private key
// was generated for that run and never written — the committed directory holds
// only device_public_key.pem, which is the point of F12.
//
// The fixture's issuer key is the mock issuer's public key, and the trust
// store under test accepts it for BOTH doctypes (eu.europa.ec.av.1 and
// ee.riik.poa.1) the same key is served under, which is what plan step 6 asks
// the verifier's store to carry.

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

const step6FixtureDir = "testdata/step6-device-pubkey"

// TestStep6DevicePubKeyFixture verifies the fixture through Verify, after the
// registry and trust-store checks a relying party runs first (EE-ZKP-023,
// EE-GOV-010), and refuses the usual negative controls.
func TestStep6DevicePubKeyFixture(t *testing.T) {
	req, proof := loadFixture(t, step6FixtureDir)
	if req.DocType != "ee.riik.poa.1" || req.Namespace != "ee.riik.poa.1" {
		t.Fatalf("fixture doctype/namespace = %q/%q, want ee.riik.poa.1", req.DocType, req.Namespace)
	}
	if req.AttrID != "age_over_18" {
		t.Fatalf("attr = %q, want age_over_18 (--over 18 is the §9.2 v1 element set)", req.AttrID)
	}

	reg, err := circuits.Load("../../circuits.json")
	if err != nil {
		t.Fatalf("load circuits.json: %v", err)
	}
	if _, err := CheckCircuit(reg, req.Version, req.NumAttributes); err != nil {
		t.Fatalf("registry must accept the fixture's circuit: %v", err)
	}

	issuers := loadStep6TrustStore(t)
	var trusted bool
	for _, is := range issuers {
		if is.PKx == req.PKx && is.PKy == req.PKy {
			trusted = true
			break
		}
	}
	if !trusted {
		t.Fatal("the minting issuer's key is not in the fixture trust store")
	}

	if err := Verify(req); err != nil {
		t.Fatalf("proof over the wallet-generated device key did not verify: %v", err)
	}

	flipped := make([]byte, len(proof))
	copy(flipped, proof)
	flipped[len(flipped)/2] ^= 0x01
	req.Proof = flipped
	if err := Verify(req); err == nil {
		t.Fatal("a one-bit-flipped proof verified; the verifier is not checking anything")
	}
}

// TestStep6TrustStoreCarriesBothDoctypes pins the fixture trust store's data
// with a local struct and asserts the mock issuer key is listed for both
// doctypes plan step 6 names. TrustStore behaviour over the same file is tested
// in oid4vp (trust_test.go).
func TestStep6TrustStoreCarriesBothDoctypes(t *testing.T) {
	issuers := loadStep6TrustStore(t)
	byDoctype := map[string][]string{}
	for _, is := range issuers {
		byDoctype[is.DocType] = append(byDoctype[is.DocType], is.PKx+"|"+is.PKy)
	}
	fixtureKey := fixtureIssuerKey(t)
	for _, dt := range []string{"ee.riik.poa.1", "eu.europa.ec.av.1"} {
		if len(byDoctype[dt]) == 0 {
			t.Errorf("trust store has no issuer for doctype %q", dt)
		}
		for _, entry := range byDoctype[dt] {
			if entry != fixtureKey {
				t.Errorf("doctype %q carries key %q, want the mock issuer key", dt, entry)
			}
		}
	}
}

// fixtureIssuerKey returns the 0x-prefixed public key coordinates the fixture
// was minted under, from request.json.
func fixtureIssuerKey(t *testing.T) string {
	t.Helper()
	meta, err := os.ReadFile(filepath.Join(step6FixtureDir, "request.json"))
	if err != nil {
		t.Fatalf("read request.json: %v", err)
	}
	var f struct {
		PKx string `json:"pkx"`
		PKy string `json:"pky"`
	}
	if err := json.Unmarshal(meta, &f); err != nil {
		t.Fatalf("parse request.json: %v", err)
	}
	return f.PKx + "|" + f.PKy
}

// loadStep6TrustStore reads the fixture's issuers.json with the same shape
// oid4vp.LoadTrustStore consumes.
func loadStep6TrustStore(t *testing.T) []struct {
	Name      string `json:"name"`
	DocType   string `json:"doc_type"`
	Namespace string `json:"namespace"`
	PKx       string `json:"pkx"`
	PKy       string `json:"pky"`
} {
	t.Helper()
	raw, err := os.ReadFile(filepath.Join(step6FixtureDir, "issuers.json"))
	if err != nil {
		t.Fatalf("read issuers.json: %v", err)
	}
	var doc struct {
		Issuers []struct {
			Name      string `json:"name"`
			DocType   string `json:"doc_type"`
			Namespace string `json:"namespace"`
			PKx       string `json:"pkx"`
			PKy       string `json:"pky"`
		} `json:"issuers"`
	}
	if err := json.Unmarshal(raw, &doc); err != nil {
		t.Fatalf("parse issuers.json: %v", err)
	}
	if len(doc.Issuers) == 0 {
		t.Fatalf("issuers.json lists no issuers")
	}
	for _, is := range doc.Issuers {
		if !strings.HasPrefix(is.PKx, "0x") || !strings.HasPrefix(is.PKy, "0x") {
			t.Fatalf("issuer %q lacks 0x-prefixed coordinates", is.Name)
		}
	}
	return doc.Issuers
}
