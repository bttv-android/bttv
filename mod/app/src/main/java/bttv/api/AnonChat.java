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
}
