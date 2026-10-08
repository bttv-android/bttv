"""Static integration checks for Twitch 31 channel-points auto-redeem."""

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
DISASS = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / ".bttv-cache" / "disass"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


helper = read(ROOT / "mod/app/src/main/java/bttv/ChannelPoints.java")
patch = read(ROOT / "patches/live/z89.smali.patch")
z89 = read(DISASS / "smali_classes2/z89.smali")

assert ".method public final D2(Lm89;)Lk89;" in z89
assert "iget-object v5, p1, Lm89;->d:Lgd;" in z89
assert "iget-object v12, p0, Lz89;->m:Lhc9;" in patch
assert patch.count("Lbttv/ChannelPoints;->shouldAutoRedeem(Ljava/lang/String;)Z") == 1
assert patch.count("Lhc9;->G(Ljava/lang/String;Ltv/twitch/android/shared/one/chat/pub/ChatModeMetadata;)V") == 1
assert "iget-object v0, p1, Lm89;->d:Lgd;" in patch
assert "iget-object v1, v0, Lgd;->a:Ljava/lang/String;" in patch
assert "const/4 v0, 0x0" in patch
assert "purchase" not in patch.lower()
assert "reward" not in patch.lower()

assert "AutoRedeemChannelPointsEnabled" in helper
assert "shouldAutoRedeem(String claimId)" in helper
assert "!autoRedeemedClaimIds.add(claimId)" in helper
assert "onClickListener.onClick(null)" in helper

print("PASS Twitch 31 channel-points auto-redeem static integration")
