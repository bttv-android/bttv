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
    "diagnostics/",
    "patches/notifications_diag/",
    "patches/notifications_reason/",
    "patches/purchase_ui_diag/",
    "mod/app/src/main/java/bttv/notifications/",
    "mod/app/src/main/java/bttv/commerce/PurchaseUiDiagnostics.java",
)


def git(root, *args):
    return subprocess.check_output(
        ["git", "-c", "safe.directory=" + str(root), *args], cwd=root
    ).decode()


def production_paths(root, additional=()):
    paths = git(root, "ls-files", "-z").split("\0")
    deleted = set(git(root, "ls-files", "--deleted", "-z").split("\0"))
    for relative in additional:
        path = Path(relative)
        if path.is_absolute() or relative.startswith("/") or ":" in relative or ".." in path.parts or "\\" in relative:
            raise ValueError("Invalid explicit input: " + relative)
        if relative.startswith(EXCLUDED):
            raise ValueError("Diagnostic input forbidden: " + relative)
        paths.append(relative)
    return sorted(set(p for p in paths if p and p not in deleted and not p.startswith(EXCLUDED)))


def stage(root, destination, additional=()):
    root, destination = root.resolve(), destination.resolve()
    if destination.exists():
        raise ValueError("Refusing to overwrite an existing staged input")
    paths = production_paths(root, additional)
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
        "head": git(root, "rev-parse", "HEAD").strip(),
        "excluded": EXCLUDED,
        "files": hashes,
    }
    (destination / "selection.json").write_text(json.dumps(selection, indent=2) + "\n")
    print("Staged", len(paths), "production files (tracked + explicit infrastructure); diagnostics excluded")


if __name__ == "__main__":
    stage(ROOT, Path(sys.argv[1]))
