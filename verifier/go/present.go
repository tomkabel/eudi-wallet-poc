package main

import (
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
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
	// carrier forces one vp_token carrier for every session (-carrier flag,
	// for tests); nil dispatches per query format, sniffing the entry bytes
	// (oid4vp.CarrierForBytes).
	carrier oid4vp.Carrier
	sem     limiter
	// reads bounds requests that are reading or decoding a body; nil is unlimited.
	reads limiter

	// Response encryption (ADR-003): responseKey is the verifier's P-256
	// response-encryption key, nil only when the operator explicitly
	// downgraded to plain direct_post. allowPlain is that operator's
	// confirmation, checked once in main, not per request.
	responseKey *jose.JWK
	allowPlain  bool

	// requestKey is the JAR signing key (-request-key-file, EC P-256); nil
	// keeps the request object unsigned (development). requestKeyPublic is
	// its published half, embedded as the JWS `jwk` header member so a
	// wallet can verify without a second fetch (the response-key pattern).
	requestKey       *jose.JWK
	requestKeyPublic *jose.JWK

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

	// client_metadata carries the response-encryption key inline (the `jwks`
	// member) so the wallet never needs a second fetch; present only under
	// direct_post.jwt.
	ClientMetadata *clientMetadata `json:"client_metadata,omitempty"`

	// Extension, not OpenID4VP: the instant the circuit checks the attestation's
	// validity window against. The verifier fixes it so the wallet cannot pick a
	// time at which an expired attestation would still verify.
	ExpectedNow string `json:"expected_now"`
}

// client_metadata is the OpenID4VP 1.0 §5.9 Verifier Metadata member set this
// verifier sends. jwks holds exactly one key: the response-encryption key.
type clientMetadata struct {
	JWKS *jose.JWKSet `json:"jwks,omitempty"`
}

func (p *presenter) request(s *oid4vp.Session) authorizationRequest {
	req := authorizationRequest{
		ResponseType: "vp_token",
		ClientID:     s.ClientID,
		ResponseURI:  s.ResponseURI,
		Nonce:        s.Nonce,
		State:        s.ID,
		DCQLQuery:    s.Query,
		ExpectedNow:  s.ExpectedNow,
	}
	if s.RequireEncryptedResponse {
		// ADR-003: publish the response key in the request itself. The JWE
		// the wallet answers with is opened by this key, and the transcript
		// binds its thumbprint.
		req.ResponseMode = "direct_post.jwt"
		if p.responseKey != nil {
			req.ClientMetadata = &clientMetadata{
				JWKS: &jose.JWKSet{Keys: []jose.JWK{p.responseKey.Public()}},
			}
		}
	} else {
		req.ResponseMode = "direct_post"
	}
	return req
}

// signedRequest serves the request object as a JAR (RFC 9101) compact JWS
// when -request-key-file is configured: the request-object JSON becomes the
// JWS payload, the verifier's public key rides in the `jwk` header member
// (the same inline pattern client_metadata.jwks uses for the response key),
// so a wallet holding only the request_uri response can verify the object
// came from the verifier and was not rewritten in transit.
func (p *presenter) signedRequest(s *oid4vp.Session) (any, error) {
	req := p.request(s)
	if p.requestKey == nil {
		return req, nil
	}
	body, err := json.Marshal(req)
	if err != nil {
		return nil, err
	}
	pub := p.requestKeyPublic
	if pub == nil {
		pk := p.requestKey.Public()
		pub = &pk
	}
	if pub.Kid == "" {
		pub.Kid = jose.ThumbprintB64(pub)
	}
	tok, err := jose.SignJWS(*p.requestKey, jose.JWSTypObject, string(body))
	if err != nil {
		return nil, err
	}
	return signedJAR{JWS: tok, JWK: *pub}, nil
}

// signedJAR is the request_uri response body for a signed request object:
// the compact JWS and the verification key, side by side.
type signedJAR struct {
	JWS string   `json:"request"`
	JWK jose.JWK `json:"jwk"`
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
	var tp []byte
	if p.responseKey != nil {
		// direct_post.jwt (ADR-003): the handover binds this key's
		// thumbprint, and the session will refuse an unencrypted response.
		var tpErr error
		tp, tpErr = p.responseKey.Thumbprint()
		if tpErr != nil {
			writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "response key thumbprint"})
			return
		}
	}
	s, err := p.store.New(p.clientID, p.baseURL+"/present/response", q, tp)
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

