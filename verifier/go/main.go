// Command zkverify is a relying-party verifier for Longfellow zero-knowledge
// proofs over ISO/IEC 18013-5 mdocs.
//
//	go build -o ../zkverify . && ../zkverify -registry ../circuits.json -issuers ../issuers.json
//
// /present/* runs a full OpenID4VP 1.0 exchange with a DCQL query: the issuer
// key comes from the verifier's own trust store and the proof is bound to the
// session transcript. The service learns whether the attribute was present, and
// nothing else.
//
// POST /zkverify checks one proof directly, with the caller supplying the issuer
// key and `now`. That is a development harness, so it is only registered under
// -unsafe-dev-api and 404s otherwise.
package main

import (
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"runtime"
	"strings"
	"time"

	"github.com/tomkabel/ee-eudiw/verifier/go/circuits"
	"github.com/tomkabel/ee-eudiw/verifier/go/oid4vp"
	"github.com/tomkabel/ee-eudiw/verifier/go/zk"
)

type verifyRequest struct {
	Version       uint32 `json:"version"`
	NumAttributes uint32 `json:"num_attributes"`
	PKx           string `json:"pkx"`
	PKy           string `json:"pky"`
	DocType       string `json:"doc_type"`
	Namespace     string `json:"namespace"`
	AttrID        string `json:"attr_id"`
	AttrCBORHex   string `json:"attr_cbor_hex"`
	Now           string `json:"now"`
	TranscriptB64 string `json:"transcript_b64"`
	ProofB64      string `json:"proof_b64"`
}

