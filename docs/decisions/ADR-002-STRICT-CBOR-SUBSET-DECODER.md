# ADR-002 — The ISO dcapi path decodes a strict CBOR subset, in-house

**Status:** accepted, 24 September 2026.

## Context

The OpenID4VP redirect path never decodes CBOR from the holder: the verifier
builds the request, and the response is a JSON envelope whose payloads are
opaque byte strings fed to the FFI. ISO/IEC 18013-7 Annex C (the Digital
Credentials API path, plan step 2a) changes that — the DeviceResponse arrives
*inside* the HPKE envelope, and every byte of it after decryption is
attacker-supplied. Decoding it is the first place this verifier parses
holder-controlled CBOR.

Two existing options were measured against that:

| Option | Rejected because |
|---|---|
| `fxamacker/cbor/v2` (full profile) | This module's first third-party runtime dependency; imports the whole CBOR surface — floats, negative ints, indefinite lengths, arbitrary tags, bigints — none of which a conformant DeviceResponse carries. Every accepted feature is attack surface on a path whose input is hostile by construction. |
| Hand-rolled decoding *ad hoc* inside `oid4vp` | Exactly the pattern that let defect S11 happen: parse first, constrain later, and the FFI reachable from malformed input. A decoder with no stated subset decays into "whatever the test data exercises". |

## Decision

`internal/cborsub` decodes exactly the subset a DeviceResponse needs, and
fails closed on everything else:

- definite lengths only; shortest-form heads only (a non-minimal argument is
  rejected, not normalised);
- maps, arrays, byte and text strings, unsigned integers, tag 24, `false`,
  `true`, `null` — nothing else, with one explicit extension: tag 0 over a
  tstr (multipaz 0.99.0 writes `ZkDocumentData.timestamp` that way), kept as
  `KTag0` so a caller can insist on it;
- tag 24 content must itself decode as this subset (RFC 8949 §8.1 requires
  tagged CBOR to be valid CBOR);
- duplicate map keys, of any kind, are a rejection, not a last-one-wins;
- hard limits on nesting depth (32, counted across tag-24 boundaries), items
  per array/map (1024) and string size (1 MiB), enforced during decode.

Anything a future DeviceResponse shape legitimately needs is an explicit
extension of the subset with a test, not a loosening.

## Consequences

- The ISO handler's error messages name the subset rule that fired, which
  makes the refusals auditable against ADR and plan in a way a generic
  library's errors are not.
- A fuzz target (`FuzzDecode`, seed corpus = the committed vectors) pins the
  fail-closed property; `go test -fuzz FuzzDecode` in `internal/cborsub` is
  the regression loop for any future subset extension.
- Cost: ~350 lines of decoder we own, including its bugs. Accepted: the
  subset is small, the fuzz corpus is committed, and the alternative
  (trusting a dependency boundary we do not control on a hostile-input path)
  is the wrong trade for a verifier whose claim is auditability.
- If a second consumer needs the same subset, promote `internal/cborsub` to a
  versioned module — do not fork it per package.

## Alternatives rejected

- **`fxamacker/cbor/v2` with a strict `DecOptions`**: still accepts the whole
  grammar between the options one remembers to set, and each new option is a
  silent widening. The subset here is closed by construction.
- **No decoder — treat the DeviceResponse as opaque bytes and hand slices to
  the FFI**: the FFI takes the proof and document fields *individually*
  (`zk.Request`), so the response must be parsed somewhere; the only question
  is whether the parser has a stated subset.
- **JSON in the envelope instead of CBOR**: the DeviceResponse is CBOR by
  ISO/IEC 18013-5; re-encoding on the wallet to please this verifier would
  fork the wire format the fork's PoC already speaks.
