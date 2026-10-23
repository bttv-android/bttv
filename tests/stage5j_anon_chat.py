"""Native-source integration checks for restart-required Twitch 31 Anonymous Chat."""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
DISASS = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / ".bttv-cache/disass"
def read(p): return p.read_text(encoding="utf-8")
def method(p, sig):
    s = read(DISASS / p)
    start = next(s.index(l) for l in s.splitlines() if l.startswith(".method") and sig in l)
    return s[start:s.index(".end method", start) + len(".end method")]

helper = read(ROOT / "mod/app/src/main/java/bttv/AnonChat.java")
qo = read(ROOT / "patches/live/qo5.smali.patch")
wp = read(ROOT / "patches/live/wp5.smali.patch")
startup = read(ROOT / "patches/live/ilg.smali.patch")
e66 = read(ROOT / "patches/live/e66.smali.patch")
settings = read(ROOT / "mod/app/src/main/java/bttv/settings/Settings.java")
activity = read(ROOT / "mod/app/src/main/java/bttv/settings/SettingsActivity.java")
quick = read(ROOT / "mod/app/src/main/java/bttv/settings/StreamQuickSettings.java")

assert '"enable_anon_chat"' in helper and '"enable_anon_chat"' in settings
assert 'if (initialized) return;' in helper
assert 'preferences.getBoolean(KEY, false)' in helper
assert 'return !initialized || appliedGuest;' in helper
assert 'return twitchVisible && !isAppliedAnonymous();' in helper
assert 'WeakReference' not in helper and '.setAnonymous(' not in helper
assert 'Restart pending' in helper and 'Connection identity not verified' in helper
assert 'Toast.makeText' in helper and 'AnonChat.status()' in activity
assert 'entry.set(SettingsActivity.this' in activity and 'AnonChat.reconnect(checked)' in activity
assert 'setting.entry.set(' in quick and 'AnonChat.reconnect(checked)' in quick
assert 'setAnonymous' not in qo and 'fxc' not in qo and 'Lbttv/AnonChat' not in qo
assert 'LiveEmoteChannels;->ensureChannel' in qo
# The surviving Stage5C body is byte-for-byte unchanged, only old Anon hunks are removed.
original = subprocess.check_output(["git", "show", "HEAD:patches/live/qo5.smali.patch"], cwd=ROOT).decode()
assert original[original.index("+    :try_start_bttv_channel"):] == qo[qo.index("+    :try_start_bttv_channel"):]
assert 'LiveEmoteAutocomplete;->attach' in e66 and 'AnonChat;->allowComposer(Z)Z' in e66
assert wp.count('AnonChat;->isAppliedAnonymous()Z') == 2
assert 'return-object v0' in wp and 'Ltf50;->a:Ltf50;' in wp
assert 'return-void' in wp and '.locals 5' in wp and '.locals 30' in wp
assert 'Lwp5;->m(' not in wp and 'Lqo5;->k(' not in wp
assert '+    invoke-static {v0}, Lbttv/AnonChat;->initialize(Landroid/content/Context;)V' in startup

factory = method("smali/ilg.smali", " j()Lhgc;")
assert factory.index('check-cast v0, Ltv/twitch/android/app/consumer/TwitchApplication;') < factory.index('new-instance v0, Lhgc;')
component = method("smali/wc1.smali", " generatedComponent()")
assert 'Lilg;->j()Lhgc;' in component and 'Lwc1;->a:Lhgc;' in component
constructor = method("smali_classes2/wp5.smali", " <init>(")
assert constructor.index('Lfxc;->l()Li5o;') < constructor.index('Lwp5;->B:Li5o;') < constructor.index('Lm8m;->h:Lh8m;')
guest = method("smali/fxc.smali", " l()Li5o;")
assert 'const-string v2, "0"' in guest and 'const/4 v3, 0x0' in guest
select = method("smali_classes2/wp5.smali", " m(Li5o;)V")
assert 'Lm8m;->h:Lh8m;' in select and 'Lwp5;->B:Li5o;' in select
handshake = method("smali_classes2/k8m.smali", " emit(")
assert 'Lm8m;->h:Lh8m;' in handshake and '"listen"' in handshake and '"NICK "' in handshake
join = method("smali_classes2/wp5.smali", " e(Lgp5;)Lep5;")
assert 'Lw7;->d:Ljava/lang/Object;' in join and 'Li5o;->c:' in join and 'Lm8m;->b(' in join
register = method("smali_classes2/zp5.smali", " a(Ljava/lang/String;Ljava/lang/String;")
assert register.index('Lwp5;->C:') < register.index('if-nez v0, :cond_a') < register.index('Lwp5;->B:Li5o;')
send = method("smali_classes2/wp5.smali", " k(Lgp5;")
assert '.locals 30' in send and 'Laqx;->v(' in send and 'Ltf50;->a:Ltf50;' in send
logout = method("smali_classes2/m8o.smali", " e(Z)V")
assert 'Lwp5;->m(Li5o;)V' in logout
print("PASS native cold-start barrier, fixed guest credentials, retained room compatibility, account-update guard, send/composer gates, pending-only settings; no device anonymity claim")
