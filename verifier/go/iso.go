package main

// The ISO/IEC 18013-7 Annex C presentation path (plan §5, step 2a): the
// request is delivered through the W3C Digital Credentials API, the response
// comes back inside an HPKE-encrypted DeviceResponse, and the proof is a
// ZkDocument verified against the circuit the session itself offered. Plan §5.3
// fixes the rules this file implements:
//
//	(a) the proof's timestamp comes from ZkDocumentData and must sit inside a
//	    window anchored in the verifier's own clock;
//	(b) issuer keys come from the trust store only; anything the presentation
//	    carries (msoX5chain) may at most select between them;
//	(c) only the zkSystemSpec ids offered in THIS session are accepted, and the
//	    (version, num_attributes) used for the FFI call come from the registry
//	    entry behind the id — never from the holder;
//	(d) no holder-supplied circuit input is hashed through the FFI before the
//	    session allowlist has matched it.
//
// The origin is a configuration value (-dcapi-origin), never a request header:
// the browser would otherwise be able to choose the bytes the handover hashes.

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"embed"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"html/template"
	"io"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/tomkabel/ee-eudiw/verifier/go/oid4vp"
	"github.com/tomkabel/ee-eudiw/verifier/go/zk"
)

//go:embed iso.html
var isoPages embed.FS

// dcapiPage is the static presentation page. The Digital Credentials API is
// only reachable from a secure context, so this page is what the holder's
// browser loads; the verifier itself never talks to the wallet.
var dcapiPage = template.Must(template.ParseFS(isoPages, "iso.html"))

// dcapiNewRequest is the body of POST /present/dcapi/new.
type dcapiNewRequest struct {
	Element string `json:"element"`
}

// dcapiNewResponse names the page that drives the request and the endpoint
// that takes the response.
type dcapiNewResponse struct {
	ID        string `json:"id"`
	PageURI   string `json:"page_uri"`
	ResultURI string `json:"result_uri"`
}

// handleDCAPINew starts an ISO 18013-7 Annex C session: same DCQL query as the
// redirect path, plus the HPKE key pair, the EncryptionInfo and the offered
// ZkSystemSpecs the whole exchange is bound to.
func (p *presenter) handleDCAPINew(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "POST only"})
		return
	}
	var body dcapiNewRequest
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 1<<16)).Decode(&body); err != nil && !errors.Is(err, io.EOF) {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "malformed JSON body"})
		return
	}
	if body.Element == "" {
		body.Element = "age_over_18"
	}
	q := oid4vp.AgeQuery("proof_of_age", p.docType, p.nsID, body.Element)
	s, err := p.store.NewISO(p.clientID, p.baseURL+"/present/dcapi/response", q, p.dcapiOrigin, p.offered)
	if err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusCreated, dcapiNewResponse{
		ID:        s.ID,
		PageURI:   fmt.Sprintf("%s/present/dcapi/%s", p.baseURL, s.ID),
		ResultURI: fmt.Sprintf("%s/present/dcapi/result/%s", p.baseURL, s.ID),
	})
}

// handleDCAPIPage serves the static page that calls navigator.credentials.get.
func (p *presenter) handleDCAPIPage(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "GET only"})
		return
	}
	id := strings.TrimPrefix(r.URL.Path, "/present/dcapi/")
	if id == "" || strings.Contains(id, "/") {
		writeJSON(w, http.StatusNotFound, map[string]string{"error": "missing session id"})
		return
	}
	s, err := p.store.Get(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	cq, err := s.Query.Single()
	if err != nil {
		log.Printf("/present/dcapi/%s: query: %v", id, err)
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "session query is unusable"})
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	// Raw strings: html/template escapes each value for its own context (the
	// element in HTML text, the id as a JS string literal).
	pageData := map[string]string{
		"ID":      s.ID,
		"Element": cq.Element(),
	}
	if err := dcapiPage.Execute(w, pageData); err != nil {
		log.Printf("/present/dcapi/%s: page: %v", id, err)
	}
}

