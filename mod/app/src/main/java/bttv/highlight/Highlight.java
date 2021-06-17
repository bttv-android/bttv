package bttv.highlight;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import bttv.Res;
import bttv.Util;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.android.shared.chat.ChatMessageInterface;

public class Highlight {
    private static final String TAG = "LBTTVHighlight";
    private static Set<String> highlightSet = null;

    public static Integer replaceNum(ChatMessageInterface chatMessageInterface, Integer num) {
        if (!(chatMessageInterface instanceof ChatMessageDelegate)) {
            Log.i(TAG, "replaceNum: " + chatMessageInterface + " is not a ChatMessageDelegate");
            return num;
        }
        ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
        if (delegate.mChatMessage.messageType.equals("bttv-highlighted-message")) {
            num = Util.getResourceId(Res.colors.bttv_sonic);
        }
        return num;
    }

    private static void loadSet() {
        if (highlightSet != null) {
            return;
        }
        // TODO load from shared prefs
        highlightSet = new HashSet<>();
        highlightSet.add("hi");
    }

    public static boolean shouldHighlight(String word) {
        loadSet();
        return highlightSet.contains(word);
    }

    public static void openDialog() {
        // TODO
        Log.i("LBTTVDEBUG", "openDialog()");

    }
}
