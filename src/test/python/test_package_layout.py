import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
BUILD_PATH = ROOT / "build" / "build.py"

def load_build():
    spec = importlib.util.spec_from_file_location("opencgl_build", BUILD_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def write_jar(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, "w") as jar:
        jar.writestr("entry.txt", "content")

class PackageLayoutTest(unittest.TestCase):
    def test_stages_only_host_dependencies_and_empty_plugin_directory(self):
        build = load_build()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            host = root / "host"
            write_jar(host / "target/OpenCGL.jar")
            write_jar(host / "target/lib/dependency.jar")

            staging = build.stage_application(host, root / "staging")

            self.assertTrue((staging / "OpenCGL.jar").is_file())
            self.assertTrue((staging / "lib/dependency.jar").is_file())
            self.assertTrue((staging / "ext-plugin").is_dir())
            self.assertEqual([], list((staging / "ext-plugin").iterdir()))
            self.assertFalse((staging / "plugin-manifest.json").exists())

    def test_staging_does_not_require_a_plugin_distribution(self):
        build = load_build()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            host = root / "host"
            write_jar(host / "target/OpenCGL.jar")
            (host / "target/lib").mkdir(parents=True)

            staging = build.stage_application(host, root / "staging")

            self.assertTrue((staging / "ext-plugin").is_dir())

    def test_release_name_contains_version_and_architecture(self):
        build = load_build()
        self.assertEqual(
            "OpenCGL-Tool-2.2.3-macos-arm64",
            build.release_basename("2.2.3", "macos-arm64"),
        )

if __name__ == "__main__":
    unittest.main()
