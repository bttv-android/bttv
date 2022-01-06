package bttv;

import android.util.Log;

import tv.twitch.android.shared.chat.ChatViewPresenter;

public class AnonChat {
    private static final String TAG = "LBTTVAnonChat";
    private static ChatViewPresenter chatViewPresenter;

    public static void setChatViewPresenter(ChatViewPresenter chatViewPr) {
        chatViewPresenter = chatViewPr;
    }

    public static int reconnectWithViewerID(int newViewerID) {
        Log.d(TAG, "reconnectWithViewerID: " + newViewerID);
        if (chatViewPresenter == null) {
            Log.w(TAG, "reconnectWithViewerID: called but chatViewPresenter null");
            return -1;
        }
        if (chatViewPresenter.chatConnectionController == null) {
            Log.w(TAG, "reconnectWithViewerID: called but chatViewPresenter.chatConnectionController null");
            return -1;
        }
        if (chatViewPresenter.channel == null) {
            Log.w(TAG, "reconnectWithViewerID: called but chatViewPresenter.channel null");
            return -1;
        }
        chatViewPresenter.chatConnectionController.BTTVdisconnect(chatViewPresenter.channel.getId());
        int oldViewerId = chatViewPresenter.chatConnectionController.viewerId;
        Log.d(TAG, "reconnectWithViewerID: oldViewerId: " + oldViewerId);
        chatViewPresenter.chatConnectionController.viewerId = newViewerID;
        chatViewPresenter.BTTVconnect();
        return oldViewerId;
    }
}
