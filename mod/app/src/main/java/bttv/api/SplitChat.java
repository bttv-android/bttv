package bttv.api;

import android.util.Log;

import tv.twitch.android.adapters.social.MessageRecyclerItem.ChatMessageViewHolder;
import tv.twitch.android.core.adapters.RecyclerAdapterItem;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

    public static void setBackgroundColor(int position, RecyclerAdapterItem viewHolder) {
        try {
            bttv.SplitChat.setBackgroundColor(position, (ChatMessageViewHolder) viewHolder);
        } catch (Throwable t) {
            Log.e(TAG, "setBackgroundColor: ", t);
        }
    }

}
