"""Static checks for highlighted/redeemed Show Deleted Messages on Twitch 31."""

from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[1]
BASE = "d7c14073fd3e82d4d8b71c5595983c5b8ac4598b"
DISASS = ROOT / "disass" / "disass"


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8").replace("\r\n", "\n")


def baseline(path: str) -> str:
    return subprocess.check_output(
        ["git", "show", f"{BASE}:{path}"], cwd=ROOT
    ).decode().replace("\r\n", "\n")


class HighlightedDeletedMessagesStaticTests(unittest.TestCase):
    def test_jc2_hook_is_one_late_body_substitution(self):
        patch = read("patches/live/jc2.smali.patch")
        self.assertEqual(patch.count("DeletedSpecialRows;->restore"), 1)
        self.assertIn("invoke-static {v0}", patch)
        self.assertIn("move-result-object v11", patch)
        self.assertIn("-    invoke-virtual {v0}, Lg86;->a()", patch)
        self.assertNotIn("Lf86;", patch)
        self.assertNotIn("Lvw90;", patch)
        self.assertNotIn("Laf60;", patch)

    def test_bridge_requires_exact_lied_and_retained_original(self):
        patch = read("patches/live/bttv.api.DeletedSpecialRows.smali.patch")
        self.assertIn("iget-object v1, p0, Lg86;->e:Ljava/lang/CharSequence;", patch)
        self.assertIn("const-class v4, Lied;", patch)
        self.assertIn("const/4 v3, 0x1", patch)
        self.assertIn("if-ne v2, v3, :return_stock", patch)
        self.assertIn("iget-object v2, v1, Lied;->b:Landroid/text/SpannedString;", patch)
        self.assertIn("DeletedMessages;->restoreSpecialContainer", patch)
        self.assertIn(".catch Ljava/lang/Throwable;", patch)
        self.assertIn(":restore_failed", patch)
        self.assertIn("return-object v0", patch)

    def test_special_helper_reuses_accepted_body_and_container_algorithms(self):
        helper = read("mod/app/src/main/java/bttv/api/DeletedMessages.java")
        start = helper.index("restoreSpecialContainer(")
        body = helper[start:]
        self.assertIn("Spanned restoredBody = reSpan(originalBody);", body)
        self.assertIn("return restoreNativeContainer(container, placeholder, restoredBody);", body)
        self.assertIn("if (container == null || placeholder == null || originalBody == null)", body)
        self.assertGreaterEqual(body.count("return container;"), 2)
        self.assertNotIn("setSpan(", body)
        self.assertNotIn("Username", body)
        self.assertNotIn("clamp", body.lower())

    def test_frozen_ordinary_and_split_chat_paths_are_unchanged(self):
        for path in (
            "patches/live/vw90.smali.patch",
            "patches/live/g1d.smali.patch",
            "patches/live/f86.smali.patch",
            "patches/live/zz_f86_relocation.patch",
            "mod/app/src/main/java/bttv/SplitChat.java",
            "mod/app/src/main/java/bttv/api/SplitChat.java",
        ):
            self.assertEqual(read(path), baseline(path), path)

    def test_no_lied_transfer_or_manual_native_container_rebuild(self):
        production = "\n".join((
            read("patches/live/jc2.smali.patch"),
            read("patches/live/bttv.api.DeletedSpecialRows.smali.patch"),
        ))
        for forbidden in (
            "Lied;->c:Z", "Lied;->onClick", "SpannableStringBuilder;->setSpan",
            "ForegroundColorSpan", "Username", "badge", "timestamp",
        ):
            self.assertNotIn(forbidden, production)

    def test_patches_parse_and_apply_normally(self):
        for patch in ("jc2.smali.patch", "bttv.api.DeletedSpecialRows.smali.patch"):
            path = ROOT / "patches/live" / patch
            subprocess.run(["git", "apply", "--numstat", str(path)], cwd=ROOT, check=True)
            subprocess.run(["git", "apply", "--check", str(path)], cwd=DISASS, check=True)


if __name__ == "__main__":
    unittest.main(verbosity=2)
