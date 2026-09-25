// EE-EUDIW PoC probe: prove `age_over_18` from an ISO 18013-5 mdoc in zero knowledge.
use compile_algebra::CompileNat;
use core_algebra::Nat;
use mdoc_zk_circuits::parse_test_data;
use mdoc_zk_runtime::{
    provider, req_attr, run_mdoc_prover_inner, run_mdoc_verifier_inner, RequestedAttribute,
};
use std::time::Instant;

/// Demonstrates generating and verifying a zero-knowledge proof for the `age_over_18`

/// attribute in an ISO 18013-5 mdoc, including verification of a tampered proof.

///

/// # Examples

///

/// ```no_run

/// // Run the demonstration binary to generate and verify the proof.

/// ```
fn main() {
    let (issuer_pk, parsed, now) =
        parse_test_data::<4, CompileNat<4>>(&mdoc_zk_testcases::vectors::TEST_DATA);

    // TEST_DATA is a published fixture, so dumping every attribute it carries —
    // including `nym` — costs nobody anything, and showing what is in the mdoc is
    // the point of the demo. Against a real credential this loop would be a
    // disclosure bug: the whole claim of the ZK path is that the holder reveals one
    // predicate and the verifier learns nothing else. Nothing below discloses these;
    // only `age_over_18` is ever requested.
    println!("attributes in test mdoc:");
    for a in parsed.attrs.iter() {
        println!("   {:<24} cbor={:02x?}", String::from_utf8_lossy(&a.name), a.cbor_value);
    }

    let target = b"age_over_18";
    let a = parsed.attrs.iter()
        .find(|a| a.name.as_slice() == target)
        .expect("age_over_18 not present in test vector");
    // The mDL namespace is the doctype without the `.mDL` suffix, so the two read
    // alike and are easy to swap by accident. Name both.
    let doc_type    = "org.iso.18013.5.1.mDL";
    let namespace   = "org.iso.18013.5.1";
    let req_attrs: Vec<RequestedAttribute> = vec![req_attr(namespace, &a.name, [0xf5])];

    let mdoc_bytes  = mdoc_zk_testcases::vectors::TEST_DATA.mdoc;
    let transcript  = mdoc_zk_testcases::vectors::TEST_DATA.transcript;
    let px = format!("0x{}", num_bigint::BigUint::from_bytes_le(&issuer_pk.0.to_bytes_le()).to_str_radix(16));
    let py = format!("0x{}", num_bigint::BigUint::from_bytes_le(&issuer_pk.1.to_bytes_le()).to_str_radix(16));

    let t = Instant::now();
    let provided = provider::materialize(7, 1).expect("materialize circuit v7/1attr");
    println!("\ncircuit  : {} ({} bytes compressed) loaded in {:?}",
             provided.spec.combined_hash_hex(), provided.compressed.len(), t.elapsed());

    let mut rng = runtime_random::SecureRandomEngine::new();
    let t = Instant::now();
    let proof = run_mdoc_prover_inner(&provided.spec, &provided.compressed, mdoc_bytes,
        &px, &py, transcript, &req_attrs, now, doc_type, &mut rng).expect("prove");
    println!("PROVE    : {} bytes in {:?}", proof.len(), t.elapsed());

    let t = Instant::now();
    let ok = run_mdoc_verifier_inner(&provided.spec, &provided.compressed,
        &px, &py, transcript, &req_attrs, now, doc_type, &proof);
    println!("VERIFY   : {:?} in {:?}", ok.as_ref().map(|_| "ok"), t.elapsed());
    assert!(ok.is_ok(), "valid proof must verify");

    // negative control: flip a bit in the proof
    let mut bad = proof.clone();
    bad[proof.len() / 2] ^= 0x01;
    let bad_res = run_mdoc_verifier_inner(&provided.spec, &provided.compressed,
        &px, &py, transcript, &req_attrs, now, doc_type, &bad);
    println!("VERIFY(tampered): {:?}", bad_res.as_ref().map(|_| "ok"));
    assert!(bad_res.is_err(), "tampered proof must not verify");
}
