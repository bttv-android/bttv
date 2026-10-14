"""Generate JVM fixtures; compile with production Blacklist.java, not Android.

Token/filter integration is checked separately by stage5l_blacklist.py against
the actual smali and Twitch's Rx filter contract.
"""
from pathlib import Path
import sys

SOURCES = {
    "android/util/Log.java": "package android.util; public final class Log {}",
    "bttv/Res.java": "package bttv; public final class Res { public enum layouts {bttv_blacklist_dialog} }",
    "bttv/settings/Settings.java": "package bttv.settings; public enum Settings {BlacklistedKeyWords}",
    "bttv/highlight/StringSetUI.java": """package bttv.highlight;
import java.util.*;
import bttv.Res;
import bttv.settings.Settings;
public abstract class StringSetUI {
    static final Set<String> SAVED = new HashSet<>();
    Set<String> stringSet;
    StringSetUI(Settings key, Res.layouts layout) {
        if (key != Settings.BlacklistedKeyWords) throw new AssertionError("wrong preference");
    }
    void loadSet() { stringSet = SAVED; }
}
""",
    "bttv/highlight/BlacklistHostTest.java": """package bttv.highlight;
import java.util.*;
public final class BlacklistHostTest {
    static int checks;
    static void check(boolean ok, String name) {
        checks++;
        if (!ok) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        check(!Blacklist.shouldBlockText("trump monkaS"), "empty default");
        StringSetUI.SAVED.add("trump");
        check(Blacklist.shouldBlockText("Fuck trump this cunt"), "ordinary match");
        check(Blacklist.shouldBlockText("TRUMP"), "capitalization");
        check(Blacklist.shouldBlockText("a  Trump  b"), "space split");
        check(!Blacklist.shouldBlockText("trumpet"), "no substring");
        check(!Blacklist.shouldBlockText("trump!"), "no punctuation stripping");
        check(!Blacklist.shouldBlockText("trump\\nother"), "no newline split");
        check(!Blacklist.shouldBlockText("trump\\tother"), "no tab split");
        check(!Blacklist.shouldBlockText("normal message"), "nonmatch");
        check(!Blacklist.shouldBlockText(null), "null text");
        check(!Blacklist.shouldBlockText(""), "empty text");
        StringSetUI.SAVED.add("monkas");
        check(Blacklist.shouldBlockText("monkaS"), "code before third-party substitution");
        StringSetUI.SAVED.add("two words");
        check(!Blacklist.shouldBlockText("two words"), "no phrase rule");
        StringSetUI.SAVED.add("<user>");
        check(!Blacklist.shouldBlockText("user"), "no username rule");
        check(Blacklist.shouldBlockText("<user>"), "literal word only");
        Set<String> before = new HashSet<>(StringSetUI.SAVED);
        Blacklist.shouldBlockText("trump");
        check(before.equals(StringSetUI.SAVED), "preferences not mutated");
        StringSetUI.SAVED.clear();
        check(!Blacklist.shouldBlockText("trump"), "subsequent message uses changed preference");
        System.out.println("PASS Blacklist JVM: " + checks + " checks");
    }
}
""",
}

if __name__ == "__main__":
    output = Path(sys.argv[1]).resolve()
    for name, source in SOURCES.items():
        target = output / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source + "\n", encoding="utf-8")
    print(f"Generated {len(SOURCES)} Blacklist JVM fixtures in {output}")
