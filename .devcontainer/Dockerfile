# Build image for `make bootstrap` and `make check`. Tool versions come from ../mise.toml,
# the same file tools/doctor.py reads. The base is pinned by the multi-arch index digest of
# ubuntu:24.04 (resolved 2026-11-04 with `skopeo inspect`); bump both lines together.
FROM ubuntu:24.04@sha256:fe81560990fae86dc4743202b2cb4d3df98d0bcf949173cecef15ce1127ef852

ARG MISE_VERSION=v2026.11.1
# sha256 of mise-${MISE_VERSION}-linux-x64.tar.gz, from that release's SHASUMS256.txt.
ARG MISE_SHA256=9c3f740597f5e5815ecb50ec6fafa080e94472d0780220bcb214eb98bd902dca

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      build-essential pkg-config openjdk-17-jdk-headless git curl ca-certificates make jq iproute2 shellcheck unzip xz-utils \
 && rm -rf /var/lib/apt/lists/* \
 && curl -fsSL -o /tmp/mise.tgz "https://github.com/jdx/mise/releases/download/${MISE_VERSION}/mise-${MISE_VERSION}-linux-x64.tar.gz" \
 && echo "${MISE_SHA256}  /tmp/mise.tgz" | sha256sum -c - \
 && tar -xzf /tmp/mise.tgz -C /usr/local --strip-components=1 mise/bin/mise \
 && rm /tmp/mise.tgz

# The base image's `ubuntu` user (uid 1000) owns the tools, so `make py-deps` (uv sync into .venv) in make bootstrap works.
USER ubuntu
ENV HOME=/home/ubuntu \
    PATH=/home/ubuntu/.local/share/mise/shims:/home/ubuntu/.cargo/bin:/usr/local/bin:/usr/bin:/bin
WORKDIR /tmp/mise-install
COPY --chown=ubuntu:ubuntu mise.toml ./
RUN mise trust ./mise.toml && mise install && mise reshim
WORKDIR /workspaces/eudi-wallet-poc
