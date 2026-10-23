"""Mutation controls for offline traceability, not AV protocol conformance."""

import copy
import json
import shutil
import tempfile
import unittest
from pathlib import Path

from check_av_registry import PROFILE, ROOT, RegistryError, validate


class AVRegistryMutations(unittest.TestCase):
    def setUp(self):
        self.registry = json.loads((ROOT / PROFILE / "clause-registry.json").read_text())
        self.source = json.loads((ROOT / PROFILE / "source.json").read_text())

    def reject(self, pattern, *, root=ROOT):
        with self.assertRaisesRegex(RegistryError, pattern):
            validate(root, self.registry, self.source)

    def mapped(self):
        return next(c for c in self.registry["clauses"] if "test" in c)

    def excluded(self):
        return next(c for c in self.registry["clauses"] if "out_of_scope_for_v1" in c)

    def checkout(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        paths = {self.source["excerpt_path"], self.source["license_path"],
                 str(PROFILE / "source.json"), str(PROFILE / "clause-registry.json")}
        paths.update(t["path"] for t in self.registry["tests"])
        for path in paths:
            target = root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / path, target)
        return root

    def test_valid_checkout(self):
        counts = validate()
        self.assertEqual(counts, {"source_blocks": 86, "clauses": 94, "mapped": 6,
                                  "excluded": 88, "tests": 6})
        self.assertEqual(counts["clauses"], counts["mapped"] + counts["excluded"])

    def test_duplicate_provenance_keys_in_json_files(self):
        for filename, key in (("source.json", "revision"), ("clause-registry.json", "source_revision")):
            with self.subTest(filename=filename):
                root = self.checkout()
                path = root / PROFILE / filename
                text = path.read_text()
                text = text.replace(f'"{key}":', f'"{key}": "conflicting revision", "{key}":', 1)
                path.write_text(text)
                with self.assertRaisesRegex(RegistryError, f"{filename}: duplicate JSON object key '{key}'"):
                    validate(root)

    def test_duplicate_nested_disposition_key_in_json_file(self):
        root = self.checkout()
        path = root / PROFILE / "clause-registry.json"
        key = "out_of_scope_for_v1"
        path.write_text(path.read_text().replace(f'"{key}":', f'"{key}": "conflicting disposition", "{key}":', 1))
        with self.assertRaisesRegex(RegistryError, f"clause-registry.json: duplicate JSON object key '{key}'"):
            validate(root)

    def test_missing_source_block_and_its_clauses_still_fails(self):
        block = self.registry["source_blocks"].pop(15)
        self.registry["clauses"] = [c for c in self.registry["clauses"] if c["block_id"] != block["id"]]
        self.reject("expected .* blocks from retained text")

    def test_normative_text_without_uppercase_keywords_is_required(self):
        block_id = "A.9-B001"  # inherited security requirements
        self.registry["clauses"] = [c for c in self.registry["clauses"] if c["block_id"] != block_id]
        self.reject("A.9-B001: missing clause coverage")

    def test_missing_clause_with_retained_block(self):
        self.registry["clauses"].pop()
        self.reject("unaccounted or inconsistent source text")

    def test_missing_conditional_table_row(self):
        self.registry["clauses"] = [c for c in self.registry["clauses"] if c["block_id"] != "A.4-B006"]
        self.reject("A.4-B006: missing clause coverage")

    def test_duplicate_clause_id(self):
        self.registry["clauses"].insert(1, copy.deepcopy(self.registry["clauses"][0]))
        self.reject("duplicate clause id")

    def test_source_blocks_out_of_order(self):
        rows = self.registry["source_blocks"]
        rows[2], rows[3] = rows[3], rows[2]
        self.reject("source block A.1-B002: missing, reordered or inconsistent")

    def test_clauses_out_of_order(self):
        rows = self.registry["clauses"]
        rows[2], rows[3] = rows[3], rows[2]
        self.reject("source order changed")

    def test_source_location_or_anchor_drift(self):
        for key, value in (("anchor", "wrong-anchor"), ("start_line", 999)):
            with self.subTest(key=key):
                original = self.registry["source_blocks"][0][key]
                self.registry["source_blocks"][0][key] = value
                self.reject("inconsistent with retained text")
                self.registry["source_blocks"][0][key] = original

    def test_partial_text_omission_and_overlap(self):
        clause = self.excluded()
        for excerpt in (clause["excerpt"][:-1], clause["excerpt"] * 2):
            with self.subTest(excerpt=excerpt):
                clause["excerpt"] = excerpt
                self.reject("unaccounted or inconsistent source text")

    def test_changed_pin_metadata(self):
        for field in ("revision", "commit_date", "source_sha256", "excerpt_sha256", "license_sha256", "attribution"):
            with self.subTest(field=field):
                original = self.source[field]
                self.source[field] = "0" * len(original)
                self.reject(f"pinned {field} mismatch")
                self.source[field] = original

    def test_changed_retained_source_bytes(self):
        root = self.checkout()
        path = root / self.source["excerpt_path"]
        path.write_bytes(path.read_bytes().replace(b"SHALL", b"SHOULD", 1))
        self.reject("source excerpt: SHA-256 pin mismatch", root=root)

    def test_changed_license_bytes(self):
        root = self.checkout()
        (root / self.source["license_path"]).write_text("different license")
        self.reject("upstream license: SHA-256 pin mismatch", root=root)

    def test_changed_registry_pin(self):
        self.registry["source_revision"] = "f" * 40
        self.reject("registry: source revision mismatch")

    def test_missing_disposition(self):
        del self.excluded()["out_of_scope_for_v1"]
        self.reject("exactly one test mapping or out of scope for v1 reason required")

    def test_both_dispositions(self):
        self.mapped()["out_of_scope_for_v1"] = "out of scope for v1: contradiction"
        self.reject("exactly one test mapping or out of scope for v1 reason required")

    def test_empty_or_implicit_exclusion(self):
        for reason in ("", "   ", "out of scope for v1: ", "deferred"):
            with self.subTest(reason=reason):
                self.excluded()["out_of_scope_for_v1"] = reason
                self.reject("out of scope for v1")

    def test_dangling_mapping(self):
        self.mapped()["test"]["id"] = "missing-test"
        self.reject("dangling test reference missing-test")

    def test_mapping_requires_scope_and_limits(self):
        self.mapped()["test"]["limits"] = " "
        self.reject("mapping limits")

    def test_missing_test_file(self):
        root = self.checkout()
        test = self.registry["tests"][0]
        (root / test["path"]).unlink()
        self.reject("missing file .*isodcapi_test.go", root=root)

    def test_missing_symbol_even_when_comment_retains_name(self):
        root = self.checkout()
        test = self.registry["tests"][0]
        path = root / test["path"]
        path.write_text(path.read_text().replace("func " + test["symbol"], "func RenamedTest", 1))
        self.reject("missing real Go test symbol", root=root)

    def test_explicit_go_build_constraints_are_rejected(self):
        for directive in ("//go:build ignore", "// +build ignore"):
            with self.subTest(directive=directive):
                root = self.checkout()
                test = self.registry["tests"][0]
                path = root / test["path"]
                path.write_text(directive + "\n\n" + path.read_text())
                self.reject("explicit Go build constraints are unsupported", root=root)

    def test_go_ignored_and_platform_filenames(self):
        cases = (("_ignored_test.go", "Go ignores filenames"),
                 (".ignored_test.go", "Go ignores filenames"),
                 ("mapped_windows_test.go", "implicit Go platform"),
                 ("mapped_windows.extra_test.go", "implicit Go platform"),
                 ("mapped_amd64_test.go", "implicit Go platform"),
                 ("mapped_linux_arm64_test.go", "implicit Go platform"),
                 ("portable_map_test.go", None),
                 ("linux_test.go", None))
        for filename, error in cases:
            with self.subTest(filename=filename):
                root = self.checkout()
                registry = copy.deepcopy(self.registry)
                test = registry["tests"][0]
                path = Path(test["path"])
                (root / path).rename(root / path.with_name(filename))
                renamed_ids = {}
                for entry in registry["tests"]:
                    if entry["path"] == str(path):
                        old_id = entry["id"]
                        entry["path"] = str(path.with_name(filename))
                        entry["id"] = entry["path"] + "::" + entry["symbol"]
                        renamed_ids[old_id] = entry["id"]
                for clause in registry["clauses"]:
                    old_id = clause.get("test", {}).get("id")
                    if old_id in renamed_ids:
                        clause["test"]["id"] = renamed_ids[old_id]
                if error:
                    with self.assertRaisesRegex(RegistryError, error):
                        validate(root, registry, self.source)
                else:
                    self.assertEqual(validate(root, registry, self.source), validate())

    def test_catalogue_command_must_select_the_mapped_test(self):
        test = self.registry["tests"][0]
        for command in (test["command"].replace(test["symbol"], "TestDoesNotExist"),
                        test["command"].replace("./oid4vp", "./internal/jose")):
            with self.subTest(command=command):
                test["command"] = command
                self.reject("command must equal")

    def test_valid_multiline_declaration_with_trailing_comma(self):
        root = self.checkout()
        test = self.registry["tests"][0]
        path = root / test["path"]
        original = f"func {test['symbol']}(t *testing.T)"
        replacement = f"func {test['symbol']}(\n t *testing.T,\n)"
        code = path.read_text()
        self.assertIn(original, code)
        path.write_text(code.replace(original, replacement, 1))
        self.assertEqual(validate(root, self.registry, self.source), validate())

    def test_valid_commented_declaration_preserves_token_separation(self):
        root = self.checkout()
        test = self.registry["tests"][0]
        path = root / test["path"]
        path.write_text(path.read_text().replace("func " + test["symbol"],
                                               "func/* declaration comment */" + test["symbol"], 1))
        self.assertEqual(validate(root, self.registry, self.source), validate())

    def test_symbol_mentions_in_literals_or_comments_are_not_declarations(self):
        root = self.checkout()
        test = self.registry["tests"][0]
        path = root / test["path"]
        fake = f"func {test['symbol']}(t *testing.T) {{"
        path.write_text(f'package oid4vp\n/*\n{fake}\n*/\nvar fake = `\n{fake}\n`\n')
        self.reject("missing real Go test symbol", root=root)

    def test_orphan_test_catalogue_entry(self):
        mapping = self.mapped()
        del mapping["test"]
        mapping["out_of_scope_for_v1"] = "out of scope for v1: test was removed"
        self.reject("orphan references")

    def test_bad_normative_strength(self):
        self.mapped()["normative_strength"] = ["PASS"]
        self.reject("invalid normative_strength")

    def test_dropped_uppercase_normative_strength(self):
        clause = next(c for c in self.registry["clauses"] if "SHALL NOT" in c["excerpt"])
        clause["normative_strength"] = ["informative"]
        self.reject("normative_strength omits source keywords")

    def test_schema_and_evidence_status(self):
        self.registry["evidence_status"] = "PASS"
        self.reject("cannot claim executed conformance")

    def test_unknown_schema_field(self):
        self.registry["passed"] = True
        self.reject("unknown fields")

    def test_schema_version_requires_integer(self):
        self.registry["schema_version"] = True
        self.reject("registry: unsupported schema_version")

    def test_no_path_escape(self):
        test = self.registry["tests"][0]
        old_id = test["id"]
        test["path"] = "../outside_test.go"
        test["id"] = test["path"] + "::" + test["symbol"]
        for clause in self.registry["clauses"]:
            if clause.get("test", {}).get("id") == old_id:
                clause["test"]["id"] = test["id"]
        self.reject("expected repository-relative path")


if __name__ == "__main__":
    unittest.main()
