package oid4vp

// W6 multi-credential DCQL: the query LIST is now the validated unit, with the
// honest scope stated as policy — N credentials, one format (mso_mdoc_zk), one
// claim per credential. These tests pin the parse edge cases the plan names:
// duplicate ids, the cap, the empty list, and mixed formats, plus the
// vp_token-level list discipline (missing/extra entries name the credential).

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"strconv"
	"strings"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

func multiCircuit() circuits.Circuit {
	return circuits.Circuit{
		Hash:          "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
		Version:       7,
		NumAttributes: 1,
		BlockEncHash:  4151,
		BlockEncSig:   4096,
	}
}

// twoZkQueries builds a two-credential mso_mdoc_zk query over two doctypes —
// the shape the e2e two-doctype session mints and requests.
func twoZkQueries() DCQL {
	c := multiCircuit()
	return DCQL{Credentials: []CredentialQuery{
		ZkCredentialQuery("age_credential", "ee.riik.poa.1", "ee.riik.poa.1", "age_over_18", []circuits.Circuit{c}),
		ZkCredentialQuery("av_credential", "eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", []circuits.Circuit{c}),
	}}
}

func TestValidateAcceptsMultiCredentialZkQuery(t *testing.T) {
	qs, err := twoZkQueries().Validate(0)
	if err != nil {
		t.Fatalf("Validate(two mso_mdoc_zk credentials): %v", err)
	}
	if len(qs) != 2 || qs[0].ID != "age_credential" || qs[1].ID != "av_credential" {
		t.Fatalf("Validate returned %+v, want the two queries in order", qs)
	}
	// Order is preserved: credential i of the answer is credential i of the
	// query, which is what ParseCarrierMulti and checkMulti rely on.
	if qs[0].Meta.DoctypeValue != "ee.riik.poa.1" || qs[1].Meta.DoctypeValue != "eu.europa.ec.av.1" {
		t.Fatalf("doctypes reordered: %q / %q", qs[0].Meta.DoctypeValue, qs[1].Meta.DoctypeValue)
	}
}

func TestValidateRejectsEmptyList(t *testing.T) {
	if _, err := (DCQL{}).Validate(0); err == nil || !strings.Contains(err.Error(), "no credentials") {
		t.Fatalf("Validate(empty) = %v, want a no-credentials refusal", err)
	}
}

func TestValidateRejectsDuplicateIds(t *testing.T) {
	q := twoZkQueries()
	q.Credentials[1].ID = q.Credentials[0].ID
	if _, err := q.Validate(0); err == nil || !strings.Contains(err.Error(), "duplicate") {
		t.Fatalf("Validate(dup ids) = %v, want a duplicate-id refusal", err)
	}
}

func TestValidateEnforcesCap(t *testing.T) {
	q := twoZkQueries()
	// Two credentials under a cap of two: accepted.
	if _, err := q.Validate(2); err != nil {
		t.Fatalf("Validate(cap 2) refused a two-credential list: %v", err)
	}
	// The same list under a cap of one: refused, and the cap is named.
	_, err := q.Validate(1)
	if err == nil || !strings.Contains(err.Error(), "cap of 1") {
		t.Fatalf("Validate(cap 1) = %v, want a cap-of-1 refusal", err)
	}
	// The default cap is MaxCredentialsDefault: a list one over it is refused
	// with the default in the text.
	qs := []CredentialQuery{}
	for i := 0; i <= MaxCredentialsDefault; i++ {
		qs = append(qs, ZkCredentialQuery(
			"c"+string(rune('a'+i)), "d", "d", "age_over_18", []circuits.Circuit{multiCircuit()}))
	}
	_, err = (DCQL{Credentials: qs}).Validate(0)
	if err == nil || !strings.Contains(err.Error(), "cap of "+strconv.Itoa(MaxCredentialsDefault)) {
		t.Fatalf("Validate(default cap) = %v, want a cap-of-%d refusal", err, MaxCredentialsDefault)
	}
}

func TestValidateRefusesUnsupportedFormatNamingIt(t *testing.T) {
	q := twoZkQueries()
	q.Credentials[1].Format = "jwt_vc_json"
	q.Credentials[1].Meta = &Meta{}
	_, err := q.Validate(0)
	if err == nil {
		t.Fatal("Validate accepted jwt_vc_json")
	}
	if !errors.Is(err, ErrUnsupportedFormat) {
		t.Fatalf("the refusal is not ErrUnsupportedFormat: %v", err)
	}
	if !strings.Contains(err.Error(), "jwt_vc_json") {
		t.Fatalf("the refusal does not name the offending format: %v", err)
	}
}

