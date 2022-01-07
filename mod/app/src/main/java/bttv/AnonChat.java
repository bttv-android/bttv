package bttv;

import android.util.Log;

import bttv.settings.Settings;
import tv.twitch.android.shared.chat.ChatViewPresenter;

public class AnonChat {
    public static final int ANON_VIEWER_ID = 0; // 0 is used by the chat SDK to connect anonymously

    private static final String TAG = "LBTTVAnonChat";
    private static ChatViewPresenter chatViewPresenter;
    private static Integer originalViewerID = null;
    public static boolean isAnonRightNow = false;

    public static void setChatViewPresenter(ChatViewPresenter chatViewPr) {
        chatViewPresenter = chatViewPr;
    }

    public static void reconnect(boolean asAnon) {
        if (originalViewerID == null) {
            Log.w(TAG, "reconnect: originalViewerID == null, wont reconnect");
            return;
        }
        int viewerID = asAnon ? ANON_VIEWER_ID : originalViewerID;
        reconnectWithViewerID(viewerID);
        isAnonRightNow = asAnon;
    }

    private static void reconnectWithViewerID(int newViewerID) {
        Log.d(TAG, "reconnectWithViewerID: " + newViewerID);
        if (chatViewPresenter == null) {
            Log.w(TAG, "reconnectWithViewerID: called but chatViewPresenter null");
            return;
        }
        if (chatViewPresenter.chatConnectionController == null) {
            Log.w(TAG, "reconnectWithViewerID: called but chatViewPresenter.chatConnectionController null");
            return;
        }
        if (chatViewPresenter.channel == null) {
            Log.w(TAG, "reconnectWithViewerID: called but chatViewPresenter.channel null");
            return;
        }
        chatViewPresenter.chatConnectionController.BTTVdisconnect(chatViewPresenter.channel.getId());
        int oldViewerId = chatViewPresenter.chatConnectionController.viewerId;
        Log.d(TAG, "reconnectWithViewerID: oldViewerId: " + oldViewerId);
        chatViewPresenter.chatConnectionController.viewerId = newViewerID;
        chatViewPresenter.BTTVconnect();
    }

    public static int getViewerId(int viewerId) {
        // Store viewerID so we can use it when reconnecting
        if (originalViewerID == null && viewerId != ANON_VIEWER_ID) {
            originalViewerID = viewerId;
        } else {
            Log.i(TAG, "getViewerId: originalViewerID=" + originalViewerID  + " viewerId=" + viewerId);
        }

        return isAnonChatEnabled() ? ANON_VIEWER_ID : viewerId;
    }

    private static boolean isAnonChatEnabled() {
        return ResUtil.getBooleanFromSettings(Settings.AnonChatEnabled);
    }
}
