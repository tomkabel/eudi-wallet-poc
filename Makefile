.PHONY: deps py-deps doctor bootstrap test-doctor keys publish-guard pin-gate lint lint-go lint-py lint-rust lint-kotlin lint-sh pki-gates test-go-fast test-go-zk test-py test-av-registry test-av-fixtures test-av-conformance test-carrier-conformance check e2e sandbox

SHELL := /bin/bash

# The x86-64-v3 baseline: longfellow-zk's own rust/.cargo/config.toml sets
# target-cpu=native, which has produced SIGILL binaries on virtualised hosts.
# zkverify-ffi pins the same baseline in its own .cargo/config.toml, so its
# cargo build needs no RUSTFLAGS here — this pin is for the prover examples,
# which build inside the longfellow-zk tree.
RUSTFLAGS := -C target-cpu=x86-64-v3

# `make py-deps` builds .venv from uv.lock; every target below then picks that python3 up.
export PATH := $(CURDIR)/.venv/bin:$(PATH)

deps:
	./scripts/bootstrap-longfellow.sh
	./scripts/prover-examples-install.sh
	RUSTFLAGS='$(RUSTFLAGS)' cargo build --locked --release --examples --features testonly \
		--manifest-path ../longfellow-zk/rust/applications/mdoc_zk/runtime/Cargo.toml
	# cargo --examples exits 0 with no binary when examples/ is empty; then record what was
	# built (sha256 and source revision) for `make doctor` to re-check.
	test -x ../longfellow-zk/rust/target/release/examples/ee_poa_demo
	echo "$$(sha256sum < ../longfellow-zk/rust/target/release/examples/ee_poa_demo | cut -d' ' -f1)" \
		"$$(tr -d '[:space:]' < verifier/zkverify-ffi/longfellow-rev.txt)" \
		> ../longfellow-zk/rust/target/release/examples/ee_poa_demo.built
	# cd rather than --manifest-path: cargo discovers .cargo/config.toml from the
	# working directory, and zkverify-ffi's is what pins its target-cpu.
	(cd verifier/zkverify-ffi && cargo build --release)
	# The staticlib can exist without being linkable; make the failure loud here
	# rather than in a linker error (cgo links it before any Go code can run).
	test -f verifier/zkverify-ffi/target/release/libzkverify.a
	rev="$$(tr -d '[:space:]' < verifier/zkverify-ffi/longfellow-rev.txt)" && \
		cd verifier/go && go build -ldflags "-X main.longfellowRev=$$rev" -o ../zkverify .

# The locked Python inputs (pyproject.toml groups `ci` and `lint`, hashes in uv.lock); fails if the two
# disagree. `lint` pins ruff, pyright and pre-commit so a laptop counts what CI counts.
py-deps:
	uv sync --locked --only-group ci --only-group lint

# One line per failed check; exit 1 on any. See tools/doctor.py for the checks and DOCTOR_SKIP.
doctor:
	python3 tools/doctor.py

# Devcontainer only (mise.toml pins the tools the image already carries): installs the pinned
# Python deps, builds everything, then runs the doctor. Android is not in the image.
bootstrap:
	@if [ ! -f /.dockerenv ] && [ ! -f /run/.containerenv ] && [ -z "$$BOOTSTRAP_HOST" ]; then \
		echo "make bootstrap installs packages: run it inside the devcontainer (BOOTSTRAP_HOST=1 to override)" >&2; \
		exit 1; fi
	if command -v mise >/dev/null; then mise install; fi
	$(MAKE) py-deps
	$(MAKE) deps
	DOCTOR_SKIP=android $(MAKE) doctor

# The doctor must fail on a machine without the prover, so this points it at an empty sibling.
test-doctor:
	python3 -m unittest discover -s tests -p 'test_doctor.py'

# local_mocks issuer keys (gitignored); Gradle runs the same script before asset merge.
keys:
	./tools/gen-mock-keys.sh

# Fails when a private key, a keystore or demo-data/ is tracked outside tools/publish-guard.allow.
publish-guard:
	python3 tools/publish_guard.py
	python3 -m unittest discover -s tests -p 'test_publish_guard.py'

# Fails on a `uses:` not pinned to a 40-hex SHA or a Dockerfile FROM without @sha256: (T2);
# exceptions, each with a reason, live in tools/pin-gate.allow. security.yml also runs zizmor.
pin-gate:
	python3 tools/pin_gate.py
	python3 -m unittest discover -s tests -p 'test_pin_gate.py'

