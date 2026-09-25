#!/usr/bin/env bash
# Copy this repository's prover examples into the longfellow-zk sibling tree so
# `cargo build --examples` has targets. CI's e2e job runs this script, so dev and
# CI install the examples identically.
#
# A fresh longfellow-zk clone has no examples/ directory. Without the mkdir the
# cp fails, and `cargo build --examples` then matches no targets, prints a
# warning and exits 0 — a green build with no binary.
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
sibling="$(dirname "$repo")/longfellow-zk"
examples="$sibling/rust/applications/mdoc_zk/runtime/examples"

if [ ! -d "$sibling/rust" ]; then
	echo "prover-examples-install: $sibling does not look like a longfellow-zk checkout — run scripts/bootstrap-longfellow.sh (make deps)" >&2
	exit 1
fi

mkdir -p "$examples"
cp "$repo"/zk-age-poc/*.rs "$examples/"
echo "installed $(cd "$examples" && ls *.rs | wc -l) prover example(s) into $examples"
