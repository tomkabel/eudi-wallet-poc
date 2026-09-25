package main

import (
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/zk"
)

// presenter holds everything one relying party needs to run presentations.
type presenter struct {
	store    *oid4vp.Store
	trust    *oid4vp.TrustStore
	registry *circuits.Registry
	clientID string
	baseURL  string
	docType  string
	nsID     string
	sem      limiter

	// dcapiOrigin is the -dcapi-origin value the ISO 18013-7 Annex C handover
	// binds; empty disables that path. offered is the circuit set the ISO
	// sessions advertise in their DeviceRequests — the same registry entries
	// the startup self-check hashed.
	dcapiOrigin string
	offered     []circuits.Circuit
}

// authorizationRequest is the OpenID4VP 1.0 request object, unsigned.
//
// A production verifier signs this as a JAR and authenticates with an
// x509_san_dns client identifier backed by a relying-party access certificate
// (EE-RP-002/EE-RP-003). Nothing here is signed, so a wallet has nothing to
// check — see verifier/README.md.
type authorizationRequest struct {
	ResponseType string      `json:"response_type"`
	ResponseMode string      `json:"response_mode"`
	ClientID     string      `json:"client_id"`
	ResponseURI  string      `json:"response_uri"`
	Nonce        string      `json:"nonce"`
	State        string      `json:"state"`
	DCQLQuery    oid4vp.DCQL `json:"dcql_query"`

	// Extension, not OpenID4VP: the instant the circuit checks the attestation's
	// validity window against. The verifier fixes it so the wallet cannot pick a
	// time at which an expired attestation would still verify.
	ExpectedNow string `json:"expected_now"`
}

func (p *presenter) request(s *oid4vp.Session) authorizationRequest {
	return authorizationRequest{
		ResponseType: "vp_token",
		ResponseMode: "direct_post",
		ClientID:     s.ClientID,
		ResponseURI:  s.ResponseURI,
		Nonce:        s.Nonce,
		State:        s.ID,
		DCQLQuery:    s.Query,
		ExpectedNow:  s.ExpectedNow,
	}
}

// handleNew starts a presentation and hands back what a wallet needs to fetch
// the request.
func (p *presenter) handleNew(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "POST only"})
		return
	}
	var body struct {
		Element string `json:"element"`
	}
	// An empty body is a legitimate "give me the default", so io.EOF is fine.
	// Anything else is a caller sending JSON it thinks we are reading.
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<16)).Decode(&body); err != nil && !errors.Is(err, io.EOF) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "malformed JSON body"})
		return
	}
	if body.Element == "" {
		body.Element = "age_over_18"
	}

	q := oid4vp.AgeQuery("proof_of_age", p.docType, p.nsID, body.Element)
	s, err := p.store.New(p.clientID, p.baseURL+"/present/response", q)
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": err.Error()})
		return
	}
	requestURI := fmt.Sprintf("%s/present/request/%s", p.baseURL, s.ID)
	writeJSON(w, http.StatusCreated, map[string]any{
		"id":          s.ID,
		"request_uri": requestURI,
		"wallet_uri": fmt.Sprintf("openid4vp://?client_id=%s&request_uri=%s",
			s.ClientID, requestURI),
		"result_uri": fmt.Sprintf("%s/present/result/%s", p.baseURL, s.ID),
	})
}

// handleRequest serves the authorization request object.
func (p *presenter) handleRequest(w http.ResponseWriter, r *http.Request) {
	id := strings.TrimPrefix(r.URL.Path, "/present/request/")
	s, err := p.store.Get(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, p.request(s))
}

// handleResponse consumes the vp_token. This is the direct_post endpoint.
func (p *presenter) handleResponse(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "POST only"})
		return
	}
	id := strings.TrimPrefix(r.URL.Path, "/present/response/")

	var token oid4vp.VPToken
	ct := r.Header.Get("Content-Type")
	switch {
	case strings.HasPrefix(ct, "application/x-www-form-urlencoded"):
		// ParseForm reads the whole body, so it needs the same ceiling the JSON
		// branch has: without it a form post is an unbounded allocation.
		r.Body = http.MaxBytesReader(w, r.Body, 8<<20)
		if err := r.ParseForm(); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "malformed form"})
			return
		}
		if err := json.Unmarshal([]byte(r.PostFormValue("vp_token")), &token); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "vp_token is not JSON"})
			return
		}
	default:
		var body struct {
			VPToken oid4vp.VPToken `json:"vp_token"`
		}
		if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8<<20)).Decode(&body); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "malformed JSON: " + err.Error()})
			return
		}
		token = body.VPToken
	}

	// Shed load BEFORE claiming. Claim burns the session permanently, so a 503
	// issued after it would tell the holder to retry a nonce that can never be
	// answered again — turning a load spike into a self-inflicted denial of
	// service. One slot covers the whole request, including the issuer loop in
	// check, so a request is never shed with half its verifications done.
	if !p.sem.tryAcquire() {
		busy(w)
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"error": "verifier busy, retry"})
		return
	}
	defer p.sem.release()

	// Claim marks the session answered, so the same nonce cannot be used twice.
	s, err := p.store.Claim(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	valid, detail, code := p.check(s, token)
	if _, err := p.store.Complete(id, valid, detail); err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, code, map[string]any{"valid": valid, "detail": detail})
}

