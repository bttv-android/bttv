"""Static checks for Twitch 31 Highlight Keywords integration."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


highlight = read("mod/app/src/main/java/bttv/highlight/Highlight.java")
settings = read("mod/app/src/main/java/bttv/settings/Settings.java")
settings_bottom = read("mod/app/src/main/java/bttv/settings/SettingsBottom.java")
bridge = read("patches/live/bttv.highlight.LiveHighlightBridge.smali.patch")
z86_patch = read("patches/live/z86.smali.patch")
f86_patch = read("patches/live/f86.smali.patch")
f86 = read(".bttv-cache/disass/smali_classes2/f86.smali")
x46 = read(".bttv-cache/disass/smali_classes2/x46.smali")

assert "Lbttv/highlight/HighlightDiagnostics;" not in bridge
assert "Lbttv/highlight/HighlightDiagnostics;" not in f86_patch

# The existing settings surface and persisted StringSet remain the source of truth.
assert "OpenHighlightedWordsButton(" in settings
assert "HighlightedKeyWords(" in settings
assert '"bttv_highlight_set"' in settings
assert "getOpenStringSetLink(ctx, container, Highlight.getInstance(), Settings.OpenHighlightedWordsButton)" in settings_bottom

# Preserve the original exact, case-insensitive, space-delimited matching rules.
assert 'text.split(" ")' in highlight
assert "if (shouldHighlight(word))" in highlight
assert 'word.startsWith("<") || word.endsWith(">")' in highlight
assert '"<" + name.toLowerCase() + ">"' in highlight
assert "Res.colors.bttv_sonic" in highlight
assert "highlightedColorResource()" in highlight
assert "return ResUtil.getResourceId(Res.colors.bttv_sonic);" in highlight
assert "getColorValue(Res.colors.bttv_sonic.name())" not in highlight

# Matching is performed exactly once at f86.b's native presentation-style
# boundary. z86 no longer rewrites Ll26.e, which is only the username color.
hook = "Lbttv/highlight/LiveHighlightBridge;->replace(Ljava/util/List;Lfmp;Ljava/lang/String;Ljava/lang/String;Lenj;)Lenj;"
assert z86_patch.count("LiveHighlightBridge;->replace") == 0
assert f86_patch.count(hook) == 1
assert "move-object/from16 v2, p5" in f86_patch
assert "move-object/from16 v2, p2" not in f86_patch
assert "invoke-static {v2, v8, v0, v1, v5}" in f86_patch
assert "move-result-object v5" in f86_patch
assert "invoke-virtual/range {v0 .. v11}, Lf86;->a(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Lenj;" in f86
assert "invoke-static/range {v4 .. v24}, Lf86;->b(" in x46

# The classes7 bridge examines text and synthetic third-party emotes, supports
# login/display-name entries, preserves every non-ordinary native style, and
# fails open to the stock style.
assert "smali_classes7/bttv/highlight/LiveHighlightBridge.smali" in bridge
assert "instance-of v0, p4, Leg30;" in bridge
assert "instance-of v2, v1, Limp;" in bridge
assert "iget-object v1, v1, Limp;->a:Ljava/lang/String;" in bridge
assert "instance-of v2, v1, Lfmp;" in bridge
assert "iget-object v1, v1, Lfmp;->b:Ljava/lang/String;" in bridge
assert "iget-object v2, p1, Lfmp;->b:Ljava/lang/String;" in bridge
assert bridge.count('const-string v3, "bttv31://"') == 1
assert bridge.count('const-string v1, "bttv31://"') == 1
assert bridge.count("startsWith(Ljava/lang/String;)Z") == 2
assert bridge.count("shouldHighlightChannel(Ljava/lang/String;)Z") == 2
assert bridge.count("shouldHighlightText(Ljava/lang/String;)Z") == 2
assert "highlightedColorResource()I" in bridge
assert "new-instance v1, Lfg30;" in bridge
assert "invoke-direct {v1, v0}, Lfg30;-><init>(I)V" in bridge
assert bridge.count("return-object p4") == 2
assert "Ljava/lang/Throwable;" in bridge

# Twitch's active f86.a renderer consumes Lfg30 as a color resource, resolves
# it through Context.getColor, and applies one BackgroundColorSpan after token
# assembly. This proves visual consumption rather than matching alone.
assert "instance-of v10, v8, Lfg30;" in f86
assert "iget v2, v2, Lfg30;->a:I" in f86
assert "new-instance v0, Landroid/text/style/BackgroundColorSpan;" in f86
assert "invoke-virtual {v1, v2}, Landroid/content/Context;->getColor(I)I" in f86
background = f86.index("new-instance v0, Landroid/text/style/BackgroundColorSpan;")
apply_span = f86.index("invoke-virtual {v12, v0, v9, v3, v2}, Landroid/text/SpannableStringBuilder;->setSpan", background)
assert background < apply_span < f86.index("return-object v0", apply_span)

# Anon Chat must remain the recovered original Stage 5J design.
assert not (ROOT / "patches/live/bttvcompat.AnonChatConnection.smali.patch").exists()
assert "allowComposer(Z)Z" in read("patches/live/e66.smali.patch")
assert "bindComposer" not in read("patches/live/e66.smali.patch")

print("PASS Twitch 31 Highlight Keywords native color integration")
