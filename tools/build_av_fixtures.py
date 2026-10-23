#!/usr/bin/env python3
"""Retain Annex A literals and package local AV fixtures; --check never writes.

Only locally constructed requests are CBOR-encoded. External CBOR and the minted
DeviceResponse are copied/decoded from their original byte strings, never decoded
and reserialized. All extraction and drift checks use the Python standard library.
"""

import argparse
import base64
import hashlib
import json
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]
OUT = Path("tests/vectors/av-profile")
PROFILE = Path("docs/profiles/av")
REVISION = "014dc2fd0f9332eeabe1eef70d234ed2aef20fd7"
LOCAL_REVISION = "cc112f6a0a7705e98fad38b9eac83ba0bb55d96c"
SOURCE_SHA256 = "54808f94484cb75e7b495b508f0c17568e41b7ca7c3657799c7b432a770538a9"
EXAMPLES_SHA256 = "c907940c18a291fce6b231bc2419fc25433218d89368553582771d695e28afe2"
LICENSE_SHA256 = "8e7a9299a281b71fa9ad3f978bcbd5c4d8809b21b2ab51ca5e173e3a16ed6ccb"
DOCTYPE = "eu.europa.ec.av.1"
SYSTEM = "longfellow-libzk-v1"
PARAMETERS = ("circuit_hash", "version", "num_attributes", "block_enc_hash", "block_enc_sig")
MINTED = Path("verifier/go/zk/testdata/step8-7-openid4vp-zk")
LOCAL_PINS = {
    "verifier/circuits.json": "aae3a319feff4ec11dc623a5161e069b193005c713d40f875e198aeb5a370e0f",
    str(MINTED / "device_response.cbor"): "23978eb511a13d4a38a743260d5ef44a7fd9488d47806c33631fe092adc7c4aa",
    str(MINTED / "request.json"): "0de691b7abb742f40dbbb392a3352cb155989e1bdfe64e58f704db936524d89d",
    str(MINTED / "GENERATED-BY"): "9bf321d9e5a3e4edd06302cb45b40da96c09ddbbf509d40df309fb14dd7acd89",
}
# Every fenced body in A.11, in original order. Even non-JSON diagnostic and
# illustrative HTTP/issuance bodies remain literal bytes, with no normalization.
LITERALS = (
    "01-attestation.diag.txt", "02-attributes.txt", "03-offer-preauthorized.txt",
    "04-offer-preauthorized-uri.txt", "05-offer-authorization.txt", "06-offer-authorization-uri.txt",
    "07-authorization-request.http", "08-authorization-response.http",
    "09-token-authorization.http", "10-token-preauthorized.http", "11-token-response.http",
    "12-credential-request.http", "13-credential-response.http",
    "14-dcapi-request.json", "15-dcapi-request.diag.txt", "16-dcapi-response.json",
    "17-dcapi-response.diag.txt", "18-dcapi-transcript.hex", "19-dcapi-transcript.diag.txt",
    "20-dcapi-zkp-request.json", "21-dcapi-zkp-request.diag.txt", "22-dcapi-zkp-response.json",
    "23-dcapi-zkp-response.diag.txt", "24-dcapi-zkp-transcript.hex", "25-dcapi-zkp-transcript.diag.txt",
    "26-oid4vp-dcql.json", "27-oid4vp-request.http", "28-oid4vp-response.http",
    "29-oid4vp-vp-token.json", "30-oid4vp-response.diag.txt",
    "31-oid4vp-transcript.hex", "32-oid4vp-transcript.diag.txt",
)


class FixtureError(ValueError):
    """An input pin or complete-output drift failure."""


def require(condition, message):
    if not condition:
        raise FixtureError(message)


def check_output_root(root):
    # The checkout root is trusted; every output directory below it must be real.
    directory = Path(root)
    for part in OUT.parts:
        directory /= part
        require(not directory.is_symlink(), f"refusing output directory symlink: {directory}")


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def json_bytes(value):
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def unique_object(pairs):
    obj = {}
    for key, value in pairs:
        require(key not in obj, f"duplicate JSON key {key!r}")
        obj[key] = value
    return obj


