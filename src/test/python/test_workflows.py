import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
BUILD = ROOT / ".github/workflows/build.yml"
RELEASE = ROOT / ".github/workflows/release.yml"
SETUP = ROOT / ".github/actions/setup-opencgl/action.yml"


class WorkflowContractTest(unittest.TestCase):

    def test_runtime_keeps_jgss_for_dynamic_plugins(self):
        build = (ROOT / "build/build.py").read_text(encoding="utf-8")
        self.assertIn('"java.security.jgss"', build)
        self.assertIn('"jdk.security.jgss"', build)

    def test_build_matrix_and_toolchain_contract(self):
        workflow = BUILD.read_text(encoding="utf-8")
        setup = SETUP.read_text(encoding="utf-8")
        for value in ("windows-2025", "macos-15-intel", "macos-15", "ubuntu-24.04"):
            self.assertIn(value, workflow)
        for value in ("windows-x64", "macos-x64", "macos-arm64", "linux-x64"):
            self.assertIn(value, workflow)
        self.assertIn("distribution: zulu", setup)
        self.assertIn("java-version: '21'", setup)
        self.assertIn("java --version", setup)
        self.assertIn("mvn --version", setup)
        self.assertIn("platform.machine()", setup)

    def test_build_preserves_diagnostics_and_cancels_superseded_runs(self):
        workflow = BUILD.read_text(encoding="utf-8")
        self.assertIn("concurrency:", workflow)
        self.assertIn("cancel-in-progress: true", workflow)
        self.assertIn("if: failure()", workflow)
        self.assertIn("surefire-reports", workflow)
        self.assertIn("build/build.py package", workflow)
        self.assertNotIn("Check out public plugins", workflow)
        self.assertNotIn("--plugins-dir", workflow)
        self.assertNotIn("plugin_ref", workflow)

    def test_release_is_all_or_nothing_and_least_privilege(self):
        workflow = RELEASE.read_text(encoding="utf-8")
        self.assertIn("needs: package", workflow)
        self.assertIn("build/verify_release.py", workflow)
        self.assertIn("permissions:\n  contents: read", workflow)
        self.assertIn("    permissions:\n      contents: write", workflow)
        self.assertIn("gh release create", workflow)


if __name__ == "__main__":
    unittest.main()
