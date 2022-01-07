package bttv.api;

import android.util.Log;

import tv.twitch.android.shared.chat.ChatViewPresenter;

public class AnonChat {
    private static final String TAG = "LBTTVAnonChat";

    // called in ChatViewPresenter.<init>
    public static void setChatViewPresenter(ChatViewPresenter chatViewPresenter) {
        try {
            Log.d(TAG, "setChatViewPresenter: " + chatViewPresenter.toString());
            bttv.AnonChat.setChatViewPresenter(chatViewPresenter);
        } catch (Throwable t) {
            Log.e(TAG, "setChatViewPresenter: ", t);
        }
    }

    // called in ChatConnectionController.connect()
    public static int getViewerId(int viewerId) {
        try {
            Log.d(TAG, "getViewerId: " + viewerId);
            int id = bttv.AnonChat.getViewerId(viewerId);
            bttv.AnonChat.isAnonRightNow = (id == bttv.AnonChat.ANON_VIEWER_ID);
            return id;
        } catch (Throwable t) {
            Log.e(TAG, "getViewerId: ", t);
            return viewerId;
        }
    }

    // called in ChatMessageInputPresenter.onStateChange()
    public static boolean isAnonChatOr(boolean or) {
        return or || bttv.AnonChat.isAnonRightNow;
    }
}