def read_json(raw):
    return json.loads(raw, object_pairs_hook=unique_object)


def examples_metadata():
    repository = "https://github.com/eu-digital-identity-wallet/av-doc-technical-specification"
    source_path = "docs/annexes/annex-A/annex-A-av-profile.md"
    return {
        "repository": repository, "revision": REVISION, "commit_date": "2026-09-01T21:59:02Z",
        "source_path": source_path,
        "source_url": f"{repository.replace('github.com', 'raw.githubusercontent.com')}/{REVISION}/{source_path}",
        "source_sha256": SOURCE_SHA256, "license_path": str(PROFILE / "LICENSE.upstream"),
        "license_sha256": LICENSE_SHA256, "schema_version": 1,
        "excerpt_path": str(PROFILE / "annex-a-examples.md"), "excerpt_sha256": EXAMPLES_SHA256,
        "source_byte_start": 20029, "source_byte_end": 1131871,
        "extraction_rule": "Exact byte suffix starting at the first '## A.11. Examples' heading through EOF; no newline or wording changes.",
        "normative": False,
        "attribution": "The European Age Verification Solution technical specification © 2025 by European Commission; upstream license: Creative Commons Attribution 4.0 International. Retained non-normative examples are unchanged; fixture packaging and commentary are additions.",
    }


def cbor_head(major, value):
    """Encode only the definite, shortest-form lengths/integers we construct."""
    for size, head in ((0, value), (1, 24), (2, 25), (4, 26), (8, 27)):
        if value < (24 if size == 0 else 1 << (size * 8)):
            return bytes([(major << 5) | head]) + (value.to_bytes(size, "big") if size else b"")
    raise FixtureError("CBOR value exceeds uint64")


def cbor(value):
    # Insertion order is deliberate and independently compared with Go's runtime
    # DeviceRequest. This encoder never receives a decoded golden CBOR object.
    if isinstance(value, bool):
        return b"\xf5" if value else b"\xf4"
    if isinstance(value, int) and value >= 0:
        return cbor_head(0, value)
    if isinstance(value, str):
        raw = value.encode("utf-8")
        return cbor_head(3, len(raw)) + raw
    if isinstance(value, bytes):
        return cbor_head(2, len(value)) + value
    if isinstance(value, list):
        return cbor_head(4, len(value)) + b"".join(cbor(item) for item in value)
    if isinstance(value, dict):
        return cbor_head(5, len(value)) + b"".join(cbor(k) + cbor(v) for k, v in value.items())
    raise FixtureError(f"unsupported constructed CBOR value: {type(value)}")


def local_request(spec):
    items = cbor({"docType": DOCTYPE, "nameSpaces": {DOCTYPE: {"age_over_18": False}},
                  "requestInfo": {"zkRequest": {"version": 1, "systemSpecs": [spec], "zkRequired": True}}})
    # The only pre-encoded member is locally built ItemsRequest, wrapped in tag 24.
    doc_request = cbor_head(5, 1) + cbor("itemsRequest") + cbor_head(6, 24) + cbor(items)
    return cbor_head(5, 2) + cbor("version") + cbor("1.0") + cbor("docRequests") + cbor_head(4, 1) + doc_request


def unbase64url(text):
    require(isinstance(text, str) and re.fullmatch(r"[A-Za-z0-9_-]+", text) is not None,
            "external container is not unpadded base64url")
    return base64.b64decode(text + "=" * (-len(text) % 4), altchars=b"-_", validate=True)


