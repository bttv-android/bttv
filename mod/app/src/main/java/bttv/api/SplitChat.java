package bttv.api;

import android.util.Log;

import java.util.List;

import tv.twitch.android.adapters.social.MessageRecyclerItem;
import tv.twitch.android.adapters.social.MessageRecyclerItem.ChatMessageViewHolder;
import tv.twitch.android.core.adapters.RecyclerAdapterItem;
import tv.twitch.android.shared.chat.adapter.ChannelChatAdapter;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

    public static void setBackgroundColor(MessageRecyclerItem msgItem, ChatMessageViewHolder viewHolder) {
        try {
            bttv.SplitChat.setBackgroundColor(msgItem, viewHolder);
        } catch (Throwable t) {
            Log.e(TAG, "setBackgroundColor: ", t);
        }
    }

    public static void addIndices(ChannelChatAdapter adapter, List<RecyclerAdapterItem> list) {
        try {
            bttv.SplitChat.addIndices(adapter, list);
        } catch (Throwable t) {
            Log.e(TAG, "addIndices: ", t);
        }
    }
}
