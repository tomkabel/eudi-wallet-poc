package oid4vp

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"net/url"
	"path"
	"sync"
	"time"
)

// Session is one presentation exchange, from request creation to result.
type Session struct {
	ID          string    `json:"id"`
	Nonce       string    `json:"nonce"`
	ClientID    string    `json:"client_id"`
	ResponseURI string    `json:"response_uri"`
	Query       DCQL      `json:"dcql_query"`
	Created     time.Time `json:"created"`
	ExpectedNow string    `json:"expected_now"`

	// RequestFetchDone marks the request_uri consumed: one request fetch
	// per session, ever (B2 request_uri replay hardening). Not in JSON: it
	// is live process state, like Answered.
	RequestFetchDone bool `json:"-"`

	// Filled in once a response arrives.
	Answered bool   `json:"answered"`
	Valid    bool   `json:"valid"`
	Detail   string `json:"detail,omitempty"`

	// The ISO 18013-7 Annex C (dcapi) extension, nil on the redirect path.
	// Unexported: it holds the HPKE private key and must never reach the JSON
	// snapshots the API hands out. origin is the configured -dcapi-origin the
	// session's handover hashes; never a request header (plan §5.3).
	iso    *ISOExtension
	origin string

	// Response encryption (direct_post.jwt), ADR-003. jwkThumbprint is the
	// RFC 7638 SHA-256 thumbprint of the verifier's response key — nil means
	// the plain, unencrypted direct_post mode. Unexported: like iso, it must
	// not silently ride along on JSON snapshots; a session either requires
	// encryption (then the response MUST be a JWE this key opens) or it does
	// not (then a JWE is still accepted — encryption is never a downgrade).
	RequireEncryptedResponse bool
	jwkThumbprint            []byte
}

// Expired reports whether the session is past its time-to-live. A presentation
// request is short-lived: the nonce is the replay defence and it must not be
// reusable for long.
func (s *Session) Expired(ttl time.Duration) bool {
	return time.Since(s.Created) > ttl
}

// Store holds in-flight sessions. A real deployment would use something
// durable and shared; nothing here is persisted, deliberately — a verifier that
// keeps presentation records is a verifier that can be subpoenaed for them.
//
// Expiry is maintained by a janitor goroutine (tick = TTL/4, delete expired
// under the store lock) instead of a full-map sweep on insert: the sweep made
// every put pay the pathological case's cost. put itself does one expiry
// check on insert only. Stop ends the janitor; main runs it on shutdown.
type Store struct {
	mu       sync.Mutex
	sessions map[string]*Session
	ttl      time.Duration
	// now is indirected for tests: the janitor tests pin a fake clock
	// instead of sleeping for a real TTL.
	now func() time.Time
	// stop closes to end the janitor; done confirms it has exited.
	stop chan struct{}
	done chan struct{}
}

const maxSessions = 10_000

func NewStore(ttl time.Duration) *Store {
	st := &Store{
		sessions: make(map[string]*Session),
		ttl:      ttl,
		now:      time.Now,
		stop:     make(chan struct{}),
		done:     make(chan struct{}),
	}
	if ttl > 0 {
		go st.janitor()
	} else {
		close(st.done) // a zero-TTL store (test fixture) has nothing to reap
	}
	return st
}

// janitor reaps expired sessions every TTL/4, so an expired entry is gone at
// most one tick after it lapses and the scan cost stays bounded: one full
// sweep per tick under the lock, never per insert. Even the pathological case
// (every entry expired) costs one bounded sweep per tick, not one per put.
func (st *Store) janitor() {
	defer close(st.done)
	tick := st.ttl / 4
	if tick < time.Second {
		tick = time.Second // never spin
	}
	t := time.NewTicker(tick)
	defer t.Stop()
	for {
		select {
		case <-st.stop:
			return
		case <-t.C:
			st.sweep()
		}
	}
}

// sweep deletes every expired session under the lock. It is the janitor's
// body and runs nowhere else — put never ranges the map.
func (st *Store) sweep() {
	st.mu.Lock()
	defer st.mu.Unlock()
	for id, existing := range st.sessions {
		if existing.Expired(st.ttl) {
			delete(st.sessions, id)
		}
	}
}

// Stop ends the janitor goroutine and waits for it to exit. It is idempotent
// (a second Stop returns immediately) and safe to call on a zero-TTL store.
// main calls it on shutdown so the process leaves no running goroutine behind.
func (st *Store) Stop() {
	select {
	case <-st.stop:
	default:
		close(st.stop)
	}
	<-st.done
}

func randomB64(n int) (string, error) {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		return "", fmt.Errorf("oid4vp: random: %w", err)
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}

