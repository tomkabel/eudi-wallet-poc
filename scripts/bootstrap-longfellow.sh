#!/usr/bin/env bash
# Fetch google/longfellow-zk at the pinned revision, as the sibling directory
# verifier/zkverify-ffi's Cargo.toml path-depends on (../../../longfellow-zk).
#
# The revision is committed in verifier/zkverify-ffi/longfellow-rev.txt — a path
# dependency has no lockfile entry, so that file is the single source of truth.
# CI's e2e job runs this script too, so dev and CI bootstrap identically.
#
# Idempotent: exits 0 fast when the sibling is already at the pinned revision.
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
parent="$(dirname "$repo")"
sibling="$parent/longfellow-zk"

rev="$(tr -d '[:space:]' <"$repo/verifier/zkverify-ffi/longfellow-rev.txt")"
if ! grep -Eq '^[0-9a-f]{40}$' <<<"$rev"; then
	echo "bootstrap-longfellow: verifier/zkverify-ffi/longfellow-rev.txt is not a 40-hex git revision: '$rev'" >&2
	exit 1
fi

# Already there? Nothing to do — make deps and CI re-run this script freely.
if [ -e "$sibling/.git" ]; then
	got="$(git -C "$sibling" rev-parse HEAD 2>/dev/null || true)"
	if [ "$got" = "$rev" ]; then
		echo "longfellow-zk already at $rev"
		exit 0
	fi
	echo "longfellow-zk exists at $got, want $rev: re-fetching" >&2
fi

git init -q "$sibling"
git -C "$sibling" remote remove origin 2>/dev/null || true
git -C "$sibling" remote add origin https://github.com/google/longfellow-zk.git
git -C "$sibling" fetch -q --depth 1 origin "$rev"
git -C "$sibling" checkout -q --detach FETCH_HEAD
# A shallow fetch of a SHA is only as trustworthy as this check.
got="$(git -C "$sibling" rev-parse HEAD)"
[ "$got" = "$rev" ] || { echo "bootstrap-longfellow: got $got, want $rev" >&2; exit 1; }
echo "longfellow-zk checked out at $rev"
