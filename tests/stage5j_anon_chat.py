"""Static integration checks for Twitch 31 anonymous chat."""

from pathlib import Path
import sys


ROOT = Path(__file__).resolve().parents[1]
DISASS = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / ".bttv-cache" / "disass"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


helper = read(ROOT / "mod/app/src/main/java/bttv/AnonChat.java")
api = read(ROOT / "mod/app/src/main/java/bttv/api/AnonChat.java")
settings = read(ROOT / "mod/app/src/main/java/bttv/settings/Settings.java")
settings_activity = read(ROOT / "mod/app/src/main/java/bttv/settings/SettingsActivity.java")
qo5_patch = read(ROOT / "patches/live/qo5.smali.patch")
e66_patch = read(ROOT / "patches/live/e66.smali.patch")
fxc = read(DISASS / "smali/fxc.smali")
wp5 = read(DISASS / "smali_classes2/wp5.smali")
qo5 = read(DISASS / "smali_classes2/qo5.smali")
e66 = read(DISASS / "smali_classes2/e66.smali")

# Existing setting contract remains unchanged and defaults OFF.
anon_setting = settings[settings.index("AnonChatEnabled("):]
anon_setting = anon_setting[:anon_setting.index("OpenHighlightedWordsButton(")]
assert '"enable_anon_chat"' in anon_setting
assert "new Entry.BoolValue(false)" in anon_setting

# Twitch 31's own anonymous identity is user ID 0 with justinfan credentials.
anonymous_factory = fxc[fxc.index(".method public static l()Li5o;"):]
anonymous_factory = anonymous_factory[:anonymous_factory.index(".end method")]
assert 'const-string v2, "0"' in anonymous_factory
assert 'const-string v1, "justinfan"' in anonymous_factory
assert "Li5o;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V" in anonymous_factory
assert "invoke-static {}, Lfxc;->l()Li5o;" in wp5
assert "Lwp5;->B:Li5o;" in wp5

# The native owner captures the authenticated identity, attaches weakly, switches
# through wp5.m(), and delegates reconnect scheduling to qo5.k().
assert "+.implements Lbttv/AnonChat$Controller;" in qo5_patch
assert "+.field public bttvOriginalUser:Li5o;" in qo5_patch
assert "+.field public bttvAnonymous:Z" in qo5_patch
assert "+.field public bttvLogin:Lws40;" in qo5_patch
assert "+    iput-object p1, p0, Lqo5;->bttvLogin:Lws40;" in qo5_patch
assert "+    iput-object v4, p0, Lqo5;->bttvOriginalUser:Li5o;" in qo5_patch
assert "+    invoke-static {p0}, Lbttv/AnonChat;->attach(Lbttv/AnonChat$Controller;)Z" in qo5_patch
assert "+.method public final setAnonymous(Z)V" in qo5_patch
assert qo5_patch.count("+    invoke-static {}, Lfxc;->l()Li5o;") == 2
assert "+    invoke-virtual {v0, v1}, Lwp5;->m(Li5o;)V" in qo5_patch
assert "+    invoke-virtual {p0, v2, v3, v0}, Lqo5;->k(" in qo5_patch
assert "+    if-eq v0, p1, :bttv_anon_done" in qo5_patch
assert "+    invoke-virtual {v4}, Lws40;->n()Z" in qo5_patch
assert "+    invoke-virtual {v4}, Lws40;->k()Ltv/twitch/android/models/UserModel;" in qo5_patch
assert "+    invoke-virtual {v4}, Lws40;->a()Ljava/lang/String;" in qo5_patch
assert qo5_patch.count("+    iput-object v1, p0, Lqo5;->bttvOriginalUser:Li5o;") == 1

# Original Twitch connect/navigation methods are still the underlying mechanism.
assert ".method public final a(Ltv/twitch/android/models/Tuid;)V" in qo5
assert ".method public final d(Ltv/twitch/android/models/channel/ChannelInfo;)V" in qo5
assert ".method public final j(Ltv/twitch/android/models/channel/ChannelInfo;Ltv/twitch/android/models/streams/StreamType;)V" in qo5
assert ".method public final k(Ltv/twitch/android/models/channel/ChannelInfo;Ltv/twitch/android/models/streams/StreamType;Z)V" in qo5
assert ".method public final m(Li5o;)V" in wp5

# Composer gate is conjunctive, so Twitch's own hidden states cannot be overridden.
assert "+    invoke-static {v0}, Lbttv/AnonChat;->allowComposer(Z)Z" in e66_patch
assert "iget-boolean v0, p1, Ld66;->a:Z" in e66
assert "return twitchVisible && !isAnonChatEnabled();" in helper

# The active settings surface persists first, then asks the current weak owner to switch.
assert "entry.set(SettingsActivity.this, new UserPreferences.Entry.BoolValue(checked));" in settings_activity
assert "entry == Settings.AnonChatEnabled.entry" in settings_activity
assert "AnonChat.reconnect(checked);" in settings_activity
assert "WeakReference<Controller>" in helper
assert "value.setAnonymous(asAnon);" in helper
assert "import tv.twitch.android.shared.chat.ChatViewPresenter" not in helper
assert "import tv.twitch.android.shared.chat.ChatViewPresenter" not in api
assert "setChatViewPresenter(ChatViewPresenter" not in helper
assert "setChatViewPresenter(ChatViewPresenter" not in api

# No patch is added to the already-full primary DEX.
for patch in (ROOT / "patches").rglob("*.smali.patch"):
    if "AnonChat" in read(patch):
        assert "a/smali/" not in read(patch), f"anonymous hook touched classes.dex: {patch}"

print("PASS Twitch 31 anonymous identity/controller/reconnect/composer integration")
