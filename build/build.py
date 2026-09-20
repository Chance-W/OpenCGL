#!/usr/bin/env python3
"""Build the OpenCGL host and optionally verify plugin repositories."""

import argparse
import hashlib
import json
import os
import platform
import shutil
import subprocess
import sys
import tarfile
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


HOST_ROOT = Path(__file__).resolve().parents[1]


def _parse_properties(text):
    properties = {}
    for raw_line in text.splitlines():
        if "=" not in raw_line:
            continue
        key, value = raw_line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def validate_java(settings_text):
    properties = _parse_properties(settings_text)
    version = properties.get("java.version", "")
    vendor = properties.get("java.vendor", "")
    if version.split(".", 1)[0] != "21" or "azul" not in vendor.lower():
        raise RuntimeError(
            f"OpenCGL requires Azul Zulu JDK 21; found java.version={version!r}, "
            f"java.vendor={vendor!r}. Set JAVA_HOME to a Zulu 21 JDK."
        )
    return properties


def current_java():
    result = subprocess.run(
        ["java", "-XshowSettings:properties", "-version"],
        check=True,
        capture_output=True,
        text=True,
    )
    return validate_java(result.stdout + result.stderr)


def run_command(command, cwd):
    # Windows 将 Maven 安装为 mvn.cmd，CreateProcess 不一定会按 PATHEXT
    # 解析裸的 mvn；显式使用 mvn.cmd 保证跨平台调用一致。
    if os.name == "nt" and command and str(command[0]).lower() == "mvn":
        command = ["mvn.cmd", *command[1:]]
    display = " ".join(str(part) for part in command)
    print(f"[{cwd}] $ {display}", flush=True)
    subprocess.run([str(part) for part in command], cwd=cwd, check=True)


def _verify_plugin_reactor(plugins_root, runner):
    plugins_root = Path(plugins_root)
    runner(["mvn", "clean", "verify"], plugins_root)
    runner(
        [
            sys.executable, str(plugins_root / "build" / "collect_plugins.py"),
            "--root", str(plugins_root),
            "--output", str(plugins_root / "target" / "plugin-dist"),
            "--manifest", str(plugins_root / "target" / "plugin-manifest.json"),
        ],
        plugins_root,
    )


def run_verify(host_root, plugins_root, runner=run_command, extra_plugin_roots=()):
    host_root = Path(host_root)
    plugins_root = Path(plugins_root)
    _verify_plugin_reactor(plugins_root, runner)
    for extra_root in extra_plugin_roots:
        _verify_plugin_reactor(extra_root, runner)
    runner(["mvn", "clean", "test"], host_root)


def git_revision(root):
    result = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def write_build_manifest(
    output,
    host_revision,
    platform_name,
    java_version,
):
    manifest = {
        "hostRevision": host_revision,
        "platform": platform_name,
        "javaVersion": java_version,
    }
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def platform_label():
    system = platform.system().lower()
    machine = platform.machine().lower()
    architecture = "arm64" if machine in {"arm64", "aarch64"} else "x64"
    names = {"darwin": "macos", "windows": "windows", "linux": "linux"}
    if system not in names:
        raise RuntimeError(f"Unsupported packaging platform: {platform.system()} {platform.machine()}")
    return f"{names[system]}-{architecture}"


def sha256(path):
    digest = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def release_basename(version, label):
    """Return the stable base name shared by all artifacts for one platform."""
    return f"OpenCGL-Tool-{version}-{label}"


def read_software_version(pom_path):
    root = ET.parse(pom_path).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    value = root.findtext("m:properties/m:software-version", namespaces=namespace)
    if not value:
        raise ValueError(f"software-version is missing from {pom_path}")
    return value.strip()


def artifact_types(label):
    system = label.split("-", 1)[0]
    try:
        return {
            "macos": ("dmg", "tar.gz"),
            "windows": ("exe", "zip"),
            "linux": ("deb", "tar.gz"),
        }[system]
    except KeyError as error:
        raise ValueError(f"Unsupported platform label: {label}") from error


