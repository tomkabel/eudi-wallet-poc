#!/usr/bin/env python3
"""Mint an EE-PoA proof-of-age attestation as an ISO/IEC 18013-5 DeviceResponse.

Produces a credential the Longfellow ZK prover can prove over, signed by a key
we generate here — so the ZK demo runs end to end on our own attestation rather
than on Google's mDL test vector.

Per EE-EUDIW-TS-1.0 §9.2 the attestation carries boolean age predicates only:
no birth_date, no age_in_years, no sex, no name, no isikukood (EE-POA-001,
EE-PID-007).

With --device-public-key the mint binds the attestation to a key the wallet
already generated inside its WSCD (plan finding F12): the holder's public key
arrives as an option and no private key is imported (EE-SEC-003). The minted
deviceSignature is an all-zero placeholder — the ZK circuits prove the device
signature against MSO.deviceKeyInfo.deviceKey rather than merely parse it, so
the holder re-signs DeviceAuthentication with the real key at presentation
time, which is the flow wallet/present.py already performs.

With --batch N the issuer emits N single-use attestations (EE-POA-010/011),
each with its own device key and its own salts, all sharing one issuer key and
one coarsened ValidityInfo (EE-POA-012) so they cannot be told apart by
timestamp. Each lands in its own subdirectory, ready for the prover. A batch
cannot be combined with --device-public-key: one holder key shared across the
batch would make its attestations linkable, which EE-POA-010 forbids.

Output (default ./out, or ./out/000 .. ./out/NNN with --batch):
  mdoc.bin        DeviceResponse, CBOR
  transcript.bin  SessionTranscript, CBOR
  issuer_dsc.pem  self-signed document signer certificate
  device_key.pem  the holder's device key, for re-signing at presentation time
                  (with --device-public-key: device_public_key.pem instead —
                  the public half the attestation binds; the mint never sees
                  the private half and writes no device_key.pem)
  vector.json     issuer public key, doctype, namespace, `now`, file paths
  params.txt      the same four values the prover needs, one per line

and, once per run, out/issuers.json — the trust-store entry a verifier needs in
order to accept attestations from this issuer.
"""
import argparse, datetime as dt, hashlib, json, os, re, secrets
import cbor2
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, utils as asym_utils

DOCTYPE = NAMESPACE = "ee.riik.poa.1"
COSE_PROTECTED_ES256 = bytes.fromhex("a10126")          # {1: -7}
SIG_STRUCT_PREFIX = bytes.fromhex("846a5369676e61747572653143a1012640")
DEVICE_AUTH_HEADER = bytes.fromhex("8474") + b"DeviceAuthentication"
DEVICE_NAMESPACES_BYTES = bytes.fromhex("d81841a0")     # Tag24(bstr({}))


def bstr_header(n: int) -> bytes:
    """
    Encode a CBOR byte-string header for the specified payload length.
    
    Parameters:
    	n (int): The byte-string length.
    
    Returns:
    	bytes: The CBOR-encoded byte-string header.
    """
    if n < 24:            return bytes([0x40 | n])
    if n < 0x100:         return bytes([0x58, n])
    if n < 0x10000:       return bytes([0x59]) + n.to_bytes(2, "big")
    return bytes([0x5A]) + n.to_bytes(4, "big")


def tstr_header(n: int) -> bytes:
    """Encode a CBOR text-string header for the specified length.
    
    Parameters:
        n (int): Text-string length in bytes.
    
    Returns:
        bytes: The CBOR-encoded text-string header.
    """
    if n < 24:            return bytes([0x60 | n])
    if n < 0x100:         return bytes([0x78, n])
    return bytes([0x79]) + n.to_bytes(2, "big")


def sign_raw(key: ec.EllipticCurvePrivateKey, msg: bytes) -> bytes:
    """Sign a message with ECDSA using SHA-256.
    
    Parameters:
        msg (bytes): The message to sign.
    
    Returns:
        bytes: The 64-byte COSE signature containing the concatenated 32-byte
            big-endian r and s values.
    """
    der = key.sign(msg, ec.ECDSA(hashes.SHA256()))
    r, s = asym_utils.decode_dss_signature(der)
    return r.to_bytes(32, "big") + s.to_bytes(32, "big")