// check runs the actual verification and returns a plain answer.
//
// The caller must hold a verification slot: this may call zk.Verify once per
// trusted issuer for the doctype.
//
// What a caller learns here is deliberately coarser than what the log records.
// Anything that is the presentation's own fault is named, because the holder
// can act on it; anything that is ours, or the Rust runtime's, becomes a
// category and the detail goes to the log under the session id.
func (p *presenter) check(s *oid4vp.Session, token oid4vp.VPToken) (bool, string, int) {
	cq, err := s.Query.Single()
	if err != nil {
		log.Printf("/present/response/%s: session query is unusable: %v", s.ID, err)
		return false, "verifier configuration error", http.StatusInternalServerError
	}
	// The mso_mdoc_zk carrier answers in the same vp_token with a CBOR
	// DeviceResponse instead of the JSON envelope (plan §8.7).
	if cq.Format == oid4vp.FormatMsoMdocZk {
		return p.checkZk(s, cq, token)
	}
	pres, proof, err := token.Parse(cq)
	if err != nil {
		// The holder sent this; telling them exactly what was wrong with it is
		// the whole value of the answer.
		return false, err.Error(), http.StatusBadRequest
	}
	if s.ExpectedNow == "" {
		return false, "session has no expected_now", http.StatusInternalServerError
	}

	// The issuer key comes from the trust store, never from the presentation.
	issuers, err := p.trust.For(pres.DocType)
	if err != nil {
		return false, err.Error(), http.StatusForbidden
	}
	// EE-ZKP-023: circuit allowlist first — it is ~200x cheaper than verifying.
	if hash, err := zk.CheckCircuit(p.registry, pres.Version, pres.NumAttributes); err != nil {
		log.Printf("/present/response/%s: circuit rejected (version %d, %d attrs, hash %q): %v",
			s.ID, pres.Version, pres.NumAttributes, hash, err)
		return false, "circuit is not in the accepted set", http.StatusForbidden
	}
	transcript, err := s.Transcript()
	if err != nil {
		log.Printf("/present/response/%s: transcript: %v", s.ID, err)
		return false, "verifier configuration error", http.StatusInternalServerError
	}
	attrCBOR, err := hex.DecodeString(pres.AttrCBORHex)
	if err != nil {
		return false, "attr_cbor_hex is not hex", http.StatusBadRequest
	}

	// Any trusted issuer for this doctype may have issued it.
	var lastErr error
	for _, is := range issuers {
		err := zk.Verify(zk.Request{
			Version: pres.Version, NumAttributes: pres.NumAttributes,
			PKx: is.PKx, PKy: is.PKy,
			DocType: pres.DocType, Namespace: pres.Namespace,
			AttrID: pres.AttrID, AttrCBOR: attrCBOR,
			Now: s.ExpectedNow, Transcript: transcript, Proof: proof,
		})
		if err == nil {
			return true, fmt.Sprintf("%s = 0x%s, issued by %s",
				pres.AttrID, pres.AttrCBORHex, is.Name), http.StatusOK
		}
		lastErr = err
	}
	log.Printf("/present/response/%s: no issuer verified the proof, last error: %v", s.ID, lastErr)
	if errors.Is(lastErr, zk.ErrInvalid) {
		return false, "no trusted issuer's key verifies this proof", http.StatusOK
	}
	return false, verifyErrorCategory(lastErr), http.StatusBadRequest
}