type verifyResponse struct {
	Valid       bool   `json:"valid"`
	CircuitHash string `json:"circuit_hash,omitempty"`
	AttrID      string `json:"attr_id,omitempty"`
	AttrCBORHex string `json:"attr_cbor_hex,omitempty"`
	TookMS      int64  `json:"took_ms"`
	Error       string `json:"error,omitempty"`
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

// limiter bounds how many verifications may be in flight at once.
//
// zk.Verify is a cgo call that runs for ~2.5 s, and a goroutine blocked in cgo
// pins its OS thread for the whole call. Go aborts the process past 10,000
// threads, so an unbounded arrival rate on either verification endpoint is a
// remote kill switch that costs the attacker one HTTP request per thread.
type limiter chan struct{}

// tryAcquire admits a request, or reports that every slot is busy. It never
// blocks: queueing would only move the problem: each waiter still holds a
// connection and a goroutine, and the wait cannot be cancelled once the call
// it is waiting for is inside cgo. Shedding early is the honest answer.
//
// This bounds admission, not work already in flight. r.Context() cannot
// interrupt a call that has entered cgo — a cancelled client just means the
// verification finishes into a closed connection.
func (l limiter) tryAcquire() bool {
	select {
	case l <- struct{}{}:
		return true
	default:
		return false
	}
}

func (l limiter) release() { <-l }

// busy sets the shed-load headers. The wait is one verification long, because
// that is when a slot actually frees up.
func busy(w http.ResponseWriter) {
	w.Header().Set("Retry-After", "3")
}

// verifyErrorCategory maps a verification failure to a stable, non-leaky
// label. The underlying message comes from the Rust runtime and can name
// internal state, so it goes to the log and never to the caller.
func verifyErrorCategory(err error) string {
	switch {
	case errors.Is(err, zk.ErrArgs):
		return "invalid verification parameters"
	case errors.Is(err, zk.ErrCircuit):
		return "unknown circuit"
	case errors.Is(err, zk.ErrInvalid):
		return "proof did not verify"
	default:
		return "verifier error"
	}
}

func handleVerify(reg *circuits.Registry, sem limiter) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, http.StatusMethodNotAllowed, verifyResponse{Error: "POST only"})
			return
		}
		start := time.Now()

		var req verifyRequest
		if err := json.NewDecoder(http.MaxBytesReader(w, r.Body, 8<<20)).Decode(&req); err != nil {
			writeJSON(w, http.StatusBadRequest, verifyResponse{Error: "malformed JSON: " + err.Error()})
			return
		}

		attrCBOR, err := hex.DecodeString(req.AttrCBORHex)
		if err != nil {
			writeJSON(w, http.StatusBadRequest, verifyResponse{Error: "attr_cbor_hex is not hex"})
			return
		}
		transcript, err := base64.StdEncoding.DecodeString(req.TranscriptB64)
		if err != nil {
			writeJSON(w, http.StatusBadRequest, verifyResponse{Error: "transcript_b64 is not base64"})
			return
		}
		proof, err := base64.StdEncoding.DecodeString(req.ProofB64)
		if err != nil {
			writeJSON(w, http.StatusBadRequest, verifyResponse{Error: "proof_b64 is not base64"})
			return
		}

		// One slot per request, taken before any verification work and released
		// on every path out.
		if !sem.tryAcquire() {
			busy(w)
			writeJSON(w, http.StatusServiceUnavailable, verifyResponse{
				Error: "verifier busy, retry", TookMS: time.Since(start).Milliseconds(),
			})
			return
		}
		defer sem.release()

		// EE-ZKP-023: check the circuit against the allowlist before verifying.
		hash, err := zk.CheckCircuit(reg, req.Version, req.NumAttributes)
		if err != nil {
			log.Printf("/zkverify: circuit rejected (version %d, %d attrs, hash %q): %v",
				req.Version, req.NumAttributes, hash, err)
			writeJSON(w, http.StatusForbidden, verifyResponse{
				CircuitHash: hash, Error: "circuit is not in the accepted set",
				TookMS: time.Since(start).Milliseconds(),
			})
			return
		}

		err = zk.Verify(zk.Request{
			Version: req.Version, NumAttributes: req.NumAttributes,
			PKx: req.PKx, PKy: req.PKy,
			DocType: req.DocType, Namespace: req.Namespace,
			AttrID: req.AttrID, AttrCBOR: attrCBOR,
			Now: req.Now, Transcript: transcript, Proof: proof,
		})
		took := time.Since(start).Milliseconds()

		switch {
		case err == nil:
			writeJSON(w, http.StatusOK, verifyResponse{
				Valid: true, CircuitHash: hash,
				AttrID: req.AttrID, AttrCBORHex: req.AttrCBORHex, TookMS: took,
			})
		case errors.Is(err, zk.ErrInvalid):
			// A proof that does not verify is an answer, not a failure: still 200,
			// still valid:false. Only the runtime's wording stays server-side.
			log.Printf("/zkverify: circuit %s: %v", hash, err)
			writeJSON(w, http.StatusOK, verifyResponse{
				Valid: false, CircuitHash: hash, TookMS: took, Error: verifyErrorCategory(err),
			})
		default:
			log.Printf("/zkverify: circuit %s: %v", hash, err)
			writeJSON(w, http.StatusBadRequest, verifyResponse{
				CircuitHash: hash, TookMS: took, Error: verifyErrorCategory(err),
			})
		}
	}
}

