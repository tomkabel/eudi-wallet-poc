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
//
// -version prints the longfellow-zk revision the FFI links, injected at build
// time via -ldflags "-X main.longfellowRev=$(cat
// verifier/zkverify-ffi/longfellow-rev.txt)" (make deps does this). The
// cross-check against circuits.json's recorded upstream release stays a human
// step: nothing fetches or compares at runtime.
package main

import (
	"context"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"
	"net/url"
	"os"
	"os/signal"
	"runtime"
	"strings"
	"syscall"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/zk"
)

// longfellowRev is set at build time by -ldflags -X; -version prints it. It
// records which longfellow-zk the linked Rust runtime was built from — the
// hash in verifier/zkverify-ffi/longfellow-rev.txt, not whatever the sibling
// checkout happens to be at now.
var longfellowRev string

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
	if l == nil {
		return true // no limit configured
	}
	select {
	case l <- struct{}{}:
		return true
	default:
		return false
	}
}

func (l limiter) release() {
	if l != nil {
		<-l
	}
}

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

func handleVerify(reg *circuits.Registry, sem, reads limiter) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			writeJSON(w, http.StatusMethodNotAllowed, verifyResponse{Error: "POST only"})
			return
		}
		start := time.Now()

		// Body reads get their own, larger pool: reading and decoding up to 8 MB
		// happens before the verification slot is taken, and must be bounded too.
		if !reads.tryAcquire() {
			busy(w)
			writeJSON(w, http.StatusServiceUnavailable, verifyResponse{Error: "verifier busy, retry"})
			return
		}
		defer reads.release()

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

// requestKeyPublicOf returns nil for a nil key, else the public half — a
// tiny indirection so the presenter literal stays assignment-shaped.
func requestKeyPublicOf(k *jose.JWK) *jose.JWK {
	if k == nil {
		return nil
	}
	pk := k.Public()
	return &pk
}

// requireTLSBaseURL refuses an externally-reachable -base-url that is not
// https: the request_uri and response_uri a wallet acts on come from it, and
// a cleartext base URL lets a network observer rewrite both (JAR signing
// protects the request object's contents, not the URIs the wallet fetches).
// Loopback spellings are allowed for development.
func requireTLSBaseURL(baseURL string) error {
	u, err := url.Parse(baseURL)
	if err != nil {
		return fmt.Errorf("parse -base-url: %w", err)
	}
	switch u.Scheme {
	case "https":
		return nil
	case "http":
		host := u.Hostname()
		if host == "127.0.0.1" || host == "::1" || host == "localhost" {
			return nil
		}
		return fmt.Errorf("-base-url %q is http on a non-loopback host; use https", baseURL)
	default:
		return fmt.Errorf("-base-url scheme %q is neither http nor https", u.Scheme)
	}
}

// requireLoopbackAddr insists an address is loopback-only. The unauthenticated
// dev API must never end up on a reachable interface — the default -addr :8080
// binds all of them, which is exactly the mistake this guard refuses.
func requireLoopbackAddr(addr string) error {
	host, _, err := net.SplitHostPort(addr)
	if err != nil {
		return fmt.Errorf("cannot parse -addr %q: %w", addr, err)
	}
	switch host {
	case "127.0.0.1", "::1", "localhost":
		return nil
	default:
		return fmt.Errorf("requires a loopback -addr (127.0.0.1, ::1 or localhost), got %q", addr)
	}
}

// checkLongfellowRev fails startup when the binary was linked against a
// longfellow-zk that is not the one verifier/zkverify-ffi/longfellow-rev.txt
// records. The revfile is the contract (make deps pins it); a drifted sibling
// checkout means the linked runtime no longer matches the published profile
// the audit chain assumes.
func checkLongfellowRev(linked, revFile string) error {
	if linked == "" || revFile == "" {
		// -version and dev builds without ldflags: nothing to compare.
		return nil
	}
	raw, err := os.ReadFile(revFile)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return fmt.Errorf("read longfellow rev file: %w", err)
	}
	want := strings.TrimSpace(string(raw))
	if want == "" {
		return nil
	}
	if linked != want {
		return fmt.Errorf("binary links longfellow-zk %s but %s records %s — rebuild with make deps, or update the rev file",
			linked, revFile, want)
	}
	return nil
}

