import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]


def load_build():
    spec = importlib.util.spec_from_file_location("opencgl_build", ROOT / "build/build.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class NativePackagingTest(unittest.TestCase):

    def test_reads_public_software_version(self):
        build = load_build()
        self.assertEqual("2.2.2", build.read_software_version(ROOT / "pom.xml"))

    def test_jpackage_command_contains_common_and_platform_options(self):
        build = load_build()
        command = build.jpackage_command(
            Path("/jdk"), Path("/input"), Path("/runtime"), Path("/out"),
            "2.2.2", "windows-x64", "exe", Path("/icon.ico")
        )
        self.assertEqual(Path("/jdk/bin/jpackage"), command[0])
        self.assertIn("--runtime-image", command)
        self.assertIn("--win-shortcut", command)
        self.assertIn("-Dfile.encoding=UTF-8", command)

    def test_supported_platform_artifact_types(self):
        build = load_build()
        self.assertEqual(("dmg", "tar.gz"), build.artifact_types("macos-arm64"))
        self.assertEqual(("exe", "zip"), build.artifact_types("windows-x64"))
        self.assertEqual(("deb", "tar.gz"), build.artifact_types("linux-x64"))


if __name__ == "__main__":
    unittest.main()
