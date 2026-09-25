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

// AvDoctypeDemo is the second fixture doctype (the EU AV Profile document
// issuer/mint_ee_poa.py mints with --doctype eu.europa.ec.av.1). The
// -multi-credentials demo switch asks for it alongside the configured
// doctype, so one session requests two documents from two issuers.
const AvDoctypeDemo = "eu.europa.ec.av.1"

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
// one claim, and meta.zk_system_type naming the accepted circuit. The entries
// are built from the registry circuit, so the advertised ids and circuit
// hashes are this verifier's own facts, not strings a caller typed. The
// system label is the de-facto one (profile rule 4): stock multipaz looks the
// query's system up by LongfellowZkSystem.getName() and finds no match for
// SystemName. The allowlist still accepts both labels.
func ZkAgeQuery(id, doctype, namespace, element string, c circuits.Circuit) DCQL {
	return DCQL{Credentials: []CredentialQuery{
		ZkCredentialQuery(id, doctype, namespace, element, []circuits.Circuit{c}),
	}}
}

// ZkCredentialQuery is one mso_mdoc_zk credential query: the doctype, one
// boolean claim, and meta.zk_system_type advertising every circuit named in
// cs, so a registry with several accepted circuits lets the wallet pick
// whichever it can prove with. Building from registry entries keeps the
// advertised ids and hashes the verifier's own facts (ZkAgeQuery is the
// single-credential form).
func ZkCredentialQuery(id, doctype, namespace, element string, cs []circuits.Circuit) CredentialQuery {
	specs := make([]ZkSystemTypeSpec, 0, len(cs))
	for _, c := range cs {
		specs = append(specs, ZkSystemTypeSpec{
			System:        SystemMultipaz,
			ID:            c.SpecID(),
			CircuitHash:   c.Hash,
			NumAttributes: c.NumAttributes,
			Version:       c.Version,
			BlockEncHash:  c.BlockEncHash,
			BlockEncSig:   c.BlockEncSig,
		})
	}
	return CredentialQuery{
		ID:     id,
		Format: FormatMsoMdocZk,
		Meta: &Meta{
			DoctypeValue: doctype,
			ZkSystemType: specs,
		},
		Claims: []ClaimPath{{Path: []string{namespace, element}, Values: []any{true}}},
	}
}

// MaxCredentialsDefault is the default ceiling on how many credential queries
// one session may carry (-max-credentials). The limit bounds the per-response
// cost: each credential is verified separately and takes one admission slot
// (main.go's cgo thread-kill math), so an unbounded query list would let one
// cheap HTTP request schedule unbounded ~2.5 s verifications.
const MaxCredentialsDefault = 4

// ErrUnsupportedFormat is the base of the format-policy refusal: a session may
// mix credentials only within one format, and the only format wired for
// multi-credential sessions is mso_mdoc_zk. errors.Is distinguishes "this
// format is not offered at all" from other malformed-query refusals.
var ErrUnsupportedFormat = errors.New("dcql: unsupported credential format")

