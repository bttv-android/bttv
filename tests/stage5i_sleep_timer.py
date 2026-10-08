"""Static integration checks for the Twitch 31 BTTV sleep timer."""

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
DISASS = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / ".bttv-cache" / "disass"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


timer = read(ROOT / "mod/app/src/main/java/bttv/SleepTimer.java")
button = read(ROOT / "mod/app/src/main/java/bttv/SleepTimerButton.java")
live_patch = read(ROOT / "patches/sleeptimer/res.layout.bottom_player_overlay_controls.xml.patch")
vod_patch = read(ROOT / "patches/sleeptimer/res.layout.bottom_player_control_overlay_widget.xml.patch")
live_owner = read(DISASS / "smali/yu30.smali")
live_delegate = read(DISASS / "smali/pop.smali")
vod_owner = read(DISASS / "smali_classes5/xjy.smali")
vod_delegate = read(DISASS / "smali_classes5/rrt.smali")

# Original behavior and setting contract.
assert "Settings.ShouldShowSleepTimer" in timer
assert '"bttv_sleep_timer_button"' in timer
assert '"5 minutes"' in timer and '"10 minutes"' in timer
assert '"15 minutes"' in timer and '"30 minutes"' in timer
assert '"45 minutes"' in timer and '"1 hour"' in timer
assert "int[] minutes = new int[]{ 5, 10, 15, 30, 45, 60 };" in timer
assert "timer = new Timer();" in timer
assert "abortSchedule();" in timer
assert "timer.cancel();" in timer and "timer = null;" in timer
assert "timer.schedule(new TimerTask()" in timer
assert "minutes * 60 * 1000L" in timer
assert "android.os.Process.killProcess(android.os.Process.myPid());" in timer

# Both verified Twitch 31 overlay layouts get exactly one self-initializing button.
for patch in (live_patch, vod_patch):
    assert patch.count("+        <bttv.SleepTimerButton") + patch.count("+    <bttv.SleepTimerButton") == 1
    assert '@id/bttv_sleep_timer_button' in patch
    assert '@drawable/bttv_ic_bedtime' in patch
    assert '@string/bttv_sleep_timer_button_descr' in patch
assert "SleepTimer.onInit(context, this);" in button
assert button.count("SleepTimer.onInit(context, this);") == 3
assert "setOnClickListener" not in button

# Verified live/general and seekable/VOD owners and delegates.
assert "const v8, 0x7f0e036e" in live_owner
assert "new-instance v10, Lpop;" in live_owner
assert ".method public constructor <init>(Landroid/content/Context;Landroid/view/View;" in live_delegate
assert ".method public final F2(Landroid/view/ViewGroup;)V" in vod_owner
assert "const v2, 0x7f0e036f" in vod_owner
assert "new-instance v0, Lrrt;" in vod_owner
assert "SeekableOverlayPresenter" in vod_owner and "VodModel" in vod_owner
assert ".method public constructor <init>(Landroid/content/Context;Landroid/view/View;" in vod_delegate

# The full classes.dex is not touched; integration is resource-driven.
assert not list((ROOT / "patches/sleeptimer").glob("*.smali.patch"))

print("PASS Twitch 31 sleep-timer static integration")