// checkZk verifies an mso_mdoc_zk vp_token (plan §8.7): the vp_token entry is a
// base64url CBOR DeviceResponse carrying zkDocuments, the proof binds the
// B.2.6.1 OpenID4VPHandover transcript this session's request was issued under,
// and the circuit allowlist is resolved here from the session's own
// (verifier-built) zk_system_type. The caller must hold a verification slot:
// this may call zk.Verify once per trusted issuer for the doctype.
//
// Rule order mirrors the ISO path: the advertised-circuit allowlist (EE-ZKP-023,
// before any FFI work), then the per-zkDocument rules, then zk.Verify.
func (p *presenter) checkZk(s *oid4vp.Session, cq oid4vp.CredentialQuery, token oid4vp.VPToken) (bool, string, int) {
	// The advertised circuits come from this session's own (verifier-built)
	// query; resolve them against the registry here, before any FFI work, so
	// verify uses the registry entry, never the query's copy.
	advertised, err := oid4vp.ZkSystemTypeAllowlist(cq.Meta, p.registry.Accepted())
	if err != nil {
		log.Printf("/present/response/%s: %v", s.ID, err)
		return false, "circuit is not in the accepted set", http.StatusForbidden
	}

	docs, err := token.ParseZkVPToken(cq)
	if err != nil {
		// The holder sent this; telling them exactly what was wrong with it is
		// the whole value of the answer.
		return false, holderDetail("/present/response/"+s.ID, err), http.StatusBadRequest
	}
	d, err := oid4vp.MatchQueryZkDocument(docs, cq)
	if err != nil {
		return false, err.Error(), http.StatusBadRequest
	}
	if d == nil {
		// The wallet answered with no provable document. That is an answer,
		// not an error — but not the yes the verifier was asking for.
		return false, "no zkDocument: the wallet presented nothing provable in this response", http.StatusOK
	}

	// Rule (c), mso_mdoc_zk form: the zkDocument's own zkSystemId must be one
	// of the circuits the query advertised (all of which are registry-listed).
	var circuit circuits.Circuit
	known := false
	for _, c := range advertised {
		if c.SpecID() == d.ZkSystemSpecID {
			circuit, known = c, true
			break
		}
	}
	if !known {
		log.Printf("/present/response/%s: refused unsolicited zkSystemSpecId %q", s.ID, d.ZkSystemSpecID)
		return false, "the zk system spec was not offered in this session", http.StatusForbidden
	}

	// Rule (a), the ISO path's window: the proof binds the wallet's own
	// ZkDocumentData.timestamp, so Now must be that value — window-checked
	// against the verifier's clock, never the session's ExpectedNow alone
	// (plan §8.7 takes CheckTimestampWindow as the freshness starting point).
	if err := oid4vp.CheckTimestampWindow(s.Created, time.Now(), d.Timestamp); err != nil {
		return false, err.Error(), http.StatusBadRequest
	}

	// Rule (b): issuer keys from the trust store only; msoX5chain selects.
	issuer, err := p.selectIssuer([][]byte{d.MSOX5Chain}, d.DocType)
	if err != nil {
		log.Printf("/present/response/%s: issuer selection: %v", s.ID, err)
		return false, "no trusted issuer matches this presentation", http.StatusForbidden
	}

	// The transcript is the session's own B.2.6.1 OpenID4VPHandover — client
	// id, nonce and response_uri come from the stored session, never from the
	// response (transcript.go).
	transcript, err := s.Transcript()
	if err != nil {
		log.Printf("/present/response/%s: transcript: %v", s.ID, err)
		return false, "verifier configuration error", http.StatusInternalServerError
	}

	err = zk.Verify(zk.Request{
		Version:       circuit.Version,
		NumAttributes: circuit.NumAttributes,
		PKx:           issuer.PKx,
		PKy:           issuer.PKy,
		DocType:       d.DocType,
		Namespace:     cq.Namespace(),
		AttrID:        cq.Element(),
		AttrCBOR:      []byte{0xf5},
		Now:           d.Timestamp,
		Transcript:    transcript,
		Proof:         d.Proof,
	})
	switch {
	case err == nil:
		return true, fmt.Sprintf("%s proved in zero knowledge, issued by %s", cq.Element(), issuer.Name), http.StatusOK
	case errors.Is(err, zk.ErrInvalid):
		// A proof that does not verify is an answer, not a failure: 200,
		// valid:false. Only the runtime's wording stays server-side.
		log.Printf("/present/response/%s: proof invalid: %v", s.ID, err)
		return false, "the proof did not verify", http.StatusOK
	default:
		log.Printf("/present/response/%s: %v", s.ID, err)
		return false, verifyErrorCategory(err), http.StatusBadRequest
	}
}

// handleResult lets the relying party's own page poll the outcome.
func (p *presenter) handleResult(w http.ResponseWriter, r *http.Request) {
	id := strings.TrimPrefix(r.URL.Path, "/present/result/")
	s, err := p.store.Get(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"id": s.ID, "answered": s.Answered, "valid": s.Valid, "detail": s.Detail,
	})
}

func statusFor(err error) int {
	switch {
	case errors.Is(err, oid4vp.ErrNoSession):
		return http.StatusNotFound
	case errors.Is(err, oid4vp.ErrExpired), errors.Is(err, oid4vp.ErrAlreadyUsed):
		return http.StatusGone
	default:
		return http.StatusBadRequest
	}
}
