# The ZK age-proof sandbox as one image: server, issuer, wallet, prover, verifier.
#
# Built by sandbox/deploy/deploy.sh, which stages the two binaries into bin/
# from a `make deps` build on the host. trixie because zkverify links against
# glibc 2.39. Both binaries target x86-64-v3 (AVX2, BMI2, FMA), so the host
# running this image needs that CPU level.
# ponytail: host-built binaries copied in. A from-source multi-stage build
# (Rust + Go + the pinned longfellow-zk checkout) belongs here once the image
# has to be reproducible on CI rather than on one laptop.
FROM debian:trixie-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 python3-cbor2 python3-cryptography python3-jwcrypto \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 10001 --home-dir /nonexistent --shell /usr/sbin/nologin sandbox

WORKDIR /app
COPY bin/ee_poa_demo bin/zkverify /usr/local/bin/
# Both binaries read their circuits from the longfellow-zk checkout they were
# built in (env!("CARGO_MANIFEST_DIR") in mdoc_zk/artifacts), so the circuits
# go to that same absolute path. deploy.sh passes it.
ARG CIRCUITS_DIR
COPY circuits/ ${CIRCUITS_DIR}/
COPY verifier/circuits.json verifier/circuits.json
COPY issuer/mint_ee_poa.py issuer/
COPY wallet/present.py wallet/
COPY sandbox/server.py sandbox/
COPY sandbox/static sandbox/static

USER sandbox
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=5s CMD python3 -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/', timeout=4)"
CMD ["python3", "sandbox/server.py", "--host", "0.0.0.0", "--port", "8000", "--trust-proxy", \
     "--prover", "/usr/local/bin/ee_poa_demo", "--verifier", "/usr/local/bin/zkverify"]