// handleDCAPIRequest hands the page the deviceRequest and encryptionInfo, both
// base64url, exactly the data shape navigator.credentials.get takes for the
// "org-iso-mdoc" protocol.
func (p *presenter) handleDCAPIRequest(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "GET only"})
		return
	}
	id := strings.TrimPrefix(r.URL.Path, "/present/dcapi/request/")
	s, err := p.store.Get(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	ext := s.SessionISO()
	if ext == nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "session has no dcapi extension"})
		return
	}
	cq, err := s.Query.Single()
	if err != nil {
		log.Printf("/present/dcapi/request/%s: query: %v", s.ID, err)
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "session query is unusable"})
		return
	}
	req, err := oid4vp.DeviceRequest(cq.Meta.DoctypeValue, cq.Namespace(), cq.Element(), p.offered)
	if err != nil {
		log.Printf("/present/dcapi/request/%s: build: %v", s.ID, err)
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "device request build failed"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"device_request":   oid4vp.Base64URL(req),
		"encryption_info":  ext.EncryptionInfoB64,
		"protocol":         "org-iso-mdoc",
		"doctype":          p.docType,
		"element":          cq.Element(),
		"offered_spec_ids": ext.OfferedSpecIDs,
	})
}

// dcapiResponseRequest is the body the page posts back.
type dcapiResponseRequest struct {
	Response string `json:"response"`
}

// handleDCAPIResponse consumes the encrypted DeviceResponse. The guard order is
// the one present.go uses: load shed before claiming, because Claim burns the
// session and a 503 after it would point the holder at a nonce that can never
// be answered again.
func (p *presenter) handleDCAPIResponse(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "POST only"})
		return
	}
	id := strings.TrimPrefix(r.URL.Path, "/present/dcapi/response/")
	var body dcapiResponseRequest
	if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8<<20)).Decode(&body); err != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "malformed JSON: " + err.Error()})
		return
	}

	if !p.sem.tryAcquire() {
		busy(w)
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"error": "verifier busy, retry"})
		return
	}
	defer p.sem.release()

	s, err := p.store.Claim(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	valid, detail, code := p.isoCheck(s, body.Response)
	if _, err := p.store.Complete(id, valid, detail); err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, code, map[string]any{"valid": valid, "detail": detail})
}

// handleDCAPIResult lets the page poll the outcome, same contract as
// /present/result.
func (p *presenter) handleDCAPIResult(w http.ResponseWriter, r *http.Request) {
	id := strings.TrimPrefix(r.URL.Path, "/present/dcapi/result/")
	s, err := p.store.Get(id)
	if err != nil {
		writeJSON(w, statusFor(err), map[string]string{"error": err.Error()})
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"id": s.ID, "answered": s.Answered, "valid": s.Valid, "detail": s.Detail,
	})
}

// isoCheck verifies one encrypted DeviceResponse. The caller must hold a
// verification slot (this may call zk.Verify once). Return order follows the
// plan: envelope, transcript, HPKE, CBOR, then the per-zkDocument rules.
func (p *presenter) isoCheck(s *oid4vp.Session, responseB64 string) (bool, string, int) {
	ext := s.SessionISO()
	if ext == nil {
		return false, "session has no dcapi extension", http.StatusInternalServerError
	}
	cq, err := s.Query.Single()
	if err != nil {
		log.Printf("/present/dcapi/response/%s: session query is unusable: %v", s.ID, err)
		return false, "verifier configuration error", http.StatusInternalServerError
	}

	// 1. The dcapi envelope: ["dcapi", {enc, cipherText}].
	envelope, err := oid4vp.DecodeBase64URL(responseB64)
	if err != nil {
		return false, "response is not base64url", http.StatusBadRequest
	}
	enc, cipherText, err := oid4vp.ParseEnvelope(envelope)
	if err != nil {
		return false, err.Error(), http.StatusBadRequest
	}

	// 2. The transcript is recomputed from the verifier's own stored state —
	// the EncryptionInfo the session minted and the configured origin. The
	// holder never gets a vote on either byte.
	transcript, err := s.ISOTranscript()
	if err != nil {
		log.Printf("/present/dcapi/response/%s: transcript: %v", s.ID, err)
		return false, "verifier configuration error", http.StatusInternalServerError
	}

	// 3. Single-shot HPKE open: ciphertext = enc||ct, info = transcript, aad
	// empty (the PoC's own comment at DigitalCredentialsViewModel.kt:424-432
	// is why the transcript belongs in info and not the aad).
	joined := make([]byte, 0, len(enc)+len(cipherText))
	joined = append(joined, enc...)
	joined = append(joined, cipherText...)
	plain, err := oid4vp.Open(ext.HPKEPrivate, transcript, joined)
	if err != nil {
		log.Printf("/present/dcapi/response/%s: %v", s.ID, err)
		return false, "the response could not be decrypted for this session", http.StatusBadRequest
	}

	// 4. Strict-CBOR DeviceResponse (ADR-002).
	docs, err := oid4vp.ParseZkDocumentsBytes(plain)
	if err != nil {
		return false, err.Error(), http.StatusBadRequest
	}

	// 5. The per-zkDocument rules, then zk.Verify.
	switch len(docs) {
	case 0:
		// The wallet answered with no provable document. That is an answer,
		// not an error — but not the yes the verifier was asking for.
		return false, "no zkDocument: the wallet presented nothing provable in this response", http.StatusOK
	case 1:
		return p.verifyZkDocument(s, ext, cq, docs[0])
	default:
		// Every offered spec proves the same 1-attribute predicate, so a
		// response claiming two ZK documents is either confused or hostile;
		// both get the same refusal.
		return false, fmt.Sprintf("expected exactly one zkDocument, got %d", len(docs)), http.StatusBadRequest
	}
}

