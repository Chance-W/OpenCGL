import importlib.util
import hashlib
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]


def load_verifier():
    path = ROOT / "build/verify_release.py"
    spec = importlib.util.spec_from_file_location("verify_release", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class VerifyReleaseTest(unittest.TestCase):

    def test_accepts_complete_release_with_valid_checksums(self):
        verifier = load_verifier()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for platform in verifier.PLATFORMS:
                folder = root / platform
                folder.mkdir()
                artifact = folder / f"OpenCGL-Tool-2.2.2-{platform}.zip"
                artifact.write_bytes(platform.encode())
                digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
                (folder / "SHA256SUMS").write_text(f"{digest}  {artifact.name}\n")
            verifier.verify_release(root)

    def test_rejects_missing_platform(self):
        verifier = load_verifier()
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(RuntimeError, "Missing platform"):
                verifier.verify_release(temp)


if __name__ == "__main__":
    unittest.main()