// Validate checks the whole query list instead of one special case: non-empty,
// at most max credentials, unique ids, one claim per credential with a boolean
// values constraint, and every credential in one supported format.
//
// The format limitation is policy, stated here where it is enforced: the ZK
// circuits prove one attribute per credential and the admission limiter prices
// per credential, so N credentials of one format is the shape this verifier
// answers. mso_mdoc (the plain-format session) stays single-credential — the
// interim envelope path was never multi-credential — and any other format is
// refused outright. The error names the first unsupported format seen.
//
// max <= 0 means MaxCredentialsDefault.
func (q DCQL) Validate(max int) ([]CredentialQuery, error) {
	if max <= 0 {
		max = MaxCredentialsDefault
	}
	if len(q.Credentials) == 0 {
		return nil, errors.New("dcql: the query carries no credentials")
	}
	if len(q.Credentials) > max {
		return nil, fmt.Errorf(
			"dcql: %d credential queries exceed the cap of %d (each credential costs one admission slot)",
			len(q.Credentials), max)
	}
	seen := make(map[string]bool, len(q.Credentials))
	singleFormat := ""
	for i := range q.Credentials {
		c := &q.Credentials[i]
		if c.ID == "" {
			return nil, fmt.Errorf("dcql: credential %d carries no id", i)
		}
		if seen[c.ID] {
			return nil, fmt.Errorf("dcql: duplicate credential id %q", c.ID)
		}
		seen[c.ID] = true
		switch c.Format {
		case FormatMsoMdocZk:
			if singleFormat == "" {
				singleFormat = c.Format
			}
		case FormatMsoMdoc:
			if singleFormat == "" {
				singleFormat = c.Format
			} else if singleFormat != c.Format {
				return nil, fmt.Errorf("dcql: credential %q is %s in a %s session; one session, one format", c.ID, c.Format, singleFormat)
			}
		default:
			return nil, fmt.Errorf("%w %q (credential %q): this verifier answers mso_mdoc and mso_mdoc_zk only",
				ErrUnsupportedFormat, c.Format, c.ID)
		}
		if c.Meta == nil || c.Meta.DoctypeValue == "" {
			return nil, fmt.Errorf("dcql: meta.doctype_value is required for %s (credential %q)", c.Format, c.ID)
		}
		if c.Format == FormatMsoMdocZk {
			// EE-ZKP-023: an mso_mdoc_zk query must say which proving systems
			// can answer it; the allowlist is keyed on what it advertises
			// (plan §8.7).
			if len(c.Meta.ZkSystemType) == 0 {
				return nil, fmt.Errorf(
					"dcql: meta.zk_system_type is required for mso_mdoc_zk (credential %q); a query with no advertised circuit cannot be allowlisted", c.ID)
			}
		}
		if len(c.Claims) != 1 || len(c.Claims[0].Path) != 2 {
			return nil, fmt.Errorf(
				"dcql: credential %q must carry exactly one claim with a [namespace, element] path", c.ID)
		}
		// EE-ZKP-021: the proof establishes the attribute has value `true`, so
		// the query has to say so. A claim with no values constraint is
		// satisfied by `false` as readily as by `true`, which is not a question
		// worth asking.
		if len(c.Claims[0].Values) == 0 {
			return nil, fmt.Errorf(
				"dcql: credential %q must constrain values; an unconstrained age predicate accepts false", c.ID)
		}
		for _, v := range c.Claims[0].Values {
			if _, ok := v.(bool); !ok {
				return nil, fmt.Errorf(
					"dcql: credential %q: only boolean claim values are supported, got %T", c.ID, v)
			}
		}
	}
	if singleFormat != FormatMsoMdocZk && len(q.Credentials) > 1 {
		// Stated as the policy it is, naming the format: N credentials is an
		// mso_mdoc_zk capability; the plain interim-envelope path stays
		// single-credential.
		return nil, fmt.Errorf(
			"dcql: %d credentials of format %s: multi-credential sessions are an mso_mdoc_zk capability; this verifier answers the %s format one credential at a time",
			len(q.Credentials), singleFormat, singleFormat)
	}
	return q.Credentials, nil
}

// Single returns the sole credential query, rejecting anything else. The
// mso_mdoc (plain interim-envelope) path and the ISO dcapi path stay
// single-credential by policy — see Validate for the multi-credential
// mso_mdoc_zk session — so their entry points keep this admission shape.
func (q DCQL) Single() (CredentialQuery, error) {
	if len(q.Credentials) != 1 {
		return CredentialQuery{}, fmt.Errorf(
			"dcql: this path supports exactly one credential query, got %d", len(q.Credentials))
	}
	cs, err := (DCQL{Credentials: q.Credentials[:1]}).Validate(1)
	if err != nil {
		return CredentialQuery{}, err
	}
	return cs[0], nil
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
		// Unreachable through Validate/Single, which demand a non-empty
		// zk_system_type list for mso_mdoc_zk; kept as this function's own
		// contract.
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
