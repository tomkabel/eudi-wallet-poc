#!/usr/bin/env python3
"""Validate AV Annex A traceability offline; this does not execute mapped tests."""

import hashlib
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PROFILE = Path("docs/profiles/av")
# Independently recorded from the public upstream bytes and GitHub commit API.
# Updating these pins is an explicit, reviewed source refresh, not regeneration.
PIN = {
    "repository": "https://github.com/eu-digital-identity-wallet/av-doc-technical-specification",
    "revision": "014dc2fd0f9332eeabe1eef70d234ed2aef20fd7",
    "commit_date": "2026-09-01T21:59:02Z",
    "source_path": "docs/annexes/annex-A/annex-A-av-profile.md",
    "source_sha256": "54808f94484cb75e7b495b508f0c17568e41b7ca7c3657799c7b432a770538a9",
    "excerpt_sha256": "72af44bd3c04a341b1a49c0ef5bb6d975dc355009f3d381962f6aefd183ba8d3",
    "license_sha256": "8e7a9299a281b71fa9ad3f978bcbd5c4d8809b21b2ab51ca5e173e3a16ed6ccb",
    "attribution": "The European Age Verification Solution technical specification © 2025 by European Commission; upstream license: Creative Commons Attribution 4.0 International. Retained excerpt is unchanged; this registry and commentary are additions.",
}
# Go's filename constraints use known historical targets as well as current ones
# (go/build.goodOSArchFile and internal/syslist). Reject all such constraints:
# a mapped reference must not depend on the host's GOOS or GOARCH.
GO_PLATFORM_SUFFIXES = set("""
    aix android darwin dragonfly freebsd hurd illumos ios js linux nacl netbsd
    openbsd plan9 solaris wasip1 windows zos
    386 amd64 amd64p32 arm armbe arm64 arm64be loong64 mips mipsle mips64
    mips64le mips64p32 mips64p32le ppc ppc64 ppc64le riscv riscv64 s390 s390x
    sparc sparc64 wasm
""".split())
EXTRACTION = "Exact byte prefix before the first '## A.10. Comparison with other profiles' heading; no newline or wording changes."
STRENGTHS = {
    "MUST", "MUST NOT", "SHALL", "SHALL NOT", "SHOULD", "MAY",
    "declarative", "conditional", "recommendation", "permission", "informative",
}


class RegistryError(ValueError):
    """An actionable registry validation error."""


def require(condition, message):
    if not condition:
        raise RegistryError(message)


def sha256(raw):
    return hashlib.sha256(raw).hexdigest()


def fields(value, required, optional, where):
    require(isinstance(value, dict), f"{where}: expected object")
    require(set(required) <= value.keys(), f"{where}: missing fields {set(required) - value.keys()}")
    require(value.keys() <= set(required) | set(optional), f"{where}: unknown fields {value.keys() - set(required) - set(optional)}")


def nonempty(value, where):
    require(isinstance(value, str) and bool(value.strip()), f"{where}: expected non-empty text")


def anchor(heading):
    return re.sub(r"[^\w\- ]", "", heading.lower()).replace(" ", "-")


def source_blocks(text):
    """Partition *all* retained non-heading text, without normative-keyword guesses.

    Each paragraph, list item, table row and fenced code block is a denominator
    item. Blank lines and Markdown headings are structural; even informative
    prose and table separators receive explicit registry dispositions.
    """
    lines = text.splitlines()
    blocks, sections = [], []
    section, heading = "Annex A", "ANNEX A - Age Verification Profile"
    counts = {}
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.startswith("#"):
            heading = line.lstrip("# ")
            match = re.match(r"^## A\.(\d+)\b", line)
            if match:
                section = f"A.{match[1]}"
                sections.append(section)
            i += 1
            continue
        start = i
        kind = "paragraph"
        if line.startswith("```"):
            kind = "code"
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                i += 1
            require(i < len(lines), f"source line {start + 1}: unclosed code fence")
            i += 1
        elif line.startswith("|"):
            kind = "table-row"
            i += 1
        else:
            if re.match(r"^(?:[-*] |\d+\. )", line):
                kind = "list-item"
            i += 1
            while i < len(lines) and lines[i].strip():
                if re.match(r"^(?:#|```|\||[-*] |\d+\. )", lines[i]):
                    break
                i += 1
        counts[section] = counts.get(section, 0) + 1
        block_id = f"{section}-B{counts[section]:03}"
        excerpt = "\n".join(lines[start:i])
        blocks.append({
            "id": block_id, "section": section, "heading": heading,
            "anchor": anchor(heading), "start_line": start + 1, "end_line": i,
            "kind": kind, "excerpt": excerpt,
            "sha256": sha256(excerpt.encode("utf-8")),
        })
    require(sections == [f"A.{n}" for n in range(1, 10)],
            f"source: expected ordered A.1–A.9 headings, got {sections}")
    return blocks


