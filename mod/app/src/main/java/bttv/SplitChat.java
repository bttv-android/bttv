package bttv;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import bttv.settings.Settings;
import tv.twitch.android.adapters.social.MessageRecyclerItem;
import tv.twitch.android.adapters.social.MessageRecyclerItem.ChatMessageViewHolder;
import tv.twitch.android.core.adapters.RecyclerAdapterItem;
import tv.twitch.android.shared.chat.adapter.ChannelChatAdapter;

public class SplitChat {
    private final static String TAG = "LBTTVSplitChat";

    public static void setBackgroundColor(MessageRecyclerItem msgItem, ChatMessageViewHolder viewHolder) {
        if (!ResUtil.getBooleanFromSettings(Settings.SplitChatEnabled)) {
            return;
        }
        TextView textView = viewHolder.getMessageTextView();
        int color = ResUtil.getResourceId("twitch_purple_13", "color");
        Log.i(TAG, "setBackgroundColor: " + msgItem.index);

        if (msgItem.index % 2 == 1) {
            textView.setBackgroundColor(color);

            // fix width
            ViewGroup.LayoutParams layoutParams = textView.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            textView.setLayoutParams(layoutParams);
        }
    }

    public static void addIndices(ChannelChatAdapter adapter, List<RecyclerAdapterItem> list) {
        if (!ResUtil.getBooleanFromSettings(Settings.SplitChatEnabled)) {
            return;
        }
        int initialSize = adapter.getItems().size();
        int i = 0;
        for (RecyclerAdapterItem item : list) {
            if (!(item instanceof MessageRecyclerItem)) {
                Log.w(TAG, "addIndices: item is not a MessageRecyclerItem", new Exception());
                continue;
            }
            MessageRecyclerItem msgItem = (MessageRecyclerItem) item;
            msgItem.index = initialSize + i;
        }
    }
}
