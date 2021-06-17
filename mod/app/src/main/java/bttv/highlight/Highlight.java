package bttv.highlight;

import android.util.Log;

import bttv.Res;
import bttv.Util;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.android.shared.chat.ChatMessageInterface;

public class Highlight {
    private static final String TAG = "LBTTVHighlight";

    public static Integer replaceNum(ChatMessageInterface chatMessageInterface, Integer num) {
        if (!(chatMessageInterface instanceof ChatMessageDelegate)) {
            Log.i(TAG, "replaceNum: " + chatMessageInterface + " is not a ChatMessageDelegate");
            return num;
        }
        ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
        if (delegate.mChatMessage.messageType.equals("highlighted-message")) {
            num = Util.getResourceId(Res.colors.bttv_sonic);
        }
        return num;
    }

}