// handleRequest serves the authorization request object — signed as a JAR
// when a request key is configured. The fetch is single-use: the first GET
// consumes the session's request-fetch token, so a leaked request_uri cannot
// be replayed to re-read the (nonce-bearing) request after the wallet's own
// fetch. The wallet needs the request only once per session, so this trades
// nothing; a 410 tells a retrying wallet to start a new session.
func (p *presenter) handleRequest(w http.ResponseWriter, r *http.Request) {
	id := strings.TrimPrefix(r.URL.Path, "/present/request/")
	s, err := p.store.Get(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	if err := p.store.ConsumeRequestFetch(s.ID); err != nil {
		log.Printf("/present/request/%s: replay refused: %v", id, err)
		writeJSON(w, http.StatusGone, map[string]string{"error": "request already fetched; start a new presentation"})
		return
	}
	signed, err := p.signedRequest(s)
	if err != nil {
		log.Printf("/present/request/%s: sign: %v", id, err)
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "request signing failed"})
		return
	}
	writeJSON(w, http.StatusOK, signed)
}

// handleResponse consumes the vp_token. This is the direct_post and
// direct_post.jwt endpoint (ADR-003): an encrypted session expects the form
// field `response=<compact JWE>` whose plaintext is the vp_token JSON; a
// plain session accepts the unencrypted JSON body as before.
func (p *presenter) handleResponse(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "POST only"})
		return
	}
	id := strings.TrimPrefix(r.URL.Path, "/present/response/")
	if !p.reads.tryAcquire() {
		busy(w)
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"error": "verifier busy, retry"})
		return
	}
	defer p.reads.release()

	// The session's own stored response mode decides how the body is read —
	// never the Content-Type or anything else on the request. Look the
	// session up without claiming it first; Claim still burns it below.
	peek, peekErr := p.store.Get(id)
	if peekErr != nil {
		writeJSON(w, statusFor(peekErr), map[string]string{"error": peekErr.Error()})
		return
	}

	var token oid4vp.VPToken
	ct := r.Header.Get("Content-Type")
	switch {
	case peek.RequireEncryptedResponse:
		// direct_post.jwt: `response=<compact JWE>`, form-urlencoded. The
		// JWE is self-delimiting, so ParseForm's ceiling here is the same
		// 8 MB the JSON branch enforces.
		r.Body = http.MaxBytesReader(w, r.Body, 8<<20)
		if err := r.ParseForm(); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "malformed form"})
			return
		}
		jweB64 := r.PostFormValue("response")
		if jweB64 == "" {
			writeJSON(w, http.StatusBadRequest, map[string]string{
				"error": "this session requires an encrypted response: post response=<JWE>, not a plain vp_token",
			})
			return
		}
		jwe, err := url.QueryUnescape(jweB64)
		if err != nil || !strings.HasPrefix(jwe, "eyJ") {
			// ParseForm already unescapes once; the double-unescape guard
			// catches clients that pre-escaped it themselves. Either way the
			// token must smell like compact JWE.
			jwe = jweB64
		}
		if p.responseKey == nil {
			// A session cannot have been created encrypted without a key in
			// the first place; this is defence in depth, not a live path.
			log.Printf("/present/response/%s: encrypted session but no response key is configured", id)
			writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "verifier configuration error"})
			return
		}
		inner, err := jose.Decrypt(p.responseKey, jwe)
		if err != nil {
			// Tampered, wrong key, or garbage: one answer, no partial parse,
			// and the distinction stays in the log.
			log.Printf("/present/response/%s: jwe decrypt failed: %v", id, err)
			writeJSON(w, http.StatusBadRequest, map[string]string{
				"error": "the encrypted response could not be decrypted",
			})
			return
		}
		if err := json.Unmarshal(inner, &token); err != nil {
			writeJSON(w, http.StatusBadRequest, map[string]string{"error": "decrypted vp_token is not JSON"})
			return
		}
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
	cp, carrier, err := token.ParseCarrier(cq, p.carrier)
	if err != nil {
		// The holder sent this; telling them exactly what was wrong with it is
		// the whole value of the answer. A forced carrier that does not match
		// the bytes is refused here too — an override never falls back.
		return false, holderDetail("/present/response/"+s.ID, err), http.StatusBadRequest
	}
	pres, proof, err := interimOfChecked(cp, carrier)
	if err != nil {
		return false, err.Error(), http.StatusBadRequest
	}
	return p.verifyPlain(s, pres, proof)
}

// interimOfChecked rebuilds the interim envelope the plain path verifies from
// a carrier-normalized presentation. The interim carrier's fields round-trip
// as parsed; a CBOR carrier has no envelope fields at all, so the attempt is
// refused — the plain path verifies interim envelopes only (the zk path runs
// the CBOR carrier's presentation through checkZk's allowlist).
func interimOfChecked(cp *oid4vp.CheckedPresentation, carrier oid4vp.Carrier) (*oid4vp.ZKPresentation, []byte, error) {
	if _, ok := carrier.(oid4vp.InterimJSON); !ok {
		return nil, nil, fmt.Errorf(
			"the %s carrier's presentation is not an interim envelope; this session asked for the plain form",
			carrier.Name())
	}
	return &oid4vp.ZKPresentation{
		ZKSystem:      cp.ZKSystem,
		Version:       cp.Version,
		NumAttributes: cp.NumAttributes,
		DocType:       cp.DocType,
		Namespace:     cp.Namespace,
		AttrID:        cp.AttrID,
		AttrCBORHex:   "f5",
		ProofB64:      base64.RawStdEncoding.EncodeToString(cp.Proof),
	}, cp.Proof, nil
}