// verifyZkDocument runs rules (a)-(d) on one parsed ZkDocument and verifies it.
func (p *presenter) verifyZkDocument(s *oid4vp.Session, ext *oid4vp.ISOExtension, cq oid4vp.CredentialQuery, d *oid4vp.ZkDocument) (bool, string, int) {
	if d.DocType != cq.Meta.DoctypeValue {
		return false, fmt.Sprintf("zkDocument doctype %q does not match the query's %q", d.DocType, cq.Meta.DoctypeValue), http.StatusBadRequest
	}

	// Rule (c): only the specs offered in THIS session. This costs one map
	// lookup; nothing holder-supplied reaches the FFI on this path.
	circuit, offered := ext.BySpecID[d.ZkSystemSpecID]
	if !offered {
		log.Printf("/present/dcapi/response/%s: refused unsolicited zkSystemSpecId %q", s.ID, d.ZkSystemSpecID)
		return false, "the zk system spec was not offered in this session", http.StatusForbidden
	}

	// Rule (a): the timestamp the proof binds, inside a window anchored in the
	// verifier's own clock.
	if err := oid4vp.CheckTimestampWindow(s.Created, time.Now(), d.Timestamp); err != nil {
		return false, err.Error(), http.StatusBadRequest
	}

	// Rule (b): issuer keys from the trust store only; msoX5chain selects.
	issuer, err := p.selectIssuer([][]byte{d.MSOX5Chain}, d.DocType)
	if err != nil {
		log.Printf("/present/dcapi/response/%s: issuer selection: %v", s.ID, err)
		return false, "no trusted issuer matches this presentation", http.StatusForbidden
	}

	// The transcript the proof is bound to is the same one the HPKE open used.
	transcript, err := s.ISOTranscript()
	if err != nil {
		log.Printf("/present/dcapi/response/%s: transcript: %v", s.ID, err)
		return false, "verifier configuration error", http.StatusInternalServerError
	}

	// Rule (c), continued: version and num_attributes come from the registry
	// entry behind the offered spec id, never from the holder. The proof
	// establishes the predicate `true` — the answer to the question asked.
	// The proof binds the wallet's own ZkDocumentData.timestamp (multipaz's
	// LongfellowZkSystem folds formatDate(timestamp) into the verifier call),
	// so Now must be that value — d.Timestamp, already window-checked above —
	// and never this session's ExpectedNow, which would fail every real
	// device response.
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
		log.Printf("/present/dcapi/response/%s: proof invalid: %v", s.ID, err)
		return false, "the proof did not verify", http.StatusOK
	default:
		log.Printf("/present/dcapi/response/%s: %v", s.ID, err)
		return false, verifyErrorCategory(err), http.StatusBadRequest
	}
}

// selectIssuer applies rule (b): take the leaf key of whatever chain the
// presentation claims, and require it to be one of the verifier's trusted
// issuer keys. The chain never adds a key to the trust set; it can only pick
// one.
func (p *presenter) selectIssuer(chains [][]byte, docType string) (oid4vp.Issuer, error) {
	leaf, err := oid4vp.LeafCertificate(chains)
	if err != nil {
		return oid4vp.Issuer{}, err
	}
	pub, ok := leaf.PublicKey.(*ecdsa.PublicKey)
	if !ok || pub.Curve != elliptic.P256() {
		return oid4vp.Issuer{}, errors.New("issuer certificate key is not ECDSA P-256")
	}
	uncompressed := elliptic.Marshal(elliptic.P256(), pub.X, pub.Y)
	pkx := "0x" + hex.EncodeToString(uncompressed[1:33])
	pky := "0x" + hex.EncodeToString(uncompressed[33:65])
	return p.trust.Select(docType, pkx, pky)
}
