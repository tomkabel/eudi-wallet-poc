# Signed offline circuit fallback

Story 3.5 implements ADR-006's pinned-document prerequisite. The verifier and Android wallet consume the same [signed document](../../circuits.json) and [public pin](pinned-key.json). It is community-maintained local acceptance material, with no scheme-owner authority. Hosted fetching, succession and session provenance remain Epic 5 work.

Go main loads LoadSigned at startup, logs list_version and checks each accepted hash against the linked FFI. Cached lookups, session creation, DCAPI request delivery and proof verification recheck expiry and name stale-list. A stored session offer cannot extend the signed document's lifetime. The optional -circuit-list-key flag replaces the default embedded public pin explicitly. Unsigned Load remains a source/test helper and is not the server startup path.

The Android generateCircuitRegistry task copies those exact files into app/build/generated/circuitRegistry/circuit-registry and packages them as Java resources. BuildConfig.CIRCUIT_LIST_VERSION is generated from the document. SignedCircuitFallbackTest checks byte/version equality and actual native circuit resolution through the production RegistryZkSystem. ZkCapability uses this wrapper; an absent/invalid fallback exposes no usable specs while keeping native capability intact so it cannot enable plain-response downgrade. No registry fetch occurs per presentation.

## Compatibility and expiry

Git revisions have no lexical order. The minimal policy requires exact Go runtime revision 61a8a735964d1b22bccf79bf14ef6767249cdf92 and maps the explicitly checked wallet artifact multipaz-longfellow:0.99.0 to that revision. BuildConfig stamps the actual Gradle dependency version. Native SpecID, hash, circuit version, attribute count and both block parameters must match. Unknown/different stamps refuse incompatible-library-version until cross-checked; a greater-looking SHA never implies compatibility. A general compatibility manifest remains distribution work.

Version 1 expires at 2027-05-13T00:00:00Z. Refresh before expiry, which applies equally to the pinned fallback. Increment the positive decimal list_version when content changes. Hosted rollback persistence is outside this prerequisite.

## Reproduce signing

Keep the PKCS#8 Ed25519 release private key in a protected directory outside the checkout (directory 0700, key 0600). Generate an initial key with openssl genpkey -algorithm ED25519 -out /secure/release-key.pem. A new public pin is a release trust-root change requiring review. The current key was generated outside the checkout. Retain or recover release keys through the operator's secure process; never commit a private key.

Prepare /secure/circuit-list-source.json with all envelope fields and existing Circuit entries, without the old _comment or any new per-entry id/system fields. Run from verifier/go:

    go run ./cmd/circuit-list -source /secure/circuit-list-source.json -key /secure/release-key.pem -key-id eudi-wallet-poc-local-YYYY-MM-DD -out ../circuits.json -public-out circuits/pinned-key.json

The Go generator shares Circuit identity, tuple/hash uniqueness and deprecation rules. It normalizes the source through the document struct before signing exactly the emitted representation, then signs RFC 8785 canonical JSON excluding signatures. The schema permits safe integer numbers only, rejecting fractions/unsafe values; do not use jq as a signature canonicalizer. Optional string metadata may be omitted, null or a string; omitted/null/empty deprecation is live and nonempty deprecation withdraws the entry. Audits may be omitted, null or an array of strings, without null elements. Both consumers exercise the same [metadata vectors](../../../tests/vectors/circuits/optional-metadata.json). Commit only the signed public document and public pin. Gradle regenerates its resource copy.

Run (cd verifier/go && go test -race ./circuits ./cmd/circuit-list), then JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testLocal_mocksUnitTest --tests '*SignedCircuitFallbackTest'. The CLI tests create ephemeral keys outside the checkout and round-trip normalized empty/null optional fields. The linked TestEstonianSignedRegistryExpiryAtHandlers advances an injected clock after session creation and asserts refusal without native verification or new session allocation.

TestPinnedOfflineAcceptance and SignedCircuitFallbackTest.offlineSharedDocumentResolvesOfferedNativeCircuit emit ESTONIAN_FALLBACK= JSON containing consumer, list_version, actual SpecID, document_sha256, signature_valid and fetched=false. The acceptance runner compares fresh successful executions; hashes alone do not prove resolution. Refusal controls cover tamper, unknown key, expiry, incompatible runtime, duplicates and version skew.

The signed-envelope migration updates the fixture generator input digest and
manifest pin for verifier/circuits.json. Circuit entries, generated request
bytes and retained proof vectors remain unchanged; their original source
revision and byte hashes remain historical fixture provenance.