func main() {
	addr := flag.String("addr", ":8080", "listen address")
	registryPath := flag.String("registry", "circuits.json", "accepted circuit registry (EE-ZKP-030)")
	trustPath := flag.String("issuers", "issuers.json", "trusted attestation providers")
	baseURL := flag.String("base-url", "http://127.0.0.1:8080", "externally reachable base URL")
	clientID := flag.String("client-id", "x509_san_dns:verifier.example.ee", "OpenID4VP client identifier")
	docType := flag.String("doctype", "ee.riik.poa.1", "doctype to request")
	sessionTTL := flag.Duration("session-ttl", 3*time.Minute, "presentation request lifetime")
	unsafeDevAPI := flag.Bool("unsafe-dev-api", false,
		"expose POST /zkverify, the unauthenticated low-level API (development only)")
	maxVerify := flag.Int("max-concurrent-verify", runtime.NumCPU(),
		"verifications allowed in flight at once; excess requests get 503")
	dcapiOrigin := flag.String("dcapi-origin", "",
		"browser origin the Digital Credentials API (ISO 18013-7 Annex C) handover binds; "+
			"required for /present/dcapi/*, never taken from request headers")
	flag.Parse()

	if *maxVerify < 1 {
		log.Fatalf("-max-concurrent-verify must be at least 1, got %d", *maxVerify)
	}
	if *dcapiOrigin == "" {
		log.Printf("WARNING: -dcapi-origin is not set; the ISO 18013-7 Annex C path (POST /present/dcapi/new) is disabled")
	}
	// Verification is CPU-bound and each call holds an OS thread, so one slot
	// per core is the useful ceiling; more only queues inside the scheduler.
	sem := make(limiter, *maxVerify)
	log.Printf("verifying at most %d proofs concurrently", *maxVerify)

	reg, err := circuits.Load(*registryPath)
	if err != nil {
		log.Fatalf("circuit registry: %v", err)
	}
	// Plan §5.3 (startup): hash every registry entry through the FFI and refuse
	// to start on mismatch — a registry file whose hash disagrees with the
	// binary would silently offer circuits the runtime cannot prove.
	for _, c := range reg.Accepted() {
		got, err := zk.CircuitHash(c.Version, c.NumAttributes)
		if err != nil {
			log.Fatalf("circuit registry: %s (v%d, %d attrs): %v", c.Hash, c.Version, c.NumAttributes, err)
		}
		if got != c.Hash {
			log.Fatalf("circuit registry: entry for (v%d, %d attrs) hashes to %s, registry says %s",
				c.Version, c.NumAttributes, got, c.Hash)
		}
		log.Printf("accepting circuit %s (v%d, %d attrs)", c.Hash, c.Version, c.NumAttributes)
	}
	trust, err := oid4vp.LoadTrustStore(*trustPath)
	if err != nil {
		log.Fatalf("trust store: %v", err)
	}
	for _, is := range trust.All() {
		log.Printf("trusting issuer %q for doctype %s", is.Name, is.DocType)
	}

	p := &presenter{
		store:       oid4vp.NewStore(*sessionTTL),
		trust:       trust,
		registry:    reg,
		clientID:    *clientID,
		baseURL:     strings.TrimSuffix(*baseURL, "/"),
		docType:     *docType,
		nsID:        *docType,
		sem:         sem,
		dcapiOrigin: *dcapiOrigin,
		offered:     reg.Accepted(),
	}

	mux := http.NewServeMux()
	// /zkverify takes the issuer key and `now` from the caller, so a caller who
	// chooses both can mint their own attestation and prove anything. It is a
	// test harness, not a presentation endpoint, and stays unregistered — and
	// therefore a 404 — unless it is asked for explicitly.
	if *unsafeDevAPI {
		log.Printf("WARNING: -unsafe-dev-api is set; POST /zkverify is exposed.")
		log.Printf("WARNING: it is unauthenticated, and it trusts the issuer public key")
		log.Printf("WARNING: and the `now` timestamp supplied by whoever calls it.")
		log.Printf("WARNING: anything it reports valid is only valid to that caller.")
		log.Printf("WARNING: never expose this on a reachable interface.")
		mux.HandleFunc("/zkverify", handleVerify(reg, sem))
	}
	mux.HandleFunc("/present/new", p.handleNew)
	mux.HandleFunc("/present/request/", p.handleRequest)
	mux.HandleFunc("/present/response/", p.handleResponse)
	mux.HandleFunc("/present/result/", p.handleResult)
	// The ISO 18013-7 Annex C path only exists when an origin is configured:
	// without one the handover has nothing to bind and every session it could
	// start would be unverifiable by construction.
	if *dcapiOrigin != "" {
		mux.HandleFunc("/present/dcapi/new", p.handleDCAPINew)
		mux.HandleFunc("/present/dcapi/request/", p.handleDCAPIRequest)
		mux.HandleFunc("/present/dcapi/response/", p.handleDCAPIResponse)
		mux.HandleFunc("/present/dcapi/result/", p.handleDCAPIResult)
		mux.HandleFunc("/present/dcapi/", p.handleDCAPIPage)
	}
	mux.HandleFunc("/circuits", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{"circuits": reg.Accepted()})
	})
	mux.HandleFunc("/issuers", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]any{"issuers": trust.All()})
	})
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintln(w, "ok")
	})

	srv := &http.Server{
		Addr:              *addr,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      60 * time.Second,
	}
	log.Printf("zkverify listening on %s", *addr)
	if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
		log.Printf("server: %v", err)
		os.Exit(1)
	}
}
