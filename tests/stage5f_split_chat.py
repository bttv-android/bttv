"""Generate an isolated JVM fixture for the Twitch 31 Split Chat core.

Usage: python tests/stage5f_split_chat.py <ignored-output-directory>
Compile the generated stubs and runner with the production bttv/SplitChat.java.
"""

from pathlib import Path
import sys


SOURCES = {
    "android/content/Context.java": """package android.content;
public class Context {
    private final android.content.res.Resources resources = new android.content.res.Resources();
    public android.content.res.Resources getResources() { return resources; }
}
""",
    "android/content/res/Configuration.java": """package android.content.res;
public class Configuration {
    public static final int UI_MODE_NIGHT_MASK = 0x30;
    public static final int UI_MODE_NIGHT_YES = 0x20;
    public static final int UI_MODE_NIGHT_NO = 0x10;
    public int uiMode = UI_MODE_NIGHT_YES;
}
""",
    "android/content/res/Resources.java": """package android.content.res;
public class Resources {
    private final Configuration configuration = new Configuration();
    public Configuration getConfiguration() { return configuration; }
}
""",
    "android/graphics/drawable/Drawable.java": """package android.graphics.drawable;
public class Drawable {}
""",
    "android/graphics/drawable/ColorDrawable.java": """package android.graphics.drawable;
public class ColorDrawable extends Drawable {
    public final int color;
    public ColorDrawable(int color) { this.color = color; }
}
""",
    "android/view/View.java": """package android.view;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.Map;
public class View {
    private final Context context;
    private final int id;
    private final Map<Integer, View> children = new HashMap<>();
    private Drawable background;
    public View(Context context, int id) { this.context = context; this.id = id; }
    public Context getContext() { return context; }
    public Drawable getBackground() { return background; }
    public void setBackground(Drawable value) { background = value; }
    public void setBackgroundColor(int color) { background = new ColorDrawable(color); }
    public View findViewById(int target) {
        if (id == target) return this;
        return children.get(target);
    }
    public void addChild(View child) { children.put(child.id, child); }
}
""",
    "bttv/settings/Settings.java": """package bttv.settings;
public enum Settings { SplitChatEnabled }
""",
    "bttv/ResUtil.java": """package bttv;
import android.content.Context;
import bttv.settings.Settings;
public class ResUtil {
    public static final int CHAT_ID = 100;
    public static boolean enabled;
    public static boolean getBooleanFromSettings(Settings setting) { return enabled; }
    public static int getResourceId(Context context, String name, String type) {
        return \"chat_message_item\".equals(name) && \"id\".equals(type) ? CHAT_ID : 0;
    }
    public static int getColorValue(String name) {
        return \"material_grey_900\".equals(name) ? 900 : 300;
    }
}
""",
    "bttv/SplitChatHostTest.java": """package bttv;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
public final class SplitChatHostTest {
    private static final Context CONTEXT = new Context();
    private static View eligible(Drawable original) {
        View root = new View(CONTEXT, 1);
        root.addChild(new View(CONTEXT, ResUtil.CHAT_ID));
        root.setBackground(original);
        return root;
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Drawable twitchBackground = new Drawable();
        View row = eligible(twitchBackground);
        ResUtil.enabled = true;
        SplitChat.setBackgroundColor(1, row);
        check(row.getBackground() instanceof ColorDrawable, \"odd row was not tinted\");
        check(((ColorDrawable) row.getBackground()).color == 900, \"wrong night tint\");
        SplitChat.setBackgroundColor(2, row);
        check(row.getBackground() == twitchBackground, \"even bind did not restore Twitch background\");

        SplitChat.setBackgroundColor(1, row);
        ResUtil.enabled = false;
        SplitChat.setBackgroundColor(1, row);
        check(row.getBackground() == twitchBackground, \"disabled bind retained BTTV tint\");

        Drawable unrelated = new Drawable();
        View untouched = eligible(unrelated);
        SplitChat.setBackgroundColor(1, untouched);
        check(untouched.getBackground() == unrelated, \"disabled bind wiped unrelated background\");

        ResUtil.enabled = true;
        View invalid = eligible(unrelated);
        SplitChat.setBackgroundColor(-1, invalid);
        check(invalid.getBackground() == unrelated, \"invalid position changed row\");

        View other = new View(CONTEXT, 2);
        other.setBackground(unrelated);
        SplitChat.setBackgroundColor(1, other);
        check(other.getBackground() == unrelated, \"non-chat root changed\");

        View beforeTrim = eligible(null);
        SplitChat.setBackgroundColor(0, beforeTrim);
        check(beforeTrim.getBackground() == null, \"position zero tinted before trim\");
        SplitChat.removedNMessages(1);
        View afterTrim = eligible(null);
        SplitChat.setBackgroundColor(0, afterTrim);
        check(afterTrim.getBackground() instanceof ColorDrawable, \"head trim did not preserve parity\");

        CONTEXT.getResources().getConfiguration().uiMode =
                android.content.res.Configuration.UI_MODE_NIGHT_NO;
        View day = eligible(null);
        SplitChat.setBackgroundColor(0, day);
        check(day.getBackground() instanceof ColorDrawable, \"day row was not tinted\");
        check(((ColorDrawable) day.getBackground()).color == 300, \"wrong day tint\");
        System.out.println(\"PASS Split Chat theme/tint/reset/eligibility/parity\");
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
    print(f"Generated {len(SOURCES)} isolated Split Chat fixtures in {output}")
