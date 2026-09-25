#!/usr/bin/env python3
"""Build the dual-carrier golden fixtures under tests/vectors/carrier-v1/.

Takes the committed multipaz proof (the step 8.7 fixture's zkDocument, a real
Longfellow proof bound to the B.2.6.1 OpenID4VPHandover transcript) and wraps
the SAME proof in both carriers:

  device_response.json.b64        the interim-JSON envelope, base64url (one line)
  device_response.cbor.b64        the de-facto CBOR DeviceResponse, base64url (one line)
  device_response.cbor            the same DeviceResponse, raw CBOR
  device_response_transcript.bin  the B.2.6.1 transcript the proof binds
  request.json                    the handover parameters (copied from the fixture)
  manifest.json                   what generated this, and the sha-256 of every file

Fidelity rule: the CBOR wrap reuses the source fixture's zkDocument bytes
VERBATIM — multipaz 0.99.0's own serialization, including its key order and
tag-0 timestamp — under a fresh DeviceResponse wrapper. Re-serializing the
decoded map would risk exactly the kind of drift this vector exists to pin
(cbor2, for one, re-tags the timestamp when it round-trips a datetime). The Go
test TestCarrierVectorsMatchTheBuilder extracts the same bytes the same way and
refuses any drift, so the committed vectors and the verifier's parser can never
disagree. Run from the repository root:

    python3 tools/build_cbor_fixture.py
"""
import base64
import cbor2
import hashlib
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FIXTURE = os.path.join(ROOT, "verifier/go/zk/testdata/step8-7-openid4vp-zk")
OUT = os.path.join(ROOT, "tests/vectors/carrier-v1")

# DeviceResponse wrapper around the verbatim zkDocument entry: canonical CBOR,
# keys in canonical (shortest-first) order version < status < zkDocuments.
WRAPPER_HEAD = (
    b"\xa3"
    + b"\x67" + b"version"
    + b"\x63" + b"1.1"
    + b"\x66" + b"status"
    + b"\x00"
    + b"\x6b" + b"zkDocuments"
    + b"\x81"
)


def zk_document_entry_bytes(source: bytes) -> bytes:
    """Extract the raw CBOR bytes of the source DeviceResponse's single
    zkDocument entry. The fixture's top-level map is version/status/
    zkDocuments with zkDocuments LAST, holding one entry, so the entry runs
    from just after the array head to end-of-file. Verify every assumption;
    refuse anything else (ADR-002 discipline)."""
    marker = b"zkDocuments"
    i = source.find(marker)
    if i < 0:
        sys.exit("source DeviceResponse has no zkDocuments key")
    p = i + len(marker)
    if source[p] != 0x81:
        sys.exit(f"zkDocuments is not a one-element array (head 0x{source[p]:02x})")
    entry = source[p + 1:]
    # Sanity: the entry must decode as {proof: bstr, documentData: tag24(bstr)}.
    doc = cbor2.loads(entry)
    if set(doc.keys()) != {"proof", "documentData"}:
        sys.exit(f"zkDocument keys are {sorted(doc.keys())}, want [documentData, proof]")
    if not isinstance(doc["proof"], bytes) or not hasattr(doc["documentData"], "tag"):
        sys.exit("zkDocument is not {proof: bstr, documentData: tag 24}")
    if doc["documentData"].tag != 24:
        sys.exit(f"documentData is tag {doc['documentData'].tag}, want 24")
    return entry


def build_interim_json(proof: bytes, timestamp: str) -> bytes:
    """The interim encoding this repository introduced (ZKPresentation JSON):
    an explicit, versioned stand-in, not an interop claim."""
    envelope = {
        "zk_system": "longfellow-libzk-v1",
        "version": 7,
        "num_attributes": 1,
        "doc_type": "eu.europa.ec.av.1",
        "namespace": "eu.europa.ec.av.1",
        "attr_id": "age_over_18",
        "attr_cbor_hex": "f5",
        "proof_b64": base64.b64encode(proof).decode(),
    }
    return json.dumps(envelope, separators=(",", ":")).encode()


def build_cbor_device_response(entry: bytes) -> bytes:
    """The de-facto mso_mdoc_zk carrier: a CBOR DeviceResponse whose top-level
    zkDocuments list carries the source fixture's own zkDocument bytes
    (docs/analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md section 2)."""
    return WRAPPER_HEAD + entry


def session_transcript(request: dict) -> bytes:
    """OpenID4VP 1.0 Appendix B.2.6.1 - the same bytes transcript.go derives
    (jwk_thumbprint null: the fixture rides plain direct_post)."""
    info = cbor2.dumps([request["client_id"], request["nonce"],
                        request.get("jwk_thumbprint"), request["response_uri"]],
                       canonical=True)
    digest = hashlib.sha256(info).digest()
    return cbor2.dumps([None, None, ["OpenID4VPHandover", digest]], canonical=True)


def main():
    with open(os.path.join(FIXTURE, "device_response.cbor"), "rb") as f:
        source = f.read()
    entry = zk_document_entry_bytes(source)
    proof = cbor2.loads(entry)["proof"]
    with open(os.path.join(FIXTURE, "request.json")) as f:
        request = json.load(f)
    os.makedirs(OUT, exist_ok=True)

    files = {}
    files["device_response.json.b64"] = base64.urlsafe_b64encode(
        build_interim_json(proof, request["timestamp"])).rstrip(b"=") + b"\n"
    cbor_bytes = build_cbor_device_response(entry)
    files["device_response.cbor.b64"] = base64.urlsafe_b64encode(cbor_bytes).rstrip(b"=") + b"\n"
    files["device_response.cbor"] = cbor_bytes
    files["device_response_transcript.bin"] = session_transcript(request)
    files["request.json"] = json.dumps(request, indent=2).encode() + b"\n"

    manifest = {
        "generated_by": "tools/build_cbor_fixture.py",
        "source_fixture": "verifier/go/zk/testdata/step8-7-openid4vp-zk",
        "source": "multipaz 0.99.0 wallet-generated zkDocument (step 8.7)",
        "carriers": ["interim-json", "mso-mdoc-zk-cbor"],
        "proof_bytes": len(proof),
        "note": ("the SAME proof in both carriers; asserted to verify "
                 "identically over the B.2.6.1 OpenID4VPHandover transcript "
                 "(verifier/go/oid4vp/carrier_test.go); the CBOR wrap reuses "
                 "the source zkDocument bytes verbatim"),
        "sha256": {},
    }
    for name, content in files.items():
        with open(os.path.join(OUT, name), "wb") as f:
            f.write(content)
        manifest["sha256"][name] = hashlib.sha256(content).hexdigest()

    with open(os.path.join(OUT, "manifest.json"), "w") as f:
        json.dump(manifest, f, indent=2)
        f.write("\n")

    print(f"wrote {len(files) + 1} files to {os.path.relpath(OUT, ROOT)}")
    print(f"  proof: {len(proof)} bytes, both carriers")


if __name__ == "__main__":
    main()