// verifyPlain runs the plain-path verification on a parsed interim envelope:
// trust store, circuit allowlist, transcript, zk.Verify over every trusted
// issuer for the doctype.
func (p *presenter) verifyPlain(s *oid4vp.Session, pres *oid4vp.ZKPresentation, proof []byte) (bool, string, int) {
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

// checkZk verifies an mso_mdoc_zk vp_token through the carrier layer (plan
// §8.7, carrier.go): the vp_token entry is a base64url CBOR DeviceResponse
// carrying zkDocuments, the proof binds the B.2.6.1 OpenID4VPHandover
// transcript this session's request was issued under, and the circuit
// allowlist is resolved here from the session's own (verifier-built)
// zk_system_type. The caller must hold a verification slot: this may call
// zk.Verify once per trusted issuer for the doctype.
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

	cp, _, err := token.ParseCarrier(cq, p.carrier)
	if err != nil {
		if errors.Is(err, oid4vp.ErrNoZkDocument) {
			// The wallet answered with no provable document. That is an
			// answer, not an error — but not the yes the verifier asked for.
			return false, err.Error(), http.StatusOK
		}
		// The holder sent this; telling them exactly what was wrong with it is
		// the whole value of the answer.
		return false, holderDetail("/present/response/"+s.ID, err), http.StatusBadRequest
	}

	// Rule (c), mso_mdoc_zk form: the presentation's own circuit spec must be
	// one of the circuits the query advertised (all of which are
	// registry-listed). The allowlist keys on the registry entry, never on
	// the holder-supplied string alone.
	var circuit circuits.Circuit
	known := false
	for _, c := range advertised {
		if c.SpecID() == cp.CircuitSpecID {
			circuit, known = c, true
			break
		}
	}
	if !known {
		log.Printf("/present/response/%s: refused unsolicited zkSystemSpecId %q", s.ID, cp.CircuitSpecID)
		return false, "the zk system spec was not offered in this session", http.StatusForbidden
	}

	// Rule (a), the ISO path's window: the proof binds the wallet's own
	// ZkDocumentData.timestamp, so Now must be that value — window-checked
	// against the verifier's clock, never the session's ExpectedNow alone
	// (plan §8.7 takes CheckTimestampWindow as the freshness starting point).
	if err := oid4vp.CheckTimestampWindow(s.Created, time.Now(), cp.Timestamp); err != nil {
		return false, err.Error(), http.StatusBadRequest
	}

	// Rule (b): issuer keys from the trust store only; msoX5chain selects.
	issuer, err := p.selectIssuer([][]byte{cp.MSOX5Chain}, cp.DocType)
	if err != nil {
		log.Printf("/present/response/%s: issuer selection: %v", s.ID, err)
		return false, "no trusted issuer matches this presentation", http.StatusForbidden
	}

	// The transcript is the session's own, derived for exactly the flow the
	// carrier reported the proof binds — client id, nonce and response_uri
	// come from the stored session, never from the response (carrier.go,
	// transcript.go).
	transcript, err := oid4vp.TranscriptForFlow(cp.TranscriptFlow, oid4vp.TranscriptParams{
		ClientID:    s.ClientID,
		Nonce:       s.Nonce,
		ResponseURI: s.ResponseURI,
		// The dcapi params are nil by construction here: a redirect-flow
		// session carries no EncryptionInfo, and a proof reporting the dcapi
		// flow on this endpoint is a flow the session was not created for.
		JWKThumbprint: s.JWKThumbprint(),
	})
	if err != nil {
		log.Printf("/present/response/%s: transcript: %v", s.ID, err)
		return false, "verifier configuration error", http.StatusInternalServerError
	}

	err = zk.Verify(zk.Request{
		Version:       circuit.Version,
		NumAttributes: circuit.NumAttributes,
		PKx:           issuer.PKx,
		PKy:           issuer.PKy,
		DocType:       cp.DocType,
		Namespace:     cp.Namespace,
		AttrID:        cp.AttrID,
		AttrCBOR:      []byte{0xf5},
		Now:           cp.Timestamp,
		Transcript:    transcript,
		Proof:         cp.Proof,
	})
	switch {
	case err == nil:
		return true, fmt.Sprintf("%s proved in zero knowledge, issued by %s", cp.AttrID, issuer.Name), http.StatusOK
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