def cose_key(pub: ec.EllipticCurvePublicKey) -> dict:
    """
    Convert a P-256 public key to a COSE EC2 key map.
    
    Parameters:
    	pub (ec.EllipticCurvePublicKey): The public key to encode.
    
    Returns:
    	dict: A COSE key map containing the P-256 curve identifier and public coordinates.
    """
    n = pub.public_numbers()
    return {1: 2, -1: 1,
            -2: n.x.to_bytes(32, "big"),
            -3: n.y.to_bytes(32, "big")}


def zero_device_sig() -> bytes:
    """
    Build the placeholder deviceSignature for a mint over a holder-supplied key.

    With --device-public-key the mint holds only the public half, so it cannot
    sign DeviceAuthentication. The ZK circuits do not merely parse the device
    signature either — they verify it against MSO.deviceKeyInfo.deviceKey
    inside the proof — so the placeholder must be replaced by a real signature
    over the live session transcript before any proving. That re-sign is the
    wallet's normal presentation step (wallet/present.py resign_device_auth).

    Returns:
        bytes: A 64-byte all-zero r||s value shaped like a COSE ES256 signature.
    """
    return bytes(64)


def load_device_public_key(text: str) -> ec.EllipticCurvePublicKey:
    """
    Read the holder's P-256 public key for --device-public-key.

    Accepts SEC1 uncompressed hex (with or without a leading 0x, the form a
    wallet exports as two 32-byte coordinates) or a PEM SubjectPublicKeyInfo
    block. Any non-P-256 key is refused: the circuits bind coordinates as
    32-byte field elements, so nothing else can be proven over.

    Parameters:
        text (str): The key in hex or PEM form.

    Returns:
        ec.EllipticCurvePublicKey: The parsed P-256 public key.
    """
    stripped = "".join(text.split())
    hex_form = stripped[2:] if stripped[:2].lower() == "0x" else stripped
    if re.fullmatch(r"(04)?[0-9a-fA-F]{128}", hex_form):
        coords = bytes.fromhex(hex_form)
        try:
            if len(coords) == 64:                  # two raw 32-byte coordinates
                return ec.EllipticCurvePublicNumbers(
                    int.from_bytes(coords[:32], "big"),
                    int.from_bytes(coords[32:], "big"),
                    ec.SECP256R1()).public_key()
            # SEC1 uncompressed 0x04||x||y
            return ec.EllipticCurvePublicKey.from_encoded_point(ec.SECP256R1(), coords)
        except ValueError as exc:
            raise SystemExit(f"--device-public-key: not a point on P-256: {exc}") from exc
    try:
        key = serialization.load_pem_public_key(text.encode())
    except ValueError as exc:
        raise SystemExit(f"--device-public-key: not hex or PEM: {exc}") from exc
    if not isinstance(key, ec.EllipticCurvePublicKey) or key.curve.name != "secp256r1":
        raise SystemExit("--device-public-key: key must be a P-256 (secp256r1) public key")
    return key


