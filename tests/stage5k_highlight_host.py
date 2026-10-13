"""Generate isolated JVM fixtures for Highlight keyword matching.

Usage: python tests/stage5k_highlight_host.py <ignored-output-directory>
Compile the generated stubs and runner with the production Highlight.java.
"""

from pathlib import Path
import sys


SOURCES = {
    "android/util/Log.java": """package android.util;
public final class Log {
    public static int w(String tag, String message, Throwable error) { return 0; }
    public static int i(String tag, String message) { return 0; }
}
""",
    "bttv/ChommentModelDelegateWrapper.java": """package bttv;
public class ChommentModelDelegateWrapper {
    public boolean BTTVshouldHighlight() { return false; }
}
""",
    "bttv/Res.java": """package bttv;
public final class Res {
    public enum layouts { bttv_highlight_dialog }
    public enum colors { bttv_sonic }
}
""",
    "bttv/ResUtil.java": """package bttv;
public final class ResUtil {
    public static int getResourceId(Res.colors color) { return 3131; }
    public static int getColorValue(String color) { return 0xff0014a5; }
}
""",
    "bttv/settings/Settings.java": """package bttv.settings;
public enum Settings { HighlightedKeyWords }
""",
    "tv/twitch/android/shared/chat/pub/messages/ui/ChatMessageInterface.java": """package tv.twitch.android.shared.chat.pub.messages.ui;
public interface ChatMessageInterface {}
""",
    "tv/twitch/android/shared/chat/ChatMessageDelegate.java": """package tv.twitch.android.shared.chat;
public class ChatMessageDelegate implements tv.twitch.android.shared.chat.pub.messages.ui.ChatMessageInterface {
    public static final class Message { public String messageType = ""; }
    public Message chatMessage = new Message();
    public String getUserName() { return null; }
    public String getDisplayName() { return null; }
}
""",
    "bttv/highlight/StringSetUI.java": """package bttv.highlight;
import java.util.HashSet;
import java.util.Set;
import bttv.Res;
import bttv.settings.Settings;
public abstract class StringSetUI {
    static final Set<String> TEST_VALUES = new HashSet<>();
    Set<String> stringSet;
    StringSetUI(Settings setting, Res.layouts layout) {}
    void loadSet() { stringSet = TEST_VALUES; }
}
""",
    "bttv/highlight/HighlightHostTest.java": """package bttv.highlight;
public final class HighlightHostTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        StringSetUI.TEST_VALUES.clear();
        StringSetUI.TEST_VALUES.add("hello");
        StringSetUI.TEST_VALUES.add("mixedcase");
        StringSetUI.TEST_VALUES.add("shit");
        StringSetUI.TEST_VALUES.add("trump");
        StringSetUI.TEST_VALUES.add("<streamer>");

        check(Highlight.shouldHighlightText("hello chat"), "leading keyword did not match");
        check(Highlight.shouldHighlightText("chat MIXEDCASE now"), "case-insensitive keyword did not match");
        check(Highlight.shouldHighlightText("this is shit chat"), "configured keyword shit did not match");
        check(Highlight.shouldHighlightText("Fuck trump this cunt"), "configured keyword trump did not match");
        check(!Highlight.shouldHighlightText("hello! chat"), "punctuation changed exact matching");
        check(!Highlight.shouldHighlightText("streamer"), "channel entry matched ordinary text");
        check(!Highlight.shouldHighlightText(null), "null text matched");
        check(Highlight.shouldHighlightChannel("Streamer"), "case-insensitive channel did not match");
        check(!Highlight.shouldHighlightChannel("someoneelse"), "unconfigured channel matched");
        check(Highlight.highlightedColorResource() == 3131, "wrong highlighted-message color resource");
        System.out.println("PASS Highlight keyword/channel/color behavior");
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
    print(f"Generated {len(SOURCES)} isolated Highlight JVM fixtures in {output}")
