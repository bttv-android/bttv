"""Copy tracked build inputs without temporary notification diagnostics.

The destination must be new. Source files, caches and archives are never changed.
Run from the primary checkout with a destination outside its build inputs.
"""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = (
    "patches/notifications_diag/",
    "patches/notifications_reason/",
    "mod/app/src/main/java/bttv/notifications/",
)


def production_paths(root):
    paths = subprocess.check_output(
        ["git", "ls-files", "-z"], cwd=root
    ).decode().split("\0")
    deleted = set(subprocess.check_output(
        ["git", "ls-files", "--deleted", "-z"], cwd=root
    ).decode().split("\0"))
    return sorted(p for p in paths if p and p not in deleted and not p.startswith(EXCLUDED))


def stage(root, destination):
    root, destination = root.resolve(), destination.resolve()
    if destination.exists():
        raise ValueError("Refusing to overwrite an existing staged input")
    paths = production_paths(root)
    if "patches/AndroidManifest.xml.patch" not in paths:
        raise ValueError("Missing production manifest patch")
    destination.mkdir(parents=True)
    hashes = {}
    for relative in paths:
        source = root / relative
        if source.is_symlink() or not source.is_file():
            raise ValueError("Unsupported tracked input: " + relative)
        source.resolve().relative_to(root)
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        hashes[relative] = hashlib.sha256(target.read_bytes()).hexdigest()
    selection = {
        "head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root).decode().strip(),
        "excluded": EXCLUDED,
        "files": hashes,
    }
    (destination / "selection.json").write_text(json.dumps(selection, indent=2) + "\n")
    print("Staged", len(paths), "tracked files; diagnostic sources/patches excluded")


if __name__ == "__main__":
    stage(ROOT, Path(sys.argv[1]))
