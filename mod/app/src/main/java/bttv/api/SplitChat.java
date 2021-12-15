package bttv.api;

import android.util.Log;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import tv.twitch.android.core.adapters.RecyclerAdapterItem;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

    public static void setBackgroundColor(int position, ViewHolder viewHolder) {
        try {
            bttv.SplitChat.setBackgroundColor(position, viewHolder);
        } catch (Throwable t) {
            Log.e(TAG, "setBackgroundColor: ", t);
        }
    }

    /** Called in ChannelChatAdapter.addItems() */
    public static void removedNMessages(int n) {
        try {
            bttv.SplitChat.removedNMessages(n);
        } catch (Throwable t) {
            Log.e(TAG, "removedNMessages: ", t);
        }
    }

}
