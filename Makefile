.PHONY: deps lint test-go-fast test-go-zk test-py check e2e sandbox

SHELL := /bin/bash

# The x86-64-v3 baseline: longfellow-zk's own rust/.cargo/config.toml sets
# target-cpu=native, which has produced SIGILL binaries on virtualised hosts.
# zkverify-ffi pins the same baseline in its own .cargo/config.toml, so its
# cargo build needs no RUSTFLAGS here — this pin is for the prover examples,
# which build inside the longfellow-zk tree.
RUSTFLAGS := -C target-cpu=x86-64-v3

deps:
	./scripts/bootstrap-longfellow.sh
	./scripts/prover-examples-install.sh
	RUSTFLAGS='$(RUSTFLAGS)' cargo build --release --examples --features testonly \
		--manifest-path ../longfellow-zk/rust/applications/mdoc_zk/runtime/Cargo.toml
	# cd rather than --manifest-path: cargo discovers .cargo/config.toml from the
	# working directory, and zkverify-ffi's is what pins its target-cpu.
	(cd verifier/zkverify-ffi && cargo build --release)
	# The staticlib can exist without being linkable; make the failure loud here
	# rather than in a linker error (cgo links it before any Go code can run).
	test -f verifier/zkverify-ffi/target/release/libzkverify.a
	rev="$$(tr -d '[:space:]' < verifier/zkverify-ffi/longfellow-rev.txt)" && \
		cd verifier/go && go build -ldflags "-X main.longfellowRev=$$rev" -o ../zkverify .

lint:
	cd verifier/go && unformatted="$$(gofmt -l .)" && if [ -n "$$unformatted" ]; then \
		echo "not gofmt-clean:"; echo "$$unformatted"; exit 1; fi
	cd verifier/go && go vet ./...
	python3 -m compileall -q issuer verifier wallet tests
	ruff check --isolated --select F,E9 --exclude eudi-arf,_bmad,.agents,.claude .
	shellcheck --severity=warning scripts/*.sh tests/*.sh tools/*.sh demo/*.sh

# -race as in CI: these tests assert concurrency behaviour (session sweeps,
# the reads limiter) that only the race detector exercises.
test-go-fast:
	cd verifier/go && go test -race ./oid4vp/... ./internal/... ./circuits/...

# The Python half of the transcript pin and the mint's device-key path. Install
# tests/requirements-ci.txt, not whatever PyPI serves: the vectors are golden.
test-py:
	python3 wallet/test_transcript.py
	python3 issuer/test_mint_device_key.py

# CI's fast job: no Rust toolchain needed.
check: lint test-go-fast test-py

# EE_BENCH_SKIP=1 as in CI: the step 8 percentile run is a host measurement,
# not a check. `EE_BENCH_SKIP= make test-go-zk` runs it.
test-go-zk:
	@if [ ! -f verifier/zkverify-ffi/target/release/libzkverify.a ]; then \
		echo "make test-go-zk: verifier/zkverify-ffi/target/release/libzkverify.a is missing —" >&2; \
		echo "  the zk tests link the Rust staticlib through cgo, and a missing one fails at" >&2; \
		echo "  the linker before any test code runs. Build it first:" >&2; \
		echo "    make deps" >&2; \
		exit 1; \
	fi
	cd verifier/go && EE_BENCH_SKIP=$${EE_BENCH_SKIP-1} go test -race . ./zk/...

e2e: deps
	@python3 -c 'import cbor2, cryptography, jwcrypto' 2>/dev/null || { \
		echo "make e2e: Python dependencies missing — pip install -r tests/requirements-ci.txt" >&2; exit 1; }
	EE_PROVER="$$(pwd)/../longfellow-zk/rust/target/release/examples/ee_poa_demo" \
		EE_VERIFIER="$$(pwd)/verifier/zkverify" tests/e2e.sh

sandbox:
	EE_PROVER="$$(pwd)/../longfellow-zk/rust/target/release/examples/ee_poa_demo" \
		EE_VERIFIER="$$(pwd)/verifier/zkverify" python3 sandbox/server.py