// New creates a session with a fresh nonce. A non-nil thumbprint opts the
// session into response encryption: the handover binds the key's RFC 7638
// thumbprint and the session requires a direct_post.jwt response. The query
// list is validated under the default credential cap (MaxCredentialsDefault).
func (st *Store) New(clientID, responseURIBase string, q DCQL, jwkThumbprint []byte) (*Session, error) {
	if _, err := q.Validate(0); err != nil {
		return nil, err
	}
	return st.newSession(clientID, responseURIBase, "", q, jwkThumbprint)
}

// NewWith is New with the nonce and response URI chosen by the caller instead
// of generated. It exists for the OpenID4VP interop tests (the nonce is part of
// the B.2.6.1 handover, so it has to be the value the holder's proof was bound
// to); production sessions keep using New. The session ID is the response
// URI's last path segment, as New's is, so /present/response/<id> finds it.
func (st *Store) NewWith(clientID, responseURI, nonce string, q DCQL, jwkThumbprint []byte) (*Session, error) {
	if _, err := q.Validate(0); err != nil {
		return nil, err
	}
	return st.newSession(clientID, responseURI, nonce, q, jwkThumbprint)
}

// NewMulti is New for an explicit multi-credential session: maxCaps bounds how
// many credential queries the session may carry (<= 0: MaxCredentialsDefault).
// The plain-format constructors above keep the default cap.
func (st *Store) NewMulti(clientID, responseURIBase string, q DCQL, maxCreds int, jwkThumbprint []byte) (*Session, error) {
	if _, err := q.Validate(maxCreds); err != nil {
		return nil, err
	}
	return st.newSession(clientID, responseURIBase, "", q, jwkThumbprint)
}

// NewWithMulti is NewWith with the same explicit credential cap.
func (st *Store) NewWithMulti(clientID, responseURI, nonce string, q DCQL, maxCreds int, jwkThumbprint []byte) (*Session, error) {
	if _, err := q.Validate(maxCreds); err != nil {
		return nil, err
	}
	return st.newSession(clientID, responseURI, nonce, q, jwkThumbprint)
}

// newSession is the shared constructor body behind New, NewWith and the Multi
// variants. nonce == "" generates the nonce, id and response URI (the New
// shape); a caller-supplied nonce means the response URI is complete and its
// last path segment is the id (the NewWith shape). The query is already
// validated when this runs.
func (st *Store) newSession(clientID, responseURI, nonce string, q DCQL, jwkThumbprint []byte) (*Session, error) {
	if err := checkThumbprint(jwkThumbprint); err != nil {
		return nil, err
	}
	if nonce == "" {
		var err error
		var id string
		if id, err = randomB64(12); err != nil {
			return nil, err
		}
		if nonce, err = randomB64(32); err != nil {
			return nil, err
		}
		responseURI = fmt.Sprintf("%s/%s", responseURI, id)
	} else {
		if clientID == "" || nonce == "" || responseURI == "" {
			return nil, errors.New("oid4vp: clientID, nonce and responseURI are all required")
		}
		u, err := url.Parse(responseURI)
		if err != nil {
			return nil, fmt.Errorf("oid4vp: responseURI: %w", err)
		}
		if id := path.Base(u.Path); id == "." || id == "/" {
			return nil, errors.New("oid4vp: responseURI has no final path segment to use as the session id")
		}
	}
	id := path.Base(responseURI)
	created := time.Now()
	s := &Session{
		ID:          id,
		Nonce:       nonce,
		ClientID:    clientID,
		ResponseURI: responseURI,
		Query:       q,
		Created:     created,
		ExpectedNow: created.UTC().Format("2006-01-02T15:04:05Z"),

		jwkThumbprint:            jwkThumbprint,
		RequireEncryptedResponse: jwkThumbprint != nil,
	}
	return st.put(s)
}

// checkThumbprint validates the optional response-key thumbprint: nil is the
// unencrypted mode, otherwise it must be exactly a SHA-256 digest.
func checkThumbprint(tp []byte) error {
	if tp == nil {
		return nil
	}
	if len(tp) != sha256.Size {
		return fmt.Errorf("oid4vp: jwkThumbprint must be %d bytes or nil, got %d", sha256.Size, len(tp))
	}
	return nil
}

// put installs a built session, applying the shared expiry/size discipline.
// It never ranges the map: a full store sweeps nothing here — the janitor is
// what reclaims expired slots, and put refuses with ErrStoreFull until it
// does. put only checks the session it is inserting, one expiry check.
func (st *Store) put(s *Session) (*Session, error) {
	id := s.ID
	st.mu.Lock()
	defer st.mu.Unlock()
	// One expiry check on insert: a session that is already past its TTL at
	// creation is refused outright, so an expired entry can never re-enter
	// the store — not even for one janitor tick.
	if s.Created.Before(st.now().Add(-st.ttl)) {
		return nil, ErrExpired
	}
	if len(st.sessions) >= maxSessions {
		return nil, ErrStoreFull
	}
	// New's 96-bit random IDs never collide; a caller-chosen NewWith ID can,
	// and must not silently replace a live session.
	if _, ok := st.sessions[id]; ok {
		return nil, ErrIDInUse
	}
	st.sessions[id] = s
	return snapshot(s), nil
}

