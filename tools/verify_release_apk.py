"""Verify an exported dual-ARM APK with SDK tools and independent ZIP/DEX checks."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import zipfile
import zlib

TEST_SIGNER = "1E08A903AEF9C3A721510B64EC764D01D3D094EB954161B62544EA8F187B5953"
ROOT = Path(__file__).resolve().parents[1]


def sha256(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(4 * 1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest().upper()


def run(*args):
    return subprocess.check_output([str(a) for a in args], stderr=subprocess.STDOUT).decode()


def verify(apk, source, sdk=None, expected_signer=None, allow_test=False):
    from prepare_twitch_input import inspect
    inspect(source)
    apk = Path(apk)
    sdk = Path(sdk or os.environ.get("ANDROID_SDK_ROOT", Path.home() / "Android/Sdk"))
    tools = sdk / "build-tools/30.0.3"
    metadata = run(tools / "aapt", "dump", "badging", apk)
    package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", metadata)
    if not package:
        raise ValueError("Missing package metadata")
    name, code, version = package.groups()
    config = json.loads((ROOT / "bttv.manifest.json").read_text())
    if (name, int(code), version) != ("tv.twitch.bttvandroid.app", int(config["versionCode"]), config["version"]):
        raise ValueError("Unexpected package/version")
    minimum = int(re.search(r"^sdkVersion:'(\d+)'", metadata, re.M).group(1))
    target = int(re.search(r"^targetSdkVersion:'(\d+)'", metadata, re.M).group(1))
    if (minimum, target) != (29, 36):
        raise ValueError("Unexpected SDK levels")
    manifest = run(tools / "aapt", "dump", "xmltree", apk, "AndroidManifest.xml")
    if not re.search(r'android:extractNativeLibs[^\n]*0x0\b', manifest):
        raise ValueError("Unexpected native extraction mode")
    if re.search(r'android:isSplitRequired[^\n]*0xffffffff', manifest) or 'uses-split' in manifest:
        raise ValueError("APK still requires splits")
    if 'com.android.vending.splits.required' in manifest or 'com.android.vending.splits"' in manifest:
        raise ValueError("Split metadata remains")
    authorities = re.findall(r'android:authorities[^\n]*="([^"\n]+)"', manifest)
    if len(authorities) != 9 or len(set(authorities)) != 9:
        raise ValueError("Expected nine unique provider authorities")
    signature = run(tools / "apksigner", "verify", "--verbose", "--print-certs", apk)
    if not re.search(r'Verified using v3 scheme[^\n]*true', signature):
        raise ValueError("APK v3 signature verification failed")
    signers = re.findall(r'certificate SHA-256 digest: ([0-9a-fA-F]{64})', signature)
    if len(signers) != 1:
        raise ValueError("Expected exactly one APK signer")
    signer = signers[0].upper()
    if expected_signer and signer != expected_signer.upper():
        raise ValueError("Unexpected signing certificate")
    if signer == TEST_SIGNER and not allow_test:
        raise ValueError("Public bundled debug key is not a production signing identity")
    alignment = run(tools / "zipalign", "-c", "-p", "4", apk)
    dex_counts, native_counts = {}, {}
    with zipfile.ZipFile(apk) as z, apk.open("rb") as raw:
        names = z.namelist()
        if len(names) != len(set(names)) or z.testzip():
            raise ValueError("Duplicate entries or ZIP CRC failure")
        dex_names = sorted(n for n in names if re.fullmatch(r"classes(?:\d+)?\.dex", n))
        expected = ["classes.dex"] + ["classes%d.dex" % i for i in range(2, 8)]
        if dex_names != expected:
            raise ValueError("Expected seven contiguous DEX files")
        for entry in dex_names:
            data = z.read(entry)
            if not data.startswith(b"dex\n") or len(data) < 112:
                raise ValueError("Invalid DEX header")
            if hashlib.sha1(data[32:]).digest() != data[12:32] or zlib.adler32(data[12:]) & 0xffffffff != struct.unpack_from('<I', data, 8)[0]:
                raise ValueError("DEX checksum failure")
            counts = dict(zip(["strings", "types", "prototypes", "fields", "methods", "classes"],
                              [struct.unpack_from('<I', data, i)[0] for i in [56, 64, 72, 80, 88, 96]]))
            if any(counts[k] > 65535 for k in ["types", "prototypes", "fields", "methods"]):
                raise ValueError("Conservative DEX pool limit exceeded")
            if b"BTTV31PUSH" in data or b"Lbttv/notifications/PushDiagnostics;" in data:
                raise ValueError("Temporary notification diagnostics in production APK")
            dex_counts[entry] = counts
        if b"Lbttv/Data;" not in z.read("classes7.dex"):
            raise ValueError("BTTV injection missing from classes7.dex")
        for notice in ["bttv_third_party_notices.txt", "bttv_apksig_license.txt"]:
            if not z.read("res/raw/" + notice):
                raise ValueError("Missing packaged license resource")
        for entry in z.infolist():
            if not entry.filename.startswith("lib/") or not entry.filename.endswith(".so"):
                continue
            abi = entry.filename.split('/')[1]
            native_counts[abi] = native_counts.get(abi, 0) + 1
            raw.seek(entry.header_offset)
            header = raw.read(30)
            filename_size, extra_size = struct.unpack_from('<HH', header, 26)
            offset = entry.header_offset + 30 + filename_size + extra_size
            if entry.compress_type != zipfile.ZIP_STORED or offset % 4096:
                raise ValueError("Native compression/alignment failure: " + entry.filename)
            data = z.read(entry)
            if data[:4] != b'\x7fELF' or len(data) < 64:
                raise ValueError("Invalid native ELF: " + entry.filename)
            if abi in ['arm64-v8a', 'armeabi-v7a']:
                elf = (data[4], data[5], struct.unpack_from('<H', data, 18)[0])
                if elf != ((2, 1, 183) if abi == 'arm64-v8a' else (1, 1, 40)):
                    raise ValueError("Native architecture mismatch")
        for abi in ['arm64-v8a', 'armeabi-v7a']:
            if native_counts.get(abi) != 39 or 'lib/' + abi + '/libglide-webp.so' not in names:
                raise ValueError("Incomplete dual-ARM inventory")
        with zipfile.ZipFile(source) as bundle:
            for split, abi in [('arm64_v8a', 'arm64-v8a'), ('armeabi_v7a', 'armeabi-v7a')]:
                with zipfile.ZipFile(io.BytesIO(bundle.read('split_config.' + split + '.apk'))) as original:
                    libs = [n for n in original.namelist() if n.startswith('lib/' + abi + '/') and n.endswith('.so')]
                    if len(libs) != 38 or any(z.read(n) != original.read(n) for n in libs):
                        raise ValueError("Original Twitch native library changed")
    return dict(packageName=name, versionCode=int(code), versionName=version, minSdk=minimum,
                targetSdk=target, signerSha256=signer, sha256=sha256(apk), sizeBytes=apk.stat().st_size,
                standalone=True, supportedAbis=['arm64-v8a', 'armeabi-v7a'], twitchVersion='31.2.0',
                dex=dex_counts, native=native_counts, authorities=authorities, v3=True, zipalign=True,
                testSigningIdentity=(signer == TEST_SIGNER))


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('apk', type=Path)
    p.add_argument('--input', required=True, type=Path)
    p.add_argument('--sdk', type=Path)
    p.add_argument('--expected-signer')
    p.add_argument('--allow-test-key', action='store_true', help='Local test verification only')
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    result = verify(a.apk, a.input, a.sdk, a.expected_signer, a.allow_test_key)
    with a.output.open('x', encoding='utf-8') as stream:
        stream.write(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))
