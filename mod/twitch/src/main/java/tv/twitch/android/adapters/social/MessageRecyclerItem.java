package tv.twitch.android.adapters.social;

import android.widget.TextView;

public class MessageRecyclerItem implements tv.twitch.android.core.adapters.RecyclerAdapterItem {
    public static class ChatMessageViewHolder {
        public final TextView getMessageTextView() {
            throw new IllegalStateException("MessageRecyclerItem.ChatMessageViewHolder.getMessageTextView mock was called");
        }
    }
}
