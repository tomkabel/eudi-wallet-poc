package oid4vp

import (
	"errors"
	"fmt"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

// The zk system strings the de-facto carrier uses. multipaz's
// LongfellowZkSystem.getName() — and therefore the system field a wallet puts
// in its zk_system_type and the specs it answers with — is "longfellow-libzk-v1"
// (verified in multipaz-longfellow-jvm 0.99.0 bytecode and the step 8.7
// fixture). The ISO dcapi DeviceRequest this PoC offers names the same system
// (isodcapi.go zkSystemSpec): MdocPresentment calls
// ZkSystemRepository.lookup(spec.system), which compares against
// ZkSystem.getName(), so the "org.iso.mdoc.zk" label an earlier draft offered
// matches no registered ZkSystem. The allowlist matches the registry on
// id+circuit_hash+params, so it accepts either label and refuses anything else.
const (
	SystemName     = "org.iso.mdoc.zk"
	SystemMultipaz = "longfellow-libzk-v1"
)

// zkAcceptedSystems are the system labels an mso_mdoc_zk query may advertise.
var zkAcceptedSystems = map[string]bool{SystemName: true, SystemMultipaz: true}

// The DCQL credential formats this verifier answers. mso_mdoc_zk is the
// de-facto OpenID4VP ZK carrier (plan §8.7) — not in any specification yet
// (OpenID4VP 1.0/1.1-draft and HAIP have no ZK text), so every wire claim
// about it is provisional until DCHP #17 or a profile settles it.
const (
	FormatMsoMdoc   = "mso_mdoc"
	FormatMsoMdocZk = "mso_mdoc_zk"
)

// DCQL is the Digital Credentials Query Language query carried in an
// OpenID4VP 1.0 authorization request. Presentation Exchange was removed from
// OpenID4VP before Final and is not used here (EE-PRO-002).
type DCQL struct {
	Credentials []CredentialQuery `json:"credentials"`
}

// CredentialQuery asks for one credential and the claims to disclose from it.
type CredentialQuery struct {
	ID     string      `json:"id"`
	Format string      `json:"format"`
	Meta   *Meta       `json:"meta,omitempty"`
	Claims []ClaimPath `json:"claims,omitempty"`
}

// Meta constrains which credential satisfies the query. For mso_mdoc this is
// the doctype.
type Meta struct {
	DoctypeValue string `json:"doctype_value,omitempty"`
	// ZkSystemType is mso_mdoc_zk's advertised proving systems (plan §8.7;
	// docs/analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md §2). Relying parties
	// advertise this list (Multipaz and Google's docs, verified; SUNET/vc PR
	// #576, verified 25 Sep 2026; SIROS [UNVERIFIED]); the fields are the same
	// ones the registry's Circuit carries. Never parsed for mso_mdoc.
	ZkSystemType []ZkSystemTypeSpec `json:"zk_system_type,omitempty"`
	// VerifierMessage is Google's example field, semantics undocumented
	// (analysis §5 item 3). Carried, never acted on: an instruction a verifier
	// sends is not a rule this verifier obeys from the wire.
	VerifierMessage string `json:"verifier_message,omitempty"`
}

// ZkSystemTypeSpec is one entry of meta.zk_system_type: the proving system a
// reader advertises it may answer with. The JSON tags match the de-facto wire
// shape exactly — that is the alignment §8.7 asks for — and the fields are the
// registry's Circuit fields, so an advertised entry can be checked against the
// accepted set element by element.
type ZkSystemTypeSpec struct {
	System        string `json:"system"`
	ID            string `json:"id"`
	CircuitHash   string `json:"circuit_hash"`
	NumAttributes uint32 `json:"num_attributes"`
	Version       uint32 `json:"version"`
	BlockEncHash  uint32 `json:"block_enc_hash"`
	BlockEncSig   uint32 `json:"block_enc_sig"`
}

// ClaimPath identifies one claim. For mso_mdoc the path is [namespace, element].
//
// Values is DCQL 1.0's value constraint: the claim only matches if it holds one
// of these. For an age predicate that is the whole question — a query that names
// no value is answered just as well by "false" (EE-ZKP-021).
type ClaimPath struct {
	Path   []string `json:"path"`
	Values []any    `json:"values,omitempty"`
}

// AgeQuery is the query a relying party sends for a single age predicate:
// one credential, one claim, nothing else.
func AgeQuery(id, doctype, namespace, element string) DCQL {
	return DCQL{Credentials: []CredentialQuery{{
		ID:     id,
		Format: "mso_mdoc",
		Meta:   &Meta{DoctypeValue: doctype},
		Claims: []ClaimPath{{Path: []string{namespace, element}, Values: []any{true}}},
	}}}
}

// ZkAgeQuery is the mso_mdoc_zk form of AgeQuery (plan §8.7): one credential,
// one claim, and meta.zk_system_type naming the accepted circuit. The single
// entry is built from the registry circuit, so the advertised id and
// circuit_hash are this verifier's own facts, not strings a caller typed. The
// system label is the de-facto one (profile rule 4): stock multipaz looks the
// query's system up by LongfellowZkSystem.getName() and finds no match for
// SystemName. The allowlist still accepts both labels.
func ZkAgeQuery(id, doctype, namespace, element string, c circuits.Circuit) DCQL {
	return DCQL{Credentials: []CredentialQuery{{
		ID:     id,
		Format: FormatMsoMdocZk,
		Meta: &Meta{
			DoctypeValue: doctype,
			ZkSystemType: []ZkSystemTypeSpec{{
				System:        SystemMultipaz,
				ID:            c.SpecID(),
				CircuitHash:   c.Hash,
				NumAttributes: c.NumAttributes,
				Version:       c.Version,
				BlockEncHash:  c.BlockEncHash,
				BlockEncSig:   c.BlockEncSig,
			}},
		},
		Claims: []ClaimPath{{Path: []string{namespace, element}, Values: []any{true}}},
	}}}
}

// Single returns the sole credential query, rejecting anything else. This
// verifier deliberately supports exactly one credential and one claim per
// request, because the ZK circuits are per (version, num_attributes) and the
// one-attribute circuit is the only one wired up.
func (q DCQL) Single() (CredentialQuery, error) {
	if len(q.Credentials) != 1 {
		return CredentialQuery{}, fmt.Errorf(
			"dcql: this verifier supports exactly one credential query, got %d", len(q.Credentials))
	}
	c := q.Credentials[0]
	switch c.Format {
	case FormatMsoMdoc:
		if c.Meta == nil || c.Meta.DoctypeValue == "" {
			return CredentialQuery{}, errors.New("dcql: meta.doctype_value is required for mso_mdoc")
		}
	case FormatMsoMdocZk:
		// EE-ZKP-023: an mso_mdoc_zk query must say which proving systems can
		// answer it; the allowlist is keyed on what it advertises (plan §8.7).
		// And like mso_mdoc it still has to name its document.
		if c.Meta == nil || c.Meta.DoctypeValue == "" {
			return CredentialQuery{}, errors.New("dcql: meta.doctype_value is required for mso_mdoc_zk")
		}
		if len(c.Meta.ZkSystemType) == 0 {
			return CredentialQuery{}, errors.New(
				"dcql: meta.zk_system_type is required for mso_mdoc_zk; a query with no advertised circuit cannot be allowlisted")
		}
	default:
		return CredentialQuery{}, fmt.Errorf("dcql: unsupported format %q", c.Format)
	}
	if len(c.Claims) != 1 || len(c.Claims[0].Path) != 2 {
		return CredentialQuery{}, errors.New(
			"dcql: expected exactly one claim with a [namespace, element] path")
	}
	// EE-ZKP-021: the proof establishes the attribute has value `true`, so the
	// query has to say so. A claim with no values constraint is satisfied by
	// `false` as readily as by `true`, which is not a question worth asking.
	if len(c.Claims[0].Values) == 0 {
		return CredentialQuery{}, errors.New(
			"dcql: the claim must constrain values; an unconstrained age predicate accepts false")
	}
	for _, v := range c.Claims[0].Values {
		if _, ok := v.(bool); !ok {
			return CredentialQuery{}, fmt.Errorf(
				"dcql: only boolean claim values are supported, got %T", v)
		}
	}
	return c, nil
}

// Namespace, Element and Values destructure the claim of a validated query.
func (c CredentialQuery) Namespace() string { return c.Claims[0].Path[0] }
func (c CredentialQuery) Element() string   { return c.Claims[0].Path[1] }
func (c CredentialQuery) Values() []any     { return c.Claims[0].Values }

// ZkSystemTypeAllowlist checks an mso_mdoc_zk query's meta.zk_system_type
// against the registry's accepted circuits (plan §8.7): every advertised entry
// must be registry-listed on id, circuit_hash, version, num_attributes and the
// block sizes — the same discipline as the ISO path's session-spec allowlist
// (plan §5.3 rule c) and EE-ZKP-023's check-before-verifying. It runs BEFORE
// any FFI work, so nothing holder-supplied ever reaches zk.CircuitHash: the
// match is against published entries only.
//
// Every entry is checked, not just the first: a query advertising one accepted
// and one unknown circuit does not slip the unknown one through.
//
// accepted is a slice rather than *circuits.Registry so a caller with a filtered
// offer (a session's offered set) keys the same way; Registry.Accepted() is
// the normal source.
func ZkSystemTypeAllowlist(meta *Meta, accepted []circuits.Circuit) ([]circuits.Circuit, error) {
	if meta == nil || len(meta.ZkSystemType) == 0 {
		// Unreachable through Single, which demands a non-empty list for
		// mso_mdoc_zk; kept as this function's own contract.
		return nil, errors.New("oid4vp: the query advertises no zk_system_type")
	}
	out := make([]circuits.Circuit, 0, len(meta.ZkSystemType))
	for _, adv := range meta.ZkSystemType {
		if !zkAcceptedSystems[adv.System] {
			return nil, fmt.Errorf("oid4vp: zk_system_type system %q is not a supported zk system", adv.System)
		}
		if adv.CircuitHash == "" || adv.ID == "" {
			return nil, fmt.Errorf("oid4vp: zk_system_type %q carries no circuit hash or id", adv.System)
		}
		found := false
		for _, c := range accepted {
			if adv.ID == c.SpecID() && adv.CircuitHash == c.Hash &&
				adv.Version == c.Version && adv.NumAttributes == c.NumAttributes &&
				adv.BlockEncHash == c.BlockEncHash && adv.BlockEncSig == c.BlockEncSig {
				out = append(out, c)
				found = true
				break
			}
		}
		if !found {
			return nil, fmt.Errorf(
				"oid4vp: zk_system_type id %q (circuit_hash %.12s…, version %d, num_attributes %d) is not in the accepted circuit set",
				adv.ID, adv.CircuitHash, adv.Version, adv.NumAttributes)
		}
	}
	return out, nil
}