JAVA_OPTIONS = (
    "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "-Xms512m",
    "-Xmx2048m",
    "-Dfile.encoding=UTF-8",
    "-Dapp.env=production",
)

# JCSMP (including its optional authentication support) references the
# standard GSS/JGSS API even when a connection uses BASIC authentication.
# The host runtime is a jlink image, so jdeps cannot discover classes that are
# loaded reflectively by an externally downloaded plugin.  Keep these modules
# explicit to prevent NoClassDefFoundError: org/ietf/jgss/GSSException in
# packaged installations.
REQUIRED_RUNTIME_MODULES = {
    "java.security.jgss",
    "jdk.security.jgss",
}


def jpackage_command(java_home, input_dir, runtime, output, version, label, package_type, icon):
    command = [
        Path(java_home) / "bin" / "jpackage",
        "--type", package_type,
        "--input", input_dir,
        "--dest", output,
        "--name", "OpenCGL-Tool",
        "--app-version", version,
        "--vendor", "Chance.W",
        "--main-jar", "OpenCGL.jar",
        "--main-class", "com.opencgl.OpenCglStartApplication",
        "--runtime-image", runtime,
        "--icon", icon,
    ]
    for option in JAVA_OPTIONS:
        command.extend(("--java-options", option))
    system = label.split("-", 1)[0]
    if system == "windows" and package_type != "app-image":
        command.extend(("--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut"))
        if package_type == "exe":
            command.extend(("--win-upgrade-uuid", "079cc66a-4f5e-45df-8198-3959f3936d6f"))
    elif system == "linux" and package_type != "app-image":
        command.extend(("--linux-shortcut", "--linux-app-category", "Utility"))
    return command


def _reset_directory(path):
    path = Path(path).resolve()
    if path == path.parent or len(path.parts) < 3:
        raise ValueError(f"Refusing to reset unsafe staging directory: {path}")
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)
    return path


def stage_application(host_root, staging):
    """Create a plugin-free jpackage input tree for the host application."""
    host_root = Path(host_root)
    host_jar = host_root / "target" / "OpenCGL.jar"
    host_lib = host_root / "target" / "lib"
    required = (host_jar, host_lib)
    missing = [str(path) for path in required if not path.exists()]
    if missing:
        raise FileNotFoundError("Missing package input(s): " + ", ".join(missing))

    staging = _reset_directory(staging)
    shutil.copy2(host_jar, staging / "OpenCGL.jar")
    shutil.copytree(host_lib, staging / "lib")
    # The application downloads plugins into its user plugin directory at runtime.
    # Keep an empty directory only as a clear package-layout contract/fallback path.
    (staging / "ext-plugin").mkdir()
    return staging


