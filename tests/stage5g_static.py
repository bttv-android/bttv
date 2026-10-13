"""Static checks for classic Show Deleted Messages on Twitch 31."""

from pathlib import Path
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[1]
BASE = "6dbb77181e05278427aec38a1cd1667134d61734"
DISASS = ROOT / ".bttv-cache" / "disass"
TEMPORARY_DIAGNOSTICS = (
    "X46_TO_F86", "F86_ENTRY", "F86_DELETE", "RESPAN_ENTER",
    "RESPAN_ENABLED", "RESPAN_RESULT", "F86_BTTV_RESULT",
    "VW90_ENTER", "VW90_LIED count=", "VW90_EXISTING_LIED",
    "VW90_ORIGINAL present=", "VW90_BTTV result=", "VW90_CALLER",
    "BTTV31DELETE2", "DeletedMessageDiagnostics", "VW90 path=",
)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8").replace("\r\n", "\n")


def baseline(path: str) -> str:
    return subprocess.check_output(
        ["git", "show", f"{BASE}:{path}"], cwd=ROOT
    ).decode().replace("\r\n", "\n")


def original(path: str) -> str:
    return (DISASS / path).read_text(encoding="utf-8").replace("\r\n", "\n")


def added_patch(path: str) -> str:
    return "\n".join(
        line[1:] for line in read(path).splitlines()
        if line.startswith("+") and not line.startswith("+++")
    )


def without_stage5k_highlight_hunk(text: str) -> str:
    start = text.index("@@ -158,6 +158,16 @@")
    end = text.index("@@ -3352,8 +3352,13 @@", start)
    return text[:start] + text[end:]