def local_file(root, relative, where):
    nonempty(relative, where)
    path = Path(relative)
    require(not path.is_absolute() and ".." not in path.parts, f"{where}: expected repository-relative path")
    resolved = (root / path).resolve()
    require(resolved.is_relative_to(root.resolve()), f"{where}: path escapes checkout")
    require(resolved.is_file(), f"{where}: missing file {relative}")
    return resolved


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f"duplicate JSON object key {key!r}")
        result[key] = value
    return result


def load_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)
    except (OSError, ValueError) as exc:
        raise RegistryError(f"{path}: {exc}") from exc


def validate(root=ROOT, registry=None, source=None):
    """Return counts, or raise RegistryError; injectable inputs support mutations."""
    root = Path(root)
    source = load_json(root / PROFILE / "source.json") if source is None else source
    fields(source, [*PIN, "schema_version", "excerpt_path", "license_path", "extraction_rule",
                    "source_url", "license_url", "excluded_sections"], [], "source")
    require(type(source["schema_version"]) is int and source["schema_version"] == 1, "source: unsupported schema_version")
    for name, value in PIN.items():
        require(source[name] == value, f"source: pinned {name} mismatch")
    require(source["extraction_rule"] == EXTRACTION, "source: extraction rule mismatch")
    require(source["excluded_sections"] == ["A.10", "A.11"], "source: excluded sections must be A.10/A.11")
    require(source["excerpt_path"] == str(PROFILE / "annex-a-normative.md"), "source: excerpt path mismatch")
    require(source["license_path"] == str(PROFILE / "LICENSE.upstream"), "source: license path mismatch")
    raw_url = source["repository"].replace("https://github.com/", "https://raw.githubusercontent.com/")
    require(source["source_url"] == f"{raw_url}/{source['revision']}/{source['source_path']}", "source: canonical source URL mismatch")
    require(source["license_url"] == f"{raw_url}/{source['revision']}/LICENSE", "source: canonical license URL mismatch")
    nonempty(source["attribution"], "source attribution")
    raw = local_file(root, source["excerpt_path"], "source excerpt").read_bytes()
    require(sha256(raw) == source["excerpt_sha256"], "source excerpt: SHA-256 pin mismatch")
    license_raw = local_file(root, source["license_path"], "upstream license").read_bytes()
    require(sha256(license_raw) == source["license_sha256"], "upstream license: SHA-256 pin mismatch")
    expected_blocks = source_blocks(raw.decode("utf-8"))

    registry = load_json(root / PROFILE / "clause-registry.json") if registry is None else registry
    fields(registry, ["schema_version", "source_revision", "source_excerpt_sha256", "evidence_status",
                      "source_blocks", "tests", "clauses"], [], "registry")
    require(type(registry["schema_version"]) is int and registry["schema_version"] == 1, "registry: unsupported schema_version")
    require(registry["source_revision"] == PIN["revision"], "registry: source revision mismatch")
    require(registry["source_excerpt_sha256"] == PIN["excerpt_sha256"], "registry: source excerpt hash mismatch")
    require(registry["evidence_status"] == "mappings only; conformance not executed", "registry: cannot claim executed conformance")
    require(isinstance(registry["source_blocks"], list), "source_blocks: expected list")
    actual_blocks = registry["source_blocks"]
    require(len(actual_blocks) == len(expected_blocks), f"source_blocks: expected {len(expected_blocks)} blocks from retained text, got {len(actual_blocks)}; missing or extra source coverage")
    for expected, actual in zip(expected_blocks, actual_blocks):
        require(actual == expected, f"source block {expected['id']}: missing, reordered or inconsistent with retained text (including excerpt/location)")

    require(isinstance(registry["tests"], list), "tests: expected list")
    tests = {}
    for test in registry["tests"]:
        fields(test, ["id", "path", "symbol", "asserts", "limits", "command", "requires"], [], "test")
        for name in test:
            nonempty(test[name], f"test {test.get('id')}: {name}")
        test_id = test["id"]
        require(test_id == f"{test['path']}::{test['symbol']}", f"test {test_id}: id must equal path::symbol")
        require(test_id not in tests, f"duplicate test id {test_id}")
        require(test["path"].endswith("_test.go"), f"test {test_id}: expected Go test file")
        test_file = local_file(root, test["path"], f"test {test_id}")
        filename = Path(test["path"]).name
        require(not filename.startswith((".", "_")), f"test {test_id}: Go ignores filenames beginning with '.' or '_'")
        stem = filename.split(".", 1)[0].removesuffix("_test")
        require(not ("_" in stem and stem.rsplit("_", 1)[-1] in GO_PLATFORM_SUFFIXES),
                f"test {test_id}: implicit Go platform filename constraints are unsupported for mapped files")
        require(test["path"].startswith("verifier/go/"), f"test {test_id}: expected test under verifier/go")
        package = Path(test["path"]).parent.relative_to("verifier/go").as_posix()
        selector = "." if package == "." else f"./{package}"
        command = f"cd verifier/go && go test {selector} -run ^{test['symbol']}$"
        require(test["command"] == command, f"test {test_id}: command must equal {command!r}")
        code = test_file.read_text(encoding="utf-8")
        require(re.search(r"(?m)^[ \t]*//(?:go:build|[ \t]+\+build)(?:[ \t]|$)", code) is None,
                f"test {test_id}: explicit Go build constraints are unsupported for mapped files")
        # Blank Go comments/literals, preserving whitespace and line boundaries:
        # mentions cannot satisfy declarations, and tokens must not concatenate.
        code = re.sub(r'//[^\n]*|/\*[\s\S]*?\*/|`[^`]*`|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',
                      lambda match: re.sub(r"[^\n]", " ", match.group()), code)
        signature = rf"(?m)^func\s+{re.escape(test['symbol'])}\s*\(\s*\w+\s+\*testing\.T\s*,?\s*\)\s*\{{"
        require(re.fullmatch(r"Test[A-Z0-9]\w*", test["symbol"]) is not None and re.search(signature, code) is not None,
                f"test {test_id}: missing real Go test symbol {test['symbol']}")
        tests[test_id] = test

    require(isinstance(registry["clauses"], list) and registry["clauses"], "clauses: expected non-empty list")
    by_block = {b["id"]: [] for b in expected_blocks}
    block_order = {b["id"]: i for i, b in enumerate(expected_blocks)}
    ids, used_tests, last_order, mapped = set(), set(), -1, 0
    for clause in registry["clauses"]:
        fields(clause, ["id", "block_id", "normative_strength", "excerpt"], ["test", "out_of_scope_for_v1"], "clause")
        clause_id = clause["id"]
        nonempty(clause_id, "clause id")
        require(re.fullmatch(r"AV-(?:ANNEX|A\.[1-9])-\d{3}(?:\.\d+)*", clause_id) is not None, f"clause {clause_id}: invalid stable id")
        require(clause_id not in ids, f"duplicate clause id {clause_id}")
        ids.add(clause_id)
        block = clause["block_id"]
        require(isinstance(block, str) and block in by_block, f"clause {clause_id}: unknown source block {block}")
        order = block_order[block]
        require(order >= last_order, f"clause {clause_id}: source order changed at {block}")
        last_order = order
        prefix = "ANNEX" if block.startswith("Annex A-") else block.split("-B")[0]
        require(clause_id.startswith(f"AV-{prefix}-"), f"clause {clause_id}: id disagrees with source section {block}")
        strengths = clause["normative_strength"]
        require(isinstance(strengths, list) and strengths and all(isinstance(s, str) and s in STRENGTHS for s in strengths)
                and len(set(strengths)) == len(strengths), f"clause {clause_id}: invalid normative_strength")
        nonempty(clause["excerpt"], f"clause {clause_id}: excerpt")
        keywords = set(re.findall(r"\b(?:MUST NOT|SHALL NOT|MUST|SHALL|SHOULD|MAY)\b", clause["excerpt"]))
        require(keywords <= set(strengths), f"clause {clause_id}: normative_strength omits source keywords {keywords - set(strengths)}")
        by_block[block].append(clause["excerpt"])
        require(("test" in clause) != ("out_of_scope_for_v1" in clause), f"clause {clause_id}: exactly one test mapping or out of scope for v1 reason required")
        if "test" in clause:
            mapping = clause["test"]
            fields(mapping, ["id", "coverage", "scope", "limits"], [], f"clause {clause_id}: test")
            require(isinstance(mapping["id"], str) and mapping["id"] in tests, f"clause {clause_id}: dangling test reference {mapping['id']}")
            require(mapping["coverage"] in ("partial", "full"), f"clause {clause_id}: invalid mapping coverage")
            nonempty(mapping["scope"], f"clause {clause_id}: mapping scope")
            nonempty(mapping["limits"], f"clause {clause_id}: mapping limits")
            used_tests.add(mapping["id"])
            mapped += 1
        else:
            reason = clause["out_of_scope_for_v1"]
            nonempty(reason, f"clause {clause_id}: out of scope for v1 reason")
            prefix = "out of scope for v1: "
            require(reason.startswith(prefix) and bool(reason[len(prefix):].strip()), f"clause {clause_id}: explicit out of scope for v1 reason required")
    for block in expected_blocks:
        excerpts = by_block[block["id"]]
        require(excerpts, f"source block {block['id']}: missing clause coverage")
        # Multiple clauses may partition a block. No omitted or invented text,
        # overlaps, or reordered fragments may disappear behind a shared ID.
        require("".join(excerpts) == block["excerpt"], f"source block {block['id']}: unaccounted or inconsistent source text in clause excerpts")
    require(used_tests == tests.keys(), f"test catalogue: orphan references {tests.keys() - used_tests}")
    return {"source_blocks": len(expected_blocks), "clauses": len(ids), "mapped": mapped,
            "excluded": len(ids) - mapped, "tests": len(tests)}


def main():
    try:
        counts = validate()
    except (RegistryError, OSError, UnicodeError) as exc:
        print(f"AV registry: FAIL: {exc}", file=sys.stderr)
        return 1
    print("AV registry: OK — " + ", ".join(f"{name}={value}" for name, value in counts.items()))
    print("Traceability only: mapped tests have not been executed by this check.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