def build_runtime(java_home, staging, runtime):
    """Analyze the staged application and create a minimized Java runtime."""
    java_home = Path(java_home)
    jars = [staging / "OpenCGL.jar", *sorted((staging / "lib").glob("*.jar"))]
    classpath = os.pathsep.join(str(path) for path in jars[1:])
    result = subprocess.run(
        [
            str(java_home / "bin" / "jdeps"), "-q", "--multi-release", "21",
            "--ignore-missing-deps", "--print-module-deps", "--class-path", classpath,
            str(jars[0]),
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    modules = {item.strip() for item in result.stdout.strip().split(",") if item.strip()}
    modules.update({"java.desktop", "jdk.charsets", "jdk.compiler", "jdk.crypto.ec", "jdk.unsupported"})
    modules.update(REQUIRED_RUNTIME_MODULES)
    if runtime.exists():
        shutil.rmtree(runtime)
    run_command(
        [
            java_home / "bin" / "jlink", "--module-path", java_home / "jmods",
            "--add-modules", ",".join(sorted(modules)), "--output", runtime,
            "--strip-debug", "--no-man-pages", "--no-header-files", "--compress=zip-6",
        ],
        staging.parent,
    )


def _portable_archive(app_image, destination, label):
    if label.startswith("windows-"):
        with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED) as archive:
            for path in sorted(app_image.rglob("*")):
                if path.is_file():
                    archive.write(path, Path(app_image.name) / path.relative_to(app_image))
    else:
        with tarfile.open(destination, "w:gz") as archive:
            archive.add(app_image, arcname=app_image.name)


def _find_installer(directory, extension):
    candidates = sorted(Path(directory).glob(f"*.{extension}"))
    if len(candidates) != 1:
        raise RuntimeError(f"Expected one .{extension} installer in {directory}, found {len(candidates)}")
    return candidates[0]


def run_package(host_root, output, runner=run_command, package_builder=None):
    host_root = Path(host_root)
    output = Path(output).resolve()
    runner(["mvn", "clean", "test"], host_root)
    runner(["mvn", "package", "-DskipTests"], host_root)
    if package_builder is not None:
        package_builder(host_root, output)
        return
    label = platform_label()
    version = read_software_version(host_root / "pom.xml")
    basename = release_basename(version, label)
    installer_type, portable_type = artifact_types(label)
    output.mkdir(parents=True, exist_ok=True)
    work = host_root / "target" / "native-package"
    staging = stage_application(host_root, work / "input")
    java_home = Path(os.environ.get("JAVA_HOME", sys.prefix))
    runtime = work / "runtime"
    build_runtime(java_home, staging, runtime)
    icon_name = {"macos": "mac-icon.icns", "windows": "icon.ico", "linux": "logo.png"}[
        label.split("-", 1)[0]
    ]
    icon = host_root / "src/main/resources/com/opencgl/icon" / icon_name
    native_output = _reset_directory(work / "output")
    runner(
        jpackage_command(java_home, staging, runtime, native_output, version, label, installer_type, icon),
        host_root,
    )
    installer = _find_installer(native_output, installer_type)
    shutil.copy2(installer, output / f"{basename}.{installer_type}")
    image_output = _reset_directory(work / "image")
    runner(
        jpackage_command(java_home, staging, runtime, image_output, version, label, "app-image", icon),
        host_root,
    )
    app_image = image_output / ("OpenCGL-Tool.app" if label.startswith("macos-") else "OpenCGL-Tool")
    portable = output / f"{basename}.{portable_type}"
    _portable_archive(app_image, portable, label)
    java = current_java()
    write_build_manifest(
        output / "build-manifest.json",
        git_revision(host_root),
        label,
        java["java.version"],
    )
    artifacts = [path for path in output.iterdir() if path.is_file() and path.name != "SHA256SUMS"]
    checksum_lines = [f"{sha256(path)}  {path.name}" for path in sorted(artifacts)]
    (output / "SHA256SUMS").write_text("\n".join(checksum_lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("verify", "package"))
    parser.add_argument("--plugins-dir", type=Path)
    parser.add_argument(
        "--extra-plugins-dir", type=Path, action="append", default=[],
        help="Additional local/private plugin reactor; may be specified more than once",
    )
    parser.add_argument("--output", type=Path, default=HOST_ROOT / "target" / "release")
    args = parser.parse_args()
    java = current_java()
    print(f"Using Azul Zulu JDK {java['java.version']} from {os.environ.get('JAVA_HOME', 'PATH')}")
    if args.command == "verify":
        if args.plugins_dir is None:
            parser.error("verify requires --plugins-dir")
        run_verify(HOST_ROOT, args.plugins_dir, extra_plugin_roots=args.extra_plugins_dir)
    else:
        if args.extra_plugins_dir:
            parser.error("--extra-plugins-dir is only valid with verify")
        run_package(HOST_ROOT, args.output)


if __name__ == "__main__":
    main()
