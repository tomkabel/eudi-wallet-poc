#!/usr/bin/env bash
# Mints the local_mocks issuer keystores and the throwaway IACA root that signs them
# into app/src/local_mocks/assets/keys/ (gitignored). `make keys` runs this; so does the
# Gradle task generateMockKeys, ahead of every local_mocks asset merge.
#
#   doc_signer_mdl_issuer.p12  alias mdl_issuer  (password changeit)
#   doc_signer_pid_issuer.p12  alias pid_issuer  (password changeit)
#   iaca_root.cer.pem          the root they chain to; same name as the app's shipped
#                              keys/iaca_root.cer.pem, so in a local_mocks build this one
#                              overrides it and mock-issued mdocs validate.
#
# Idempotent: does nothing when all three exist. FORCE=1 re-mints. KEYTOOL overrides the binary.
set -euo pipefail

out="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/app/src/local_mocks/assets/keys"
kt="${KEYTOOL:-keytool}"
pw=changeit

if [ -z "${FORCE:-}" ] && [ -f "$out/doc_signer_mdl_issuer.p12" ] && [ -f "$out/doc_signer_pid_issuer.p12" ] \
   && [ -f "$out/iaca_root.cer.pem" ]; then
  exit 0
fi

mkdir -p "$out"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

# keytool writes progress and warnings to stderr; show them only when a step fails.
k() { "$kt" "$@" 2>"$tmp/err" || { cat "$tmp/err" >&2; exit 1; }; }
dn() { echo "CN=$1,O=TEST Authority,ST=Harju,C=EE"; }
kp() { k -genkeypair -keyalg EC -groupname secp256r1 -sigalg SHA256withECDSA -validity 3650 \
         -storetype PKCS12 -storepass "$pw" -keypass "$pw" "$@"; }

# Root: CA, certSign + cRLSign (what the app's shipped IACA root declares).
kp -alias iaca -keystore "$tmp/root.p12" -dname "$(dn 'TEST IACA Root CA (local mocks)')" \
   -ext bc:c=ca:true,pathlen:0 -ext ku:c=keyCertSign,cRLSign
k -exportcert -rfc -alias iaca -keystore "$tmp/root.p12" -storepass "$pw" -file "$tmp/iaca_root.cer.pem"

# Document signers: not a CA, digitalSignature, mdlDS EKU (the app's MDOC_DS profile requires all three).
mint() {  # alias, subject, file
  local ks="$tmp/$3"
  kp -alias "$1" -keystore "$ks" -dname "$(dn "$2")"
  k -certreq -alias "$1" -keystore "$ks" -storepass "$pw" -file "$tmp/$1.csr"
  k -gencert -rfc -alias iaca -keystore "$tmp/root.p12" -storepass "$pw" -validity 3650 \
     -sigalg SHA256withECDSA -infile "$tmp/$1.csr" -outfile "$tmp/$1.pem" \
     -ext bc:c=ca:false -ext ku:c=digitalSignature -ext eku=1.0.18013.5.1.2
  # keytool only accepts the signed reply once the root is trusted in the same store, so
  # the root rides along as a trusted-cert entry and the key's chain is [DS, root].
  k -importcert -noprompt -alias iaca -file "$tmp/iaca_root.cer.pem" -keystore "$ks" -storepass "$pw"
  k -importcert -noprompt -alias "$1" -file "$tmp/$1.pem" -keystore "$ks" -storepass "$pw"
  mv "$ks" "$out/$3"
}
mint mdl_issuer "TEST mDL Issuer" doc_signer_mdl_issuer.p12
mint pid_issuer "TEST PID Issuer" doc_signer_pid_issuer.p12
cp "$tmp/iaca_root.cer.pem" "$out/iaca_root.cer.pem"
echo "mock keys written to $out"