func main() {
	addr := flag.String("addr", ":8080", "listen address")
	registryPath := flag.String("registry", "circuits.json", "accepted circuit registry (EE-ZKP-030)")
	trustPath := flag.String("issuers", "issuers.json", "trusted attestation providers")
	trustRootFile := flag.String("trust-root", "",
		"PEM file holding the EC P-256 key that signs the trust store (B3); "+
			"empty disables signature verification (development)")
	ignoreTrustFreshness := flag.Bool("ignore-trust-freshness", false,
		"skip the signed trust store's not_before/not_after windows (development)")
	baseURL := flag.String("base-url", "http://127.0.0.1:8080", "externally reachable base URL")
	clientID := flag.String("client-id", "x509_san_dns:verifier.example.ee", "OpenID4VP client identifier")
	docType := flag.String("doctype", "ee.riik.poa.1", "doctype to request")
	sessionTTL := flag.Duration("session-ttl", 3*time.Minute, "presentation request lifetime")
	responseMode := flag.String("response-mode", "direct_post.jwt",
		"response mode: direct_post.jwt (encrypted, default) or direct_post (plain)")
	responseKeyFile := flag.String("response-key-file", "",
		"PEM file holding the EC P-256 response-encryption key (SEC 1 or PKIX); "+
			"generated fresh at startup when empty — persist one for restart-stable thumbprints")
	requestKeyFile := flag.String("request-key-file", "",
		"PEM file holding the EC P-256 request-object signing key (JAR, RFC 9101); "+
			"empty keeps the request object unsigned (development)")
	allowUnencrypted := flag.Bool("allow-unencrypted-response", false,
		"permit -response-mode direct_post; the explicit downgrade switch for the encrypted default (ADR-003)")
	unsafeDevAPI := flag.Bool("unsafe-dev-api", false,
		"expose POST /zkverify, the unauthenticated low-level API (development only)")
	carrierName := flag.String("carrier", "",
		"force the vp_token carrier for every session: interim-json, mso-mdoc-zk-cbor, "+
			"or empty (the default) to dispatch per query format, sniffing the entry bytes")
	maxVerify := flag.Int("max-concurrent-verify", runtime.NumCPU(),
		"verifications allowed in flight at once; excess requests get 503")
	dcapiOrigin := flag.String("dcapi-origin", "",
		"browser origin the Digital Credentials API (ISO 18013-7 Annex C) handover binds; "+
			"required for /present/dcapi/*, never taken from request headers")
	version := flag.Bool("version", false, "print the linked longfellow-zk revision and exit")
	flag.Parse()

	if *version {
		if longfellowRev == "" {
			fmt.Println("zkverify: longfellow revision not recorded (binary built without -ldflags -X main.longfellowRev)")
		} else {
			fmt.Println("zkverify longfellow-zk", longfellowRev)
		}
		return
	}

	if *maxVerify < 1 {
		log.Fatalf("-max-concurrent-verify must be at least 1, got %d", *maxVerify)
	}
	if err := checkLongfellowRev(longfellowRev, "../zkverify-ffi/longfellow-rev.txt"); err != nil {
		log.Fatalf("%v", err)
	}
	if *unsafeDevAPI {
		if err := requireLoopbackAddr(*addr); err != nil {
			log.Fatalf("-unsafe-dev-api: %v", err)
		}
	}
	if err := requireTLSBaseURL(*baseURL); err != nil {
		log.Fatalf("%v", err)
	}
	carrierOverride, err := oid4vp.ParseCarrierName(*carrierName)
	if err != nil {
		log.Fatalf("%v", err)
	}
	switch *responseMode {
	case "direct_post.jwt":
	case "direct_post":
		if !*allowUnencrypted {
			log.Fatalf("-response-mode direct_post needs -allow-unencrypted-response: " +
				"the plain mode ships the ~360 KB proof and the holder's answers unencrypted (ADR-003)")
		}
		log.Printf("WARNING: -response-mode direct_post: responses are NOT encrypted; " +
			"HAIP 1.0 and EE-PRO-001 require direct_post.jwt — this is an explicit downgrade")
	default:
		log.Fatalf("-response-mode must be direct_post.jwt or direct_post, got %q", *responseMode)
	}

	// The response-encryption key. The default is generate-at-startup, which
	// rotates the key (and every transcript's thumbprint) per process — fine
	// for the demo, where sessions die with it. Persist a PEM via
	// -response-key-file when sessions must outlive a restart.
	var responseKey *jose.JWK
	if *responseMode == "direct_post.jwt" {
		if *responseKeyFile != "" {
			pemBytes, err := os.ReadFile(*responseKeyFile)
			if err != nil {
				log.Fatalf("response key: %v", err)
			}
			responseKey, err = jose.LoadPEMKey(pemBytes)
			if err != nil {
				log.Fatalf("response key %s: %v", *responseKeyFile, err)
			}
			log.Printf("response key: loaded %s (thumbprint %s)",
				*responseKeyFile, jose.ThumbprintB64(responseKey))
		} else {
			k, err := jose.GenerateKey()
			if err != nil {
				log.Fatalf("response key generation: %v", err)
			}
			responseKey = &k
			log.Printf("response key: generated fresh for this run (thumbprint %s); "+
				"use -response-key-file to persist one across restarts",
				jose.ThumbprintB64(responseKey))
		}
	}
	var requestKey *jose.JWK
	if *requestKeyFile != "" {
		pemBytes, err := os.ReadFile(*requestKeyFile)
		if err != nil {
			log.Fatalf("request key: %v", err)
		}
		requestKey, err = jose.LoadPEMKey(pemBytes)
		if err != nil {
			log.Fatalf("request key %s: %v", *requestKeyFile, err)
		}
		rkPub := requestKey.Public()
		log.Printf("request key: loaded %s (thumbprint %s); request objects are signed as JAR",
			*requestKeyFile, jose.ThumbprintB64(&rkPub))
	}
	if *dcapiOrigin == "" {
		log.Printf("WARNING: -dcapi-origin is not set; the ISO 18013-7 Annex C path (POST /present/dcapi/new) is disabled")
	}
	// Verification is CPU-bound and each call holds an OS thread, so one slot
	// per core is the useful ceiling; more only queues inside the scheduler.
	sem := make(limiter, *maxVerify)
	// Requests reading or decoding a body. Larger than sem so parsing never
	// starves verification, but bounded so a flood is shed before any body is read.
	// The overlap with sem is short and load-shed already exists, so the 4x
	// multiplier is accepted rather than capped (audit GO-I8).
	reads := make(limiter, 4**maxVerify)
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
	var trust *oid4vp.TrustStore
	if *trustRootFile != "" {
		pemBytes, err := os.ReadFile(*trustRootFile)
		if err != nil {
			log.Fatalf("trust root: %v", err)
		}
		root, err := jose.LoadPEMKey(pemBytes)
		if err != nil {
			log.Fatalf("trust root %s: %v", *trustRootFile, err)
		}
		rootPub := root.Public()
		trust, err = oid4vp.LoadSignedTrustStore(*trustPath, &rootPub, nil, time.Now(), *ignoreTrustFreshness)
		if err != nil {
			log.Fatalf("trust store: %v", err)
		}
		log.Printf("trust store: verified signature under trust root (freshness checks %s)",
			map[bool]string{true: "skipped", false: "enforced"}[*ignoreTrustFreshness])
	} else {
		t, err := oid4vp.LoadTrustStore(*trustPath)
		if err != nil {
			log.Fatalf("trust store: %v", err)
		}
		trust = t
	}
	for _, is := range trust.All() {
		log.Printf("trusting issuer %q for doctype %s", is.Name, is.DocType)
	}

	p := &presenter{
		store:            oid4vp.NewStore(*sessionTTL),
		trust:            trust,
		registry:         reg,
		clientID:         *clientID,
		baseURL:          strings.TrimSuffix(*baseURL, "/"),
		docType:          *docType,
		nsID:             *docType,
		carrier:          carrierOverride,
		sem:              sem,
		reads:            reads,
		responseKey:      responseKey,
		allowPlain:       *allowUnencrypted,
		requestKey:       requestKey,
		requestKeyPublic: requestKeyPublicOf(requestKey),
		dcapiOrigin:      *dcapiOrigin,
		offered:          reg.Accepted(),
	}

	srvAddr := *addr
	mux := http.NewServeMux()
	// /zkverify takes the issuer key and `now` from the caller, so a caller who
	// chooses both can mint their own attestation and prove anything. It is a
	// test harness, not a presentation endpoint, and stays unregistered — and
	// therefore a 404 — unless it is asked for explicitly.
	if *unsafeDevAPI {
		// Defence in depth: the flag was checked against *addr at startup; the
		// registered interface is the server's actual bind address, so recheck
		// against it too. Default -addr :8080 binds all interfaces — exactly
		// the trap this guard closes.
		if err := requireLoopbackAddr(srvAddr); err != nil {
			log.Fatalf("-unsafe-dev-api: %v", err)
		}
		log.Printf("WARNING: -unsafe-dev-api is set; POST /zkverify is exposed.")
		log.Printf("WARNING: it is unauthenticated, and it trusts the issuer public key")
		log.Printf("WARNING: and the `now` timestamp supplied by whoever calls it.")
		log.Printf("WARNING: anything it reports valid is only valid to that caller.")
		log.Printf("WARNING: never expose this on a reachable interface.")
		mux.HandleFunc("/zkverify", handleVerify(reg, sem, reads))
	}
	mux.HandleFunc("/present/new", p.handleNew)
	mux.HandleFunc("/present/request/", p.handleRequest)
	mux.HandleFunc("/present/response/", p.handleResponse)
	mux.HandleFunc("/present/result/", p.handleResult)
	// The response-encryption key, published as a JWKS for wallets that fetch
	// instead of reading client_metadata.jwks from the request. The key is
	// generated at startup (or loaded with -response-key-file), so this is
	// stable for the life of the process.
	if responseKey != nil {
		mux.HandleFunc("/present/jwks.json", func(w http.ResponseWriter, r *http.Request) {
			writeJSON(w, http.StatusOK, jose.JWKSet{Keys: []jose.JWK{responseKey.Public()}})
		})
	}
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
		_, _ = fmt.Fprintln(w, "ok")
	})

	srv := &http.Server{
		Addr:              *addr,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      60 * time.Second,
		// Keep-alives parked longer than this are closed; a fresh connection
		// costs far less than the file descriptors of idle browsers piling up.
		IdleTimeout: 60 * time.Second,
	}
	log.Printf("zkverify listening on %s", *addr)

	// Graceful shutdown: SIGINT/SIGTERM stops accepting and drains in-flight
	// requests instead of cutting them off mid-verify. A cgo verification can
	// hold a request for seconds, so the drain window is ~10s — beyond it the
	// context closes the connection anyway.
	serveErr := make(chan error, 1)
	go func() { serveErr <- srv.ListenAndServe() }()
	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt, syscall.SIGTERM)
	select {
	case err := <-serveErr:
		if err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Printf("server: %v", err)
			os.Exit(1)
		}
	case s := <-sig:
		log.Printf("zkverify: %v received, draining", s)
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := srv.Shutdown(ctx); err != nil {
			log.Printf("zkverify: drain did not finish in time: %v", err)
		} else {
			log.Printf("zkverify: drained cleanly")
		}
		// ListenAndServe returns ErrServerClosed once Shutdown completes; the
		// goroutine's send lands in the buffered channel and is dropped.
		<-serveErr
	}
}