def build(root=ROOT):
    """Return the complete expected output, including its deterministic manifest."""
    root = Path(root)
    meta = read_json((root / PROFILE / "examples-source.json").read_bytes())
    # JSON booleans and numbers must remain distinct (Python False == 0).
    require(json.dumps(meta, sort_keys=True) == json.dumps(examples_metadata(), sort_keys=True),
            "examples-source.json: pinned provenance mismatch")
    source = read_json((root / PROFILE / "source.json").read_bytes())
    for key in ("repository", "revision", "commit_date", "source_path", "source_url", "source_sha256", "license_path", "license_sha256"):
        require(source.get(key) == meta[key], f"source.json: pinned {key} mismatch")
    raw = (root / meta["excerpt_path"]).read_bytes()
    require(digest(raw) == EXAMPLES_SHA256, "annex-a-examples.md: SHA-256 pin mismatch")
    require(digest((root / meta["license_path"]).read_bytes()) == LICENSE_SHA256, "upstream license: SHA-256 pin mismatch")
    for path, expected in LOCAL_PINS.items():
        require(digest((root / path).read_bytes()) == expected, f"{path}: SHA-256 pin mismatch")

    files, artifacts = {}, []

    def add(path, content, origin, source_ref, **labels):
        require(path not in files, f"duplicate output {path}")
        files[path] = content
        artifacts.append({"path": path, "profile_revision": REVISION, "origin": origin,
                          "source_revision": REVISION if origin.startswith("upstream") else LOCAL_REVISION,
                          "source": source_ref, "sha256": digest(content), "bytes": len(content), **labels})

    matches = list(re.finditer(rb"(?ms)^```([^\r\n]*)\r?\n(.*?)^```[ \t]*\r?$", raw))
    require(len(matches) == len(LITERALS), f"expected {len(LITERALS)} A.11 fences, got {len(matches)}")
    for number, (name, match) in enumerate(zip(LITERALS, matches), 1):
        ref = {"path": meta["excerpt_path"], "fence": number, "byte_start": match.start(2),
               "byte_end": match.end(2), "language": match[1].decode("ascii")}
        add(f"external/{name}", match[2], "upstream-literal", ref)

    for prefix in ("14-dcapi", "20-dcapi-zkp"):
        path = f"external/{prefix}-request.json"
        data = read_json(files[path])["requests"][0]["data"]
        for key, suffix in (("deviceRequest", "device-request"), ("encryptionInfo", "encryption-info")):
            add(f"external/{prefix}-{suffix}.cbor", unbase64url(data[key]), "upstream-base64url",
                {"path": path, "json_pointer": f"/requests/0/data/{key}"})
    for prefix in ("16-dcapi", "22-dcapi-zkp"):
        path = f"external/{prefix}-response.json"
        add(f"external/{prefix}-encrypted-response.cbor", unbase64url(read_json(files[path])["response"]),
            "upstream-base64url", {"path": path, "json_pointer": "/response"}, encrypted=True)
    path = "external/29-oid4vp-vp-token.json"
    add("external/29-oid4vp-device-response.cbor", unbase64url(read_json(files[path])["proof_of_age"][0]),
        "upstream-base64url", {"path": path, "json_pointer": "/proof_of_age/0"})
    for prefix in ("18-dcapi", "24-dcapi-zkp", "31-oid4vp"):
        path = f"external/{prefix}-transcript.hex"
        add(f"external/{prefix}-transcript.bin", bytes.fromhex(files[path].decode("ascii")),
            "upstream-hex", {"path": path})

    circuit = read_json((root / "verifier/circuits.json").read_bytes())["circuits"][0]
    params = {key: circuit[key] for key in PARAMETERS}
    spec_id = f"{SYSTEM}_{params['version']}_{params['num_attributes']}_{params['block_enc_hash']}_{params['block_enc_sig']}_{params['circuit_hash']}"
    spec = {"zkSystemId": spec_id, "id": spec_id, "system": SYSTEM, "params": params}
    ref = {"path": "verifier/circuits.json"}
    add("local/zk-system-spec.json", json_bytes(spec), "generated-local-tuple", ref, circuit_version=7)
    add("local/device-request.cbor", local_request(spec), "generated-local-request", ref, circuit_version=7)
    query = {"credentials": [{"id": "proof_of_age", "format": "mso_mdoc_zk",
              "meta": {"doctype_value": DOCTYPE, "zk_system_type": [{"system": SYSTEM, "id": spec_id, **params}]},
              "claims": [{"path": [DOCTYPE, "age_over_18"], "values": [True]}]}]}
    add("local/dcql.json", json_bytes(query), "generated-local-request", ref,
        circuit_version=7, carrier="unofficial-openid4vp-mso_mdoc_zk")
    for original, name in (("device_response.cbor", "device-response.cbor"),
                           ("request.json", "proof-request.json"), ("GENERATED-BY", "proof-provenance.txt")):
        path = str(MINTED / original)
        add(f"local/{name}", (root / path).read_bytes(), "copied-minted-multipaz-fixture", {"path": path},
            circuit_version=7, carrier="unofficial-openid4vp-mso_mdoc_zk",
            transcript="OpenID4VPHandover; not a DC API proof")

    inputs = [str(PROFILE / name) for name in ("source.json", "examples-source.json", "annex-a-examples.md", "LICENSE.upstream")]
    inputs += list(LOCAL_PINS)
    manifest = {
        "schema_version": 1, "generated_by": "tools/build_av_fixtures.py",
        "origin": "generated-local-manifest", "source_revision": LOCAL_REVISION,
        "profile_revision": REVISION, "profile_source_sha256": SOURCE_SHA256,
        "profile_examples_sha256": EXAMPLES_SHA256, "doctype": DOCTYPE, "system": SYSTEM,
        "required_parameters": list(PARAMETERS), "local_source_revision": LOCAL_REVISION,
        "upstream_example_circuit_version": 6, "local_circuit_version": 7,
        "limits": ["A.11 is non-normative; examples establish container formats only.",
                   "Encrypted upstream responses are retained without decryption or proof verification.",
                   "The existing minted AV proof binds OpenID4VPHandover, not DC API.",
                   "Local acceptance is not scheme-owner circuit approval or certification.",
                   "The unofficial OpenID4VP ZK carrier supplies no AV transport conformance evidence."],
        "inputs": [{"path": path, "sha256": digest((root / path).read_bytes())} for path in inputs],
        "artifacts": artifacts,
    }
    files["manifest.json"] = json_bytes(manifest)
    return files


