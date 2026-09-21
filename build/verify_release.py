#!/usr/bin/env python3
"""Fail unless a release directory contains every supported platform and valid hashes."""

import hashlib
import sys
from pathlib import Path


PLATFORMS = ("windows-x64", "macos-x64", "macos-arm64", "linux-x64")


def verify_release(root):
    root = Path(root)
    files = [path for path in root.rglob("*") if path.is_file()]
    names = {path.name for path in files}
    missing = [platform for platform in PLATFORMS if not any(platform in name for name in names)]
    if missing:
        raise RuntimeError("Missing platform artifact(s): " + ", ".join(missing))
    sums = list(root.rglob("SHA256SUMS"))
    if not sums:
        raise RuntimeError("No SHA256SUMS files found")
    for checksum_file in sums:
        for line in checksum_file.read_text(encoding="utf-8").splitlines():
            expected, name = line.split(maxsplit=1)
            artifact = checksum_file.parent / name.strip()
            if not artifact.is_file():
                matches = list(root.rglob(name.strip()))
                if len(matches) == 1:
                    artifact = matches[0]
                elif not matches:
                    raise RuntimeError(f"Missing artifact referenced by checksum: {artifact}")
                else:
                    raise RuntimeError(f"Ambiguous artifact referenced by checksum: {name.strip()}")
            actual = hashlib.sha256(artifact.read_bytes()).hexdigest()
            if actual != expected:
                raise RuntimeError(f"Checksum mismatch: {artifact}")


if __name__ == "__main__":
    verify_release(sys.argv[1])
