import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
BUILD_PATH = ROOT / "build" / "build.py"


def load_build():
    spec = importlib.util.spec_from_file_location("opencgl_build", BUILD_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class BuildOrchestratorTest(unittest.TestCase):

    def test_rejects_java_other_than_zulu_21(self):
        build = load_build()
        with self.assertRaisesRegex(RuntimeError, "Zulu JDK 21"):
            build.validate_java(
                "java.version = 23.0.1\njava.vendor = Oracle Corporation\n"
            )
        with self.assertRaisesRegex(RuntimeError, "Zulu JDK 21"):
            build.validate_java(
                "java.version = 21.0.8\njava.vendor = Oracle Corporation\n"
            )

    def test_accepts_zulu_21(self):
        build = load_build()
        details = build.validate_java(
            "java.version = 21.0.8\njava.vendor = Azul Systems, Inc.\n"
        )
        self.assertEqual("21.0.8", details["java.version"])

    def test_runs_plugin_reactor_collection_then_host(self):
        build = load_build()
        commands = []

        build.run_verify(
            host_root=Path("/host"),
            plugins_root=Path("/plugins"),
            runner=lambda command, cwd: commands.append((command, cwd)),
        )

        self.assertEqual("mvn", commands[0][0][0])
        self.assertEqual(["clean", "verify"], commands[0][0][1:])
        self.assertEqual(Path("/plugins"), commands[0][1])
        self.assertEqual("collect_plugins.py", Path(commands[1][0][1]).name)
        self.assertEqual(["clean", "test"], commands[2][0][1:])
        self.assertEqual(Path("/host"), commands[2][1])

    def test_verifies_and_collects_optional_plugin_reactor(self):
        build = load_build()
        commands = []

        build.run_verify(
            Path("/host"), Path("/plugins"),
            runner=lambda command, cwd: commands.append((command, cwd)),
            extra_plugin_roots=(Path("/private-plugins"),),
        )

        self.assertEqual(Path("/plugins"), commands[0][1])

        self.assertEqual("collect_plugins.py", Path(commands[3][0][1]).name)
        self.assertEqual(Path("/host"), commands[4][1])

    def test_stops_after_the_first_failed_command(self):
        build = load_build()
        calls = []

        def failing_runner(command, cwd):
            calls.append((command, cwd))
            raise RuntimeError("reactor failed")

        with self.assertRaisesRegex(RuntimeError, "reactor failed"):
            build.run_verify(Path("/host"), Path("/plugins"), failing_runner)

        self.assertEqual(1, len(calls))

    def test_package_runs_only_host_build_commands(self):
        build = load_build()
        commands = []

        build.run_package(
            Path("/host"), Path("/release"),
            runner=lambda command, cwd: commands.append((command, cwd)),
            package_builder=lambda *_: None,
        )

        self.assertEqual(["mvn", "clean", "test"], commands[0][0])
        self.assertEqual(["mvn", "package", "-DskipTests"], commands[1][0])
        self.assertEqual({Path("/host")}, {cwd for _, cwd in commands})

    def test_manifest_records_only_host_build_information(self):
        build = load_build()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            output = root / "build-manifest.json"

            manifest = build.write_build_manifest(
                output=output,
                host_revision="host-sha",
                platform_name="macos-arm64",
                java_version="21.0.8",
            )

            self.assertEqual("host-sha", manifest["hostRevision"])
            self.assertEqual("macos-arm64", manifest["platform"])
            self.assertEqual("21.0.8", manifest["javaVersion"])
            self.assertNotIn("pluginRevision", manifest)
            self.assertNotIn("validatedPluginCount", manifest)
            self.assertEqual(manifest, json.loads(output.read_text(encoding="utf-8")))


if __name__ == "__main__":
    unittest.main()