def check(root=ROOT):
    """Compare every expected byte and the complete file inventory, without writes."""
    root = Path(root)
    expected = build(root)
    output = root / OUT
    check_output_root(root)
    actual = {p.relative_to(output).as_posix() for p in output.rglob("*") if p.is_file() or p.is_symlink()}
    errors = [f"missing output: {name}" for name in sorted(expected.keys() - actual)]
    errors += [f"extra output: {name}" for name in sorted(actual - expected.keys())]
    for name in sorted(expected.keys() & actual):
        path = output / name
        if path.is_symlink() or path.read_bytes() != expected[name]:
            errors.append(f"byte drift: {name}")
    require(not errors, "\n".join(errors))
    return len(expected)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="read-only comparison of bytes, manifest, provenance and file inventory")
    args = parser.parse_args()
    try:
        if args.check:
            count = check()
            print(f"AV fixtures: OK — {count} files match; read-only check")
        else:
            expected = build()
            output = ROOT / OUT
            check_output_root(ROOT)
            # Do not silently delete unexpected data during a rebuild.
            actual = {p.relative_to(output).as_posix() for p in output.rglob("*") if p.is_file() or p.is_symlink()}
            require(not actual - expected.keys(), f"unexpected output files; remove deliberately: {sorted(actual - expected.keys())}")
            for name, raw in expected.items():
                path = output / name
                require(not path.is_symlink(), f"refusing output symlink: {name}")
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(raw)
            print(f"AV fixtures: wrote {len(expected)} files to {OUT}")
    except (FixtureError, OSError, ValueError) as exc:
        print(f"AV fixtures: FAIL: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
