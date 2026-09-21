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
                artifact = folder / f"OpenCGL-Tool-2.2.3-{platform}.zip"
                artifact.write_bytes(platform.encode())
                digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
                (folder / "SHA256SUMS").write_text(f"{digest}  {artifact.name}\n")
            verifier.verify_release(root)

    def test_rejects_missing_platform(self):
        verifier = load_verifier()
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(RuntimeError, "Missing platform"):
                verifier.verify_release(temp)

    def test_rejects_checksum_entry_when_artifact_is_missing(self):
        verifier = load_verifier()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for platform in verifier.PLATFORMS:
                folder = root / platform
                folder.mkdir()
                artifact = folder / f"OpenCGL-Tool-2.2.3-{platform}.zip"
                artifact.write_bytes(platform.encode())
                digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
                name = artifact.name if platform != "macos-arm64" else "OpenCGL-Tool-2.2.3-macos-arm64.dmg"
                (folder / "SHA256SUMS").write_text(f"{digest}  {name}\n")
            with self.assertRaisesRegex(RuntimeError, "Missing artifact referenced by checksum"):
                verifier.verify_release(root)

    def test_accepts_artifact_nested_once_by_upload_download_layout(self):
        verifier = load_verifier()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for platform in verifier.PLATFORMS:
                folder = root / platform
                nested = folder / f"OpenCGL-Tool-{platform}"
                nested.mkdir(parents=True)
                artifact = nested / f"OpenCGL-Tool-2.2.3-{platform}.zip"
                artifact.write_bytes(platform.encode())
                digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
                (folder / "SHA256SUMS").write_text(f"{digest}  {artifact.name}\n")
            verifier.verify_release(root)

    def test_uses_matching_hash_when_same_name_exists_in_multiple_download_dirs(self):
        verifier = load_verifier()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for platform in verifier.PLATFORMS:
                folder = root / platform
                folder.mkdir()
                artifact = folder / f"OpenCGL-Tool-2.2.3-{platform}.zip"
                artifact.write_bytes(platform.encode())
                digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
                if platform == "macos-arm64":
                    stale = root / "old-artifact" / artifact.name
                    stale.parent.mkdir()
                    stale.write_bytes(b"stale")
                (folder / "SHA256SUMS").write_text(f"{digest}  {artifact.name}\n")
            verifier.verify_release(root)

    def test_can_verify_one_platform_output_locally(self):
        verifier = load_verifier()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            artifact = root / "OpenCGL-Tool-2.2.3-macos-arm64.dmg"
            artifact.write_bytes(b"mac artifact")
            digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
            (root / "SHA256SUMS").write_text(f"{digest}  {artifact.name}\n")
            verifier.verify_release(root, ["macos-arm64"])


if __name__ == "__main__":
    unittest.main()
