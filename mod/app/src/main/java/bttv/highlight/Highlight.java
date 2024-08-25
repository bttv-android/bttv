package bttv.highlight;

import android.util.Log;

import bttv.ChommentModelDelegateWrapper;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.android.shared.chat.pub.messages.ui.ChatMessageInterface;

public class Highlight extends StringSetUI {
    private static Highlight INSTANCE;

    private static final String TAG = "LBTTVHighlight";

    public Highlight() {
        super(Settings.HighlightedKeyWords, Res.layouts.bttv_highlight_dialog);
    }

    public static Highlight getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Highlight();
        }
        return INSTANCE;
    }

    public static Integer replaceNum(ChatMessageInterface chatMessageInterface, Integer num) {
        if (chatMessageInterface == null) {
            Log.w(TAG, "replaceNum: chatMessageInterface is null", new Exception());
            return num;
        }
        if (chatMessageInterface instanceof ChatMessageDelegate) {
            ChatMessageDelegate delegate = (ChatMessageDelegate) chatMessageInterface;
            return replaceNumLive(delegate, num);
        }
        if (chatMessageInterface instanceof ChommentModelDelegateWrapper) {
            ChommentModelDelegateWrapper delegate = (ChommentModelDelegateWrapper) chatMessageInterface;
            return replaceNumVOD(delegate, num);
        }

        Log.i(TAG, "replaceNum: " + chatMessageInterface + " is not a ChatMessageDelegate");
        return num;
    }

    private static Integer replaceNumLive(ChatMessageDelegate delegate, Integer num) {
        if (delegate.mChatMessage == null) {
            Log.w(TAG, "replaceNum: delegate.mChatMessage is null " + delegate, new Exception());
            return num;
        }
        if (delegate.mChatMessage.messageType == null) {
            Log.w(TAG, "replaceNum: delegate.mChatMessage.messageType is null " + delegate.mChatMessage, new Exception());
            return num;
        }

        if (delegate.mChatMessage.messageType.equals("bttv-highlighted-message")
                || (delegate.getUserName() != null && Highlight.shouldHighlightChannel(delegate.getUserName()))
                || (delegate.getDisplayName() != null && Highlight.shouldHighlightChannel(delegate.getDisplayName()))
        ) {
            num = ResUtil.getResourceId(Res.colors.bttv_sonic);
        }
        return num;
    }

    private static Integer replaceNumVOD(ChommentModelDelegateWrapper delegateWrapper, Integer num) {
        if (!delegateWrapper.BTTVshouldHighlight()) {
            return num;
        }
        return ResUtil.getResourceId(Res.colors.bttv_sonic);
    }

    public static boolean shouldHighlight(String word) {
        getInstance().loadSet();
        if (word.startsWith("<") || word.endsWith(">"))
            return false;
        return INSTANCE.stringSet.contains(word.toLowerCase());
    }

    public static boolean shouldHighlightChannel(String name) {
        getInstance().loadSet();
        return INSTANCE.stringSet.contains("<" + name.toLowerCase() + ">");
    }

}