var (
	ErrNoSession   = errors.New("oid4vp: no such session")
	ErrExpired     = errors.New("oid4vp: session expired")
	ErrAlreadyUsed = errors.New("oid4vp: session already answered")
	ErrStoreFull   = errors.New("oid4vp: too many active sessions")
	ErrIDInUse     = errors.New("oid4vp: session id already in use")
	// ErrRequestFetched: the request_uri was already consumed (B2).
	ErrRequestFetched = errors.New("oid4vp: request already fetched")
)

// ConsumeRequestFetch marks the session's request object as fetched. The
// first call succeeds; every later call refuses — a leaked request_uri
// cannot be replayed to re-read the nonce-bearing request (B2).
func (st *Store) ConsumeRequestFetch(id string) error {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return ErrNoSession
	}
	if s.RequestFetchDone {
		return ErrRequestFetched
	}
	s.RequestFetchDone = true
	return nil
}

// delete removes a session under the store lock. It exists so a failed NewISO
// does not strand a half-built session (its HPKE private key included) in the
// store: New already published the id, so the caller cannot just "not put" it.
func (st *Store) delete(id string) {
	st.mu.Lock()
	defer st.mu.Unlock()
	delete(st.sessions, id)
}

func snapshot(s *Session) *Session {
	copy := *s
	return &copy
}

// Get returns a snapshot of a live session.
func (st *Store) Get(id string) (*Session, error) {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return nil, ErrNoSession
	}
	if s.Expired(st.ttl) {
		delete(st.sessions, id)
		return nil, ErrExpired
	}
	return snapshot(s), nil
}

// Claim returns the session and marks it answered, so a captured vp_token
// cannot be replayed against the same nonce.
func (st *Store) Claim(id string) (*Session, error) {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return nil, ErrNoSession
	}
	if s.Expired(st.ttl) {
		delete(st.sessions, id)
		return nil, ErrExpired
	}
	if s.Answered {
		return nil, ErrAlreadyUsed
	}
	s.Answered = true
	return snapshot(s), nil
}

// Complete records the result and returns a snapshot of the completed session.
// It refuses to write a verdict onto a session that was never claimed (the
// response handler's Claim is what marks it answered) or that has expired —
// either way the result would attach to a presentation nobody verified.
func (st *Store) Complete(id string, valid bool, detail string) (*Session, error) {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return nil, ErrNoSession
	}
	if s.Expired(st.ttl) {
		delete(st.sessions, id)
		return nil, ErrExpired
	}
	if !s.Answered {
		return nil, ErrNoSession
	}
	s.Valid = valid
	s.Detail = detail
	return snapshot(s), nil
}

// AttachISO installs the dcapi extension on the STORED session. NewISO calls
// this because New returns a snapshot: mutating the snapshot would silently
// drop the extension (the store copy keeps iso nil).
func (st *Store) AttachISO(id string, ext *ISOExtension, origin string) error {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return ErrNoSession
	}
	s.iso = ext
	s.origin = origin
	return nil
}

// SingleQuery returns the session's sole credential query. Sessions created
// through New, NewWith, NewISO and NewISOWith carry exactly one credential by
// construction — the plain-format and ISO dcapi paths are single-credential by
// policy (dcql.go Validate); a session from NewMulti/NewWithMulti carries a
// list and callers walk it with s.Query.Credentials instead.
func (s *Session) SingleQuery() (CredentialQuery, error) {
	return s.Query.Single()
}

// ISOTranscript is the dcapi session transcript, computed from the session's
// own stored EncryptionInfo and origin — never from anything on the wire
// (plan §5.3).
func (s *Session) ISOTranscript() ([]byte, error) {
	if s.iso == nil {
		return nil, errors.New("oid4vp: session has no dcapi extension")
	}
	return ISOTranscript(s.iso.EncryptionInfoB64, s.origin)
}

// Transcript is the session transcript this session's holder must sign over.
// The third handover element is the response key's RFC 7638 thumbprint when
// the session was created for direct_post.jwt, and null for plain
// direct_post — which bytes it is was fixed at session creation, never by
// anything on the response.
func (s *Session) Transcript() ([]byte, error) {
	return SessionTranscript(s.ClientID, s.Nonce, s.jwkThumbprint, s.ResponseURI)
}

// JWKThumbprint returns the response-key thumbprint the session's handover
// binds: nil for the unencrypted direct_post mode.
func (s *Session) JWKThumbprint() []byte { return s.jwkThumbprint }
