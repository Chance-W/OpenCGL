#!/usr/bin/env python3
"""Fail unless a release directory contains every supported platform and valid hashes."""

import argparse
import hashlib
import sys
from pathlib import Path


PLATFORMS = ("windows-x64", "macos-x64", "macos-arm64", "linux-x64")


def verify_release(root, platforms=None):
    root = Path(root)
    platforms = tuple(platforms or PLATFORMS)
    files = [path for path in root.rglob("*") if path.is_file()]
    names = {path.name for path in files}
    missing = [platform for platform in platforms if not any(platform in name for name in names)]
    if missing:
        raise RuntimeError("Missing platform artifact(s): " + ", ".join(missing))
    sums = list(root.rglob("SHA256SUMS"))
    if not sums:
        raise RuntimeError("No SHA256SUMS files found")
    for checksum_file in sums:
        for line in checksum_file.read_text(encoding="utf-8").splitlines():
            expected, name = line.split(maxsplit=1)
            name = name.strip()
            direct = checksum_file.parent / name
            candidates = [direct] if direct.is_file() else []
            candidates.extend(path for path in root.rglob(name) if path != direct)
            if not candidates:
                raise RuntimeError(f"Missing artifact referenced by checksum: {direct}")
            matching = [
                path for path in candidates
                if hashlib.sha256(path.read_bytes()).hexdigest() == expected
            ]
            if len(matching) == 1:
                continue
            if len(matching) > 1:
                raise RuntimeError(f"Ambiguous matching artifact referenced by checksum: {name}")
            actual = hashlib.sha256(candidates[0].read_bytes()).hexdigest()
            raise RuntimeError(
                f"Checksum mismatch: {candidates[0]} (expected {expected}, actual {actual})"
            )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Verify release artifacts and SHA256SUMS files")
    parser.add_argument("root", help="release directory or a single platform output directory")
    parser.add_argument(
        "--platform",
        dest="platforms",
        action="append",
        choices=PLATFORMS,
        help="verify only this platform (repeat for multiple platforms)",
    )
    args = parser.parse_args()
    verify_release(args.root, args.platforms)
