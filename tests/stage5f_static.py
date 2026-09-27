"""Static integration checks for the Twitch 31 Split Chat port."""

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
DISASS = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / ".bttv-cache" / "disass"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


core = read(ROOT / "mod/app/src/main/java/bttv/SplitChat.java")
api = read(ROOT / "mod/app/src/main/java/bttv/api/SplitChat.java")
bind_patch = read(ROOT / "patches/live/g1d.smali.patch")
trim_patch = read(ROOT / "patches/live/y15.smali.patch")
g1d = read(DISASS / "smali_classes2/g1d.smali")
y15 = read(DISASS / "smali_classes2/y15.smali")

assert ".method public final u(Luiw;)V" in g1d
assert "const v0, 0x7f0b0480" in g1d
assert "check-cast v1, Ltlp;" in g1d
assert bind_patch.count("Lbttv/api/SplitChat;->setBackgroundColor(ILandroid/view/View;)V") == 1
assert "move-object/from16 v0, p0" in bind_patch
assert "iget-object v0, v0, Landroidx/recyclerview/widget/i;->a:Landroid/view/View;" in bind_patch

assert ".method public final w(Ljava/util/List;)V" in y15
assert "invoke-virtual {v3, v0}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;" in y15
assert "invoke-virtual {p0, v0, v2}, Ldjw;->j(II)V" in y15
assert trim_patch.index(":cond_2") < trim_patch.index("removedNMessages(I)V")
assert trim_patch.index("removedNMessages(I)V") < trim_patch.index("Ldjw;->j(II)V")

assert "setBackgroundColor(int position, View view)" in core
assert "view.findViewById(chatMessageItemResId)" in core
assert "Configuration.UI_MODE_NIGHT_MASK" in core
assert "Configuration.UI_MODE_NIGHT_YES" in core
assert "ThemeManager" not in core
assert "originalBackgrounds.put(view, view.getBackground())" in core
assert "view.setBackground(originalBackgrounds.remove(view))" in core
assert "!ResUtil.getBooleanFromSettings(Settings.SplitChatEnabled) || position < 0" in core
assert "setLayoutParams" not in core
assert "TextView" not in core
assert "ChatMessageViewHolder" not in core
assert "RecyclerView$ViewHolder" not in api
assert "setBackgroundColor(int position, View view)" in api

print("PASS Twitch 31 Split Chat static integration")
