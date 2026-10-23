# AV issuance invocation (entry only)

The wallet advertises `av-vci` as a VIEW/DEFAULT/BROWSABLE scheme on its exported,
`singleTask` MainActivity. Cold-start data and subsequent `onNewIntent` data enter
one bounded, Activity-retained inbox of typed outcomes, which waits for the
loaded wallet before navigation. AV offers retain only the issuer, never grants.
The active outcome and provider-unavailable state survive Activity recreation;
process-death persistence is outside this implementation. Refusals remain visible
until dismissed and later outcomes wait in order. A new offer can replace a sole
visible valid offer, including an identical one. The inbox holds 16 outcomes plus
one counted capacity-refusal notice: excess arrivals are explicitly refused and
must be retried after that notice is dismissed. All incoming URI strings are
limited to 65,536 characters before being retained, including existing links.
A valid
external offer opens an issuance dialog showing `eu.europa.ec.av.1` and the actual
supplied issuer. The issuer is explicitly unverified. Cancel exits; Continue
reports **Provider integration unavailable** and that no attestation was obtained.
This path does not call the mock issuance routes or perform offer/provider network
requests. It establishes entry handling, not completed issuance, AVSUA, trust,
profile conformance or certification.

The pinned public [Annex A.5](av/annex-a-normative.md#credential-offer) requires
`av-vci://` and permits an internal issuance interface, but does not specify the
query grammar. The public [A.11 credential-offer examples](av/annex-a-examples.md#credential-offer)
use **`av://`**, with an inline `credential_offer`, HTTPS `credential_issuer`,
`["proof_of_age"]` configuration IDs and optional grants. The supported subset
combines the normative **`av-vci://`** scheme with those inline example fields.
The older `av://` presentation route remains available; it is not an alias for
external issuance. The examples' formatted JSON snippets include trailing commas;
the supported input is strict JSON, as in their percent-encoded links.

## Supported grammar

```text
av-vci://?credential_offer=<percent-encoded JSON object>
av-vci:///?credential_offer=<percent-encoded JSON object>
```

The parser matches schemes case-insensitively after delivery. Android's manifest
scheme matching is case-sensitive: implicit callers must use lowercase `av-vci`.
An uppercase spelling can only be tested by explicitly targeting the Activity.
Authority must be absent, path must be empty
or `/`, and fragments (including an empty `#`) are refused. Exactly one query
parameter, `credential_offer`, is accepted. Split query components are decoded
once as UTF-8 form components (`+` means a space; encode literal plus as `%2B`).
Percent escapes use ASCII hexadecimal digits only, and UTF-8 must be valid. Duplicate names, including an escaped
spelling of the same name, unknown query parameters, empty query segments and
`credential_offer_uri` are refused; remote offer resolution is unavailable.
The parser does not decode a whole URI. An encoded separator inside the offer
stays inside that value; literal percent escapes in the JSON issuer are retained.
Invocation input is limited to 65,536 characters.

The offer must have:

- `credential_issuer`: a string containing an HTTPS URL with a host, no user info,
  query, fragment, explicit empty port, or bidirectional control characters, and
  an absent or valid port. It is displayed as supplied, without issuer lookup or
  any assertion of trust.
  Query exclusion follows [OpenID4VCI 1.0 §12.2.1](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-credential-issuer-identifie)
  (primary source checked 2026-10-23).
- `credential_configuration_ids`: exactly `["proof_of_age"]`.
- Optional `grants`: a JSON object. Public authorization-code and pre-authorized
  code grants are accepted as inert data. Grant values and offer extension fields
  are never executed, displayed or logged, and are not retained in the typed
  issuance-entry state.

Unknown offer object fields are accepted as inert JSON data. JSON nesting is
limited to 64 levels. JSON must have one
object, quoted keys, strict values, no comments/trailing commas/trailing tokens,
no duplicate keys at any nesting level and valid Unicode strings. Duplicate keys
and general JSON errors name `credential_offer`; invalid issuer and configuration
fields name `credential_issuer` and `credential_configuration_ids`. Unsupported
query names are identified when safe to display; malformed or unsafe query names
are reported as `query`. Unsupported AV locations name `invocation`; unknown
schemes name `scheme`. Generic navigation failures also produce a visible
`invocation` refusal without exposing exception or URI contents.

Generate an example with the Python standard library:

```sh
python3 - <<'PY'
import json
from urllib.parse import quote
offer = {
    "credential_issuer": "https://issuer.example.com",
    "credential_configuration_ids": ["proof_of_age"],
    "grants": {"authorization_code": {"issuer_state": "synthetic-example"}},
}
print("av-vci://?credential_offer=" + quote(json.dumps(offer, separators=(",", ":")), safe=""))
PY
```

## Reproduce the checks

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest
make check
python3 tests/test_av_vci_android.py --help
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:installLocal_mocks
python3 tests/test_av_vci_android.py --serial emulator-5554
```

Use a local JDK 17/Android SDK path as appropriate, an unlocked Android 31+ runtime
and the current `local_mocks` APK, with the repository's configured debug signing
properties. On API33+ the smoke test runs both English and Estonian and restores the original
per-app locale and night-mode setting even on failure. API31/32 lack the per-app
locale shell command: configure the app/system language first, then run separate
`--legacy-locale en` and `--legacy-locale et` passes. Both passes are needed for
locale coverage. The harness force-stops for cold-start coverage, uses distinct
issuers for warm-start coverage, and toggles night mode to reproduce Activity
recreation of cold/warm valid, provider-unavailable and refused UI. It verifies
FIFO refusals followed by a valid offer, repeated identical intents, complete
localized refusal explanations, actual dialog dismissal, manifest resolution,
and existing onboarding/presentation destinations through NavController, plus a
known-scheme unmatched-link refusal. It does not clear app data or onboard the wallet.
The runtime test uses UIAutomator, so a JVM unit pass alone cannot establish
Android surface behavior. The App workflow also runs the harness on an Android
35 emulator for app and harness changes. The harness waits for a changed local
Activity instance before checking restored UI and verifies the visible overflow
count after draining a full inbox. See [the test catalogue](../../tests/README.md).
