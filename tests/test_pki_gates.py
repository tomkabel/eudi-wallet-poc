"""Seeded failures for tests/check_pki_gates.py: every gate must turn red on the
defect it exists for, and stay green on a clean tree."""
import datetime as dt
import hashlib
import importlib.util
import json
import pathlib
import subprocess
import tempfile
import unittest

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("gates", HERE / "check_pki_gates.py")
gates = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gates)

NOW = dt.datetime(2030, 1, 1, tzinfo=dt.timezone.utc)
GITLEAKS = """[[allowlists]]
description = "Test-only key material; each file is listed in trust/MANIFEST.json"
paths = ['^keys/test\\.pem$']
"""


def pem_cert(days_left):
    key = ec.generate_private_key(ec.SECP256R1())
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "gate test")])
    cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
            .serial_number(1).not_valid_before(NOW - dt.timedelta(days=1))
            .not_valid_after(NOW + dt.timedelta(days=days_left)).sign(key, hashes.SHA256()))
    return cert.public_bytes(serialization.Encoding.PEM)


def pem_key():
    return ec.generate_private_key(ec.SECP256R1()).private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())


class Tree:
    """A throwaway git repo with a clean trust manifest."""

    def __init__(self, tmp):
        self.root = pathlib.Path(tmp)
        subprocess.run(["git", "init", "-q"], cwd=self.root, check=True)
        self.write(".gitleaks.toml", GITLEAKS.encode())
        self.write("keys/test.pem", pem_key())
        self.write("certs/root.pem", pem_cert(365))
        self.entries = []
        self.list("keys/test.pem", test_only=True)
        self.list("certs/root.pem", test_only=False)

    def write(self, rel, data):
        p = self.root / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(data)
        subprocess.run(["git", "add", rel], cwd=self.root, check=True)

    def list(self, rel, **fields):
        self.entries.append({"path": rel, "purpose": "t", "source": "t", "owner": "t", **fields})
        self.save()

    def save(self):
        (self.root / "trust").mkdir(exist_ok=True)
        (self.root / gates.MANIFEST).write_text(json.dumps({"artifacts": self.entries}))
        gates.update(self.root)
        subprocess.run(["git", "add", "-A"], cwd=self.root, check=True)

    def failures(self):
        return {k: v for k, v in gates.run(self.root, NOW).items() if v}


class PkiGatesTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.tree = Tree(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def assertOnly(self, gate):
        failed = self.tree.failures()
        self.assertEqual(list(failed), [gate], failed)

    def test_clean_tree_passes(self):
        self.assertEqual(self.tree.failures(), {})

    def test_trust_all_comes_back(self):
        self.tree.write("app/src/main/kotlin/V.kt", b"object V { fun setTrustAll(e: Boolean) {} }\n")
        self.assertOnly("trust-all")

    def test_mock_in_release_sources(self):
        self.tree.write("app/src/main/kotlin/FooMock.kt", b"class FooMock\n")
        self.assertOnly("mocks")

    def test_mock_status_uri_in_release_sources(self):
        self.tree.write("app/src/main/kotlin/S.kt", b'val u = "https://aarmam.github.io/statuslists/1"\n')
        self.assertOnly("mocks")

    def test_mock_in_local_mocks_is_fine(self):
        self.tree.write("app/src/local_mocks/kotlin/FooMock.kt", b"class FooMock\n")
        self.assertEqual(self.tree.failures(), {})

    def test_freshness_flag_in_default_build(self):
        src = b'package main\nvar _ = flag.Bool("ignore-trust-freshness", false, "")\n'
        self.tree.write("verifier/go/main.go", src)
        self.assertOnly("freshness")
        self.tree.write("verifier/go/main.go", b"//go:build devflags\n\n" + src)
        self.assertEqual(self.tree.failures(), {})

    def test_unlisted_trust_material(self):
        self.tree.write("certs/extra.pem", pem_cert(365))
        self.assertOnly("manifest")

    def test_changed_file_without_manifest_update(self):
        self.tree.write("certs/root.pem", pem_cert(365))
        self.assertOnly("manifest")

    def test_expiry_inside_30_days(self):
        self.tree.write("certs/root.pem", pem_cert(10))
        self.tree.save()
        self.assertOnly("manifest")

    def test_expiry_gate_off_for_pinned_fixture(self):
        self.tree.write("certs/root.pem", pem_cert(10))
        self.tree.entries[1]["expiry_gate"] = False
        self.tree.save()
        self.assertEqual(self.tree.failures(), {})

    def test_private_key_not_test_only(self):
        self.tree.entries[0]["test_only"] = False
        self.tree.save()
        failed = self.tree.failures()
        self.assertIn("manifest", failed)
        self.assertIn("key-material", failed)

    def test_tracked_demo_data(self):
        self.tree.write("demo-data/device_key.pem.txt", b"x")
        self.assertOnly("key-material")

    def test_history_only_entry_matching_a_tracked_file(self):
        self.tree.write(".gitleaks.toml", (GITLEAKS + '''
[[allowlists]]
description = "History-only key material (removed from HEAD)"
paths = ['^keys/test\\.pem$']
''').encode())
        self.tree.save()
        self.assertIn("key-material", self.tree.failures())

    def test_other_allowlist_naming_a_private_key(self):
        self.tree.write(".gitleaks.toml", b'''[[allowlists]]
description = "Example tokens"
paths = ['^keys/test\\.pem$']
''')
        self.tree.save()
        self.assertIn("key-material", self.tree.failures())

    def test_digest_is_sha256_of_bytes(self):
        e = json.loads((self.tree.root / gates.MANIFEST).read_text())["artifacts"][1]
        self.assertEqual(e["sha256"], hashlib.sha256((self.tree.root / "certs/root.pem").read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()