class DeletedMessagesStaticTests(unittest.TestCase):
    def test_g1d_vw90_call_is_strictly_deleted_only(self):
        method = original("smali_classes2/g1d.smali")
        call = method.index("Lvw90;->n(")
        gate = method.rfind("iget-boolean v13, v1, Ltlp;->j:Z", 0, call)
        branch = method.index("if-eqz v13, :cond_7", gate, call)
        non_deleted = method.index("\n    :cond_7\n", call)
        self.assertLess(gate, branch)
        self.assertLess(branch, call)
        self.assertLess(call, non_deleted)
        self.assertEqual(method.count("Lvw90;->n("), 1)

    def test_classic_helper_is_clean_and_preserves_spans(self):
        path = "mod/app/src/main/java/bttv/api/DeletedMessages.java"
        text = read(path)
        self.assertIn("Settings.ShowDeletedMessagesEnabled", text)
        self.assertIn("return null;", text)
        self.assertIn("new SpannableStringBuilder(original)", text)
        self.assertIn('new SpannableString("(" + removedByModText + ")")', text)
        self.assertIn("new StyleSpan(Typeface.ITALIC)", text)
        self.assertNotIn('" " +', text)
        self.assertNotIn("ForegroundColorSpan", text)

    def test_count1_restores_native_container_without_rebuilding_author(self):
        helper = read("mod/app/src/main/java/bttv/api/DeletedMessages.java")
        self.assertIn("restoreNativeContainer(Spanned container, Object placeholder, Spanned restoredBody)", helper)
        self.assertIn("new SpannableStringBuilder(container)", helper)
        self.assertIn("result.removeSpan(placeholder)", helper)
        self.assertIn("result.replace(start, end, restoredBody)", helper)
        self.assertIn("if (container == null)", helper)
        self.assertIn("if (placeholder == null || restoredBody == null)", helper)
        self.assertIn("start < 0 || end < 0 || end <= start || end > container.length()", helper)
        self.assertGreaterEqual(helper.count("return container;"), 2)
        self.assertNotIn('"Username"', helper)
        self.assertNotIn("container.setSpan", helper)
        patch = read("patches/live/vw90.smali.patch")
        body = patch.index("DeletedMessages;->reSpan(Landroid/text/Spanned;)", patch.index(":cond_6"))
        gate = patch.index("if-eqz v6, :bttv31_count1_stock", body)
        restore = patch.index("DeletedMessages;->restoreNativeContainer", gate)
        restored = patch.index("move-result-object v6", restore)
        custom_return = patch.index("return-object v6", restored)
        self.assertLess(body, gate)
        self.assertLess(gate, restore)
        self.assertLess(restore, restored)
        self.assertLess(restored, custom_return)

    def test_count0_returns_enabled_result_before_stock_lied_construction(self):
        patch = read("patches/live/vw90.smali.patch")
        helper = patch.index("DeletedMessages;->reSpan(Landroid/text/Spanned;)")
        result = patch.index("move-result-object v6", helper)
        stock_branch = patch.index("if-eqz v6, :bttv31_count0_stock", result)
        immediate_return = patch.index("return-object v6", stock_branch)
        stock_label = patch.index(":bttv31_count0_stock", immediate_return)
        construct = patch.index("new-instance v0, Lied;", stock_label)
        source = original("smali_classes2/vw90.smali")
        ctor = source.index("invoke-direct {v0, p0, p1, p4, p3}, Lied;-><init>")
        stock = source.index("const p3, 0x7f150328", ctor)
        source_attach = source.index("SpannableStringBuilder;->setSpan")
        self.assertLess(source_attach, source.index("\n    :cond_5\n", source_attach))
        self.assertLess(helper, result)
        self.assertLess(result, stock_branch)
        self.assertLess(stock_branch, immediate_return)
        self.assertLess(immediate_return, stock_label)
        self.assertLess(stock_label, construct)
        self.assertLess(ctor, stock)
        self.assertNotIn("-    invoke-direct {v0, p0, p1, p4, p3}, Lied;-><init>", patch)
        self.assertNotIn("invoke-direct {v0, p0, v6", patch)
        self.assertNotIn("iput-object v6", patch)

    def test_count1_uses_retained_original_and_fails_open_to_stock(self):
        patch = read("patches/live/vw90.smali.patch")
        existing = patch.index(":cond_6")
        first = patch.index("aget-object v3, v1, v0", existing)
        original_field = patch.index("Lied;->b:Landroid/text/SpannedString;", first)
        null_original = patch.index("if-eqz v0, :bttv31_count1_stock", original_field)
        call = patch.index("DeletedMessages;->reSpan(Landroid/text/Spanned;)", null_original)
        result = patch.index("move-result-object v6", call)
        enabled = patch.index("if-eqz v6, :bttv31_count1_stock", result)
        custom_return = patch.index("return-object v6", enabled)
        stock = patch.index(":bttv31_count1_stock", custom_return)
        stock_return = patch.index("return-object p1", stock)
        self.assertLess(first, original_field)
        self.assertLess(original_field, null_original)
        self.assertLess(null_original, call)
        self.assertLess(call, result)
        self.assertLess(result, enabled)
        self.assertLess(enabled, custom_return)
        self.assertLess(custom_return, stock)
        self.assertLess(stock, stock_return)
        self.assertEqual(patch.count("DeletedMessages;->reSpan"), 2)

    def test_no_manual_lied_transfer_or_cross_sequence_span_range(self):
        added = added_patch("patches/live/vw90.smali.patch")
        self.assertNotIn("SpannableStringBuilder;->getSpanStart", added)
        self.assertNotIn("SpannedString;->getSpanStart", added)
        self.assertNotIn("SpannableStringBuilder;->setSpan", added)
        self.assertNotIn("SpannableStringBuilder;-><init>(Ljava/lang/CharSequence;)", added)
        self.assertNotIn("const/16 p0, 0x22", added)
        self.assertNotIn("const/16 v1, 0x22", added)

    def test_count0_stock_block_remains_structurally_intact(self):
        source = original("smali_classes2/vw90.smali")
        patch = read("patches/live/vw90.smali.patch")
        for marker in (
            "new-instance v0, Lied;",
            "invoke-direct {v0, p0, p1, p4, p3}, Lied;-><init>",
            "const p3, 0x7f150328",
        ):
            self.assertIn(marker, source)
        self.assertIn("new-instance v0, Lied;", patch)
        self.assertIn("SpannableStringBuilder;->setSpan(Ljava/lang/Object;III)V", source)
        self.assertIn("return-object v3", source)
        self.assertNotRegex(patch, r"(?m)^-(?!--)(?!    \.locals 6$)")

    def test_no_native_click_reveal_or_highlighted_path_patch(self):
        self.assertFalse((ROOT / "patches/live/ied.smali.patch").exists())
        patch = added_patch("patches/live/vw90.smali.patch")
        for forbidden in (
            "Lied;->c:Z", "Lied;->onClick", "Lied;->updateDrawState",
            "ForegroundColorSpan", "setAlpha", "EventDispatcher",
        ):
            self.assertNotIn(forbidden, patch)
        f86_path = "patches/live/f86.smali.patch"
        self.assertEqual(without_stage5k_highlight_hunk(read(f86_path)), baseline(f86_path), f86_path)
        relocation = "patches/live/zz_f86_relocation.patch"
        self.assertEqual(read(relocation), baseline(relocation), relocation)
        for path in ("bf60", "xe60", "kw5", "af60"):
            self.assertFalse((ROOT / f"patches/live/{path}.smali.patch").exists())

    def test_all_temporary_diagnostics_are_removed(self):
        self.assertFalse((ROOT / "mod/app/src/main/java/bttv/api/DeletedMessageDiagnostics.java").exists())
        production = "\n".join(
            path.read_text(encoding="utf-8", errors="replace")
            for directory in (ROOT / "mod/app/src/main/java", ROOT / "patches")
            for path in directory.rglob("*") if path.is_file()
        )
        for diagnostic in TEMPORARY_DIAGNOSTICS:
            self.assertNotIn(diagnostic, production)
        self.assertFalse((ROOT / "patches/live/gos.smali.patch").exists())

    def test_patch_applies_and_unrelated_paths_are_unchanged(self):
        for patch in ("vw90.smali.patch", "g1d.smali.patch"):
            subprocess.run(
                ["git", "apply", "--check", str(ROOT / "patches/live" / patch)],
                cwd=DISASS,
                check=True,
            )
        f86_path = "patches/live/f86.smali.patch"
        self.assertEqual(without_stage5k_highlight_hunk(read(f86_path)), baseline(f86_path), f86_path)
        for path in (
            "patches/live/zz_f86_relocation.patch",
            "patches/live/y15.smali.patch",
            "mod/app/src/main/java/bttv/SplitChat.java",
            "mod/app/src/main/java/bttv/api/SplitChat.java",
            "mod/app/src/main/java/bttv/emote/LiveEmotes.java",
        ):
            self.assertEqual(read(path), baseline(path), path)
        self.assertIn("rename to smali_classes7/f86.smali", read("patches/live/zz_f86_relocation.patch"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