func TestValidateKeepsPlainFormatSingleCredential(t *testing.T) {
	// The stated policy: N credentials is an mso_mdoc_zk capability. A
	// two-credential mso_mdoc list is refused with the limitation named, not
	// silently truncated.
	q := DCQL{Credentials: []CredentialQuery{
		AgeQuery("a", "d", "d", "age_over_18").Credentials[0],
		AgeQuery("b", "d", "d", "age_over_21").Credentials[0],
	}}
	_, err := q.Validate(0)
	if err == nil || !strings.Contains(err.Error(), "mso_mdoc") {
		t.Fatalf("Validate(2x mso_mdoc) = %v, want the single-credential policy refusal", err)
	}
	// One mso_mdoc credential stays valid (the plain path's shape).
	if _, err := AgeQuery("a", "d", "d", "age_over_18").Validate(0); err != nil {
		t.Fatalf("Validate(1x mso_mdoc): %v", err)
	}
}

func TestValidateRefusesMixedFormats(t *testing.T) {
	q := twoZkQueries()
	q.Credentials[1] = AgeQuery("av", "d", "d", "age_over_18").Credentials[0]
	if _, err := q.Validate(0); err == nil || !strings.Contains(err.Error(), "one session, one format") {
		t.Fatalf("Validate(mixed formats) = %v, want the one-format refusal", err)
	}
}

func TestValidateStillRejectsMissingBooleanConstraint(t *testing.T) {
	q := twoZkQueries()
	q.Credentials[1].Claims[0].Values = nil
	if _, err := q.Validate(0); err == nil || !strings.Contains(err.Error(), "av_credential") {
		t.Fatalf("Validate(unconstrained claim) = %v, want a refusal naming the credential", err)
	}
}

// zkEntryB64 is one valid interim-JSON vp_token entry answering q: the
// well-behaved wallet's envelope for that credential (vptoken_test.go's
// validPresentation, per-query).
func zkEntryB64(t *testing.T, q CredentialQuery) string {
	t.Helper()
	p := validPresentation()
	p.DocType = q.Meta.DoctypeValue
	p.Namespace = q.Namespace()
	p.AttrID = q.Element()
	raw, err := json.Marshal(p)
	if err != nil {
		t.Fatalf("marshal presentation: %v", err)
	}
	return base64.RawURLEncoding.EncodeToString(raw)
}

// The vp_token envelope: one entry per queried id, no extras, and both
// failure directions name the offending credential.
func TestParseCarrierMultiMissingEntryNamesId(t *testing.T) {
	qs, err := twoZkQueries().Validate(0)
	if err != nil {
		t.Fatal(err)
	}
	// Only the first credential answers.
	token := VPToken{qs[0].ID: []string{zkEntryB64(t, qs[0])}}
	if _, _, err := token.ParseCarrierMulti(qs, InterimJSON{}); err == nil ||
		!strings.Contains(err.Error(), "av_credential") {
		t.Fatalf("ParseCarrierMulti(missing entry) = %v, want a refusal naming av_credential", err)
	}
}

func TestParseCarrierMultiExtraEntryNamesId(t *testing.T) {
	qs, err := twoZkQueries().Validate(0)
	if err != nil {
		t.Fatal(err)
	}
	token := VPToken{
		qs[0].ID:   []string{zkEntryB64(t, qs[0])},
		qs[1].ID:   []string{zkEntryB64(t, qs[1])},
		"smuggled": []string{zkEntryB64(t, qs[0])},
	}
	if _, _, err := token.ParseCarrierMulti(qs, InterimJSON{}); err == nil ||
		!strings.Contains(err.Error(), "smuggled") {
		t.Fatalf("ParseCarrierMulti(extra entry) = %v, want a refusal naming smuggled", err)
	}
}

func TestParseCarrierMultiAcceptsExactAnswer(t *testing.T) {
	qs, err := twoZkQueries().Validate(0)
	if err != nil {
		t.Fatal(err)
	}
	token := VPToken{qs[0].ID: []string{zkEntryB64(t, qs[0])}, qs[1].ID: []string{zkEntryB64(t, qs[1])}}
	cps, carriers, err := token.ParseCarrierMulti(qs, InterimJSON{})
	if err != nil {
		t.Fatalf("ParseCarrierMulti(exact): %v", err)
	}
	if len(cps) != 2 || len(carriers) != 2 {
		t.Fatalf("ParseCarrierMulti returned %d presentations / %d carriers", len(cps), len(carriers))
	}
	// Order follows the query, not the map.
	if cps[0].DocType != qs[0].Meta.DoctypeValue || cps[1].DocType != qs[1].Meta.DoctypeValue {
		t.Fatalf("presentations out of query order: %+v", cps)
	}
	for _, c := range carriers {
		if c.Name() != interimName() {
			t.Fatalf("carrier %q parsed an interim envelope", c.Name())
		}
	}
}

// interimName is the interim carrier's Name(), kept off the composite-literal
// selector that confused the parser inside a map composite literal.
func interimName() string { return InterimJSON{}.Name() }
