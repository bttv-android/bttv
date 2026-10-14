package bttv.highlight;

import android.util.Log;

import bttv.Res;
import bttv.settings.Settings;

public class Blacklist extends StringSetUI {
    private static final String TAG = "LBTTVBlacklist";
    private static Blacklist INSTANCE;

    Blacklist() {
        super(Settings.BlacklistedKeyWords, Res.layouts.bttv_blacklist_dialog);
    }

    public static Blacklist getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Blacklist();
        }
        return INSTANCE;
    }
    public static boolean shouldBlock(String word) {
        getInstance().loadSet();
        return INSTANCE.stringSet.contains(word.toLowerCase());
    }

    // Match original text tokens before third-party emote substitution.
    public static boolean shouldBlockText(String text) {
        if (text == null) return false;
        for (String word : text.split(" ")) {
            if (shouldBlock(word)) return true;
        }
        return false;
    }
}