# `make lint` is every linter, once: Go, Python, Rust, Kotlin and shell. Violations that predate a
# gate are held by a baseline that may only shrink (tools/lint-baseline.json, app/detekt-baseline.xml,
# spotless ratchetFrom in app/build.gradle.kts); anything new fails. Needs `make py-deps` (ruff,
# pyright), cargo with ../longfellow-zk (clippy), JDK 17 and the Gradle caches (ktlint, detekt).
lint: lint-go lint-py lint-rust lint-kotlin lint-sh

lint-go:
	cd verifier/go && unformatted="$$(gofmt -l .)" && if [ -n "$$unformatted" ]; then \
		echo "not gofmt-clean:"; echo "$$unformatted"; exit 1; fi
	cd verifier/go && go vet ./...
	cd verifier/go && golangci-lint run ./...

# `ruff check .` is the blocking set (F, E9 in pyproject.toml); lint_ratchet.py holds the wider rule
# families, pyright and `ruff format` of new files to the committed baseline.
lint-py:
	python3 -m compileall -q issuer verifier wallet tests sandbox
	ruff check .
	python3 tools/lint_ratchet.py

lint-rust:
	cd verifier/zkverify-ffi && cargo fmt --check
	cd verifier/zkverify-ffi && cargo clippy --locked --all-targets -- -D warnings

# Gradle here needs JDK 17: $JAVA_HOME if it is one, else the first /usr/lib/jvm/*17* (as tools/doctor.py).
GRADLE_JAVA_HOME ?= $(shell for d in "$$JAVA_HOME" /usr/lib/jvm/*17*; do \
	[ -x "$$d/bin/java" ] && "$$d/bin/java" -version 2>&1 | grep -q '"17\.' && { echo "$$d"; break; }; done)

lint-kotlin:
	@test -n "$(GRADLE_JAVA_HOME)" || { echo "make lint-kotlin: no JDK 17 found (set JAVA_HOME)" >&2; exit 1; }
	JAVA_HOME="$(GRADLE_JAVA_HOME)" ./gradlew --console=plain -q :app:spotlessCheck :app:detekt

lint-sh:
	shellcheck --severity=warning scripts/*.sh tests/*.sh tools/*.sh demo/*.sh

# -race as in CI: these tests assert concurrency behaviour (session sweeps,
# the reads limiter) that only the race detector exercises.
test-go-fast:
	cd verifier/go && go test -race ./oid4vp/... ./internal/... ./circuits/...

# The Python half of the transcript pin and the mint's device-key path. Run `make py-deps`
# first, not whatever PyPI serves: the vectors are golden and uv.lock pins their inputs.
test-py:
	python3 wallet/test_transcript.py
	python3 issuer/test_mint_device_key.py
	python3 issuer/test_mint_custody.py
	python3 sandbox/test_age.py

# Everything that runs without the prover or an emulator: the lint job and ci.yml's fast job. Needs
# the toolchains `make lint` lists; `make test-go-zk` and `make e2e` are the rest of CI.
check: lint test-doctor pki-gates publish-guard pin-gate test-go-fast test-py test-av-registry test-av-fixtures test-av-conformance test-carrier-conformance

# PKI plan gates and their seeded failures (security.yml runs the same).
pki-gates:
	python3 tests/check_pki_gates.py
	python3 -m unittest discover -s tests -p 'test_pki_gates.py'

# Offline traceability validation and its malformed-input controls use stdlib only.
test-av-registry:
	python3 tests/check_av_registry.py
	python3 -m unittest discover -s tests -p 'test_av_registry.py'

# Exact retained examples, copied proof and locally built requests; no writes.
test-av-fixtures:
	python3 tools/build_av_fixtures.py --check
	python3 -m unittest discover -s tests -p 'test_av_fixtures.py'

# Runner/report regressions cover ordered evidence, host diagnostics and fail-closed adapters.
test-av-conformance:
	python3 -m unittest discover -s tests -p 'test_av_conformance.py'

# Runner inventory/order/outcome controls use fake child processes and need no Rust build.
test-carrier-conformance:
	python3 tests/check_carrier_versions.py
	python3 -m unittest discover -s tests -p 'test_carrier_versions.py'
	python3 -m unittest discover -s tests -p 'test_carrier_conformance.py'

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
		echo "make e2e: Python dependencies missing — make py-deps" >&2; exit 1; }
	EE_PROVER="$$(pwd)/../longfellow-zk/rust/target/release/examples/ee_poa_demo" \
		EE_VERIFIER="$$(pwd)/verifier/zkverify" python3 tests/run_carrier_conformance.py

sandbox:
	EE_PROVER="$$(pwd)/../longfellow-zk/rust/target/release/examples/ee_poa_demo" \
		EE_VERIFIER="$$(pwd)/verifier/zkverify" python3 sandbox/server.py
