"""Validate the exact supported five-APK bundle; optionally make a root-layout ZIP.

Only pinned, user-supplied inputs are accepted. No APK is downloaded or patched.
The outer APKM metadata is not a replacement for validating its APK payloads.
"""
import argparse
from contextlib import ExitStack
import hashlib
import io
import json
from pathlib import Path
import zipfile

PIN = Path(__file__).with_name("twitch-31.2.0-inputs.json")


def inspect(source, pin=PIN, armv7_bundle=None):
    expected = json.loads(Path(pin).read_text())["files"]
    armv7 = "split_config.armeabi_v7a.apk"
    with ExitStack() as stack:
        bundle = stack.enter_context(zipfile.ZipFile(source))
        extra = stack.enter_context(zipfile.ZipFile(armv7_bundle)) if armv7_bundle else None
        names = bundle.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate bundle entries")
        apks = {name for name in names if name.lower().endswith(".apk")}
        primary = set(expected) - {armv7} if extra else set(expected)
        if apks != primary:
            raise ValueError("Unexpected APK inventory; see the pinned input requirements in " + str(pin))
        if extra and (len(extra.namelist()) != len(set(extra.namelist())) or armv7 not in extra.namelist()):
            raise ValueError("Secondary bundle must contain one root-level ARMv7 split")
        hashes = {}
        for name, wanted in expected.items():
            data = (extra if extra and name == armv7 else bundle).read(name)
            actual = hashlib.sha256(data).hexdigest().upper()
            if actual != wanted:
                raise ValueError("Unvalidated Twitch APK variant: " + name)
            with zipfile.ZipFile(io.BytesIO(data)) as apk:
                if len(apk.namelist()) != len(set(apk.namelist())) or apk.testzip():
                    raise ValueError("Invalid APK ZIP: " + name)
            hashes[name] = actual
        return hashes


def prepare(source, destination, armv7_bundle=None):
    hashes = inspect(source, armv7_bundle=armv7_bundle)
    destination = Path(destination)
    # Exclusive creation also prevents replacing the currently proven input.
    with destination.open("xb") as target, zipfile.ZipFile(target, "w", zipfile.ZIP_STORED) as result:
        with ExitStack() as stack:
            bundle = stack.enter_context(zipfile.ZipFile(source))
            extra = stack.enter_context(zipfile.ZipFile(armv7_bundle)) if armv7_bundle else None
            for name in sorted(hashes):
                entry = zipfile.ZipInfo(name, (2020, 1, 1, 0, 0, 0))
                entry.compress_type = zipfile.ZIP_STORED
                result.writestr(entry, (extra if extra and name == "split_config.armeabi_v7a.apk" else bundle).read(name))
    assert inspect(destination) == hashes
    return hashes


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", nargs="?", type=Path)
    parser.add_argument("--armv7-bundle", type=Path, help="Select ONLY the pinned ARMv7 split; primary bundle must contain the other four pinned APKs")
    args = parser.parse_args()
    print(json.dumps(prepare(args.source, args.destination, args.armv7_bundle) if args.destination else inspect(args.source, armv7_bundle=args.armv7_bundle), indent=2))
