package bttv.highlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import bttv.ChommentModelDelegateWrapper;
import bttv.Data;
import bttv.Res;
import bttv.ResUtil;
import bttv.settings.Settings;
import bttv.settings.UserPreferences;
import tv.twitch.android.shared.chat.ChatMessageDelegate;
import tv.twitch.android.provider.chat.ChatMessageInterface;

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
        return stringSet.contains(word.toLowerCase());
    }

    public static boolean shouldHighlightChannel(String name) {
        getInstance().loadSet();
        return stringSet.contains("<" + name.toLowerCase() + ">");
    }

}
