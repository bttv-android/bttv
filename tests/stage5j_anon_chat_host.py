"""Generate isolated JVM fixtures for the production AnonChat helper.

Usage: python tests/stage5j_anon_chat_host.py <ignored-output-directory>
Compile the generated stubs and runner with AnonChat.java.
"""

from pathlib import Path
import sys


SOURCES = {
    "bttv/settings/Settings.java": """package bttv.settings;
public enum Settings { AnonChatEnabled }
""",
    "bttv/ResUtil.java": """package bttv;
public final class ResUtil {
    public static boolean enabled;
    public static boolean getBooleanFromSettings(bttv.settings.Settings setting) { return enabled; }
}
""",
    "bttv/AnonChatHostTest.java": """package bttv;
public final class AnonChatHostTest {
    private static final class FakeController implements AnonChat.Controller {
        boolean anonymous;
        int transitions;
        @Override public void setAnonymous(boolean value) {
            if (anonymous == value) return;
            anonymous = value;
            transitions++;
        }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        ResUtil.enabled = false;
        AnonChat.reconnect(false); // no active owner must fail open
        check(AnonChat.allowComposer(true), "OFF hid a normally visible composer");
        check(!AnonChat.allowComposer(false), "OFF overrode Twitch's hidden composer state");
        check(AnonChat.getViewerId(1234) == 1234, "OFF replaced authenticated viewer ID");

        FakeController first = new FakeController();
        check(!AnonChat.attach(first), "OFF requested anonymous initial identity");
        ResUtil.enabled = true;
        AnonChat.reconnect(true);
        check(first.anonymous && first.transitions == 1, "ON did not switch exactly once");
        AnonChat.reconnect(true);
        check(first.transitions == 1, "repeated ON caused a reconnect loop");
        check(AnonChat.getViewerId(1234) == AnonChat.ANON_VIEWER_ID, "ON did not substitute ID 0");
        check(!AnonChat.allowComposer(true), "ON left composer visible");

        FakeController replacement = new FakeController();
        check(AnonChat.attach(replacement), "new owner did not retain anonymous mode");
        AnonChat.reconnect(true);
        check(replacement.transitions == 1, "replacement owner did not enter anonymous mode");
        check(first.transitions == 1, "stale owner was called after replacement");

        ResUtil.enabled = false;
        AnonChat.reconnect(false);
        check(!replacement.anonymous && replacement.transitions == 2, "OFF did not restore auth mode");
        AnonChat.reconnect(false);
        check(replacement.transitions == 2, "repeated OFF caused a reconnect loop");
        check(AnonChat.getViewerId(5678) == 5678, "OFF restored a stale viewer ID");
        check(AnonChat.allowComposer(true), "OFF did not restore normal composer visibility");
        check(!AnonChat.allowComposer(false), "OFF overrode a Twitch restriction");
        System.out.println("PASS AnonChat OFF/ON/idempotence/replacement/restoration/composer behavior");
    }
}
""",
}


if __name__ == "__main__":
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding="utf-8")
    print(f"Generated {len(SOURCES)} isolated AnonChat JVM fixtures in {output}")
