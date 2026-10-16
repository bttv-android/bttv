"""Manifest migration invariants and production selection, without a device.

Uses git apply against the actual decoded manifest and compares every XML
element/attribute with the committed production patch's result. Optional final
APK validation is performed separately against the accepted signed baseline.
"""
import copy
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OLD = "tv.twitch.android.app"
NEW = "tv.twitch.bttvandroid.app"
A = "{http://schemas.android.com/apk/res/android}"
PERMISSION = ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
SUFFIXES = (
    "firebaseinitprovider", "provider", "androidx-startup", "fileprovider",
    "SentryInitProvider", "SentryPerformanceProvider", "SentryNdkPreloadProvider",
    "backgrounddetector",
)
AUTHORITIES = {NEW + "." + s for s in SUFFIXES} | {
    "com.amazon.identity.auth.device.MapInfoProvider." + NEW
}


def apply_manifest(patch):
    temp_root = ROOT / ".tmp"
    temp_root.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="bttv-migration-", dir=temp_root) as folder:
        folder = Path(folder)
        # Normalize generated fixtures only; the original decode stays untouched.
        source = (ROOT / ".bttv-cache/disass/AndroidManifest.xml").read_text()
        (folder / "AndroidManifest.xml").write_text(source, newline="\n")
        p = folder / "manifest.patch"
        p.write_text(patch, newline="\n")
        for args in (["--check"], []):
            subprocess.run(["git", "apply", *args, str(p)], cwd=folder, check=True,
                           env={**os.environ, "GIT_CEILING_DIRECTORIES": str(temp_root)})
        return ET.parse(folder / "AndroidManifest.xml").getroot()


def shape(element):
    return (element.tag, dict(element.attrib), (element.text or "").strip(),
            [shape(child) for child in element])


class MigrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        old_patch = subprocess.check_output(
            ["git", "show", "303d0923:patches/AndroidManifest.xml.patch"], cwd=ROOT
        ).decode()
        cls.old = apply_manifest(old_patch)
        cls.new = apply_manifest((ROOT / "patches/AndroidManifest.xml.patch").read_text())

    def test_only_approved_manifest_attributes_change(self):
        expected = copy.deepcopy(self.old)
        expected.set("package", NEW)
        for p in expected.findall("application/provider"):
            p.set(A + "authorities", p.get(A + "authorities").replace(OLD, NEW))
        for tag in ("permission", "uses-permission"):
            for p in expected.findall(tag):
                if p.get(A + "name") == OLD + PERMISSION:
                    p.set(A + "name", NEW + PERMISSION)
        for data in expected.findall("application/activity/intent-filter/data"):
            if data.get(A + "scheme") == "amzn" and data.get(A + "host") == OLD:
                data.set(A + "host", NEW)
        # Includes metadata/credentials, native class names, OAuth/deep links,
        # component flags, version code, resource references and permissions.
        self.assertEqual(shape(expected), shape(self.new))

    def test_nine_unique_authorities(self):
        values = [p.get(A + "authorities") for p in self.new.findall("application/provider")]
        self.assertEqual(len(values), 9)
        self.assertEqual(set(values), AUTHORITIES)
        self.assertTrue(set(values).isdisjoint(
            p.get(A + "authorities") for p in self.old.findall("application/provider")))

    def test_permission_and_amazon_callback(self):
        defined = self.new.findall("permission")
        self.assertEqual(len(defined), 1)
        self.assertEqual(defined[0].get(A + "name"), NEW + PERMISSION)
        self.assertEqual(defined[0].get(A + "protectionLevel"), "signature")
        self.assertEqual(sum(p.get(A + "name") == NEW + PERMISSION
                             for p in self.new.findall("uses-permission")), 1)
        hosts = [d.get(A + "host") for d in self.new.findall("application/activity/intent-filter/data")
                 if d.get(A + "scheme") == "amzn"]
        self.assertEqual(hosts, [NEW])
        path = ROOT / ".bttv-cache/disass/smali_classes3/com/amazon/identity/auth/device/appid/ThirdPartyAppIdentifier.smali"
        method = path.read_text().split(".method public getRedirectUrl(", 1)[1].split(".end method", 1)[0]
        self.assertIn('"amzn://"', method)
        self.assertIn("Context;->getPackageName()", method)

    def test_ua_and_firebase_have_no_new_patch(self):
        spec = importlib.util.spec_from_file_location("selection", ROOT / "tools/stage_production_input.py")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        paths = module.production_paths(ROOT)
        # Also guard against accidentally packaging untracked patches.
        all_patches = sorted(p.relative_to(ROOT).as_posix() for p in (ROOT / "patches").rglob("*.patch")
                             if not p.relative_to(ROOT).as_posix().startswith(module.EXCLUDED))
        self.assertEqual(sorted(p for p in paths if p.startswith("patches/") and p.endswith(".patch")), all_patches)
        for p in paths:
            if p.startswith("patches/") and p.endswith(".patch"):
                text = (ROOT / p).read_text()
                self.assertNotIn("BTTV31PUSH", text)
                self.assertNotIn("Lbttv/notifications/", text)
                for target in ("a660.smali", "jgh.smali", "mgh.smali"):
                    self.assertNotIn("/" + target, text)
        self.assertFalse(any(p.startswith(module.EXCLUDED) for p in paths))
        ua = (ROOT / ".bttv-cache/disass/smali_classes2/a660.smali").read_text()
        self.assertIn("Context;->getPackageName()", ua)
        self.assertIn('"/31.2.0/3102006"', ua)
        self.assertEqual(int(self.new.get(A + "versionCode")), 1208000338)


if __name__ == "__main__":
    unittest.main()
