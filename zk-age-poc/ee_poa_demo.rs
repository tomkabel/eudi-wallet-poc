// EE-EUDIW PoC: prove `age_over_18` in zero knowledge over an EE-PoA attestation
// minted by issuer/mint_ee_poa.py — our own issuer key, our own doctype.
//
//   cargo run --release --example ee_poa_demo --features testonly -- <dir>
//
// where <dir> holds mdoc.bin, transcript.bin and params.txt as written by the issuer.
use compile_algebra::CompileNat;
use mdoc_zk_circuits::parse_mdoc;
use mdoc_zk_runtime::{
    provider, req_attr, run_mdoc_prover_inner, run_mdoc_verifier_inner, RequestedAttribute,
};
use std::{env, fs, time::Instant};

/// Opt-in to proving a predicate the attestation says is false. See `main`.
const ALLOW_FALSE_FLAG: &str = "--allow-false-predicate";

/// Canonical CBOR `true` — the only value a predicate proof normally asserts.
const CBOR_TRUE: [u8; 1] = [0xf5];
/// Canonical CBOR `false`.
const CBOR_FALSE: [u8; 1] = [0xf4];

/// Demonstrates generating and verifying a zero-knowledge proof for one mdoc attribute.
///
/// The input directory must contain `mdoc.bin`, `transcript.bin`, and `params.txt`.
/// The optional arguments select the input directory and attribute name; they default
/// to `out` and `age_over_18`.
///
/// # Examples
///
/// ```text
/// cargo run -- out age_over_18
/// ```
fn main() {
    // EE-ZKP-021(b): a predicate proof asserts the attribute is `true`. Proving
    // `false` is only useful for testing that a verifier refuses it, so it takes
    // an explicit opt-in rather than being what happens by default.
    let allow_false = env::args().any(|a| a == ALLOW_FALSE_FLAG);
    let positional: Vec<String> = env::args().skip(1).filter(|a| a != ALLOW_FALSE_FLAG).collect();
    let dir = positional.first().cloned().unwrap_or_else(|| "out".to_string());
    let want = positional
        .get(1)
        .cloned()
        .unwrap_or_else(|| "age_over_18".to_string());

    let mdoc = fs::read(format!("{dir}/mdoc.bin")).expect("mdoc.bin");
    let transcript = fs::read(format!("{dir}/transcript.bin")).expect("transcript.bin");
    let params = fs::read_to_string(format!("{dir}/params.txt")).expect("params.txt");
    let p: Vec<&str> = params.lines().collect();
    let (pkx, pky, now, doc_type) = (p[0], p[1], p[2], p[3]);

    let parsed = parse_mdoc::<CompileNat<4>>(&mdoc, &transcript, doc_type)
        .expect("mdoc must parse as an ISO 18013-5 DeviceResponse");

    println!("doctype    : {doc_type}");
    println!("mdoc       : {} bytes", mdoc.len());
    println!("attributes the issuer signed:");
    for a in parsed.attrs.iter() {
        println!("   {:<20} cbor={:02x?}", String::from_utf8_lossy(&a.name), a.cbor_value);
    }

    let a = parsed
        .attrs
        .iter()
        .find(|a| a.name.as_slice() == want.as_bytes())
        .unwrap_or_else(|| panic!("{want} is not in this attestation"));
    // The value the circuit proves is the value the *request* names, never the
    // one read out of the attestation — otherwise the prover happily proves
    // whatever the credential happens to say, and `age_over_18 = false` comes
    // back looking like an answer.
    let claimed: &[u8] = match a.cbor_value.as_slice() {
        v if v == CBOR_TRUE => &CBOR_TRUE,
        v if v == CBOR_FALSE && allow_false => {
            println!("\n!! {ALLOW_FALSE_FLAG}: proving {want} = false. This proof asserts the");
            println!("!! holder does NOT satisfy the predicate. A verifier must refuse it.");
            &CBOR_FALSE
        }
        v => panic!(
            "{want} is {v:02x?} in this attestation, not {CBOR_TRUE:02x?} (CBOR true); \
             pass {ALLOW_FALSE_FLAG} to prove it anyway"
        ),
    };
    let req: Vec<RequestedAttribute> = vec![req_attr(doc_type, &a.name, claimed)];
    println!("\ndisclosing : {want} — and nothing else");

    let t = Instant::now();
    let c = provider::materialize(7, 1).expect("circuit v7 / 1 attribute");
    println!("circuit    : {} ({} bytes) in {:?}", c.spec.combined_hash_hex(), c.compressed.len(), t.elapsed());

    let mut rng = runtime_random::SecureRandomEngine::new();
    let t = Instant::now();
    let proof = run_mdoc_prover_inner(&c.spec, &c.compressed, &mdoc, pkx, pky,
                                      &transcript, &req, now, doc_type, &mut rng)
        .expect("prove");
    println!("PROVE      : {} bytes in {:?}", proof.len(), t.elapsed());

    let t = Instant::now();
    let ok = run_mdoc_verifier_inner(&c.spec, &c.compressed, pkx, pky,
                                     &transcript, &req, now, doc_type, &proof);
    println!("VERIFY     : {:?} in {:?}", ok.as_ref().map(|_| "ok"), t.elapsed());
    assert!(ok.is_ok(), "valid proof must verify");

    // Dump everything a separate verifier needs, so the Go service can be driven
    // from the same artefacts.
    fs::write(format!("{dir}/proof.bin"), &proof).expect("write proof.bin");
    let cbor_hex: String = claimed.iter().map(|b| format!("{b:02x}")).collect();
    fs::write(
        format!("{dir}/request.json"),
        format!(
            "{{\n  \"version\": 7,\n  \"num_attributes\": 1,\n  \"pkx\": \"{pkx}\",\n  \"pky\": \"{pky}\",\n  \
\"doc_type\": \"{doc_type}\",\n  \"namespace\": \"{doc_type}\",\n  \"attr_id\": \"{want}\",\n  \
\"attr_cbor_hex\": \"{cbor_hex}\",\n  \"now\": \"{now}\",\n  \"transcript\": \"transcript.bin\",\n  \
\"proof\": \"proof.bin\"\n}}\n"
        ),
    )
    .expect("write request.json");
    println!("wrote      : {dir}/proof.bin, {dir}/request.json");

    // Negative control 1: a different issuer key must not verify.
    let wrong_pkx = "0xb4682ec20e06e8df840b5dd32959798ab20c544d4da50109ff4684d06fd261fc";
    let bad_issuer = run_mdoc_verifier_inner(&c.spec, &c.compressed, wrong_pkx, pky,
                                             &transcript, &req, now, doc_type, &proof);
    println!("VERIFY(wrong issuer key): {:?}", bad_issuer.as_ref().map(|_| "ok"));
    assert!(bad_issuer.is_err(), "wrong issuer key must not verify");

    // Negative control 2: a tampered proof must not verify.
    let mut bad = proof.clone();
    bad[proof.len() / 2] ^= 0x01;
    let bad_proof = run_mdoc_verifier_inner(&c.spec, &c.compressed, pkx, pky,
                                            &transcript, &req, now, doc_type, &bad);
    println!("VERIFY(tampered proof)  : {:?}", bad_proof.as_ref().map(|_| "ok"));
    assert!(bad_proof.is_err(), "tampered proof must not verify");
}
