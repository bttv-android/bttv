package bttv.api;

import android.util.Log;
import android.view.View;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

    public static void setBackgroundColor(int position, View view) {
        try {
            bttv.SplitChat.setBackgroundColor(position, view);
        } catch (Throwable t) {
            Log.e(TAG, "setBackgroundColor: ", t);
        }
    }

    /** Called in the Twitch 31 live-chat adapter when it trims rows from the head. */
    public static void removedNMessages(int n) {
        try {
            bttv.SplitChat.removedNMessages(n);
        } catch (Throwable t) {
            Log.e(TAG, "removedNMessages: ", t);
        }
    }

}