def self_signed_dsc(key: ec.EllipticCurvePrivateKey, now: dt.datetime) -> x509.Certificate:
    """
    Create a self-signed document-signer certificate for the EE-PoA demo issuer.
    
    Parameters:
        key: Private P-256 key used as the certificate's public key source and to sign it.
        now: Reference time for the certificate validity period.
    
    Returns:
        A certificate valid from one day before `now` through 90 days after `now`.
    """
    name = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, "EE"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "EE-EUDIW demo issuer"),
        x509.NameAttribute(NameOID.COMMON_NAME, "EE-PoA Document Signer (TEST)"),
    ])
    return (x509.CertificateBuilder()
            .subject_name(name).issuer_name(name)
            .public_key(key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(now - dt.timedelta(days=1))
            .not_valid_after(now + dt.timedelta(days=90))
            .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
            .add_extension(x509.KeyUsage(digital_signature=True, content_commitment=False,
                                         key_encipherment=False, data_encipherment=False,
                                         key_agreement=False, key_cert_sign=False,
                                         crl_sign=False, encipher_only=False, decipher_only=False),
                           critical=True)
            .sign(key, hashes.SHA256()))


def build_transcript() -> bytes:
    """
    Build a canonical CBOR session transcript for browser-based handover.

    SessionTranscript = [DeviceEngagementBytes, EReaderKeyBytes, Handover].

    Returns:
    	bytes: Canonically encoded transcript containing null device and reader
    	engagement values, the relying-party URL, and randomized nonce data.
    """
    handover = ["BrowserHandoverv1",
                secrets.token_bytes(32),
                {"cat": 1, "type": 1, "details": {"baseUrl": "https://cinema.example.ee"}},
                secrets.token_bytes(32)]
    return cbor2.dumps([None, None, handover], canonical=True)


def main() -> None:
    """
    Generate a signed EE proof-of-age DeviceResponse and supporting files.
    
    The command-line options control the output directory, attestation validity period, and age thresholds. The generated files include the DeviceResponse, session transcript, issuer certificate, verification vector, and circuit parameters.
    """
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", default="out", help="output directory (default: out)")
    ap.add_argument("--batch", type=int, default=1, metavar="N",
                    help="mint N single-use attestations into out/000 .. (EE-POA-010/011)")
    ap.add_argument("--validity-days", type=int, default=90,
                    help="attestation lifetime; EE-POA-016 caps this at 3 months")
    ap.add_argument("--over", type=int, nargs="*", default=[18],
                    help="age thresholds to attest (default: 18; §9.2 omits "
                         "age_over_16/21 in v1 — spec finding S10)")
    ap.add_argument("--under", type=int, nargs="*", default=[],
                    help="thresholds to attest as false, e.g. --over 16 --under 18 21")
    ap.add_argument("--device-public-key", metavar="KEY",
                    help="bind the attestation to this holder-held P-256 public "
                         "key (F12) instead of generating a device key; hex "
                         "(0x04||x||y or x||y) or PEM. The minted "
                         "deviceSignature is a placeholder the wallet re-signs "
                         "at presentation; no device_key.pem is written")
    args = ap.parse_args()

    if args.batch < 1:
        ap.error("--batch must be at least 1")
    if not 1 <= args.validity_days <= 90:
        ap.error("--validity-days must be between 1 and 90")
    if len(args.over) != len(set(args.over)):
        ap.error("--over thresholds must not contain duplicates")
    if len(args.under) != len(set(args.under)):
        ap.error("--under thresholds must not contain duplicates")
    if set(args.over) & set(args.under):
        ap.error("--over and --under thresholds must be disjoint")
    if args.device_public_key and args.batch > 1:
        ap.error("--device-public-key binds one holder key; --batch > 1 would share it "
                 "(EE-POA-010: a distinct key per attestation)")

    # Coarsened per EE-POA-012: every attestation in a batch shares these to the
    # second, so timestamps cannot be used to correlate presentations.
    now = dt.datetime.now(dt.timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    valid_until = now + dt.timedelta(days=args.validity_days)

    issuer_key = ec.generate_private_key(ec.SECP256R1())
    dsc = self_signed_dsc(issuer_key, now)
    dsc_der = dsc.public_bytes(serialization.Encoding.DER)

    elements = ([(f"age_over_{n}", True) for n in sorted(args.over)] +
                [(f"age_over_{n}", False) for n in sorted(args.under)] +
                [("issuing_country", "EE"),
                 ("issuing_authority", "EE-EUDIW demo issuer"),
                 ("expiry_date", valid_until.date().isoformat())])

    device_pub = (load_device_public_key(args.device_public_key)
                  if args.device_public_key else None)

    def mint_one():
        """
        Mint one single-use attestation.

        Fresh device key and fresh salts per attestation, sharing the issuer key
        and the coarsened ValidityInfo of the surrounding batch. With
        --device-public-key the device key is the holder's; the signature is the
        placeholder zero_device_sig() documents.

        Returns:
        \ttuple: the DeviceResponse bytes, the session transcript, the encoded
        \tMSO, the issuer's raw r||s signature over it, the device private key
        \t(None over a holder-supplied key) and the device public key.
        """
        if device_pub is None:
            device_key = ec.generate_private_key(ec.SECP256R1())
            device_pk = device_key.public_key()
        else:
            device_key = None                           # the mint must not hold it (EE-SEC-003)
            device_pk = device_pub
        items, digests = [], {}
        for digest_id, (element_id, value) in enumerate(elements):
            item = {"digestID": digest_id,
                    "random": secrets.token_bytes(32),
                    "elementIdentifier": element_id,
                    "elementValue": value}
            encoded = cbor2.dumps(item, canonical=True)
            items.append(cbor2.CBORTag(24, encoded))
            digests[digest_id] = hashlib.sha256(
                cbor2.dumps(cbor2.CBORTag(24, encoded), canonical=True)).digest()

        mso = {"version": "1.0",
               "digestAlgorithm": "SHA-256",
               "docType": DOCTYPE,
               "valueDigests": {NAMESPACE: digests},
               "deviceKeyInfo": {"deviceKey": cose_key(device_pk)},
               "validityInfo": {"signed": now, "validFrom": now, "validUntil": valid_until}}
        mso_bytes = cbor2.dumps(mso, canonical=True)
        if len(mso_bytes) < 256:
            raise SystemExit(f"MSO is {len(mso_bytes)} bytes; the circuit requires >= 256. "
                             "Attest more thresholds.")

        # issuerAuth: COSE_Sign1 over Tag24(MSO), detached-payload Sig_structure.
        mso_wrapped = cbor2.dumps(cbor2.CBORTag(24, mso_bytes), canonical=True)
        issuer_sig = sign_raw(issuer_key,
                              SIG_STRUCT_PREFIX + bstr_header(len(mso_wrapped)) + mso_wrapped)
        issuer_auth = [COSE_PROTECTED_ES256, {33: dsc_der}, mso_wrapped, issuer_sig]

        # deviceSignature: COSE_Sign1 over Tag24(DeviceAuthentication). Over a
        # holder-supplied key this is zero_device_sig(): unsigned here by
        # construction, and every prover refuses it until the wallet re-signs.
        transcript = build_transcript()
        device_auth_cbor = (DEVICE_AUTH_HEADER + transcript +
                            tstr_header(len(DOCTYPE)) + DOCTYPE.encode() +
                            DEVICE_NAMESPACES_BYTES)
        payload = bytes.fromhex("d818") + bstr_header(len(device_auth_cbor)) + device_auth_cbor
        if device_key is None:
            device_sig = zero_device_sig()
        else:
            device_sig = sign_raw(device_key, SIG_STRUCT_PREFIX + bstr_header(len(payload)) + payload)

        device_response = {
            "version": "1.0",
            "documents": [{
                "docType": DOCTYPE,
                "issuerSigned": {"nameSpaces": {NAMESPACE: items}, "issuerAuth": issuer_auth},
                "deviceSigned": {
                    "nameSpaces": cbor2.CBORTag(24, cbor2.dumps({}, canonical=True)),
                    "deviceAuth": {
                        "deviceSignature": [COSE_PROTECTED_ES256, {}, None, device_sig]},
                },
            }],
            "status": 0,
        }
        return (cbor2.dumps(device_response), transcript, mso_bytes, issuer_sig,
                device_key, device_pk)

    ipub = issuer_key.public_key().public_numbers()
    pkx, pky = f"0x{ipub.x:064x}", f"0x{ipub.y:064x}"
    now_s = now.strftime("%Y-%m-%dT%H:%M:%SZ")

    print(f"doctype      : {DOCTYPE}")
    print(f"elements     : {', '.join(e for e, _ in elements)}")
    print(f"validity     : {now_s} .. {valid_until.strftime('%Y-%m-%dT%H:%M:%SZ')}"
          f"  (shared across the batch, EE-POA-012)")
    print(f"issuer pk x  : {pkx}")
    print()

    # A trust-store entry for the verifier: the issuer key it should accept for
    # this doctype. The verifier takes issuer keys from here, never from a
    # presentation (EE-GOV-010).
    os.makedirs(args.out, exist_ok=True)
    json.dump({"issuers": [{"name": "EE-EUDIW demo issuer", "doc_type": DOCTYPE,
                            "namespace": NAMESPACE, "pkx": pkx, "pky": pky}]},
              open(os.path.join(args.out, "issuers.json"), "w"), indent=2)

    sigs, validity_bytes = set(), set()
    for i in range(args.batch):
        d = args.out if args.batch == 1 else os.path.join(args.out, f"{i:03d}")
        os.makedirs(d, exist_ok=True)
        mdoc, transcript, mso_bytes, issuer_sig, device_key, device_pk = mint_one()
        sigs.add(issuer_sig)
        validity_bytes.add(cbor2.dumps(
            {"signed": now, "validFrom": now, "validUntil": valid_until}, canonical=True))

        p = lambda n: os.path.join(d, n)
        open(p("mdoc.bin"), "wb").write(mdoc)
        open(p("transcript.bin"), "wb").write(transcript)
        open(p("issuer_dsc.pem"), "wb").write(dsc.public_bytes(serialization.Encoding.PEM))
        # The holder needs this to re-sign deviceAuth over a live session
        # transcript at presentation time. A real wallet keeps it in a WSCD and
        # it never leaves the device (EE-SEC-001, EE-SEC-003). With
        # --device-public-key the mint never had the private half: it records
        # the public half it bound instead, and the holder's own key stays
        # wherever the wallet generated it.
        if device_key is None:
            pub_path = p("device_public_key.pem")
            open(pub_path, "wb").write(device_pk.public_bytes(
                serialization.Encoding.PEM,
                serialization.PublicFormat.SubjectPublicKeyInfo))
        else:
            key_path = p("device_key.pem")
            key_fd = os.open(key_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
            try:
                # os.open's mode does not change an existing file, so tighten the
                # descriptor before writing and enforce the mode again afterward.
                os.fchmod(key_fd, 0o600)
                with os.fdopen(key_fd, "wb") as key_file:
                    key_fd = -1  # fdopen owns the descriptor now.
                    key_file.write(device_key.private_bytes(
                        serialization.Encoding.PEM,
                        serialization.PrivateFormat.PKCS8,
                        serialization.NoEncryption()))
            finally:
                if key_fd >= 0:
                    os.close(key_fd)
                os.chmod(key_path, 0o600)
        open(p("params.txt"), "w").write("\n".join([pkx, pky, now_s, DOCTYPE, NAMESPACE]) + "\n")
        json.dump({"pkx": pkx, "pky": pky, "now": now_s,
                   "doc_type": DOCTYPE, "namespace": NAMESPACE,
                   "elements": [e for e, _ in elements],
                   "mdoc": "mdoc.bin", "transcript": "transcript.bin",
                   "index": i, "batch_size": args.batch},
                  open(p("vector.json"), "w"), indent=2)
        if args.batch == 1 or i < 3 or i == args.batch - 1:
            print(f"  [{i:03d}] MSO {len(mso_bytes)} B, DeviceResponse {len(mdoc)} B "
                  f"-> {p('mdoc.bin')}")
        elif i == 3:
            print(f"  ... {args.batch - 4} more")

    if args.batch > 1:
        print()
        print(f"batch        : {args.batch} single-use attestations (EE-POA-010/011)")
        print(f"  distinct issuer signatures : {len(sigs)}/{args.batch}"
              f"   {'ok' if len(sigs) == args.batch else 'COLLISION'}")
        print(f"  distinct ValidityInfo      : {len(validity_bytes)}"
              f"   {'ok (identical across batch)' if len(validity_bytes) == 1 else 'LEAK'}")
        print("  EE-POA-013 wants each attestation presented at most once and then "
              "deleted by the")
        print("  holder — the wallet's job, and ../wallet does not do it yet.")


if __name__ == "__main__":
    main()
