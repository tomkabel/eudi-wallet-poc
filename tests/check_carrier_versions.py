#!/usr/bin/env python3
"""Offline provenance check for numbered carrier rules and carrier-v1 files."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

PROFILE = Path("docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md")
INVENTORY = Path("docs/profiles/carrier-versions.json")
VECTORS = Path("tests/vectors/carrier-v1")
VERSION_DECLARATION = re.compile(r"^\*\*Version:", re.MULTILINE)
VERSION = re.compile(r"^\*\*Version:\*\*\s+(\d+\.\d+(?:\.\d+)?)\s+—", re.MULTILINE)
RULE = re.compile(
    r"^\s*(?:#{1,6}\s+)?(?:-\s+)?(?:\*\*)?(?:Profile rule|Rule)\s+(\d+)\b[^\n]*",
    re.MULTILINE,
)
LABEL = re.compile(r"\[carrier-profile: ([^;\]\s]+); sources: ([a-z0-9., -]+)\]")


def unique_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON key {key!r}")
        result[key] = value
    return result


def read_object(path: Path) -> dict[str, Any]:
    value = json.loads(
        path.read_text(encoding="utf-8"), object_pairs_hook=unique_object
    )
    if not isinstance(value, dict):
        raise ValueError(f"{path.name}: expected a JSON object")
    return value


def current_version(root: Path) -> str:
    """The profile header is the only authority; never pin an expected version here."""
    profile = (root / PROFILE).read_text(encoding="utf-8")
    versions = VERSION.findall(profile)
    if len(VERSION_DECLARATION.findall(profile)) != 1 or len(versions) != 1:
        raise ValueError("profile must have exactly one well-formed Version header")
    return versions[0]


def local_file(root: Path, name: Any) -> bool:
    if not isinstance(name, str) or not name or Path(name).is_absolute():
        return False
    path = (root / name).resolve()
    return path.is_relative_to(root.resolve()) and path.is_file()


def validate_carrier_versions(root: Path) -> list[str]:
    """Return actionable errors; never write files or access the network."""
    problems: list[str] = []
    try:
        version = current_version(root)
        profile = (root / PROFILE).read_text(encoding="utf-8")
        inventory = read_object(root / INVENTORY)
        manifest = read_object(root / VECTORS / "manifest.json")
    except (OSError, UnicodeError, ValueError) as exc:
        return [f"carrier version evidence: {exc}"]
    if (
        inventory.get("schema_version") != 1
        or inventory.get("profile") != PROFILE.as_posix()
    ):
        problems.append(
            "carrier source inventory: invalid schema_version or profile path"
        )
    sources = inventory.get("sources")
    if not isinstance(sources, dict) or not sources:
        problems.append("carrier source inventory: sources must be a nonempty object")
        sources = {}
    for source_id, source in sources.items():
        prefix = f"source {source_id}"
        if not isinstance(source, dict):
            problems.append(f"{prefix}: expected an object")
            continue
        disposition = source.get("disposition")
        if disposition not in ("local-evidence", "primary-checked", "[UNVERIFIED]"):
            problems.append(f"{prefix}: missing/unknown evidence disposition")
        if source.get("kind") not in (
            "repository-policy",
            "implementation-release",
            "external-document",
        ):
            problems.append(f"{prefix}: missing/unknown source kind")
        if not isinstance(source.get("scope"), str) or not source["scope"].strip():
            problems.append(f"{prefix}: missing provenance scope")
        if (
            not isinstance(source.get("version"), str) or not source["version"].strip()
        ) and disposition != "[UNVERIFIED]":
            problems.append(
                f"{prefix}: missing version without [UNVERIFIED] disposition"
            )
        evidence = source.get("evidence")
        if not isinstance(evidence, list) or not evidence:
            problems.append(f"{prefix}: missing local evidence")
        else:
            for path in evidence:
                if not local_file(root, path):
                    problems.append(f"{prefix}: unresolved local evidence {path!r}")
        if (
            source.get("kind") == "external-document"
            or disposition == "primary-checked"
        ):
            url = source.get("primary_url")
            if disposition != "[UNVERIFIED]" and (
                not isinstance(url, str) or not url.startswith("https://")
            ):
                problems.append(f"{prefix}: missing primary source URL")
        if disposition == "[UNVERIFIED]":
            issue = source.get("spec_issue")
            match = (
                re.fullmatch(r"(spec/[^#]+)#23:(\d+)", issue)
                if isinstance(issue, str)
                else None
            )
            if not match or not local_file(root, match[1]):
                problems.append(
                    f"{prefix}: [UNVERIFIED] requires a resolvable spec §23 reference"
                )
            else:
                try:
                    spec = (root / match[1]).read_text(encoding="utf-8")
                except (OSError, UnicodeError) as exc:
                    problems.append(f"{prefix}: could not read uncertainty spec: {exc}")
                    continue
                section_match = re.search(
                    r"^## 23\.[^\n]*\n(.*?)(?=^## |\Z)",
                    spec,
                    re.MULTILINE | re.DOTALL,
                )
                if not section_match:
                    problems.append(f"{prefix}: missing spec §23 section")
                    continue
                item = re.search(
                    rf"^{match[2]}\. (.+)$", section_match[1], re.MULTILINE
                )
                if not item or "[UNVERIFIED]" not in item[1]:
                    problems.append(
                        f"{prefix}: missing [UNVERIFIED] spec §23 item {match[2]}"
                    )

    def label(item: str, entry: Any) -> None:
        if not isinstance(entry, dict):
            problems.append(f"{item}: missing label metadata")
            return
        if entry.get("carrier_profile_version") != version:
            labelled_version = entry.get("carrier_profile_version")
            problems.append(
                f"{item}: carrier_profile_version {labelled_version!r}, "
                f"expected current {version!r}"
            )
        refs = entry.get("sources")
        if (
            not isinstance(refs, list)
            or not refs
            or any(not isinstance(ref, str) or ref not in sources for ref in refs)
        ):
            problems.append(f"{item}: missing or unknown source reference {refs!r}")
        elif len(refs) != len(set(refs)):
            problems.append(f"{item}: duplicate source reference")

    rules = inventory.get("rules")
    if not isinstance(rules, dict):
        problems.append("rules: metadata must be an object")
        rules = {}
    discovered: set[str] = set()
    for match in RULE.finditer(profile):
        rule_id = match[1]
        item = f"rule {rule_id}"
        if rule_id in discovered:
            problems.append(f"{item}: duplicate numbered rule")
        discovered.add(rule_id)
        entry = rules.get(rule_id)
        label(item, entry)
        visible = LABEL.findall(match[0])
        if len(visible) != 1:
            problems.append(f"{item}: missing/malformed visible version/source label")
        else:
            visible_version, refs_text = visible[0]
            refs = refs_text.split(", ")
            if visible_version != version:
                problems.append(
                    f"{item}: visible version {visible_version!r}, "
                    f"expected current {version!r}"
                )
            if isinstance(entry, dict) and (
                visible_version != entry.get("carrier_profile_version")
                or refs != entry.get("sources")
            ):
                problems.append(
                    f"{item}: visible label disagrees with source inventory"
                )
        if isinstance(entry, dict) and (
            not isinstance(entry.get("derivation"), str)
            or not entry["derivation"].strip()
        ):
            problems.append(f"{item}: missing rule-specific derivation")
    if not discovered:
        problems.append("profile: no numbered rules discovered")
    for orphan in sorted(set(rules) - discovered):
        problems.append(f"rule {orphan}: orphan metadata")
    hashes = manifest.get("sha256")
    provenance = manifest.get("provenance")
    if not isinstance(hashes, dict):
        problems.append("fixtures: sha256 inventory must be an object")
        hashes = {}
    if not isinstance(provenance, dict):
        problems.append("fixtures: provenance must be an object")
        provenance = {}
    files = {
        path.relative_to(root / VECTORS).as_posix()
        for path in (root / VECTORS).rglob("*")
        if path.is_file() and path != root / VECTORS / "manifest.json"
    }
    if not files:
        problems.append("fixtures: no published files discovered")
    for name in sorted(files | set(hashes) | set(provenance)):
        if name not in files:
            problems.append(f"fixture {name}: indexed file missing or orphan metadata")
        if name not in hashes:
            problems.append(f"fixture {name}: missing sha256 inventory entry")
        digest = hashes.get(name)
        if name in hashes and (
            not isinstance(digest, str) or not re.fullmatch(r"[0-9a-f]{64}", digest)
        ):
            problems.append(f"fixture {name}: malformed sha256 digest")
        elif name in files and name in hashes:
            try:
                actual = hashlib.sha256(
                    (root / VECTORS / name).read_bytes()
                ).hexdigest()
            except OSError as exc:
                problems.append(f"fixture {name}: could not read payload: {exc}")
            else:
                if actual != digest:
                    problems.append(f"fixture {name}: SHA-256 mismatch")
        label(f"fixture {name}", provenance.get(name))
    return problems


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root", type=Path, default=Path(__file__).resolve().parents[1]
    )
    args = parser.parse_args()
    problems = validate_carrier_versions(args.root.resolve())
    for problem in problems:
        print("ERROR:", problem)
    if not problems:
        print(
            "Carrier version evidence OK: all discovered rules and carrier-v1 files "
            "use the current profile version"
        )
    return bool(problems)


if __name__ == "__main__":
    raise SystemExit(main())
