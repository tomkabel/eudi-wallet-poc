.PHONY: deps lint test-go-fast test-go-zk e2e

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
	rev="$$(cat verifier/zkverify-ffi/longfellow-rev.txt)" && \
		cd verifier/go && go build -ldflags "-X main.longfellowRev=$$rev" -o ../zkverify .

lint:
	cd verifier/go && unformatted="$$(gofmt -l .)" && if [ -n "$$unformatted" ]; then \
		echo "not gofmt-clean:"; echo "$$unformatted"; exit 1; fi
	cd verifier/go && go vet ./...
	ruff check --isolated --select F,E9 --exclude eudi-arf .
	shellcheck --severity=warning scripts/*.sh tests/*.sh

test-go-fast:
	cd verifier/go && go test ./oid4vp/... ./internal/... ./circuits/...

test-go-zk:
	@if [ ! -f verifier/zkverify-ffi/target/release/libzkverify.a ]; then \
		echo "make test-go-zk: verifier/zkverify-ffi/target/release/libzkverify.a is missing —" >&2; \
		echo "  the zk tests link the Rust staticlib through cgo, and a missing one fails at" >&2; \
		echo "  the linker before any test code runs. Build it first:" >&2; \
		echo "    make deps" >&2; \
		exit 1; \
	fi
	cd verifier/go && go test . ./zk/...

e2e: deps
	EE_PROVER="$$(pwd)/../longfellow-zk/rust/target/release/examples/ee_poa_demo" \
		EE_VERIFIER="$$(pwd)/verifier/zkverify" tests/e2e.sh
